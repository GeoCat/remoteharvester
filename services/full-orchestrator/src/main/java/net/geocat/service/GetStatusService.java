/*
 *  =============================================================================
 *  ===  Copyright (C) 2021 Food and Agriculture Organization of the
 *  ===  United Nations (FAO-UN), United Nations World Food Programme (WFP)
 *  ===  and United Nations Environment Programme (UNEP)
 *  ===
 *  ===  This program is free software; you can redistribute it and/or modify
 *  ===  it under the terms of the GNU General Public License as published by
 *  ===  the Free Software Foundation; either version 2 of the License, or (at
 *  ===  your option) any later version.
 *  ===
 *  ===  This program is distributed in the hope that it will be useful, but
 *  ===  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  ===  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  ===  General Public License for more details.
 *  ===
 *  ===  You should have received a copy of the GNU General Public License
 *  ===  along with this program; if not, write to the Free Software
 *  ===  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *  ===
 *  ===  Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 *  ===  Rome - Italy. email: geonetwork@osgeo.org
 *  ===
 *  ===  Development of this program was financed by the European Union within
 *  ===  Service Contract NUMBER – 941143 – IPR – 2021 with subject matter
 *  ===  "Facilitating a sustainable evolution and maintenance of the INSPIRE
 *  ===  Geoportal", performed in the period 2021-2023.
 *  ===
 *  ===  Contact: JRC Unit B.6 Digital Economy, Via Enrico Fermi 2749,
 *  ===  21027 Ispra, Italy. email: JRC-INSPIRE-SUPPORT@ec.europa.eu
 *  ==============================================================================
 */

package net.geocat.service;

import net.geocat.database.orchestrator.entities.LogbackLoggingEvent;
import net.geocat.database.orchestrator.entities.LogbackLoggingEventException;
import net.geocat.database.orchestrator.entities.OrchestratedHarvestProcess;
import net.geocat.database.orchestrator.entities.OrchestratedHarvestProcessState;
import net.geocat.database.orchestrator.repos.LogbackLoggingEventExceptionRepo;
import net.geocat.database.orchestrator.repos.LogbackLoggingEventRepo;
import net.geocat.database.orchestrator.repos.OrchestratedHarvestProcessRepo;
import net.geocat.model.HarvestStatus;
import net.geocat.model.IngestStatus;
import net.geocat.model.LinkCheckStatus;
import net.geocat.model.OrchestratedHarvestProcessStatus;
import net.geocat.service.exernalservices.HarvesterService;
import net.geocat.service.exernalservices.IngesterService;
import net.geocat.service.exernalservices.LinkCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class GetStatusService {

    public static Boolean DEFAULT_QUICK = Boolean.FALSE;

    Logger logger = LoggerFactory.getLogger(GetStatusService.class);


    @Autowired
    LogbackLoggingEventRepo logbackLoggingEventRepo;

    @Autowired
    LogbackLoggingEventExceptionRepo logbackLoggingEventExceptionRepo;

    @Autowired
    OrchestratedHarvestProcessRepo orchestratedHarvestProcessRepo;

    @Autowired
    HarvesterService harvesterService;

    @Autowired
    LinkCheckService linkCheckService;

    @Autowired
    IngesterService ingesterService;

    public OrchestratedHarvestProcessStatus getStatus(String processID, Boolean quick) throws Exception {

        logger.debug("getstatus called for " + processID + ", with quick=" + quick);

        if (quick == null) {
            quick = DEFAULT_QUICK;
        }

        Optional<OrchestratedHarvestProcess> jobOptional = orchestratedHarvestProcessRepo.findById(processID);

        if (jobOptional.isPresent()) {
            OrchestratedHarvestProcess job = jobOptional.get();

            OrchestratedHarvestProcessStatus result = new OrchestratedHarvestProcessStatus(processID, job.getState());

            if (!quick) {
                if (!StringUtils.isEmpty(job.getHarvesterJobId())) {
                    HarvestStatus harvestState = harvesterService.getHarvestState(job.getHarvesterJobId(), quick);
                    result.setHarvestStatus(harvestState);
                }

                if (!StringUtils.isEmpty(job.getLinkCheckJobId())) {
                    LinkCheckStatus linkCheckState = linkCheckService.getLinkCheckState(job.getLinkCheckJobId(), quick);
                    result.setLinkCheckStatus(linkCheckState);
                }

                if (!StringUtils.isEmpty(job.getInjectJobId())) {
                    IngestStatus ingestState = ingesterService.getIngestState(job.getInjectJobId(), quick);
                    result.setIngestStatus(ingestState);

                }

                setupErrorMessages(result);
            }
            return result;
        } else {
            OrchestratedHarvestProcessStatus result = new OrchestratedHarvestProcessStatus(processID, null);
            result.setOrchestratedHarvestProcessState(OrchestratedHarvestProcessState.ERROR);
            result.errorMessage.add(String.format("Process id '%s' not found", processID));

            return result;
        }
    }

    private void setupErrorMessages(OrchestratedHarvestProcessStatus result) {
        List<LogbackLoggingEvent> exceptionLogMessages = logbackLoggingEventRepo.findExceptions(result.getProcessID());

        for (LogbackLoggingEvent exceptionLogMessage : exceptionLogMessages) {

            List<LogbackLoggingEventException> exceptionlines = logbackLoggingEventExceptionRepo.findByEventIdOrderByI(exceptionLogMessage.eventId);

            List<String> ex_messages = exceptionlines.stream()
                    .filter(x -> x.getI() == 0)
                    .sorted(Comparator.comparingInt(x -> x.getCausedByDepth()))
                    .map(x -> x.getTraceLine())
                    .collect(Collectors.toList());

            result.errorMessage.add(
                    String.join("\n  -> ", ex_messages)
            );

            Map<Short, List<LogbackLoggingEventException>> grouped = exceptionlines.stream()
                    .collect(Collectors.groupingBy(LogbackLoggingEventException::getCausedByDepth));

            List<Map.Entry<Short, List<LogbackLoggingEventException>>> exceptions = grouped.entrySet().stream()
                    .sorted(Comparator.comparingInt(x -> x.getKey()))
                    .collect(Collectors.toList());

            List<String> single_stacktrace = exceptions.stream()
                    .map(x -> makeString(x.getValue()))
                    .collect(Collectors.toList());

            result.stackTraces.add(single_stacktrace);
        }
    }


    public String makeString(List<LogbackLoggingEventException> items) {
        List<String> strs = items.stream()
                .sorted(Comparator.comparingInt(x -> x.getI()))
                .map(x -> x.getTraceLine())
                .collect(Collectors.toList());
        return String.join("\n   ", strs);
    }

}
