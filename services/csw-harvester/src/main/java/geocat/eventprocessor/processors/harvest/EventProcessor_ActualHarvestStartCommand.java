package geocat.eventprocessor.processors.harvest;

import geocat.database.entities.EndpointJob;
import geocat.database.entities.EndpointJobState;
import geocat.database.entities.HarvestJob;
import geocat.database.entities.HarvestJobState;
import geocat.database.service.EndpointJobService;
import geocat.database.service.HarvestJobService;
import geocat.eventprocessor.BaseEventProcessor;
import geocat.events.Event;
import geocat.events.EventFactory;
import geocat.events.actualRecordCollection.ActualHarvestEndpointStartCommand;
import geocat.events.actualRecordCollection.ActualHarvestStartCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")

public class EventProcessor_ActualHarvestStartCommand extends BaseEventProcessor<ActualHarvestStartCommand> {

    @Autowired
    HarvestJobService harvestJobService;

    @Autowired
    EndpointJobService endpointJobService;

    @Autowired
    EventFactory eventFactory;

    public EventProcessor_ActualHarvestStartCommand() {
        super();
    }


    @Override
    public EventProcessor_ActualHarvestStartCommand externalProcessing() {
        return this;
    }


    @Override
    public EventProcessor_ActualHarvestStartCommand internalProcessing() {
        String harvestId = getInitiatingEvent().getHarvesterId();

        harvestJobService.updateHarvestJobStateInDB(harvestId, HarvestJobState.GETTING_RECORDS);

        List<EndpointJob> jobs = endpointJobService.findAll(harvestId);
        for (EndpointJob job : jobs) {
            endpointJobService.updateState(job.getEndpointJobId(), EndpointJobState.GETTING_RECORDS);
        }

        return this;
    }


    @Override
    public List<Event> newEventProcessing() throws Exception {
        List<Event> result = new ArrayList<>();
        String harvestId = getInitiatingEvent().getHarvesterId();

        HarvestJob harvestJob = harvestJobService.getById(harvestId).get();
        List<EndpointJob> endpointJobs = harvestJobService.getEndpointJobs(harvestId);

        for (EndpointJob job : endpointJobs) {
            ActualHarvestEndpointStartCommand e = eventFactory.create_ActualHarvestEndpointStartCommand(job, harvestJob);
            result.add(e);
        }
        return result;
    }
}