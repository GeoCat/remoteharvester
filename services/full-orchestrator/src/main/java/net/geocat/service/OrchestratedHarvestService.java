package net.geocat.service;

import net.geocat.database.orchestrator.entities.OrchestratedHarvestProcess;
import net.geocat.database.orchestrator.entities.OrchestratedHarvestProcessState;
import net.geocat.database.orchestrator.repos.OrchestratedHarvestProcessRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Scope("prototype")
public class OrchestratedHarvestService {
    Logger logger = LoggerFactory.getLogger(OrchestratedHarvestService.class);

    @Autowired
    OrchestratedHarvestProcessRepo orchestratedHarvestProcessRepo;

    public OrchestratedHarvestProcess createOrchestratedHarvestProcess(String processId) {
        logger.info(String.format("createOrchestratedHarvestProcess, processId: %s", processId));

        Optional<OrchestratedHarvestProcess> job = orchestratedHarvestProcessRepo.findById(processId);
        if (job.isPresent()) {
            logger.info(String.format("createOrchestratedHarvestProcess, processId: %s exists", processId));
            return job.get();
        }

        logger.info(String.format("createOrchestratedHarvestProcess, creating OrchestratedHarvestProcess with processId: %s", processId));

        OrchestratedHarvestProcess result = new OrchestratedHarvestProcess();
        result.setJobId(processId);
        result.setState(OrchestratedHarvestProcessState.CREATED);
        OrchestratedHarvestProcess resultToReturn = orchestratedHarvestProcessRepo.save(result);

        logger.info(String.format("createOrchestratedHarvestProcess, created OrchestratedHarvestProcess processId: %s", processId));

        return resultToReturn;
    }

    public List<OrchestratedHarvestProcess> getOutstandingProcesses() throws Exception {
        return orchestratedHarvestProcessRepo.findOutstanding();
    }
}
