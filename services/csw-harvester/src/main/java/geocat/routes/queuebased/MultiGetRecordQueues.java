package geocat.routes.queuebased;

import geocat.database.service.DatabaseUpdateService;
import geocat.eventprocessor.EventProcessorRouteCreator;
import geocat.events.actualRecordCollection.GetRecordsCommand;
import geocat.service.QueueChooserService;
import geocat.service.QueueInfo;
import org.apache.camel.BeanScope;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MultiGetRecordQueues extends SpringRouteBuilder {

    Logger logger = LoggerFactory.getLogger(MultiGetRecordQueues.class);


    @Autowired
    EventProcessorRouteCreator eventProcessorRouteCreator;

    @Autowired
    QueueChooserService queueChooserService;


    @Override
    public void configure() throws Exception {

        this.errorHandler(this.transactionErrorHandler()
                .maximumRedeliveries(1)
                .redeliveryDelay(1000));

        this.onException().onExceptionOccurred(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Exception ex = exchange.getException();
                        exchange.getMessage().setHeader("exception", ex);
                        logger.error("exception occurred", ex);

                    }
                })
                .bean(DatabaseUpdateService.class, "errorOccurred", BeanScope.Request).maximumRedeliveries(1)
        ;


        for (QueueInfo info : queueChooserService.enumerateAllQueues()) {
            create(info.queueName(), info.parallelism());
        }

    }

    private void create(String queueName, int parallelism) throws Exception {
        String queue = "activemq:" + queueName;
        if (parallelism > 1)
            queue = queue + "?concurrentConsumers=" + parallelism;
        eventProcessorRouteCreator.addEventProcessor(
                this,
                GetRecordsCommand.class,
                queue,
                "activemq:" + ActualRecordCollectionOrchestrator.myJMSQueueName,
                queueName,
                true,
                null
        );
    }
}
