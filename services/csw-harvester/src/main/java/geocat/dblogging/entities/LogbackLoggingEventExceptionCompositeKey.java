package geocat.dblogging.entities;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Objects;

public class LogbackLoggingEventExceptionCompositeKey implements Serializable {
    @Column(name = "event_id", columnDefinition = "bigint")
    @Id
    private long eventId;

    @Column(name = "i", columnDefinition = "smallint")
    @Id
    private short i;

    @Column(name = "caused_by_depth", columnDefinition = "smallint")
    private short causedByDepth;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogbackLoggingEventExceptionCompositeKey that = (LogbackLoggingEventExceptionCompositeKey) o;
        return eventId == that.eventId && i == that.i && causedByDepth == that.causedByDepth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, i, causedByDepth);
    }
}
