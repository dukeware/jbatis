/*-
 * Copyright (c) 2008-2010 iBATIS
 * All rights reserved. 
 * LoggingSqlExecutor.java
 * Date: 2011-11-15
 * Author: Song Sun
 */
package org.ibatis.client;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.ibatis.common.RunStats;
import com.ibatis.common.Statsable;
import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.sqlmap.client.BatchResult;
import com.ibatis.sqlmap.client.event.PageHandler;
import com.ibatis.sqlmap.engine.cache.CacheModel;
import com.ibatis.sqlmap.engine.cache.CacheRoot;
import com.ibatis.sqlmap.engine.config.SqlMapConfiguration;
import com.ibatis.sqlmap.engine.execution.Batch;
import com.ibatis.sqlmap.engine.execution.BatchException;
import com.ibatis.sqlmap.engine.execution.DefaultSqlExecutor;
import com.ibatis.sqlmap.engine.execution.ExecuteNotifier;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.statement.RowHandlerCallback;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.scope.SessionScope;
import com.ibatis.sqlmap.engine.scope.StatementScope;

/**
 * A debug SqlExecutor
 * <p>
 * Date: 2011-11-15,22:37:47 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
public class SqlExecutor implements com.ibatis.sqlmap.engine.execution.SqlExecutor, Statsable {
    private static final ILog log = ILogFactory.getLog(SqlExecutor.class);
    final DefaultSqlExecutor delegate;
    SqlMapConfiguration config;
    long threshold = 1000L;
    long page_threshold = 60000L;
    boolean traceSql = false;
    boolean debugSql = false;
    boolean statsSql = false;
    boolean checkSql = false;

    public static class SqlStat implements Comparable<SqlStat> {
        // statement id or sql
        String id;
        String sql;
        // total execution times
        long count;
        // total elapsed time
        long time;

        public SqlStat(String id, String sql, long t) {
            this.id = id;
            this.sql = sql;
            this.time = t;
            this.count = 1;
        }

        public void addCount(long t) {
            count++;
            time += t;
        }

        @Override
        public String toString() {
            return count + ", " + time + " -> " + sql;
        }

        @Override
        public int compareTo(SqlStat o) {
            if (o == null) {
                return -1;
            }
            if (o.count == this.count) {
                return (int) (o.time - time);
            }
            return (int) (o.count - count);
        }
    }

    Map<String, SqlStat> sqlStats = new ConcurrentHashMap<String, SqlStat>();

    public SqlExecutor() {
        this.delegate = new DefaultSqlExecutor(null);
    }

    @Override
    public int executeUpdate(String id, StatementScope statementScope, Connection conn, String sql, Object[] parameters)
        throws SQLException {
        long t = System.currentTimeMillis();
        int rows = 0;
        try {
            rows = delegate.executeUpdate(id, statementScope, conn, sql, parameters);
        } finally {
            t = System.currentTimeMillis() - t;
            statsSql(id, sql, t);
            logSql("executeUpdate", statementScope.getErrorContext(), t, ZERO, NO_LIMIT, rows);
        }
        return rows;
    }

    @Override
    public <T> T executeInsert(String id, StatementScope statementScope, Connection conn, String sql,
        Object[] parameters, ResultMap keyResultMap) throws SQLException {
        long t = System.currentTimeMillis();
        T r = null;
        try {
            r = delegate.executeInsert(id, statementScope, conn, sql, parameters, keyResultMap);
            return r;
        } finally {
            t = System.currentTimeMillis() - t;
            statsSql(id, sql, t);
            logInsert("executeInsert", statementScope.getErrorContext(), t, r);
        }
    }

    @Override
    public int addBatch(StatementScope statementScope, Connection conn, String sql, Object[] parameters)
        throws SQLException {
        long t = System.currentTimeMillis();
        try {
            return delegate.doAddBatch(true, statementScope, conn, sql, parameters);
        } finally {
            Batch batch = statementScope.getSession().getBatch();
            if (batch != null && batch.isCleanup()) { // batch auto executed, so stats it.
                t = System.currentTimeMillis() - t;
                Map<ExecuteNotifier, String> idSqls = batch.popBatchedMap();
                if (idSqls != null) {
                    if (statsSql) {
                        for (ExecuteNotifier en : idSqls.keySet()) {
                            statsSql("~" + en.getId(), idSqls.get(en), t / idSqls.size());
                        }
                    }
                    idSqls.clear();
                }
                logBatch("addBatch", batch.toString(), batch.popErrorContexts(), t, batch.getTotalRows());
            }
        }
    }

    @Override
    public int executeBatch(SessionScope sessionScope) throws SQLException {
        long t = System.currentTimeMillis();
        Batch batch = sessionScope.getBatch();
        try {
            return delegate.executeBatch(sessionScope);
        } finally {
            if (batch != null) {
                t = System.currentTimeMillis() - t;
                Map<ExecuteNotifier, String> idSqls = batch.popBatchedMap();
                if (idSqls != null) {
                    if (statsSql) {
                        for (ExecuteNotifier en : idSqls.keySet()) {
                            statsSql("~" + en.getId(), idSqls.get(en), t / idSqls.size());
                        }
                    }
                    idSqls.clear();
                }
                logBatch("executeBatch", batch.toString(), batch.popErrorContexts(), t, batch.getTotalRows());
            }
        }
    }

    @Override
    public List<BatchResult> executeBatchDetailed(SessionScope sessionScope) throws SQLException, BatchException {
        long t = System.currentTimeMillis();
        Batch batch = sessionScope.getBatch();
        try {
            return delegate.executeBatchDetailed(sessionScope);
        } finally {
            if (batch != null) {
                t = System.currentTimeMillis() - t;
                Map<ExecuteNotifier, String> idSqls = batch.popBatchedMap();
                if (idSqls != null) {
                    if (statsSql) {
                        for (ExecuteNotifier en : idSqls.keySet()) {
                            statsSql("~" + en.getId(), idSqls.get(en), t / idSqls.size());
                        }
                    }
                    idSqls.clear();
                }
                logBatch("executeBatchDetailed", batch.toString(), batch.popErrorContexts(), t, batch.getTotalRows());
            }
        }
    }

    @Override
    public void executeQuery(String id, StatementScope statementScope, Connection conn, String sql, Object[] parameters,
        int skip, int max, RowHandlerCallback callback) throws SQLException {
        long t = System.currentTimeMillis();
        Integer rows = null;
        try {
            delegate.executeQuery(id, statementScope, conn, sql, parameters, skip, max, callback);
            rows = callback.getRowHandler().getRows();
        } finally {
            t = System.currentTimeMillis() - t;
            statsSql(id, sql, t);
            logSql("executeQuery", statementScope.getErrorContext(), t, skip, max, rows);
        }
    }

    @Override
    public void executeQueryPage(String id, StatementScope statementScope, Connection conn, String sql,
        Object[] parameters, int skip, int max, RowHandlerCallback callback, PageHandler pageHandler)
        throws SQLException {
        long t = System.currentTimeMillis();
        Integer rows = null;
        try {
            delegate.executeQueryPage(id, statementScope, conn, sql, parameters, skip, max, callback, pageHandler);
            rows = callback.getRowHandler().getRows();
        } finally {
            t = System.currentTimeMillis() - t;
            statsSql(id, sql, t);
            logSql("executeQueryPage", statementScope.getErrorContext(), t, skip, max, rows);
        }
    }

    @Override
    public int executeUpdateProcedure(String id, StatementScope statementScope, Connection conn, String sql,
        Object[] parameters) throws SQLException {
        long t = System.currentTimeMillis();
        int r = 0;
        try {
            r = delegate.executeUpdateProcedure(id, statementScope, conn, sql, parameters);
            return r;
        } finally {
            t = System.currentTimeMillis() - t;
            statsSql(id, sql, t);
            logSql("executeUpdateProcedure", statementScope.getErrorContext(), t, ZERO, NO_LIMIT, r);
        }
    }

    @Override
    public void executeQueryProcedure(String id, StatementScope statementScope, Connection conn, String sql,
        Object[] parameters, int skip, int max, RowHandlerCallback callback) throws SQLException {
        long t = System.currentTimeMillis();
        Integer rows = null;
        try {
            delegate.executeQueryProcedure(id, statementScope, conn, sql, parameters, skip, max, callback);
            rows = callback.getRowHandler().getRows();
        } finally {
            t = System.currentTimeMillis() - t;
            statsSql(id, sql, t);
            logSql("executeQueryProcedure", statementScope.getErrorContext(), t, skip, max, rows);
        }
    }

    void statsSql(String id, String sql, long t) {
        if (statsSql) {
            String key = id;

            if (key == null) {
                key = sql;
            }
            SqlStat s = sqlStats.get(key);
            if (s == null) {
                s = new SqlStat(key, sql, t);
                sqlStats.put(key, s);
            } else {
                s.addCount(t);
            }
        }
    }

    @Override
    public void cleanup(SessionScope sessionScope) {
        Batch batch = sessionScope.getBatch();
        delegate.cleanup(sessionScope);
        if (batch != null) {
            Map<ExecuteNotifier, String> idSqls = batch.popBatchedMap();
            if (idSqls != null) {
                idSqls.clear();
            }
        }
    }

    void logBatch(String methodName, String batchInfo, List<ErrorContext> list, long t, int rows) {
        if (t > threshold) {
            ErrorContext ec = new ErrorContext();
            ec.setBatchInfo(batchInfo);
            ec.setDebugInfo("Batch execution time = " + t + " > " + threshold);
            log.warn(methodName + "() total rows = " + rows + ec.toStr(list));
        } else if (debugSql) {
            ErrorContext ec = new ErrorContext();
            ec.setBatchInfo(batchInfo);
            log.info(methodName + "() total rows = " + rows + ", time = " + t + ec.toStr(list));
        }
    }

    void logSql(String methodName, ErrorContext ec, long t, int skip, int limit, Integer ret) {
        String skipTotal = "";
        if (limit > 0 && ec.getTotal() > 0) {
            skipTotal = ", skip = " + skip + ", total = " + ec.getTotal();
        }
        if (t > threshold) {
            ec.setDebugInfo("Execution time = " + t + " > " + threshold);
            log.warn(methodName + "() rows = " + ret + skipTotal + ec.toStr());
        } else if (limit > 0 && ec.getTotal() > page_threshold) {
            ec.setDebugInfo("Result set total rows " + ec.getTotal() + " > " + page_threshold);
            log.warn(methodName + "() rows = " + ret + skipTotal + ", time = " + t + ec.toStr());
        } else if (debugSql) {
            log.info(methodName + "() rows = " + ret + skipTotal + ", time = " + t + ec.toStr());
        }
    }

    void logInsert(String methodName, ErrorContext ec, long t, Object ret) {
        if (t > threshold) {
            ec.setDebugInfo("Execution time = " + t + " > " + threshold);
            log.warn(methodName + "() genKey = " + ret + ec.toStr());
        } else if (debugSql) {
            log.info(methodName + "() genKey = " + ret + ", time = " + t + ec.toStr());
        }
    }

    @Override
    public void init(SqlMapConfiguration config, Properties globalProps) {
        this.config = config;
        delegate.init(config, globalProps);
        try {
            String str = globalProps.getProperty("sql_executor_threshold", "1000");
            long t = Long.parseLong(str);
            if (t > 0) {
                threshold = t;
            }
        } catch (Exception e) {
        }
        try {
            String str = globalProps.getProperty("sql_executor_page_threshold", "60000");
            long t = Long.parseLong(str);
            if (t > 0) {
                page_threshold = t;
            }
        } catch (Exception e) {
        }
        try {
            debugSql = traceSql = "true".equals(globalProps.getProperty("sql_executor_trace_sql"));
        } catch (Exception e) {
        }
        try {
            debugSql = "true".equals(globalProps.getProperty("sql_executor_debug_sql"));
        } catch (Exception e) {
        }
        try {
            statsSql = !"false".equals(globalProps.getProperty("sql_executor_stats_sql"));
        } catch (Exception e) {
        }
        try {
            checkSql = "true".equals(globalProps.getProperty("sql_executor_check_sql"));
        } catch (Exception e) {
        }
        log.info("SqlExecutor " + hashCode() + " init() called, threshold=" + threshold + ", debug_sql=" + debugSql
            + ", paging_threshold=" + page_threshold + ", stats_sql=" + statsSql + ", check_sql=" + checkSql);

        RunStats.getInstance().addStat(this);
    }

    @Override
    public boolean isTraceSql() {
        return traceSql;
    }

    @Override
    public boolean isCheckSql() {
        return checkSql;
    }

    @Override
    public boolean isDebugSql() {
        return debugSql;
    }

    @Override
    public String getStatus(String h) {
        StringBuilder buf = new StringBuilder("SqlExecutor " + hashCode() + " :");
        if (h == null) {
            h = "\n";
        } else {
            h = "\n" + h;
        }
        String f = " ----------------------- %12s:  %-4d -------------------------------------";
        List<SqlStat> list = new ArrayList<SqlStat>();
        for (SqlStat ss : sqlStats.values()) {
            if (ss.count > 0) {
                list.add(ss);
            }
        }
        Collections.sort(list);
        if (list.size() > 0) {
            buf.append(h).append(String.format(f, "sql stats", list.size()));
            String th = " %9s %9s  %s";
            buf.append(h).append(String.format(th, "count", "avg.time", "id/sql"));
            String tr = " %9d %9d  %s";
            for (SqlStat ss : list) {
                buf.append(h);
                int timeAvg = (int) (((double) ss.time) / ss.count);
                buf.append(String.format(tr, ss.count, timeAvg, ss.id != null ? ss.id : ss.sql));
            }
        }

        Cache[] caches = config.getDelegate().getCacheModels();
        Arrays.sort(caches, new Comparator<Cache>() {
            @Override
            public int compare(Cache o1, Cache o2) {
                if (o2.getRequests() == o1.getRequests()) {
                    return (int) (o2.getHits() - o2.getHits());
                }
                return (int) (o2.getRequests() - o1.getRequests());
            }
        });
        if (caches.length > 0 && caches[0].getRequests() > 0) {
            buf.append(h).append(String.format(f, "cache stats", caches.length));
            String th = " %9s %9s %9s %9s %9s  %s";
            buf.append(h).append(String.format(th, "requests", "hits", "flushs", "age", "age.max", "id"));
            String tr = " %9d %9d %9d %9d %9d  %s";
            for (Cache c : caches) {
                long max = 0;
                if (c instanceof CacheModel) {
                    max = ((CacheModel) c).getFlushIntervalSeconds();
                }
                buf.append(h);
                buf.append(String.format(tr, c.getRequests(), c.getHits(), c.getFlushs(), c.getPeriodMillis() / 1000L,
                    max, c.getId()));
            }
        }

        CacheRoot[] cacheRoots = config.getDelegate().getCacheRoots().getRoots();
        Arrays.sort(cacheRoots, new Comparator<CacheRoot>() {
            @Override
            public int compare(CacheRoot o1, CacheRoot o2) {
                if (o1.isReal() != o2.isReal()) {
                    if (o1.isReal()) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
                if (o2.getFlushCount() == o1.getFlushCount()) {
                    return (int) (o2.getLastFlush() - o2.getLastFlush());
                }
                return (int) (o2.getFlushCount() - o1.getFlushCount());
            }
        });

        if (cacheRoots.length > 0 && cacheRoots[0].getLastFlush() > 0) {
            long t = System.currentTimeMillis();
            buf.append(h).append(String.format(f, "roots stats", cacheRoots.length));
            String th = " %9s %9s  %s";
            buf.append(h).append(String.format(th, "flushs", "age", "id"));
            String tr = " %9d %9d  %s";
            for (CacheRoot c : cacheRoots) {
                if (c.getFlushCount() > 0) {
                    String r = c.isReal() ? "" : "~";
                    buf.append(h);
                    buf.append(String.format(tr, c.getFlushCount(), (t - c.getLastFlush()) / 1000L, r + c.getId()));
                }
            }
        }
        buf.append(h).append(" ---------------------------------------------------------------------------------");
        return buf.toString();
    }
}
