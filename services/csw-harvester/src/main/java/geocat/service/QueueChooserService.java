package geocat.service;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Scope("prototype")
public class QueueChooserService {

    static int MAX_RECORDS_FOR_LARGE = 2000;

    static Map<String, QueueGroupInfo> queueGroupsMap = new HashMap<>();
    static List<QueueGroupInfo> queueGroups = new ArrayList<>();

    static {
        QueueGroupInfo normal = new QueueGroupInfo("GET_RECORDS_QUEUE_NORMAL_", 10, 1);
        QueueGroupInfo large = new QueueGroupInfo("GET_RECORDS_QUEUE_LARGE_", 3, 1);
        QueueGroupInfo parallel_2 = new QueueGroupInfo("GET_RECORDS_QUEUE_PARALLEL2_", 3, 2);
        QueueGroupInfo parallel_3 = new QueueGroupInfo("GET_RECORDS_QUEUE_PARALLEL3_", 3, 3);
        QueueGroupInfo parallel_4 = new QueueGroupInfo("GET_RECORDS_QUEUE_PARALLEL4_", 3, 4);

        queueGroups.add(normal);
        queueGroups.add(large);
        queueGroups.add(parallel_2);
        queueGroups.add(parallel_3);
        queueGroups.add(parallel_4);


        queueGroupsMap.put("normal", normal);
        queueGroupsMap.put("large", large);
        queueGroupsMap.put("parallel_2", parallel_2);
        queueGroupsMap.put("parallel_3", parallel_3);
        queueGroupsMap.put("parallel_4", parallel_4);
    }

    Pattern parallelPattern = Pattern.compile("^PARALLEL(\\d)$");

    public List<QueueInfo> enumerateAllQueues() {
        List<QueueInfo> result = new ArrayList<>();
        for (QueueGroupInfo info : queueGroupsMap.values()) {
            for (int t = 0; t < info.getNumberOfQueues(); t++) {
                result.add(info.queueInfo(t));
            }
        }
        return result;
    }

    public String chooseQueue(String hint, int expectedNumberOfRecords) throws Exception {
        if ((hint == null) || (hint.isEmpty()) || (!parallelPattern.matcher(hint).matches())) {
            //we choose
            if (expectedNumberOfRecords < MAX_RECORDS_FOR_LARGE) {
                return chooseQueueByGroup(queueGroupsMap.get("normal")); // always choose the first one
            }
            return chooseQueueByGroup(queueGroupsMap.get("large")); // always choose the 2nd one
        }
        //user specified something - we should choose something
        Matcher m = parallelPattern.matcher(hint);
        m.find();
        int parallellism = Integer.parseInt(m.group(1));
        if (parallellism <= 1)
            throw new Exception("requested parallelism of 0 or 1 - must be 2+!");
        if (!queueGroupsMap.containsKey("parallel_" + parallellism))
            throw new Exception("requested parallelism too high!");

        return chooseQueueByGroup(queueGroupsMap.get("parallel_" + parallellism));
    }

    static Object lockobject = new Object();

    public String chooseQueueByGroup(QueueGroupInfo groupInfo) {
        synchronized (lockobject) {
            QueueInfo result = groupInfo.currentQueueInfo();
            groupInfo.useNextQueue();
            return result.queueName();
        }
    }
}
