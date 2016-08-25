package net.p0f.openshift.examples.consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class Consumer extends RouteBuilder implements InitializingBean, DisposableBean {
	private static final Logger log = LoggerFactory.getLogger(Consumer.class); 

	private JmsComponent jmsComponent;
	
	public void setJmsComponent(JmsComponent jmsc) {
		this.jmsComponent = jmsc;
	}

	@PostConstruct
	public void afterPropertiesSet() throws Exception {
		log.info("Starting up JMS Consumer with configuration:");
		log.info(" - Endpoint: " +
					this.jmsComponent.getEndpointClass().getCanonicalName());
		log.info(" - ConnectionFactory: " +
					this.jmsComponent.getConfiguration().getConnectionFactory().toString());
	}

	@PreDestroy
	public void destroy() throws Exception {
		log.info("Shutting down JMS Consumer.");
	}

    public void configure() throws Exception {
	from("jms:queue:testQueue")
	    .log("Got message \"${body}\" from jms:queue:testQueue");
    }
}
