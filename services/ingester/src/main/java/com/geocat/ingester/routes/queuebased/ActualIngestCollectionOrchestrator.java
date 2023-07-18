package com.geocat.ingester.routes.queuebased;

import com.geocat.ingester.eventprocessor.MainLoopRouteCreator;
import com.geocat.ingester.eventprocessor.RedirectEvent;
import com.geocat.ingester.events.ingest.ActualIngestCompleted;
import com.geocat.ingester.events.ingest.ActualIngestStartCommand;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;


@Component
public class ActualIngestCollectionOrchestrator extends SpringRouteBuilder {

    public static String myJMSQueueName = "ingest.ActualRecordProcessingEvents";

    @Autowired
    MainLoopRouteCreator mainLoopRouteCreator;

    @Override
    public void configure() throws Exception {

        mainLoopRouteCreator.createEventProcessingLoop(this,
                "activemq:" + myJMSQueueName,
                new Class[]{ActualIngestStartCommand.class},
                Arrays.asList(new RedirectEvent(ActualIngestCompleted.class, "activemq:" + IngestMainOrchestrator.myJMSQueueName))
        );

    }
}
