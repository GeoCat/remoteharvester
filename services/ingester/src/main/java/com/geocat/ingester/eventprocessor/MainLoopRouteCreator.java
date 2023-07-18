package com.geocat.ingester.eventprocessor;

import com.geocat.ingester.events.Event;
import com.geocat.ingester.service.DatabaseUpdateService;
import org.apache.camel.BeanScope;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class MainLoopRouteCreator {

    Logger logger = LoggerFactory.getLogger(MainLoopRouteCreator.class);


    @Autowired
    EventProcessorRouteCreator eventProcessorRouteCreator;

    public void createEventProcessingLoop(SpringRouteBuilder routeBuilder,
                                          String from,
                                          Class[] eventTypes,
                                          List<RedirectEvent> redirectEventList) throws Exception {

        String mainRouteName = extractName(from);
        JacksonDataFormat jsonDefHarvesterConfig = new JacksonDataFormat(Event.class);

        routeBuilder.errorHandler(routeBuilder.transactionErrorHandler().maximumRedeliveries(2).redeliveryDelay(1000));
        routeBuilder.onException().onExceptionOccurred(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Exception ex = exchange.getException();
                        exchange.getMessage().setHeader("exception", ex);
                    }
                })
                .bean(DatabaseUpdateService.class, "errorOccurred", BeanScope.Request).maximumRedeliveries(2)
        ;

        ChoiceDefinition choice = routeBuilder
                .from(from)
                .routeId(mainRouteName + ".eventprocessor")
                //.transacted("PROPAGATION_INGEST_REQUIRED")
                .unmarshal(jsonDefHarvesterConfig)
                .setHeader("eventType", routeBuilder.simple("${body.getClass().getSimpleName()}"))
                .log(mainRouteName + " received event of type ${headers.eventType} and body=${body} ")
                .choice();
        // special events - re-broadcast to parent
        for (RedirectEvent redirectEvent : redirectEventList) {
            choice = choice
                    .when(routeBuilder.simple("${headers.eventType} == '" + redirectEvent.getEventType().getSimpleName() + "'"))
                    .log("redirection event of type ${headers.eventType} to " + redirectEvent.getEndpoint())
//                    .process(new Processor() {
//                        @Override
//                        public void process(Exchange exchange) throws Exception {
//                            logger.debug("redirect processor");
//                            logger.debug("exchange.getmessage = "+exchange.getMessage());
//                            logger.debug("exchange.getmessage.getbody = "+exchange.getMessage().getBody());
//
//                            int t=0;
//                        }
//                    })
                    .marshal().json()
                    .to(redirectEvent.getEndpoint());
        }


        // all other events, route to direct:
        for (Class eventType : eventTypes) {
            choice = choice
                    .when(routeBuilder.simple("${headers.eventType} == '" + eventType.getSimpleName() + "'"))
                    .to("direct:" + mainRouteName + "_" + eventType.getSimpleName());
        }

        //add in individual processors
        for (Class eventType : eventTypes) {
            eventProcessorRouteCreator.addEventProcessor(routeBuilder,
                    eventType,
                    "direct:" + mainRouteName + "_" + eventType.getSimpleName(),
                    from,
                    mainRouteName);
        }


    }

    // activemq:abc?...  -> abc
    public String extractName(String from) {
        return from.replaceFirst("[^:]+:", "")
                .replaceFirst("\\?[.]+", "");

    }
}
