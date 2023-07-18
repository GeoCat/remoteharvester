package com.geocat.ingester.model.ingester;

import javax.persistence.*;

//taken from;
// https://raw.githubusercontent.com/qos-ch/logback/master/logback-classic/src/main/resources/ch/qos/logback/classic/db/script/postgresql.sql


@Entity
@Table(name = "logging_event"
        ,
        indexes = {
                @Index(
                        name = "logging_correlationid_idx",
                        columnList = "jmsCorrelationId",
                        unique = false
                )
        }
)
public class LogbackLoggingEvent {

    @Column(columnDefinition = "bigint")
    public long timestmp;

    @Column(columnDefinition = "text")
    public String formattedMessage;

    @Column(columnDefinition = "varchar(254)")
    public String loggerName;

    @Column(columnDefinition = "varchar(254)")
    public String levelString;

    @Column(columnDefinition = "varchar(254)")
    public String threadName;

    @Column(columnDefinition = "smallint")
    public short referenceFlag;

    @Column(columnDefinition = "varchar(254)")
    public String arg0;
    @Column(columnDefinition = "varchar(254)")
    public String arg1;
    @Column(columnDefinition = "varchar(254)")
    public String arg2;
    @Column(columnDefinition = "varchar(254)")
    public String arg3;

    @Column(columnDefinition = "varchar(254)")
    public String callerFilename;

    @Column(columnDefinition = "varchar(254)")
    public String callerClass;

    @Column(columnDefinition = "varchar(254)")
    public String callerMethod;

    @Column(columnDefinition = "char(4)")
    public String callerLine;

    @Column(columnDefinition = "varchar(254)")
    public String jmsCorrelationId;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT")
    public long eventId;


}
