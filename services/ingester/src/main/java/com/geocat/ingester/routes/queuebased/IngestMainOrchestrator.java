package com.geocat.ingester.routes.queuebased;


import com.geocat.ingester.eventprocessor.MainLoopRouteCreator;
import com.geocat.ingester.eventprocessor.RedirectEvent;
import com.geocat.ingester.events.IngestRequestedEvent;
import com.geocat.ingester.events.ingest.AbortCommand;
import com.geocat.ingester.events.ingest.ActualIngestCompleted;
import com.geocat.ingester.events.ingest.ActualIngestStartCommand;
import com.geocat.ingester.service.IngestJobService;
import org.apache.camel.BeanScope;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class IngestMainOrchestrator extends SpringRouteBuilder {

    public static String myJMSQueueName = "ingest.MainOrchestrator";

    public static String myJMSQueueNameProcessing = "ingest.ActualRecordProcessingEvents";

    @Autowired
    MainLoopRouteCreator mainLoopRouteCreator;

    @Override
    public void configure() throws Exception {

        mainLoopRouteCreator.createEventProcessingLoop(this,
                "activemq:" + myJMSQueueName,
                new Class[]{IngestRequestedEvent.class, ActualIngestCompleted.class, AbortCommand.class},
                Arrays.asList(
                        new RedirectEvent(ActualIngestStartCommand.class, "activemq:" + ActualIngestCollectionOrchestrator.myJMSQueueName)
                )
        );


        // TODO: Distinguish by type for harvester / ingester?
        from("activemq:ActiveMQ.DLQ")
                .routeId("MainOrchestrator.DLQ")
                .onException(Throwable.class).to("activemq:ActiveMQ.DLQ_DLQ").end()
                .bean(IngestJobService.class, "updateIngestJobStateInDBToError( ${header.processID} )", BeanScope.Request)
        ;

    }
}
