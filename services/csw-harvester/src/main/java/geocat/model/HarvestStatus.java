package geocat.model;

import geocat.database.entities.HarvestJob;
import geocat.database.entities.HarvestJobState;

import java.util.ArrayList;
import java.util.List;

public class HarvestStatus {
    public String processID;
    public String url;
    public String longTermTag;
    public String state;
    public String createTimeUTC;
    public String lastUpdateUTC;
    public List<String> errorMessage;

    public List<List<String>> stackTraces;
    public List<EndpointStatus> endpoints;


    private HarvestStatus(String processID) {
        this.processID = processID;
        endpoints = new ArrayList<>();
        stackTraces = new ArrayList<>();
        errorMessage = new ArrayList<>();
    }

    public HarvestStatus(HarvestJob job) {
        this.processID = job.getJobId();
        this.url = job.getInitialUrl();
        this.longTermTag = job.getLongTermTag();
        this.state = job.getState().toString();
        this.createTimeUTC = job.getCreateTimeUTC().toInstant().toString();
        this.lastUpdateUTC = job.getLastUpdateUTC().toInstant().toString();
        endpoints = new ArrayList<>();
        stackTraces = new ArrayList<>();
        errorMessage = new ArrayList<>();
    }

    public static HarvestStatus createHarvestStatusNoProcessId(String processID) {
        HarvestStatus harvestStatus = new HarvestStatus(processID);
        harvestStatus.state = HarvestJobState.ERROR.toString();
        harvestStatus.errorMessage.add(String.format("Harvester with processID %s doesn't exist", processID));
        return harvestStatus;
    }
}
