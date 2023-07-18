package geocat.routes.queuebased;

import geocat.database.service.DatabaseUpdateService;
import geocat.database.service.HarvestJobService;
import geocat.eventprocessor.MainLoopRouteCreator;
import geocat.eventprocessor.RedirectEvent;
import geocat.events.HarvestAbortEvent;
import geocat.events.HarvestRequestedEvent;
import geocat.events.actualRecordCollection.ActualHarvestCompleted;
import geocat.events.actualRecordCollection.ActualHarvestStartCommand;
import geocat.events.determinework.DetermineWorkStartCommand;
import geocat.events.determinework.WorkedDeterminedFinished;
import org.apache.camel.BeanScope;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class MainOrchestrator extends SpringRouteBuilder {

    public static String myJMSQueueName = "harvest.MainOrchestrator";
    @Autowired
    MainLoopRouteCreator mainLoopRouteCreator;

    @Override
    public void configure() throws Exception {


        mainLoopRouteCreator.createEventProcessingLoop(this,
                "activemq:" + myJMSQueueName,
                new Class[]{HarvestRequestedEvent.class, WorkedDeterminedFinished.class, ActualHarvestCompleted.class, HarvestAbortEvent.class},
                Arrays.asList(
                        new RedirectEvent(DetermineWorkStartCommand.class, "activemq:" + DetermineWorkOrchestrator.myJMSQueueName),
                        new RedirectEvent(ActualHarvestStartCommand.class, "activemq:" + ActualRecordCollectionOrchestrator.myJMSQueueName)
                ),
                Arrays.asList(new Class[0]),
                2
        );


        from("activemq:ActiveMQ.DLQ")
                .routeId("MainOrchestrator.DLQ")
                .onException(Exception.class).onExceptionOccurred(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Exception ex = exchange.getException();
                        exchange.getMessage().setHeader("exception", ex);
                        exchange.getMessage().setHeader("exceptionTxt", DatabaseUpdateService.convertToString(ex));
                    }
                })
                .handled(true)
                .to("activemq:ActiveMQ.DLQ_DLQ")
                .end()

                .bean(HarvestJobService.class, "updateHarvestJobStateInDBToError( ${header.processID} )", BeanScope.Request)
        ;

    }
}
