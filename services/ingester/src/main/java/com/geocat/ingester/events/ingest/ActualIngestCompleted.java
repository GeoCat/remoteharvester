package com.geocat.ingester.events.ingest;


import com.geocat.ingester.events.Event;

public class ActualIngestCompleted extends Event {

    private String jobId;

    public ActualIngestCompleted() {
    }

    public ActualIngestCompleted(String jobId) {
        this.jobId = jobId;
    }


    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
