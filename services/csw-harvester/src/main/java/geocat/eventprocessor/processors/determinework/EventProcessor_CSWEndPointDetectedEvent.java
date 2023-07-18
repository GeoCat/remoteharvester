package geocat.eventprocessor.processors.determinework;

import geocat.csw.CSWMetadata;
import geocat.csw.CSWService;
import geocat.database.entities.EndpointJobState;
import geocat.database.service.DatabaseUpdateService;
import geocat.database.service.EndpointJobService;
import geocat.eventprocessor.BaseEventProcessor;
import geocat.events.Event;
import geocat.events.EventFactory;
import geocat.events.determinework.CSWEndPointDetectedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class EventProcessor_CSWEndPointDetectedEvent extends BaseEventProcessor<CSWEndPointDetectedEvent> {

    @Autowired
    EndpointJobService endpointJobService;

    @Autowired
    CSWService cswService;

    @Autowired
    DatabaseUpdateService databaseUpdateService;

    @Autowired
    EventFactory eventFactory;

    CSWMetadata result;
    List<CSWEndPointDetectedEvent> newEndpoints;


    @Override
    public EventProcessor_CSWEndPointDetectedEvent externalProcessing() throws Exception {
        endpointJobService.updateState(getInitiatingEvent().getEndPointId(), EndpointJobState.DETERMINING_WORK);
        result = cswService.getMetadata(getInitiatingEvent());
        return this;
    }


    @Override
    public EventProcessor_CSWEndPointDetectedEvent internalProcessing() {
        newEndpoints = databaseUpdateService.updateDatabase(result);
        // endpointJobService.updateState(getInitiatingEvent().getEndPointId(),EndpointJobState.DETERMINING_WORK);
        return this;
    }


    @Override
    public List<Event> newEventProcessing() {
        List<Event> result = new ArrayList<>();
        if ((newEndpoints != null) && (!newEndpoints.isEmpty()))
            result.addAll(newEndpoints);

        Event e = eventFactory.create_CSWEndpointWorkDetermined(getInitiatingEvent().getHarvesterId(),
                getInitiatingEvent().getEndPointId());
        result.add(e);

        //  endpointJobService.updateState(getInitiatingEvent().getEndPointId(),EndpointJobState.WORK_DETERMINED);
        return result;
    }

}