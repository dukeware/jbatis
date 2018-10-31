package com.ibatis.common.jdbc;

import java.io.UnsupportedEncodingException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ibatis.common.Objects;
import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;

public class SimpleStatement implements Statement {
    static final ILog log = ILogFactory.getLog("org.ibatis.Debuger");

    Statement stat;
    SimplePooledConnection conn;
    protected String sql;
    Map<Object, Object> args = new LinkedHashMap<Object, Object>();

    public SimpleStatement(SimplePooledConnection conn, Statement stat, String sql) {
        this.conn = conn;
        this.stat = stat;
        this.sql = sql;
    }

    protected void cleanArgs() {
        args.clear();
    }

    protected void addArg(Object idx, Object x) {
        args.put(idx, x);
    }

    protected void clean() {
        sql = null;
        args.clear();
    }

    protected void debugSql(String call, long startTime, String sql, Object result) {
        long t = System.currentTimeMillis() - startTime;
        if (t >= conn.logThreshold) {
            call = call + " -> " + result;
            if (args.isEmpty()) {
                log.warn(call + " - " + t + "\n  +-- " + sql);
            } else {
                log.warn(call + " - " + t + "\n  +-- " + sql + "\n  +-- " + toStr(args));
            }
        } else if (t + 1 >= conn.logOverdue) {
            call = call + " -> " + result;
            if (args.isEmpty()) {
                log.info(call + " - " + t + "\n  +-- " + sql);
            } else {
                log.info(call + " - " + t + "\n  +-- " + sql + "\n  +-- " + toStr(args));
            }
        }
    }

    static String toStr(Map<Object, Object> args) {
        StringBuilder buf = new StringBuilder("{ ");
        for (Map.Entry<Object, Object> arg : args.entrySet()) {
            if (buf.length() > 2) {
                buf.append(", ");
            }
            if (arg.getKey() instanceof Integer) {
                Object obj = arg.getValue();
                if (obj instanceof Date) {
                    Objects.outputDate(buf, (Date) obj);
                } else if (obj instanceof String) {
                    buf.append("'").append(obj).append("'");
                } else if (obj instanceof byte[]) {
                    String s = "0x**";
                    try {
                        s = new String((byte[]) obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                    }
                    buf.append("'").append(s).append("'");
                } else {
                    buf.append(obj);
                }
            } else {
                buf.append(String.valueOf(arg.getKey())).append(" : ");
                Object obj = arg.getValue();
                if (obj instanceof Date) {
                    Objects.outputDate(buf, (Date) obj);
                } else if (obj instanceof String) {
                    buf.append("'").append(obj).append("'");
                } else if (obj instanceof byte[]) {
                    String s = "0x**";
                    try {
                        s = new String((byte[]) obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                    }
                    buf.append("'").append(s).append("'");
                } else {
                    buf.append(obj);
                }
            }
        }
        buf.append(" }");
        return buf.toString();

    }

    protected Statement stat() {
        return stat;
    }

    protected PreparedStatement pstat() {
        return (PreparedStatement) stat;
    }

    protected CallableStatement cstat() {
        return (CallableStatement) stat;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return stat().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return stat().isWrapperFor(iface);
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        long t = System.currentTimeMillis();
        clean();
        ResultSet rset = stat().executeQuery(sql);
        debugSql("executeQuery(String)", t, sql, "[ResultSet]");
        return rset;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        long t = System.currentTimeMillis();
        clean();
        int ret = stat().executeUpdate(sql);
        debugSql("executeUpdate(String)", t, sql, ret);
        return ret;
    }

    @Override
    public void close() throws SQLException {
        clean();
        stat().close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return stat().getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        stat().setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return stat().getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        stat().setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        stat().setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return stat().getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        stat().setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        stat().cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return stat().getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        stat().clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        stat().setCursorName(name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        long t = System.currentTimeMillis();
        clean();
        boolean ret = stat().execute(sql);
        debugSql("execute(String)", t, sql, ret);
        return ret;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return stat().getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return stat().getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return stat().getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        stat().setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return stat().getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        stat().setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return stat().getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return stat().getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return stat().getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        this.sql = sql;
        stat().addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        this.sql = null;
        stat().clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        long t = System.currentTimeMillis();
        int[] ret = stat().executeBatch();
        int total = 0;
        int fail = 0;
        if (ret != null) {
            for (int r : ret) {
                if (r > 0) {
                    total += r;
                } else {
                    fail++;
                }
            }
        }
        if (fail == 0) {
            debugSql("executeBatch()", t, sql, total);
        } else {
            debugSql("executeBatch()", t, sql, total + " | " + fail);
        }
        return ret;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return conn;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return stat().getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return stat().getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        long t = System.currentTimeMillis();
        clean();
        int ret = stat().executeUpdate(sql, autoGeneratedKeys);
        debugSql("executeUpdate(String, int)", t, sql, ret);
        return ret;
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        long t = System.currentTimeMillis();
        clean();
        int ret = stat().executeUpdate(sql, columnIndexes);
        debugSql("executeUpdate(String, int[])", t, sql, ret);
        return ret;
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        long t = System.currentTimeMillis();
        clean();
        int ret = stat().executeUpdate(sql, columnNames);
        debugSql("executeUpdate(String, String[])", t, sql, ret);
        return ret;
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        long t = System.currentTimeMillis();
        clean();
        boolean ret = stat().execute(sql, autoGeneratedKeys);
        debugSql("execute(String, int)", t, sql, ret);
        return ret;
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        long t = System.currentTimeMillis();
        clean();
        boolean ret = stat().execute(sql, columnIndexes);
        debugSql("execute(String, int[])", t, sql, ret);
        return ret;
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        long t = System.currentTimeMillis();
        clean();
        boolean ret = stat().execute(sql, columnNames);
        debugSql("execute(String, String[])", t, sql, ret);
        return ret;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return stat().getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return stat().isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        stat().setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return stat().isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        stat().closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return stat().isCloseOnCompletion();
    }

}
