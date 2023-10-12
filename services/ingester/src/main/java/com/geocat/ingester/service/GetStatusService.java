package com.geocat.ingester.service;

import com.geocat.ingester.model.ingester.IngestJob;
import com.geocat.ingester.model.ingester.IngestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope("prototype")
public class GetStatusService {

    @Autowired
    IngestJobService ingestJobService;

    public IngestStatus getStatus(String processId) {
        Optional<IngestJob> jobOptional = ingestJobService.getById(processId);

        IngestStatus result;

        if (jobOptional.isPresent()) {
            result = new IngestStatus(jobOptional.get());
        } else {
            result = IngestStatus.createIngestStatusNoProcessId(processId);
        }

        return result;
    }

    /*private long computeNumberReceived(EndpointJob endpointJob) {
        return metadataRecordRepo.countByEndpointJobId(endpointJob.getEndpointJobId());
    }*/
}
