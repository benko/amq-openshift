package net.p0f.openshift.examples.producer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class Producer extends RouteBuilder implements InitializingBean, DisposableBean {
	private static final Logger log = LoggerFactory.getLogger(Producer.class); 

	private JmsComponent jmsComponent;
	
	public void setJmsComponent(JmsComponent jmsc) {
		this.jmsComponent = jmsc;
	}

	@PostConstruct
	public void afterPropertiesSet() {
		log.info("Starting up JMS Producer with configuration:");
		log.info(" - Endpoint: " +
					this.jmsComponent.getEndpointClass().getCanonicalName());
		log.info(" - ConnectionFactory: " +
					this.jmsComponent.getConfiguration().getConnectionFactory().toString());
	}

	@PreDestroy
	public void destroy() {
		log.info("Shutting down JMS Producer.");
	}

    public void configure() throws Exception {
	from("timer:foo?period=5s")
	    .setBody(simple("Ohai!"))
	    .log("Sending message \"${body}\" to jms:queue:testQueue")
	    .to("jms:queue:testQueue");
    }
}
