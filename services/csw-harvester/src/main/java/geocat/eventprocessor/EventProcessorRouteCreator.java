package geocat.eventprocessor;

import geocat.events.Event;
import geocat.service.StopProcessingMessageService;
import org.apache.camel.BeanScope;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.ExpressionNode;
import org.apache.camel.model.ProcessorDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class EventProcessorRouteCreator {
    static JacksonDataFormat jsonDefHarvesterConfig = new JacksonDataFormat(Event.class);

    /**
     * eventType example - eventType=ActualHarvestEndpointStartCommand
     * Then there should be a class called
     * EventProcessor_ActualHarvestEndpointStartCommand extends BaseEventProcessor<ActualHarvestEndpointStartCommand>
     *
     * @param routeBuilder
     * @param eventType
     * @param from
     * @param to
     * @throws Exception
     */
    public void addEventProcessor(RouteBuilder routeBuilder,
                                  Class eventType,
                                  String from,
                                  String to,
                                  String tag,
                                  boolean unmarshal //will unmarshal message + transacted
            , List<RedirectEvent> redirectEventList
    ) throws Exception {
        validate(eventType);

        if (redirectEventList == null)
            redirectEventList = new ArrayList<>();

        ProcessorDefinition<ExpressionNode> route;
        //=====================================================================
        if (unmarshal)
            route = routeBuilder
                    .from(from)  // ${body} will be JMS message - unmarshal
                    .transacted()
                    .unmarshal(jsonDefHarvesterConfig)
                    .routeId(tag + "_" + eventType.getSimpleName())
                    // .log("processing event of type " + eventType.getSimpleName() + " from " + from)
                    .log(from + ": event = ${body}")
                    .bean(StopProcessingMessageService.class, "checkIfShouldBeProcessed", BeanScope.Request)
                    .bean(EventProcessorFactory.class, "create( ${body} )", BeanScope.Request)
                    .transform().simple("${body.externalProcessing()}")
                    .transform().simple("${body.internalProcessing()}")
                    .transform().simple("${body.newEventProcessing()}")
                    .split().simple("${body}")
                    // .marshal().json()
                    // .to(to)
                    ;
        else
            route = routeBuilder
                    .from(from)  // ${body} will be of type eventType
                    .transacted()
                    .routeId(tag + "_" + eventType.getSimpleName())
                    // .log("processing event of type " + eventType.getSimpleName() + " from " + from)
                    .log(from + ": event = ${body}")
                    .bean(StopProcessingMessageService.class, "checkIfShouldBeProcessed", BeanScope.Request)
                    .bean(EventProcessorFactory.class, "create( ${body} )", BeanScope.Request)
                    .transform().simple("${body.externalProcessing()}")
                    .transform().simple("${body.internalProcessing()}")
                    .transform().simple("${body.newEventProcessing()}")
                    .split().simple("${body}")
                    // .marshal().json()
                    // .to(to)
                    ;

        // ------------------------------------------------------------------

        if (!redirectEventList.isEmpty()) {
            ChoiceDefinition choice = route
                    .setHeader("eventType", routeBuilder.simple("${body.getClass().getSimpleName()}"))
                    //  .log(mainRouteName + " received event of type ${headers.eventType} and body=${body} ")
                    .choice();

            for (RedirectEvent redirectEvent : redirectEventList) {
                choice = choice
                        .when(routeBuilder.simple("${headers.eventType} == '" + redirectEvent.getEventType().getSimpleName() + "'"))
                        .log("direct redirecting event of type ${headers.eventType} to " + redirectEvent.getEndpoint())
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
            choice.otherwise()
                    .marshal().json()
                    .to(to);
        } else {
            route.marshal().json()
                    .to(to);
        }

        //=====================================================================
    }

    public void validate(Class eventType) throws Exception {
        if (!Event.class.isAssignableFrom(eventType))
            throw new Exception("RouteCreator - eventType must be Event");
        EventProcessorFactory.processorClass(eventType);//will throw if not found
    }


}
