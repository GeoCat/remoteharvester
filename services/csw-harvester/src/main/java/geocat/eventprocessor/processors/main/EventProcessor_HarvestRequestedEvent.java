package geocat.eventprocessor.processors.main;

import geocat.database.entities.HarvestJob;
import geocat.database.entities.HarvestJobState;
import geocat.database.service.HarvestJobService;
import geocat.eventprocessor.BaseEventProcessor;
import geocat.events.Event;
import geocat.events.EventFactory;
import geocat.events.HarvestRequestedEvent;
import geocat.service.DeleteJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class EventProcessor_HarvestRequestedEvent extends BaseEventProcessor<HarvestRequestedEvent> {

    @Autowired
    HarvestJobService harvestJobService;

    @Autowired
    EventFactory eventFactory;

    @Autowired
    DeleteJobService deleteJobService;

    HarvestJob job;


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public EventProcessor_HarvestRequestedEvent externalProcessing() {
        job = harvestJobService.createNewHarvestJobInDB(getInitiatingEvent());
        return this;
    }


    @Override
    public EventProcessor_HarvestRequestedEvent internalProcessing() throws Exception {

        deleteJobService.ensureAtMost(getInitiatingEvent().getLongTermTag(),
                getInitiatingEvent().getStoreAtMostNHistoricalRuns(),
                job.getJobId());

        job = harvestJobService.updateHarvestJobStateInDB(getInitiatingEvent().getHarvestId(), HarvestJobState.DETERMINING_WORK);
        return this;
    }


    @Override
    public List<Event> newEventProcessing() {
        List<Event> result = new ArrayList<>();
        Event newEvent = eventFactory.create_StartWorkCommand(job);
        result.add(newEvent);
        return result;
    }

}