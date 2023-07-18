package geocat.eventprocessor.processors.harvest;

import geocat.database.entities.RecordSet;
import geocat.database.repos.RecordSetRepo;
import geocat.database.service.RecordSetService;
import geocat.eventprocessor.BaseEventProcessor;
import geocat.events.Event;
import geocat.events.EventFactory;
import geocat.events.actualRecordCollection.ActualHarvestEndpointStartCommand;
import geocat.events.actualRecordCollection.GetRecordsCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class EventProcessor_ActualHarvestEndpointStartCommand extends BaseEventProcessor<ActualHarvestEndpointStartCommand> {
    Logger logger = LoggerFactory.getLogger(EventProcessor_ActualHarvestEndpointStartCommand.class);

    @Autowired
    RecordSetService recordSetService;

    @Autowired
    EventFactory eventFactory;

    @Autowired
    RecordSetRepo recordSetRepo;

    public EventProcessor_ActualHarvestEndpointStartCommand() {
        super();
    }


    @Override
    public EventProcessor_ActualHarvestEndpointStartCommand externalProcessing() {
        return this;
    }


    @Override
    public EventProcessor_ActualHarvestEndpointStartCommand internalProcessing() throws Exception {
        ActualHarvestEndpointStartCommand cmd = getInitiatingEvent();
        if (cmd.getExpectedNumberOfRecords() <= 0)
            throw new Exception("getExpectedNumberOfRecords <= 0");
        if (cmd.getnRecordPerRequest() <= 0)
            throw new Exception("getnRecordPerRequest <= 0");

        List<RecordSet> records = new ArrayList<>();

        logger.debug("Adding RecordSets to Database... (approx " + cmd.getExpectedNumberOfRecords() / cmd.getnRecordPerRequest() + " record sets)");
        //for example, for getting 10 records;
        //  first one - 1 to 10 (start at 1, get 10)
        //  2nd       - 11 to 20 (start at 11, get 10)
        for (int idx = 1; idx <= cmd.getExpectedNumberOfRecords(); idx += cmd.getnRecordPerRequest()) {
            int start = idx;
            int end = idx + cmd.getnRecordPerRequest() - 1;
            if (end > cmd.getExpectedNumberOfRecords())
                end = cmd.getExpectedNumberOfRecords();
            boolean lastOne = (end == cmd.getExpectedNumberOfRecords());
            RecordSet recordSet = recordSetService.create(start, end, cmd, lastOne);
            records.add(recordSet);
        }
        jiggle(records);
        return this;
    }

    // some servers (GN) do not like it when there is a request made to "close to, but not exactly" the end
    // i.e. if there are 81 records, it doesn't like a 61-80 request (final request will have 1 item in it).
    // the 61-80 will say there are no more records (even through there is a record remaining).
    //   NOTE: request for 81-81 will work
    //
    // so, if the final one is a request for 1 or 2 record, we will "jiggle" the previous request so its a few records fewer
    private void jiggle(List<RecordSet> records) {
        if (records.size() <= 1)
            return; //nothing to do

        ActualHarvestEndpointStartCommand cmd = getInitiatingEvent();

        if (cmd.getnRecordPerRequest() <= 3)
            return; //too few to jiggle

        RecordSet last = records.get(records.size() - 1);
        RecordSet secondToLast = records.get(records.size() - 2);

        if (last.getExpectedNumberRecords() > 2)
            return; //nothing to do

        secondToLast.setEndRecordNumber(secondToLast.getEndRecordNumber() - 2);
        secondToLast.setExpectedNumberRecords(secondToLast.getExpectedNumberRecords() - 2);

        last.setStartRecordNumber(last.getStartRecordNumber() - 2);
        last.setExpectedNumberRecords(last.getExpectedNumberRecords() + 2);

        recordSetRepo.save(last);
        recordSetRepo.save(secondToLast);
    }


    @Override
    public List<Event> newEventProcessing() {
        List<Event> result = new ArrayList<>();
        long endpointId = getInitiatingEvent().getEndPointId();
        List<RecordSet> records = recordSetService.getAll(endpointId);
        logger.debug("Creating GetRecordsCommand events... (" + records.size() + " commands)");

        for (RecordSet record : records) {
            GetRecordsCommand command = eventFactory.create_GetRecordsCommand(getInitiatingEvent(),
                    record.getStartRecordNumber(),
                    record.getEndRecordNumber(),
                    record.getRecordSetId(),
                    record.isLastSet());
            result.add(command);
        }
        return result;
    }
}