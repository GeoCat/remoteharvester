package net.geocat.eventprocessor.processors.main;

import net.geocat.database.orchestrator.entities.OrchestratedHarvestProcess;
import net.geocat.database.orchestrator.entities.OrchestratedHarvestProcessState;
import net.geocat.database.orchestrator.repos.OrchestratedHarvestProcessRepo;
import net.geocat.eventprocessor.BaseEventProcessor;
import net.geocat.events.Event;
import net.geocat.events.OrchestratedHarvestAbortEvent;
import net.geocat.service.OrchestratedHarvestProcessService;
import net.geocat.service.exernalservices.HarvesterService;
import net.geocat.service.exernalservices.IngesterService;
import net.geocat.service.exernalservices.LinkCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@Scope("prototype")
public class EventProcessor_OrchestratedHarvestAbortEvent extends BaseEventProcessor<OrchestratedHarvestAbortEvent> {

    Logger logger = LoggerFactory.getLogger(EventProcessor_OrchestratedHarvestAbortEvent.class);

    @Autowired
    OrchestratedHarvestProcessRepo orchestratedHarvestProcessRepo;

    @Autowired
    OrchestratedHarvestProcessService orchestratedHarvestProcessService;

    @Autowired
    HarvesterService harvesterService;

    @Autowired
    LinkCheckService linkCheckService;

    @Autowired
    IngesterService ingesterService;


    @Override
    public EventProcessor_OrchestratedHarvestAbortEvent externalProcessing() {
        return this;
    }


    @Override
    public EventProcessor_OrchestratedHarvestAbortEvent internalProcessing() throws Exception {
        String processID = getInitiatingEvent().getProcessID();
        logger.warn("attempting to user abort for " + processID);

        OrchestratedHarvestProcess job = orchestratedHarvestProcessRepo.findById(processID).get();
        if ((job.getState() != OrchestratedHarvestProcessState.COMPLETE)
                && (job.getState() != OrchestratedHarvestProcessState.ERROR)
                && (job.getState() != OrchestratedHarvestProcessState.USERABORT)) {

            if ((job.getHarvesterJobId() != null) && (!job.getHarvesterJobId().trim().isEmpty())) {
                String response = harvesterService.abortHarvest(job.getHarvesterJobId().trim());
                logger.info("harvester abort response: " + response);
            }

            if ((job.getLinkCheckJobId() != null) && (!job.getLinkCheckJobId().trim().isEmpty())) {
                String response = linkCheckService.abortLinkCheck(job.getLinkCheckJobId().trim());
                logger.info("linkcheck abort response: " + response);

            }

            if ((job.getInjectJobId() != null) && (!job.getInjectJobId().trim().isEmpty())) {
                String response = ingesterService.abortIngest(job.getInjectJobId().trim());
                logger.info("ingest abort response: " + response);
            }

            orchestratedHarvestProcessService.updateLinkCheckJobStateInDB(processID, OrchestratedHarvestProcessState.USERABORT);
            logger.warn("user abort processed for " + processID);
        } else {
            logger.warn("user abort - process is already in state: " + job.getState());
        }

        return this;
    }


    @Override
    public List<Event> newEventProcessing() {
        List<Event> result = new ArrayList<>();

        return result;
    }

}
