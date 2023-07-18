package net.geocat.routes.queuebased;

import net.geocat.service.PollingService;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.stereotype.Component;


@Component
public class PollingQueue extends SpringRouteBuilder {

    public static String myJMSQueueName = "Orchestrator.Polling";

    @Override
    public void configure() throws Exception {

        onException().process(new Processor() {
            public void process(Exchange exchange) throws Exception {
                System.out.println("handling ex");
            }
        }).log("Received body ").handled(true);

        from("timer://pollingTimer?fixedRate=true&period=30000&delay=3000")
                .bean(PollingService.class, "ping")
                .split()
                .simple("${body}")
                .marshal().json()
                .to("activemq:" + MainOrchestrator.myJMSQueueName)
        ;
    }
}
