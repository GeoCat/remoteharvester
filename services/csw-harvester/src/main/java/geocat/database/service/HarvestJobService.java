package geocat.database.service;

import geocat.database.entities.EndpointJob;
import geocat.database.entities.EndpointJobState;
import geocat.database.entities.HarvestJob;
import geocat.database.entities.HarvestJobState;
import geocat.database.repos.EndpointJobRepo;
import geocat.database.repos.HarvestJobRepo;
import geocat.events.HarvestRequestedEvent;
import geocat.events.determinework.WorkedDeterminedFinished;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Scope("prototype")
public class HarvestJobService {

    @Autowired
    private HarvestJobRepo harvestJobRepo;
    @Autowired
    private EndpointJobRepo endpointJobRepo;

    //idempotent tx
    public HarvestJob createNewHarvestJobInDB(HarvestRequestedEvent event) {
        Optional<HarvestJob> job = harvestJobRepo.findById(event.getHarvestId());

        if (job.isPresent()) //2nd attempt
        {
            job.get().setState(HarvestJobState.CREATING);
            return harvestJobRepo.save(job.get());
        }
        HarvestJob newJob = new HarvestJob();
        newJob.setDoNotSort(event.getDoNotSort());
        newJob.setJobId(event.getHarvestId());
        newJob.setFilter(event.getFilter());
        newJob.setLookForNestedDiscoveryService(event.isLookForNestedDiscoveryService());
        newJob.setInitialUrl(event.getUrl());
        newJob.setLongTermTag(event.getLongTermTag());
        newJob.setState(HarvestJobState.CREATING);
        newJob.setProblematicResultsConfigurationJSON(event.getProblematicResultsConfigurationJSON());
        newJob.setNrecordsPerRequest(event.getNumberRecordsPerRequest());
        newJob.setGetRecordQueueHint(event.getGetRecordQueueHint());
        return harvestJobRepo.save(newJob);
    }

    public HarvestJob updateHarvestJobStateInDBToError(String guid) throws Exception {
//        if (true)
//            throw new Exception("BOOMY!!");
        return updateHarvestJobStateInDB(guid, HarvestJobState.ERROR);
    }

    public HarvestJob updateHarvestJobStateInDB(String guid, HarvestJobState state) {
        HarvestJob job = harvestJobRepo.findById(guid).get();
        job.setState(state);
        return harvestJobRepo.save(job);
    }

    static Object lockobject = new Object();

    public WorkedDeterminedFinished determineIfWorkCompleted(String harvestId) {
        synchronized (lockobject) {
            HarvestJob harvestJob = harvestJobRepo.findById(harvestId).get();
            if (!(harvestJob.getState() == HarvestJobState.DETERMINING_WORK))
                return null; //already completed earlier
            List<EndpointJob> outstandingJobs = endpointJobRepo.findByHarvestJobIdAndState(harvestId, EndpointJobState.DETERMINING_WORK);
            boolean workCompleted = outstandingJobs.isEmpty();
            if (workCompleted) {
                //move state
                updateHarvestJobStateInDB(harvestId, HarvestJobState.WORK_DETERMINED);
                return new WorkedDeterminedFinished(harvestId);
            }
            return null;
        }
    }

    public Optional<HarvestJob> getById(String id) {
        return harvestJobRepo.findById(id);
    }

    public Optional<HarvestJob> getLastCompletedHarvestJobIdByLongTermTag(String longTermTag) {
        return harvestJobRepo.findMostRecentHarvestJobCompletedByLongTermTag(longTermTag);
    }

    public Optional<HarvestJob> getLastHarvestJobIdByLongTermTag(String longTermTag) {
        return harvestJobRepo.findMostRecentHarvestJobByLongTermTag(longTermTag);
    }

    public List<EndpointJob> getEndpointJobs(String harvestId) {
        return endpointJobRepo.findByHarvestJobId(harvestId);
    }

}
