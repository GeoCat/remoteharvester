package com.geocat.ingester.eventprocessor.processors.ingest;

import com.geocat.ingester.eventprocessor.BaseEventProcessor;
import com.geocat.ingester.events.Event;
import com.geocat.ingester.events.ingest.ActualIngestCompleted;
import com.geocat.ingester.events.ingest.ActualIngestStartCommand;
import com.geocat.ingester.service.IngesterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@Scope("prototype")
public class EventProcessor_ActualIngestStartCommand extends BaseEventProcessor<ActualIngestStartCommand> {

    @Autowired
    IngesterService ingesterService;

    boolean ingesterServiceComplete = false;

    public EventProcessor_ActualIngestStartCommand() {
        super();
    }

    @Override
    public EventProcessor_ActualIngestStartCommand internalProcessing() throws Exception {
        ActualIngestStartCommand cmd = getInitiatingEvent();

        ingesterServiceComplete = ingesterService.run(cmd.getJobId(), cmd.getHarvesterJobId());

        return this;
    }

    @Override
    public EventProcessor_ActualIngestStartCommand externalProcessing() {
        return this;
    }

    @Override
    public List<Event> newEventProcessing() throws Exception {
        List<Event> result = new ArrayList<>();
        if (ingesterServiceComplete)
            result.add(new ActualIngestCompleted(this.getInitiatingEvent().getJobId()));
        return result;
    }
}
