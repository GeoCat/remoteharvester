package com.geocat.ingester.eventprocessor.processors.main;

import com.geocat.ingester.eventprocessor.BaseEventProcessor;
import com.geocat.ingester.events.Event;
import com.geocat.ingester.events.ingest.AbortCommand;
import com.geocat.ingester.service.IngestJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.geocat.ingester.model.ingester.IngestJobState.USERABORT;

@Component
@Scope("prototype")
public class EventProcessor_AbortCommand extends BaseEventProcessor<AbortCommand> {

    Logger logger = LoggerFactory.getLogger(EventProcessor_AbortCommand.class);

    @Autowired
    IngestJobService ingestJobService;

    @Override
    public EventProcessor_AbortCommand externalProcessing() {
        return this;
    }


    @Override
    public EventProcessor_AbortCommand internalProcessing() {
        String processID = getInitiatingEvent().getIngestJobId();
        logger.warn("attempting to user abort for " + processID);
        ingestJobService.updateIngestJobStateInDB(processID, USERABORT);
        logger.warn("user abort processed for " + processID);
        // linkCheckJobService.finalize(getInitiatingEvent().getProcessID());
        return this;
    }


    @Override
    public List<Event> newEventProcessing() {
        List<Event> result = new ArrayList<>();
        return result;
    }
}
