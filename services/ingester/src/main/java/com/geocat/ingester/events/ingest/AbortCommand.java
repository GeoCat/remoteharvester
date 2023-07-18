package com.geocat.ingester.events.ingest;

import com.geocat.ingester.events.Event;

public class AbortCommand extends Event {

    private String ingestJobId;

    //---


    public AbortCommand() {
    }

    public AbortCommand(String ingestJobId) {
        this.ingestJobId = ingestJobId;
    }


    //---


    public String getIngestJobId() {
        return ingestJobId;
    }

    public void setIngestJobId(String ingestJobId) {
        this.ingestJobId = ingestJobId;
    }

    //--
    @Override
    public String toString() {
        return "AbortCommand for processID=" + ingestJobId;
    }
}
