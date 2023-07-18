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

package net.geocat.dblogging.service;


import net.geocat.database.orchestrator.entities.LogbackLoggingEvent;
import net.geocat.database.orchestrator.entities.LogbackLoggingEventException;
import net.geocat.database.orchestrator.repos.LogbackLoggingEventExceptionRepo;
import net.geocat.database.orchestrator.repos.LogbackLoggingEventRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class GetLogService {

    @Autowired
    LogbackLoggingEventRepo logbackLoggingEventRepo;

    @Autowired
    LogbackLoggingEventExceptionRepo logbackLoggingEventExceptionRepo;

    public List<LogLine> queryLogByProcessID(String processID) {
        List<LogbackLoggingEvent> events = logbackLoggingEventRepo.findByJmsCorrelationIdOrderByTimestmp(processID);
        List<LogLine> result = new ArrayList<>();
        for (LogbackLoggingEvent event : events) {
            result.add(create(event));
        }
        return result;
    }

    public LogLine create(LogbackLoggingEvent event) {
        LogLine result = new LogLine();
        result.when = (Instant.ofEpochSecond(event.timestmp / 1000)).toString();
        result.isException = (event.referenceFlag == 2) || (event.referenceFlag == 3);
        result.level = event.levelString;
        result.message = event.formattedMessage;
        result.processID = event.jmsCorrelationId;
        result.threadName = event.threadName;

        //exception
        if (result.isException) {
            List<LogbackLoggingEventException> exceptionlines = logbackLoggingEventExceptionRepo.findByEventIdOrderByI(event.eventId);
            List<String> ex_messages = exceptionlines.stream()
                    .filter(x -> x.getI() == 0)
                    .sorted(Comparator.comparingInt(x -> x.getCausedByDepth()))
                    .map(x -> x.getTraceLine())
                    .collect(Collectors.toList());

            result.message = String.join("\n  -> ", ex_messages);

            Map<Short, List<LogbackLoggingEventException>> grouped = exceptionlines.stream()
                    .collect(Collectors.groupingBy(LogbackLoggingEventException::getCausedByDepth));

            List<Map.Entry<Short, List<LogbackLoggingEventException>>> exceptions = grouped.entrySet().stream()
                    .sorted(Comparator.comparingInt(x -> x.getKey()))
                    .collect(Collectors.toList());

            List<String> single_stacktrace = exceptions.stream()
                    .map(x -> makeString(x.getValue()))
                    .collect(Collectors.toList());

            result.stackTraces = single_stacktrace.toArray(new String[0]);

        }
        return result;
    }

    public String makeString(List<LogbackLoggingEventException> items) {
        List<String> strs = items.stream()
                .sorted(Comparator.comparingInt(x -> x.getI()))
                .map(x -> x.getTraceLine())
                .collect(Collectors.toList());
        return String.join("\n   ", strs);
    }

}
