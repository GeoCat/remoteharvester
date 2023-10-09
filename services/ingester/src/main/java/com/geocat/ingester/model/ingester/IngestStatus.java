package com.geocat.ingester.model.ingester;


import java.util.ArrayList;
import java.util.List;

public class IngestStatus {
    public String processID;
    public String harvesterJobId;
    public String state;
    public String createTimeUTC;
    public String lastUpdateUTC;
    public long totalRecords;
    public long numberOfRecordsIngested;
    public long numberOfRecordsIndexed;

    public List<String> errorMessage;

    private IngestStatus(String processID) {
        this.processID = processID;
        errorMessage = new ArrayList<>();
    }

    public IngestStatus(IngestJob job) {
        this.processID = job.getJobId();
        this.totalRecords = (job.getTotalRecords() == null ? 0 : job.getTotalRecords());
        this.numberOfRecordsIngested = (job.getTotalIngestedRecords() == null ? 0 : job.getTotalIngestedRecords());
        this.numberOfRecordsIndexed = (job.getTotalIndexedRecords() == null ? 0 : job.getTotalIndexedRecords());
        this.harvesterJobId = job.getHarvestJobId();
        this.state = job.getState().toString();
        this.createTimeUTC = job.getCreateTimeUTC().toInstant().toString();
        this.lastUpdateUTC = job.getLastUpdateUTC().toInstant().toString();
        this.errorMessage = new ArrayList<>();
    }

    public static IngestStatus createIngestStatusNoProcessId(String processID) {
        IngestStatus ingestStatus = new IngestStatus(processID);
        ingestStatus.state = IngestJobState.ERROR.toString();
        ingestStatus.errorMessage.add(String.format("Ingester with processID %s doesn't exist", processID));

        return ingestStatus;
    }
}
