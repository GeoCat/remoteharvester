package geocat.service;

import geocat.database.entities.EndpointJob;
import geocat.database.entities.HarvestJob;
import geocat.database.repos.MetadataRecordRepo;
import geocat.database.service.EndpointJobService;
import geocat.database.service.HarvestJobService;
import geocat.dblogging.entities.LogbackLoggingEvent;
import geocat.dblogging.entities.LogbackLoggingEventException;
import geocat.dblogging.repos.LogbackLoggingEventExceptionRepo;
import geocat.dblogging.repos.LogbackLoggingEventRepo;
import geocat.model.EndpointStatus;
import geocat.model.HarvestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class GetStatusService {

    public static Boolean DEFAULT_QUICK = Boolean.FALSE;


    @Autowired
    HarvestJobService harvestJobService;

    @Autowired
    EndpointJobService endpointJobService;

    @Autowired
    MetadataRecordRepo metadataRecordRepo;

    @Autowired
    LogbackLoggingEventRepo logbackLoggingEventRepo;

    @Autowired
    LogbackLoggingEventExceptionRepo logbackLoggingEventExceptionRepo;

    public HarvestStatus getStatus(String processId, Boolean quick) {
        if (quick == null) {
            quick = DEFAULT_QUICK;
        }

        HarvestStatus result;

        Optional<HarvestJob> jobOptional = harvestJobService.getById(processId);

        if (jobOptional.isPresent()) {
            List<EndpointJob> endpointJobs = endpointJobService.findAll(processId);

            result = new HarvestStatus(jobOptional.get());
            if (!quick) {
                setupErrorMessages(result);
                for (EndpointJob endpointJob : endpointJobs) {
                    long numberReceived = computeNumberReceived(endpointJob);
                    result.endpoints.add(new EndpointStatus(endpointJob, (int) numberReceived));
                }
            }
        } else {
            result = HarvestStatus.createHarvestStatusNoProcessId(processId);
        }


        return result;
    }

    private void setupErrorMessages(HarvestStatus result) {
        List<LogbackLoggingEvent> exceptionLogMessages = logbackLoggingEventRepo.findExceptions(result.processID);

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

    private long computeNumberReceived(EndpointJob endpointJob) {
        return metadataRecordRepo.countByEndpointJobId(endpointJob.getEndpointJobId());
    }
}
