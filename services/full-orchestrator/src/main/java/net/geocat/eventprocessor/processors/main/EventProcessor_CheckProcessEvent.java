package net.geocat.eventprocessor.processors.main;

import net.geocat.database.orchestrator.entities.OrchestratedHarvestProcess;
import net.geocat.database.orchestrator.entities.OrchestratedHarvestProcessState;
import net.geocat.database.orchestrator.repos.OrchestratedHarvestProcessRepo;
import net.geocat.eventprocessor.BaseEventProcessor;
import net.geocat.events.CheckProcessEvent;
import net.geocat.events.Event;
import net.geocat.model.HarvestStartResponse;
import net.geocat.model.HarvestStatus;
import net.geocat.model.IngestStatus;
import net.geocat.model.LinkCheckStatus;
import net.geocat.service.OrchestratedHarvestProcessService;
import net.geocat.service.ProcessLockingService;
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
import java.util.Optional;
import java.util.concurrent.locks.Lock;


@Component
@Scope("prototype")
public class EventProcessor_CheckProcessEvent extends BaseEventProcessor<CheckProcessEvent> {

    Logger logger = LoggerFactory.getLogger(EventProcessor_CheckProcessEvent.class);

    @Autowired
    OrchestratedHarvestProcessRepo orchestratedHarvestProcessRepo;

    @Autowired
    OrchestratedHarvestProcessService orchestratedHarvestProcessService;

    @Autowired
    ProcessLockingService processLockingService;


    @Autowired
    HarvesterService harvesterService;

    @Autowired
    LinkCheckService linkCheckService;

    @Autowired
    IngesterService ingesterService;

    @Override
    public EventProcessor_CheckProcessEvent externalProcessing() {
        return this;
    }


    @Override
    public EventProcessor_CheckProcessEvent internalProcessing() throws Exception {
        String processID = getInitiatingEvent().getProcessID();
        Lock lock = processLockingService.getLock(processID);
        try {
            lock.lock();
            Optional<OrchestratedHarvestProcess> processOptional = orchestratedHarvestProcessRepo.findById(processID);

            if (processOptional.isPresent()) {
                OrchestratedHarvestProcess process = processOptional.get();

                switch (process.getState()) {
                    case ERROR:
                    case USERABORT:
                    case COMPLETE:
                        break; // do nothing - process is finished (this should not happen - this message shouldnt have been generated...)
                    case CREATED:
                        break; // shouldn't happen - first state should be harvesting (likely got the ping a bit too eary - HarvestRequestedEvent will handle this
                    case HAVESTING:
                        handle_HAVESTING(process);
                        break;
                    case LINKCHECKING:
                        handle_LINKCHECKING(process);
                        break;
                    case INGESTING:
                        handle_INGESTING(process);
                        break;
                }
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    private void handle_INGESTING(OrchestratedHarvestProcess process) throws Exception {
        IngestStatus ingestState = ingesterService.getIngestState(process.getInjectJobId(), true);
        String ingester_state = ingestState.getState();

        if (ingester_state.equals("COMPLETE")) {
            //transition to end of orchestrator
            //throw new Exception("dont know how to run injester");
            process.setState(OrchestratedHarvestProcessState.COMPLETE);
            orchestratedHarvestProcessRepo.save(process);
        } else if (ingester_state.equals("ERROR")) {
            process.setState(OrchestratedHarvestProcessState.ERROR);
            orchestratedHarvestProcessRepo.save(process);
        } else if (ingester_state.equals("USERABORT")) {
            process.setState(OrchestratedHarvestProcessState.USERABORT);
            orchestratedHarvestProcessRepo.save(process);
        } else {
            // nothing to do right now (wait longer)
            // TODO: reporting
        }
        //handle ERROR, USERABORT
    }

    private void handle_LINKCHECKING(OrchestratedHarvestProcess process) throws Exception {
        if (process.getLinkCheckJobId() == null) {
            return;
        }

        LinkCheckStatus linkCheckState = linkCheckService.getLinkCheckState(process.getLinkCheckJobId(), true);
        String linkcheck_state = linkCheckState.getLinkCheckJobState();

        if (linkcheck_state.equals("COMPLETE")) {
            //transition to ingest
            HarvestStartResponse response = ingesterService.startIngest(process.getHarvesterJobId());
            process.setInjectJobId(response.getProcessID());
            process.setState(OrchestratedHarvestProcessState.INGESTING);
            orchestratedHarvestProcessRepo.save(process);
        } else if (linkcheck_state.equals("ERROR")) {
            process.setState(OrchestratedHarvestProcessState.ERROR);
            orchestratedHarvestProcessRepo.save(process);
        } else if (linkcheck_state.equals("USERABORT")) {
            process.setState(OrchestratedHarvestProcessState.USERABORT);
            orchestratedHarvestProcessRepo.save(process);
        } else {
            // nothing to do right now (wait longer)
            // TODO: reporting
        }
        //handle ERROR, USERABORT
    }

    private void changePhaseFromHarvesting(OrchestratedHarvestProcess process) throws Exception {
        if (process.getExecuteLinkChecker()) {
            //transition to linkchecker
            HarvestStartResponse response = linkCheckService.startLinkCheck(process);
            process.setLinkCheckJobId(response.getProcessID());
            process.setState(OrchestratedHarvestProcessState.LINKCHECKING);
            orchestratedHarvestProcessRepo.save(process);
        } else {
            //transition to ingest
            HarvestStartResponse response = ingesterService.startIngest(process.getHarvesterJobId());
            process.setInjectJobId(response.getProcessID());
            process.setState(OrchestratedHarvestProcessState.INGESTING);
            orchestratedHarvestProcessRepo.save(process);
        }
    }

    private void handle_HAVESTING(OrchestratedHarvestProcess process) throws Exception {
        if (process.getSkipHarvesting()) {
            changePhaseFromHarvesting(process);

        } else {
            HarvestStatus status = harvesterService.getHarvestState(process.getHarvesterJobId(), true);
            String harvest_state = status.state;
            if (harvest_state.equals("COMPLETE")) {
                //process.setState(OrchestratedHarvestProcessState.LINKCHECKING);
                //orchestratedHarvestProcessRepo.save(process);

                changePhaseFromHarvesting(process);

            } else if (harvest_state.equals("ERROR")) {
                process.setState(OrchestratedHarvestProcessState.ERROR);
                orchestratedHarvestProcessRepo.save(process);
            } else if (harvest_state.equals("USERABORT")) {
                process.setState(OrchestratedHarvestProcessState.USERABORT);
                orchestratedHarvestProcessRepo.save(process);
            } else {
                // nothing to do right now (wait longer)
                // TODO: reporting
            }
        }

        //handle ERROR, USERABORT
        int t = 0;
    }


    @Override
    public List<Event> newEventProcessing() {
        List<Event> result = new ArrayList<>();

        return result;
    }

}
