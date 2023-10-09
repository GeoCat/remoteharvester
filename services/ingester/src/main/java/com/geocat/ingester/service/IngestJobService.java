package com.geocat.ingester.service;

import com.geocat.ingester.dao.ingester.IngestJobRepo;
import com.geocat.ingester.events.IngestRequestedEvent;
import com.geocat.ingester.model.ingester.IngestJob;
import com.geocat.ingester.model.ingester.IngestJobState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope("prototype")
public class IngestJobService {

    @Autowired
    private IngestJobRepo ingestJobRepo;

    //idempotent tx
    public IngestJob createNewIngestJobInDB(IngestRequestedEvent event) {
        Optional<IngestJob> job = ingestJobRepo.findById(event.getJobId());

        if (job.isPresent()) //2nd attempt
        {
            job.get().setState(IngestJobState.CREATING);
            return ingestJobRepo.save(job.get());
        }
        IngestJob newJob = new IngestJob();
        newJob.setJobId(event.getJobId());
        newJob.setHarvestJobId(event.getHarvestJobId());
        newJob.setState(IngestJobState.CREATING);

        return ingestJobRepo.save(newJob);
    }

    public IngestJob updateIngestJobStateInDBToError(String guid) {
        return updateIngestJobStateInDB(guid, IngestJobState.ERROR);
    }

    public IngestJob updateIngestJobStateInDB(String guid, IngestJobState state) {
        IngestJob job = ingestJobRepo.findById(guid).get();
        job.setState(state);
        return ingestJobRepo.save(job);
    }

    public IngestJob updateIngestJobStateInDBIngestedRecords(String guid, long totalIngestedRecords,
                                                             long totalIndexedRecords, long totalDeletedRecords,
                                                             long totalRecordsToProcess) {
        IngestJob job = ingestJobRepo.findById(guid).get();
        job.setState(IngestJobState.INGESTING_RECORDS);
        job.setTotalIngestedRecords(totalIngestedRecords);
        job.setTotalIngestedRecords(totalIndexedRecords);
        job.setTotalDeletedRecords(totalDeletedRecords);
        job.setTotalRecords(totalRecordsToProcess);
        return ingestJobRepo.save(job);
    }

    public IngestJob updateIngestJobStateInDBIngestedRecords(String guid, long totalIngestedRecords) {
        IngestJob job = ingestJobRepo.findById(guid).get();
        // job.setState(IngestJobState.INGESTING_RECORDS);
        job.setTotalIngestedRecords(totalIngestedRecords);
        return ingestJobRepo.save(job);
    }

    public IngestJob updateIngestJobStateInDBIndexedRecords(String guid, long totalIngestedRecords) {
        IngestJob job = ingestJobRepo.findById(guid).get();
        // job.setState(IngestJobState.INDEXING_RECORDS);
        job.setTotalIndexedRecords(totalIngestedRecords);
        return ingestJobRepo.save(job);
    }

    public IngestJob updateIngestJobStateInDBDeletedRecords(String guid, long totalDeletedRecords) {
        IngestJob job = ingestJobRepo.findById(guid).get();
        //  job.setState(IngestJobState.DELETING_RECORDS);
        job.setTotalDeletedRecords(totalDeletedRecords);
        return ingestJobRepo.save(job);
    }

    public Optional<IngestJob> getById(String id) {
        return ingestJobRepo.findById(id);
    }
}
