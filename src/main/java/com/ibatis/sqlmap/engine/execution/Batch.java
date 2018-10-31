package com.ibatis.sqlmap.engine.execution;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ibatis.common.Objects;
import com.ibatis.common.jdbc.exception.NestedSQLException;
import com.ibatis.sqlmap.client.BatchResult;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;
import com.ibatis.sqlmap.engine.mapping.statement.StatementType;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.scope.SessionScope;
import com.ibatis.sqlmap.engine.scope.StatementScope;

/**
 * new Batch
 */
public class Batch {
    final boolean debug;
    private boolean cleanup;

    private Map<Object, BatchResult> batchResultList = new LinkedHashMap<Object, BatchResult>();
    private int totalSize;
    private int totalRows;
    final int batchSize;
    private Map<ExecuteNotifier, String> statementSqls = new HashMap<ExecuteNotifier, String>();

    @Override
    public String toString() {
        return "Batch total size: " + totalSize + ", total affected rows: " + totalRows + ", sql count: "
            + batchResultList.size();
    }

    public int getTotalRows() {
        return totalRows;
    }

    /**
     * Create a new batch
     */
    public Batch(int batchSize, boolean debug) {
        this.debug = debug;
        this.batchSize = batchSize;
        this.totalSize = 0;
    }

    /**
     * Getter for the batch size
     * 
     * @return - the batch size
     */
    public int getSize() {
        return totalSize;
    }

    public boolean isCleanup() {
        return cleanup;
    }

    /**
     * Add a prepared statement to the batch
     * 
     * @param statementScope
     *            - the request scope
     * @param conn
     *            - the database connection
     * @param sql
     *            - the SQL to add
     * @param parameters
     *            - the parameters for the SQL
     * @return
     * @throws SQLException
     *             - if the prepare for the SQL fails
     */
    public void addBatch(StatementScope statementScope, Connection conn, String sql, Object[] parameters)
        throws SQLException {
        MappedStatement ms = statementScope.getStatement();
        boolean isCall = ms.getStatementType() == StatementType.PROCEDURE;
        statementSqls.put(ms, sql);
        Object key = Objects.getKey(ms.getId(), sql);

        BatchResult br = batchResultList.get(key);
        if (br == null) {
            br = new BatchResult(batchSize, sql, statementScope.getErrorContext());
            batchResultList.put(key, br);
        }
        PreparedStatement ps = br.getPreparedStatement();
        if (ps == null) {
            if (isCall) {
                ps = DefaultSqlExecutor.prepareCall(statementScope.getSession(), conn, sql);
            } else {
                ps = DefaultSqlExecutor.prepareStatement(statementScope.getSession(), conn, sql, false);
            }
            DefaultSqlExecutor.setStatementTimeout(ms, ps);
            br.setPreparedStatement(ps);
        }
        Object[] args = statementScope.getParameterMap().setParameters(statementScope, ps, parameters);
        ps.addBatch();

        br.total++;
        br.addArgs(args);
        totalSize++;
    }

    /**
     * Execute the current session's batch
     * 
     * @return - the number of rows updated
     * @throws SQLException
     *             - if the batch fails
     */
    public int executeBatch() throws SQLException {
        int totalRowCount = 0;
        for (BatchResult br : batchResultList.values()) {
            PreparedStatement ps = br.getPreparedStatement();
            int[] rowCounts = null;
            try {
                rowCounts = ps.executeBatch();
            } catch (BatchUpdateException e) {
                ErrorContext ec = br.getErrorContext();
                ec.setCause(e);
                int[] uc = e.getUpdateCounts();
                if (uc != null) {
                    if (uc.length == br.total) {
                        // the driver continues to process commands after an error
                        int i = 0;
                        for (; i < uc.length; i++) {
                            if (uc[i] == Statement.EXECUTE_FAILED) {
                                break;
                            }
                        }

                        ec.setArgs(br.getArgs(i));
                        ec.setBatchInfo("Batch failed at: " + (i + 1) + " of " + br.total);
                    } else {
                        ec.setArgs(br.getArgs(uc.length));
                        ec.setBatchInfo("Batch failed at: " + (uc.length + 1) + " of " + br.total);
                    }
                } else {
                    // shit driver: no error index
                    ec.setArgs(br.getArgs(-1));
                    ec.setBatchInfo("Batch error: " + "total = " + br.total);
                }
                throw new NestedSQLException(ec.toString(), e.getSQLState(), e.getErrorCode(), e);
            }
            int rows = 0;
            for (int j = 0; j < rowCounts.length; j++) {
                if (rowCounts[j] == Statement.SUCCESS_NO_INFO) {
                    // do nothing
                } else if (rowCounts[j] == Statement.EXECUTE_FAILED) {
                    ErrorContext ec = br.getErrorContext();
                    ec.setBatchInfo("Batch failed: " + (j + 1) + " of " + br.total);
                    ec.setArgs(br.getArgs(j));
                    ec.setCause("The batched statement: " + (j + 1) + " failed to execute.");
                    throw new SQLException(ec.toString());
                } else {
                    rows += rowCounts[j];
                    totalRowCount += rowCounts[j];
                }
            }
            br.totalRows = rows;
        }
        totalRows = totalRowCount;
        return totalRowCount;
    }

    /**
     * Batch execution method that returns all the information the driver has to offer.
     * 
     * @return a List of BatchResult objects
     * @throws BatchException
     *             (an SQLException sub class) if any nested batch fails
     * @throws SQLException
     *             if a database access error occurs, or the drive does not support batch statements
     * @throws BatchException
     *             if the driver throws BatchUpdateException
     */
    public List<BatchResult> executeBatchDetailed() throws SQLException, BatchException {
        List<BatchResult> answer = new ArrayList<BatchResult>();
        int totalRowCount = 0;
        int i = 0;
        for (BatchResult br : batchResultList.values()) {
            PreparedStatement ps = br.getPreparedStatement();
            try {
                br.setUpdateCounts(ps.executeBatch());
            } catch (BatchUpdateException e) {
                ErrorContext ec = br.getErrorContext();
                ec.setCause(e);
                int[] uc = e.getUpdateCounts();
                if (uc != null) {
                    if (uc.length == br.total) {
                        // the driver continues to process commands after an error
                        int j = 0;
                        for (; j < uc.length; j++) {
                            if (uc[i] == Statement.EXECUTE_FAILED) {
                                break;
                            }
                        }

                        ec.setArgs(br.getArgs(i));
                        ec.setBatchInfo("Batch(" + (i + 1) + ") failed at: " + (j + 1) + " of " + br.total);
                    } else {
                        ec.setArgs(br.getArgs(uc.length));
                        ec.setBatchInfo("Batch(" + (i + 1) + ") failed at: " + (uc.length + 1) + " of " + br.total);
                    }
                } else {
                    ec.setArgs(br.getArgs(-1));
                    ec.setBatchInfo("Batch(" + (i + 1) + ") error: " + "total = " + br.total);
                }

                throw new BatchException(ec.toString(), e, answer, br.getStatementId(), br.getSql());
            }

            int rows = 0;
            int[] rowCounts = br.getUpdateCounts();
            for (int j = 0; j < rowCounts.length; j++) {
                if (rowCounts[j] == Statement.SUCCESS_NO_INFO) {
                    // do nothing
                } else if (rowCounts[j] == Statement.EXECUTE_FAILED) {
                    // do nothing
                } else {
                    rows += rowCounts[j];
                    totalRowCount += rowCounts[j];
                }
            }
            br.totalRows = rows;
            answer.add(br);
            i++;
        }
        totalRows = totalRowCount;
        return answer;
    }

    public boolean hasNotifier(ExecuteNotifier en) {
        return statementSqls != null && statementSqls.containsKey(en);
    }

    /**
     * Close all the statements in the batch and clear all the statements
     * 
     * @param sessionScope
     */
    public void cleanupBatch(SessionScope sessionScope) {
        this.cleanup = true;
        for (BatchResult br : batchResultList.values()) {
            DefaultSqlExecutor.closeStatement(sessionScope, br.getPreparedStatement());
            br.setPreparedStatement(null);
        }
        if (!debug) {
            batchResultList.clear();
        }
        if (statementSqls != null) {
            Long timestamp = System.currentTimeMillis();
            for (ExecuteNotifier en : statementSqls.keySet()) {
                en.notifyListeners(timestamp);
            }
            if (!debug) {
                statementSqls.clear();
                statementSqls = null;
            }
        }
    }

    public List<ErrorContext> popErrorContexts() {
        List<ErrorContext> list = new ArrayList<ErrorContext>();
        for (BatchResult br : batchResultList.values()) {
            ErrorContext ec = br.getErrorContext();
            ec.setBatchInfo("Batch size: " + br.total + ", affected rows: " + br.totalRows);
            ec.setArgs(br.getArgs(-1));
            list.add(ec);
        }
        batchResultList.clear();
        return list;
    }

    public Map<ExecuteNotifier, String> popBatchedMap() {
        Map<ExecuteNotifier, String> ret = statementSqls;
        statementSqls = null;
        return ret;
    }

}
