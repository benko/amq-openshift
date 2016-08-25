# amq-openshift

This project demonstrates how multiple A-MQ brokers can be set up in a mesh in
OpenShift without sharing a DeploymentConfig (that is, how an A-MQ mesh can
work without a ReplicationController, just several stand-alone independent
Pods).

For added value, it uses a RWO (ReadWriteOnly) PersistentVolumeClaim, which is
apparently the only write-access mode currently supported by EBS. In this mode,
PVs can not be shared by multiple brokers in *split* mode - each broker must
have its own PVC (and a corresponding PV on the cluster side).

## before you begin

1. Create a new OpenShift project or reuse an existing one (switch to it).


2. Make sure there are as many PersistentVolumes available as the number of brokers you intend to run.

## deploying

oc new-project <project-name>
oc create -f amq-service.json
oc create -f serviceaccount.json
oc create -f amq-claims.json

Edit and create deployment configurations for the two brokers:

 - option 1: use DNS for resolving service endpoints
    {
	"name": "AMQ_MESH_DISCOVERY_TYPE",
	"value": "dns"
    },
    {
	"name": "AMQ_MESH_SERVICE_NAME",
	"value": "amq-mesh.<project-name>.svc.cluster.local"
    },

    Replace "<project-name>" with the actual project name.

 - option 1: use DNS for resolving service endpoints
    {
	"name": "AMQ_MESH_DISCOVERY_TYPE",
	"value": "kube"
    },
    {
	"name": "AMQ_MESH_SERVICE_NAME",
	"value": "amq-mesh"
    },

    Add "view" privilege to the service account used to run the pods:

    $ oc adm policy add-role-to-user \
	    view system:serviceaccount:<project-name>:amq-service-account

Upon start-up, the last log messages displayed by either broker should be:

 INFO | Adding service: [tcp://10.x.y.z:61616, failed:false, connectionFailures:0]
 INFO | Establishing network connection from
	vm://amq-outgoing-x-yyyyy?async=false&network=true to tcp://10.x.y.z:61616
 INFO | Connector vm://amq-outgoing-x-yyyyy started
 INFO | Network connection between vm://amq-outgoing-x-yyyyy#0 and
	tcp:///10.x.y.z:61616@36988 (amq-incoming-x-yyyyy) has been established.

After that, simply create two new apps, the producer and the consumer, and wait
for them to be deployed to start seeing the messages being passed between the
two (the environment should already contain the service lookup variables):

    oc new-app --name=producer fis-karaf-openshift~http://<gitserver>/<project-url>/

    oc new-app --name=consumer fis-karaf-openshift~http://<gitserver>/<project-url>/

When these two are built and deployed, the following messages of level INFO
should appear at the start in the logs of the producer and consumer pods
(respectively):

TODO: WTF is with initializers?

The following messages of level INFO (among others) should also appear
periodically every 5 seconds, in the logs of the producer and consumer pods
(respectively):

2016-08-24 05:56:52,869
| INFO 
| #0 - timer://foo
| route1
| 69 - org.apache.camel.camel-core - 2.17.2
| Sending message Ohai! to jms:queue:testQueue

2016-08-24 05:56:52,879
| INFO
| sumer[testQueue]
| route1
| 69 - org.apache.camel.camel-core - 2.17.2
| Got message Ohai! from jms:queue:testQueue

