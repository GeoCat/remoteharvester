package geocat.eventprocessor.processors.harvest;

import geocat.csw.CSWService;
import geocat.csw.csw.CSWGetRecordsHandler;
import geocat.database.entities.EndpointJob;
import geocat.database.entities.EndpointJobState;
import geocat.database.entities.HarvestJob;
import geocat.database.entities.RecordSet;
import geocat.database.service.EndpointJobService;
import geocat.database.service.HarvestJobService;
import geocat.database.service.RecordSetService;
import geocat.eventprocessor.BaseEventProcessor;
import geocat.eventprocessor.processors.GetRecordsResult;
import geocat.events.Event;
import geocat.events.actualRecordCollection.EndpointHarvestComplete;
import geocat.events.actualRecordCollection.GetRecordsCommand;
import geocat.model.GetRecordsResponseInfo;
import geocat.service.GetRecordsResponseEvaluator;
import geocat.service.MetadataExploderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class EventProcessor_GetRecordsCommand extends BaseEventProcessor<GetRecordsCommand> {

    Logger logger = LoggerFactory.getLogger(EventProcessor_GetRecordsCommand.class);

    @Autowired
    CSWService cswService;

    @Autowired
    CSWGetRecordsHandler cswGetRecordsHandler;

    @Autowired
    RecordSetService recordSetService;

    @Autowired
    HarvestJobService harvestJobService;

    @Autowired
    EndpointJobService endpointJobService;

    @Autowired
    MetadataExploderService metadataExploderService;

    @Autowired
    GetRecordsResponseEvaluator getRecordsResponseEvaluator;

    GetRecordsResult result;

    public EventProcessor_GetRecordsCommand() {
        super();
    }

    @Override
    public EventProcessor_GetRecordsCommand externalProcessing() throws Exception {

        GetRecordsCommand e = getInitiatingEvent();
        String xml = cswService.GetRecords(e.getGetRecordsURL(), e.getFilter(), e.getStartRecordNumber(), e.getEndRecordNumber(), e.getDoNotSort());
        //logger.debug("starting to parse xml");
        Document xmlParsed = MetadataExploderService.parseXML(xml);
        //logger.debug("finish parse xml");

        GetRecordsResponseInfo info = new GetRecordsResponseInfo(xmlParsed);
        HarvestJob harvestJob = harvestJobService.getById(e.getHarvesterId()).get();
        EndpointJob endpointJob = endpointJobService.getById(e.getEndPointId());
        RecordSet recordSet = recordSetService.getById(e.getRecordSetId());

        //logger.debug("eval...");

        getRecordsResponseEvaluator.evaluate(harvestJob, endpointJob, info, recordSet);

        int nrecords = cswGetRecordsHandler.extractActualNumberOfRecordsReturned(xmlParsed);
        result = new GetRecordsResult(xml, nrecords, xmlParsed);
        //logger.debug("done external");

        return this;
    }


    @Override
    public EventProcessor_GetRecordsCommand internalProcessing() throws Exception {
        GetRecordsCommand cmd = getInitiatingEvent();
        recordSetService.update(cmd.getRecordSetId(), result.getNumberRecordsReturned());
        RecordSet recordSet = recordSetService.getById(cmd.getRecordSetId());
        //logger.debug("exploding");

        metadataExploderService.explode(recordSet, result.getParsedXML());
        //logger.debug("exploded");

        return this;
    }


    @Override
    public List<Event> newEventProcessing() {
        List<Event> result = new ArrayList<>();
        if (recordSetService.complete(getInitiatingEvent().getEndPointId())) {
            EndpointHarvestComplete e = new EndpointHarvestComplete(getInitiatingEvent().getEndPointId(),
                    getInitiatingEvent().getHarvesterId());
            endpointJobService.updateState(getInitiatingEvent().getEndPointId(), EndpointJobState.RECORDS_RECEIVED);
            result.add(e);
        }
        return result;
    }
}
