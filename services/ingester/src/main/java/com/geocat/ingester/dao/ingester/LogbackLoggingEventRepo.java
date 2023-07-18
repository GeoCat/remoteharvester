package com.geocat.ingester.dao.ingester;

import com.geocat.ingester.model.ingester.LogbackLoggingEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;


public interface LogbackLoggingEventRepo extends CrudRepository<LogbackLoggingEvent, Long> {
    List<LogbackLoggingEvent> findByJmsCorrelationIdOrderByTimestmp(String jms_correlation_id);

    @Query(value = "select l from LogbackLoggingEvent l  where l.jmsCorrelationId= ?1 and (l.referenceFlag = 2 or l.referenceFlag =3) order by l.timestmp")
    List<LogbackLoggingEvent> findExceptions(String jms_correlation_id);
}
