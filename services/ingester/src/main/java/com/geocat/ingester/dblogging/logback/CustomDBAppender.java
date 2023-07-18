package com.geocat.ingester.dblogging.logback;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import static ch.qos.logback.core.db.DBHelper.closeStatement;

public class CustomDBAppender extends ch.qos.logback.classic.db.DBAppender {


    String SQL = "update logging_event  set jms_correlation_id=? where event_id = ?";

    String SQL_ERROR = "INSERT INTO logging_event_exception (event_id, i, caused_by_depth, trace_line) VALUES (?, ?, ?, ?)";

    @Override
    protected void insertProperties(Map<String, String> mergedMap, Connection connection, long eventId)
            throws SQLException {
        if (!mergedMap.containsKey("JMSCorrelationID"))
            return; // nothing to do

        String value = mergedMap.get("JMSCorrelationID");
        if ((value == null) || (value.isEmpty()))
            return; //nothing to do

        PreparedStatement pstmt = connection.prepareStatement(SQL);
        pstmt.setString(1, value);
        pstmt.setLong(2, eventId);

        int affectedRows = pstmt.executeUpdate();
    }

    // throwable;
    // original throwable will have causedByIndex = 0
    // message is always i=0
    protected void insertThrowable(IThrowableProxy tp, Connection connection, long eventId) throws SQLException {

        PreparedStatement exceptionStatement = null;
        try {
            exceptionStatement = connection.prepareStatement(SQL_ERROR);

            short causedByIndex = 0;
            while (tp != null) {
                buildExceptionStatement(tp, exceptionStatement, eventId, causedByIndex);
                causedByIndex++;
                tp = tp.getCause();
            }

            if (cnxSupportsBatchUpdates) {
                exceptionStatement.executeBatch();
            }
        } finally {
            closeStatement(exceptionStatement);
        }

    }


    void buildExceptionStatement(IThrowableProxy tp,
                                 PreparedStatement insertExceptionStatement,
                                 long eventId,
                                 short causedByIndex) throws SQLException {

        short index = 0;
        StringBuilder buf = new StringBuilder();
        ThrowableProxyUtil.subjoinFirstLine(buf, tp);
        updateExceptionStatement(insertExceptionStatement, buf.toString(), causedByIndex, index++, eventId);

        int commonFrames = tp.getCommonFrames();
        StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
        for (int i = 0; i < stepArray.length - commonFrames; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(CoreConstants.TAB);
            ThrowableProxyUtil.subjoinSTEP(sb, stepArray[i]);
            updateExceptionStatement(insertExceptionStatement, sb.toString(), causedByIndex, index++, eventId);
        }

        if (commonFrames > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(CoreConstants.TAB).append("... ").append(commonFrames).append(" common frames omitted");
            updateExceptionStatement(insertExceptionStatement, sb.toString(), causedByIndex, index++, eventId);
        }


    }


    /**
     * Add an exception statement either as a batch or execute immediately if
     * batch updates are not supported.
     */
    void updateExceptionStatement(PreparedStatement exceptionStatement, String txt, short causedByIndex, short i, long eventId) throws SQLException {
        exceptionStatement.setLong(1, eventId);
        exceptionStatement.setShort(2, i);
        exceptionStatement.setShort(3, causedByIndex);
        exceptionStatement.setString(4, txt);

        if (cnxSupportsBatchUpdates) {
            exceptionStatement.addBatch();
        } else {
            exceptionStatement.execute();
        }
    }


}
