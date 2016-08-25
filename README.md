# amq-openshift

This project demonstrates how multiple A-MQ brokers can be set up in a mesh in
OpenShift without sharing a `DeploymentConfig` (that is, how an A-MQ mesh can
work without a `ReplicationController`, just several stand-alone independent
Pods).

For added value, it uses a RWO (`ReadWriteOnly`) `PersistentVolumeClaim`, which
is apparently the only write-access mode currently supported by EBS. This means
that only one pod will be able to acquire write access and all others trying to
access that particular PV will have to be happy just observing.

That is  also why a PVC can not be shared by multiple brokers in *split* mode
(that is, sharing one single PVC by creating multiple message stores inside
subdirectories) - each broker must have its own PVC (and a corresponding PV on
the cluster side).

## before you begin

* Log in to OpenShift (either online or on-premise).

```shell
$ oc login -u johndoe -p password https://your.on-premise.broker:8443/
$ oc login --token=xxxxxx https://api.preview.openshift.com/
```

* Create a new OpenShift project or reuse an existing one (switch to it).

```shell
$ oc new-project <new-project>
$ oc project <existing-project>
```

* Make sure there are as many `PersistentVolume`s available as the number of
  brokers you intend to run. (Unfortunately there is no way to obtain a list of
  PVs as a normal user - ask your cluster admin or look it up in
  documentation.)

## deploying

### create dependencies

```shell
$ oc create -f amq-claims.json
$ oc create -f amq-service.json
$ oc create -f amq-svc-account.json
```

### edit and create the two brokers

Edit the deployment configurations in `amq-incoming.json` and `amq-outgoing.json`.

* option 1: use DNS for resolving service endpoints
    (replace "`<project-name>`" with the actual project name)

```json
    {
	"name": "AMQ_MESH_DISCOVERY_TYPE",
	"value": "dns"
    },
    {
	"name": "AMQ_MESH_SERVICE_NAME",
	"value": "amq-mesh.<project-name>.svc.cluster.local"
    },
```

* option 2: use Kube API for resolving service endpoints

```json
    {
	"name": "AMQ_MESH_DISCOVERY_TYPE",
	"value": "kube"
    },
    {
	"name": "AMQ_MESH_SERVICE_NAME",
	"value": "amq-mesh"
    },
```

NOTE: You must add the "view" privilege to the service account used to run
  the pods if using the Kube API:

```shell
    $ oc adm policy add-role-to-user \
	    view system:serviceaccount:<project-name>:amq-service-account
```

### start the brokers

Having created the DCs, there are no triggers that would start a deployment, so
do it manually:

```shell
    $ oc deploy amq-incoming --latest
    $ oc deploy amq-outgoing --latest
```

Upon start-up, the last log messages displayed by either broker should be:

```
 INFO | Adding service: [tcp://10.x.y.z:61616, failed:false, connectionFailures:0]
 INFO | Establishing network connection from
	vm://amq-outgoing-x-yyyyy?async=false&network=true to tcp://10.x.y.z:61616
 INFO | Connector vm://amq-outgoing-x-yyyyy started
 INFO | Network connection between vm://amq-outgoing-x-yyyyy#0 and
	tcp:///10.x.y.z:61616@36988 (amq-incoming-x-yyyyy) has been established.
```

## deploying the clients

### create new apps

Once the brokers are running, simply create two new apps, the producer and the
consumer.

We will be using the `fis-karaf-openshift` image with those to initiate a S2I
build of the OSGi bundles that will eventually do the sending and receiving of
the JMS messages.

NOTE: Since these two sample bundles are just subdirectories of this Git
repository, you need to use the `--context-dir=` option to tell the S2I build
not to invoke Maven from the top level of the Git module.

```shell
$ oc new-app --name=producer --context-dir=producer \
	fis-karaf-openshift~http://github.com/benko/amq-openshift/
$ oc new-app --name=consumer --context-dir=consumer \
	fis-karaf-openshift~http://github.com/benko/amq-openshift/
```

Wait for the builds to finish and the pods to be deployed to start seeing the
messages being passed between the two every 5 seconds (the environment should
already contain the service lookup variables as required to use the services).

### monitor the output

When these two are built and deployed, the following messages of level INFO
should appear at the start in the logs of the producer and consumer pods
(respectively):

*TODO: WTF is up with initializers?*

The following messages of level INFO (among others) should also appear
periodically every 5 seconds, in the logs of the producer and consumer pods
(respectively):

```
yyyy-mm-dd hh:mm:ss,µµµ
| INFO 
| #0 - timer://foo
| route1
| 69 - org.apache.camel.camel-core - 2.17.2
| Sending message Ohai! to jms:queue:testQueue
yyyy-mm-dd hh:mm:ss,µµµ
| INFO
| sumer[testQueue]
| route1
| 69 - org.apache.camel.camel-core - 2.17.2
| Got message Ohai! from jms:queue:testQueue
```
