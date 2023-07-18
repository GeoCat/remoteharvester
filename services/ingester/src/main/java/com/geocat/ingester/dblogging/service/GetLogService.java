package com.geocat.ingester.dblogging.service;

import com.geocat.ingester.dao.ingester.LogbackLoggingEventExceptionRepo;
import com.geocat.ingester.dao.ingester.LogbackLoggingEventRepo;
import com.geocat.ingester.model.ingester.LogbackLoggingEvent;
import com.geocat.ingester.model.ingester.LogbackLoggingEventException;
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
