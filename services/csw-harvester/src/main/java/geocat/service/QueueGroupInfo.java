package geocat.service;

public class QueueGroupInfo {
    private String queueGroupName;
    private int numberOfQueues;
    private int nextQueueNumber;
    private int parallelism;

    public QueueGroupInfo(String queueGroupName, int numberOfQueues, int parallelism) {
        this.queueGroupName = queueGroupName;
        this.numberOfQueues = numberOfQueues;
        this.parallelism = parallelism;
        this.nextQueueNumber = 0;
    }

    public QueueInfo currentQueueInfo() {
        return new QueueInfo(this, nextQueueNumber);
    }

    static Object lockobject = new Object();

    public void useNextQueue() {
        synchronized (lockobject) {
            nextQueueNumber++;
            if (nextQueueNumber >= numberOfQueues)
                nextQueueNumber = 0;
        }
    }

    public QueueInfo queueInfo(int subQueueNumber) {
        return new QueueInfo(this, subQueueNumber);
    }

    public String queueName(int subQueueNumber) {
        return queueGroupName + subQueueNumber;
    }


    public String getQueueGroupName() {
        return queueGroupName;
    }

    public void setQueueGroupName(String queueGroupName) {
        this.queueGroupName = queueGroupName;
    }

    public int getNumberOfQueues() {
        return numberOfQueues;
    }

    public void setNumberOfQueues(int numberOfQueues) {
        this.numberOfQueues = numberOfQueues;
    }

    public int getNextQueueNumber() {
        return nextQueueNumber;
    }

    public void setNextQueueNumber(int nextQueueNumber) {
        this.nextQueueNumber = nextQueueNumber;
    }

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }
}
