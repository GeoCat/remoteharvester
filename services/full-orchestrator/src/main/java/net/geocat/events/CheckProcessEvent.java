package net.geocat.events;

public class CheckProcessEvent extends Event {


    public CheckProcessEvent(String orchestratorProcessId) {
        super(orchestratorProcessId);
    }

    public CheckProcessEvent() {
    }

    //----


    //---


    @Override
    public String toString() {
        return "CheckProcessEvent{" +
                "orchestratorProcessId='" + getProcessID() + '\'' +
                '}';
    }
}
