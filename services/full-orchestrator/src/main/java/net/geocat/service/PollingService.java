package net.geocat.service;


import net.geocat.database.orchestrator.entities.OrchestratedHarvestProcess;
import net.geocat.database.orchestrator.repos.OrchestratedHarvestProcessRepo;
import net.geocat.events.CheckProcessEvent;
import net.geocat.events.EventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class PollingService {

    Logger logger = LoggerFactory.getLogger(PollingService.class);

    @Autowired
    OrchestratedHarvestProcessRepo orchestratedHarvestProcessRepo;

    @Autowired
    EventFactory eventFactory;


    public List<CheckProcessEvent> ping() {
        List<OrchestratedHarvestProcess> items = orchestratedHarvestProcessRepo.findOutstanding();
        List<CheckProcessEvent> result = items.stream()
                .map(x -> eventFactory.createCheckProcessEvent(x.getJobId())).collect(Collectors.toList());
        String summary = String.join(",", items.stream().map(x -> x.getJobId() + "(" + x.getState() + ")").collect(Collectors.toList()));
        logger.debug("PollingService: ping received, checking on " + items.size() + " items - " + summary);

        return result;
    }
}
