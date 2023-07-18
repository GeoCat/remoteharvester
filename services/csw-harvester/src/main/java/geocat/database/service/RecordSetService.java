package geocat.database.service;

import geocat.database.entities.RecordSet;
import geocat.database.repos.RecordSetRepo;
import geocat.events.actualRecordCollection.ActualHarvestEndpointStartCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class RecordSetService {
    Logger logger = LoggerFactory.getLogger(RecordSetService.class);

    @Autowired
    RecordSetRepo recordSetRepo;

    //idempotent
    public RecordSet create(int start, int end, ActualHarvestEndpointStartCommand cmd, boolean lastSet) {
        RecordSet record = recordSetRepo.findByHarvestJobIdAndEndpointJobIdAndStartRecordNumber(cmd.getHarvesterId(),
                cmd.getEndPointId(),
                start
        );
        if (record != null)
            return record;

        RecordSet result = new RecordSet();
        //result.setRecordSetId(UUID.randomUUID().toString());
        result.setStartRecordNumber(start);
        result.setEndRecordNumber(end);
        result.setExpectedNumberRecords(end - start + 1);
        result.setEndpointJobId(cmd.getEndPointId());
        result.setHarvestJobId(cmd.getHarvesterId());
        result.setLastSet(lastSet);

        return recordSetRepo.save(result);
    }

    //idempotent
    public RecordSet update(long recordSetId, int numberRecordsReturned) {
        RecordSet record = recordSetRepo.findById(recordSetId).get();
        //  record.setGetRecordResponse(xmlGetRecordsResult);
        record.setActualNumberRecords(numberRecordsReturned);
        return recordSetRepo.save(record);
    }

    public boolean complete(long endpointId) {
        // logger.debug("start determine done");
        List<RecordSet> records = recordSetRepo.findByEndpointJobId(endpointId);
        boolean allDone = true;
        for (RecordSet recordSet : records) {
            boolean thisRecordDone = (recordSet.getActualNumberRecords() != null);
            allDone = allDone && thisRecordDone;
        }
        // logger.debug("finish determine done");
        return allDone;
    }

    public RecordSet getById(long recordSetId) {
        return recordSetRepo.findById(recordSetId).get();
    }


    public List<RecordSet> getAll(long endpointId) {
        return recordSetRepo.findByEndpointJobId(endpointId);
    }
}
