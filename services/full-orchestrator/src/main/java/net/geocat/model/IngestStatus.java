package net.geocat.model;


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

    public List<String> errorMessage = new ArrayList<>();

    public String getProcessID() {
        return processID;
    }

    public void setProcessID(String processID) {
        this.processID = processID;
    }

    public String getHarvesterJobId() {
        return harvesterJobId;
    }

    public void setHarvesterJobId(String harvesterJobId) {
        this.harvesterJobId = harvesterJobId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCreateTimeUTC() {
        return createTimeUTC;
    }

    public void setCreateTimeUTC(String createTimeUTC) {
        this.createTimeUTC = createTimeUTC;
    }

    public String getLastUpdateUTC() {
        return lastUpdateUTC;
    }

    public void setLastUpdateUTC(String lastUpdateUTC) {
        this.lastUpdateUTC = lastUpdateUTC;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public long getNumberOfRecordsIngested() {
        return numberOfRecordsIngested;
    }

    public void setNumberOfRecordsIngested(long numberOfRecordsIngested) {
        this.numberOfRecordsIngested = numberOfRecordsIngested;
    }

    public long getNumberOfRecordsIndexed() {
        return numberOfRecordsIndexed;
    }

    public void setNumberOfRecordsIndexed(long numberOfRecordsIndexed) {
        this.numberOfRecordsIndexed = numberOfRecordsIndexed;
    }

    public List<String> getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(List<String> errorMessage) {
        this.errorMessage = errorMessage;
    }
}
