/*
 *  Copyright 2004 Clinton Begin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.ibatis.common.jdbc;

import com.ibatis.common.resources.Resources;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.common.RunStats;
import com.ibatis.common.Statsable;
import com.ibatis.common.logging.ILog;

import javax.sql.DataSource;

import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/*-
 * This is a simple, synchronous, thread-safe database connection pool.
 * <p/>
 * REQUIRED PROPERTIES
 * -------------------
 * JDBC.Driver
 * JDBC.ConnectionURL
 * JDBC.Username
 * JDBC.Password
 * <p/>
 * JDBC.DefaultAutoCommit
 * JDBC.DefaultTransactionIsolation
 * Pool.MaximumActiveConnections
 * Pool.MaximumIdleConnections
 * Pool.MaximumCheckoutTime
 * Pool.TimeToWait
 * Pool.PingQuery
 * Pool.PingEnabled
 * Pool.PingConnectionsOlderThan
 * Pool.PingConnectionsNotUsedFor
 * Pool.PingIdleConnectionsAfter
 * Pool.EraseIdleConnectionsAfter
 * Pool.ShutdownDelay
 * Pool.LogSqlOverdueThan
 * Pool.CommitOnReturn
 */
public class SimpleDataSource implements DataSource, Statsable {

    private static final ILog log = ILogFactory.getLog(SimpleDataSource.class);

    // Required Properties
    private static final String PROP_JDBC_DRIVER = "JDBC.Driver";
    private static final String PROP_JDBC_URL = "JDBC.ConnectionURL";
    private static final String PROP_JDBC_USERNAME = "JDBC.Username";
    private static final String PROP_JDBC_PASSWORD = "JDBC.Password";
    private static final String PROP_JDBC_DEFAULT_AUTOCOMMIT = "JDBC.DefaultAutoCommit";
    private static final String PROP_JDBC_DEFAULT_TRANSACTIONISOLATION = "JDBC.DefaultTransactionIsolation";

    // Optional Properties
    private static final String PROP_POOL_MAX_ACTIVE_CONN = "Pool.MaximumActiveConnections";
    private static final String PROP_POOL_MAX_IDLE_CONN = "Pool.MaximumIdleConnections";
    private static final String PROP_POOL_MAX_CHECKOUT_TIME = "Pool.MaximumCheckoutTime";
    private static final String PROP_POOL_TIME_TO_WAIT = "Pool.TimeToWait";
    private static final String PROP_POOL_PING_QUERY = "Pool.PingQuery";
    private static final String PROP_POOL_PING_CONN_OLDER_THAN = "Pool.PingConnectionsOlderThan";
    private static final String PROP_POOL_PING_ENABLED = "Pool.PingEnabled";
    private static final String PROP_POOL_PING_CONN_NOT_USED_FOR = "Pool.PingConnectionsNotUsedFor";
    private static final String PROP_POOL_PING_IDLE_CONN_AFTER = "Pool.PingIdleConnectionsAfter";
    private static final String PROP_POOL_Erase_IDLE_CONN_AFTER = "Pool.EraseIdleConnectionsAfter";
    private static final String PROP_POOL_ShutdownDelay = "Pool.ShutdownDelay";
    private static final String PROP_POOL_LogSqlOverdueThan = "Pool.LogSqlOverdueThan";
    private static final String PROP_POOL_CommitOnReturn = "Pool.CommitOnReturn";
    // Additional Driver Properties prefix
    private static final String ADD_DRIVER_PROPS_PREFIX = "Driver.";
    private static final int ADD_DRIVER_PROPS_PREFIX_LENGTH = ADD_DRIVER_PROPS_PREFIX.length();

    // ----- BEGIN: FIELDS LOCKED BY POOL_LOCK -----
    private Que<SimplePooledConnection> idleConnections = new Que<SimplePooledConnection>();
    private Que<SimplePooledConnection> activeConnections = new Que<SimplePooledConnection>();
    private final Stat stat = new Stat();

    final ReentrantLock poolLock = new ReentrantLock();
    final Condition forIdle  = poolLock.newCondition();

    static class Stat {
        volatile long requestCount = 0;
        volatile long reuseCount = 0;
        volatile long closeCount = 0;
        volatile long eraseCount = 0;
        volatile long claimedOverdueCount = 0;
        volatile long badConnectionCount = 0;
        volatile long totalRequestTime = 0;
        volatile long totalCheckoutTime = 0;
        volatile long totalOverdueCheckoutTime = 0;
        volatile long totalWaitTime = 0;
        volatile long hadToWaitCount = 0;
    }

    long maxIdleCount = 0;
    long maxActiveCount = 0;
    // ----- END: FIELDS LOCKED BY POOL_LOCK -----

    // ----- BEGIN: PROPERTY FIELDS FOR CONFIGURATION -----
    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;
    private Boolean jdbcDefaultAutoCommit;
    protected Integer defaultTransactionIsolation;
    private Properties driverProps;
    private String driverProperties;
    private boolean useDriverProps;

    private int poolMaxActive;
    private int poolMaxIdle;
    private int poolMaxCheckoutTime;
    private int poolTimeToWait;
    private String poolPingQuery, realPingQuery;
    private boolean poolPingEnabled;
    private int poolPingConnOlderThan;
    private int poolPingConnNotUsedFor;
    private int poolPingIdleConnAfter;
    private int poolEraseIdleConnAfter;
    private int poolShutdownDelay;
    int poolLogSqlOverdueThan;
    private boolean poolCommitOnReturn;
    int sql_executor_threshold;
    // ----- END: PROPERTY FIELDS FOR CONFIGURATION -----

    IdleThread idle;
    AtomicBoolean threadDumped = new AtomicBoolean(false);

    /**
     * Constructor to allow passing in a map of properties for configuration
     *
     * @param props
     *            - the configuration parameters
     */
    public SimpleDataSource(Map<Object, Object> props) {
        HashMap<Object, Object> map = new HashMap<Object, Object>();
        if (props != null) {
            map.putAll(props);
        }
        Properties p = Resources.getIbatisIniProperties();
        if (p != null) {
            map.putAll(p);
        }
        if (map.isEmpty()) {
            throw new RuntimeException("SimpleDataSource: The properties map passed to the initializer was empty.");
        }

        try {
            initialize(map);
        } catch (RuntimeException e) {
            log.error("SimpleDataSource: Error while loading properties. Cause: " + e.toString(), e);
            throw e;
        } catch (Exception e) {
            log.error("SimpleDataSource: Error while loading properties. Cause: " + e.toString(), e);
            throw new RuntimeException("SimpleDataSource: Error while loading properties. Cause: " + e, e);
        }

        idle = new IdleThread(hashCode());
        idle.start();
    }

    void initialize(Map<Object, Object> props) throws Exception {
        if (!(props.containsKey(PROP_JDBC_DRIVER) && props.containsKey(PROP_JDBC_URL)
            && props.containsKey(PROP_JDBC_USERNAME) && props.containsKey(PROP_JDBC_PASSWORD))) {
            throw new RuntimeException("SimpleDataSource: Some properties were not set.");
        }

        jdbcDriver = (String) props.get(PROP_JDBC_DRIVER);
        jdbcUrl = (String) props.get(PROP_JDBC_URL);
        jdbcUsername = (String) props.get(PROP_JDBC_USERNAME);
        jdbcPassword = (String) props.get(PROP_JDBC_PASSWORD);

        poolMaxActive = propInt(props, PROP_POOL_MAX_ACTIVE_CONN, 10);
        poolMaxIdle = propInt(props, PROP_POOL_MAX_IDLE_CONN, 5);
        poolMaxCheckoutTime = propInt(props, PROP_POOL_MAX_CHECKOUT_TIME, 20000);
        poolTimeToWait = propInt(props, PROP_POOL_TIME_TO_WAIT, 20000);
        if (poolTimeToWait < 1000) {
            poolTimeToWait = 1000;
        }
        poolPingEnabled = propBool(props, PROP_POOL_PING_ENABLED, false);
        poolPingQuery = propStr(props, PROP_POOL_PING_QUERY, null);
        poolPingConnOlderThan = propInt(props, PROP_POOL_PING_CONN_OLDER_THAN, 0);
        poolPingConnNotUsedFor = propInt(props, PROP_POOL_PING_CONN_NOT_USED_FOR, 0);
        poolPingIdleConnAfter = propInt(props, PROP_POOL_PING_IDLE_CONN_AFTER, 0);
        poolEraseIdleConnAfter = propInt(props, PROP_POOL_Erase_IDLE_CONN_AFTER, 0);
        poolShutdownDelay = propInt(props, PROP_POOL_ShutdownDelay, 10000);
        poolLogSqlOverdueThan = propInt(props, PROP_POOL_LogSqlOverdueThan, 0);
        sql_executor_threshold = propInt(props, "sql_executor_threshold", 1000);
        jdbcDefaultAutoCommit = propBool(props, PROP_JDBC_DEFAULT_AUTOCOMMIT, null);
        defaultTransactionIsolation = toTransactionIsolation(props.get(PROP_JDBC_DEFAULT_TRANSACTIONISOLATION));
        poolCommitOnReturn = propBool(props, PROP_POOL_CommitOnReturn, false);

        useDriverProps = false;
        driverProps = new Properties();
        driverProps.put("user", jdbcUsername);
        driverProps.put("password", jdbcPassword);
        for (Object key : props.keySet()) {
            String name = String.valueOf(key);
            String value = String.valueOf(props.get(key));
            if (name.startsWith(ADD_DRIVER_PROPS_PREFIX)) {
                driverProps.put(name.substring(ADD_DRIVER_PROPS_PREFIX_LENGTH), value);
                useDriverProps = true;
            }
        }
        Properties driverPropsCopy = new Properties();
        driverPropsCopy.putAll(driverProps);
        if (driverPropsCopy.containsKey("password")) {
            driverPropsCopy.setProperty("password", "******");
        }
        driverProperties = useDriverProps ? driverPropsCopy.toString() : "{}";

        Resources.instantiate(jdbcDriver);

        RunStats.getInstance().addStat(this);
    }

    static String propStr(Map<Object, Object> props, String key, String defVal) {
        Object i = props.get(key);
        if (i != null) {
            return String.valueOf(i);
        }
        return defVal;
    }

    static Boolean propBool(Map<Object, Object> props, String key, Boolean defVal) {
        Object i = props.get(key);
        if (i instanceof Boolean) {
            return (Boolean) i;
        } else if (i != null) {
            return Boolean.parseBoolean(String.valueOf(i));
        }
        return defVal;
    }

    static int propInt(Map<Object, Object> props, String key, int defVal) {
        Object i = props.get(key);
        if (i instanceof Number) {
            return ((Number) i).intValue();
        } else if (i != null) {
            return Integer.parseInt(String.valueOf(i));
        }
        return defVal;
    }

    static String toTransactionIsolationName(Integer ti) {
        if (ti == null) {
            return "Default";
        }

        if (ti == Connection.TRANSACTION_NONE) {
            return "NONE";
        }

        if (ti == Connection.TRANSACTION_READ_UNCOMMITTED) {
            return "READ_UNCOMMITTED";
        }

        if (ti == Connection.TRANSACTION_READ_COMMITTED) {
            return "READ_COMMITTED";
        }

        if (ti == Connection.TRANSACTION_REPEATABLE_READ) {
            return "REPEATABLE_READ";
        }

        if (ti == Connection.TRANSACTION_SERIALIZABLE) {
            return "SERIALIZABLE";
        }

        return "UNKNOWN: " + ti;
    }

    static Integer toTransactionIsolation(Object oti) {
        if (oti == null) {
            return null;
        }
        if (oti instanceof Integer) {
            return (Integer) oti;
        }
        String ti = String.valueOf(oti).toUpperCase();

        if (ti.contains("NONE")) {
            return Connection.TRANSACTION_NONE;
        }
        if (ti.contains("READ") && ti.contains("UNCOMMITTED")) {
            return Connection.TRANSACTION_READ_UNCOMMITTED;
        }
        if (ti.contains("READ") && ti.contains("COMMITTED")) {
            return Connection.TRANSACTION_READ_COMMITTED;
        }
        if (ti.contains("REPEATABLE") && ti.contains("READ")) {
            return Connection.TRANSACTION_REPEATABLE_READ;
        }
        if (ti.endsWith("SERIALIZABLE")) {
            return Connection.TRANSACTION_SERIALIZABLE;
        }

        return null;
    }

    /**
     * @see javax.sql.DataSource#getConnection()
     */
    public Connection getConnection() throws SQLException {
        return popConnection().open();
    }

    /**
     * @see javax.sql.DataSource#getConnection(java.lang.String, java.lang.String)
     */
    public Connection getConnection(String username, String password) throws SQLException {
        if (!jdbcUsername.equals(username)) {
            throw new SQLFeatureNotSupportedException();
        }
        return popConnection().open();
    }

    /**
     * @see javax.sql.DataSource#setLoginTimeout(int)
     */
    public void setLoginTimeout(int loginTimeout) throws SQLException {
        DriverManager.setLoginTimeout(loginTimeout);
    }

    /**
     * @see javax.sql.DataSource#getLoginTimeout()
     */
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    /**
     * @see javax.sql.DataSource#setLogWriter(java.io.PrintWriter)
     */
    public void setLogWriter(PrintWriter logWriter) throws SQLException {
        DriverManager.setLogWriter(logWriter);
    }

    /**
     * @see javax.sql.DataSource#getLogWriter()
     */
    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    /**
     * If a connection has not been used in this many milliseconds, ping the database to make sure the connection is
     * still good.
     *
     * @return the number of milliseconds of inactivity that will trigger a ping
     */
    public int getPoolPingConnectionsNotUsedFor() {
        return poolPingConnNotUsedFor;
    }

    /**
     * Getter for the name of the JDBC driver class used
     *
     * @return The name of the class
     */
    public String getJdbcDriver() {
        return jdbcDriver;
    }

    /**
     * Getter of the JDBC URL used
     *
     * @return The JDBC URL
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Getter for the JDBC user name used
     *
     * @return The user name
     */
    public String getJdbcUsername() {
        return jdbcUsername;
    }

    /**
     * Getter for the JDBC password used
     *
     * @return The password
     */
    public String getJdbcPassword() {
        return jdbcPassword;
    }

    /**
     * Getter for the maximum number of active connections
     *
     * @return The maximum number of active connections
     */
    public int getPoolMaximumActiveConnections() {
        return poolMaxActive;
    }

    /**
     * Getter for the maximum number of idle connections
     *
     * @return The maximum number of idle connections
     */
    public int getPoolMaximumIdleConnections() {
        return poolMaxIdle;
    }

    /**
     * Getter for the maximum time a connection can be used before it *may* be given away again.
     *
     * @return The maximum time
     */
    public int getPoolMaximumCheckoutTime() {
        return poolMaxCheckoutTime;
    }

    /**
     * Getter for the time to wait before retrying to get a connection
     *
     * @return The time to wait
     */
    public int getPoolTimeToWait() {
        return poolTimeToWait;
    }

    /**
     * Getter for the query to be used to check a connection
     *
     * @return The query
     */
    public String getPoolPingQuery() {
        return poolPingQuery;
    }

    /**
     * Getter to tell if we should use the ping query
     *
     * @return True if we need to check a connection before using it
     */
    public boolean isPoolPingEnabled() {
        return poolPingEnabled;
    }

    /**
     * Getter for the age of connections that should be pinged before using
     *
     * @return The age
     */
    public int getPoolPingConnectionsOlderThan() {
        return poolPingConnOlderThan;
    }

    /**
     * Getter for the number of connection requests made
     *
     * @return The number of connection requests made
     */
    public long getRequestCount() {
        return stat.requestCount;
    }

    /**
     * Getter for the average time required to get a connection to the database
     *
     * @return The average time
     */
    public long getAvgRequestTime() {
        return stat.requestCount == 0 ? 0 : stat.totalRequestTime / stat.requestCount;
    }

    /**
     * Getter for the average time spent waiting for connections that were in use
     *
     * @return The average time
     */
    public long getAvgWaitTime() {
        return stat.hadToWaitCount == 0 ? 0 : stat.totalWaitTime / stat.hadToWaitCount;
    }

    /**
     * Getter for the number of requests that had to wait for connections that were in use
     *
     * @return The number of requests that had to wait
     */
    public long getHadToWaitCount() {
        return stat.hadToWaitCount;
    }

    /**
     * Getter for the number of invalid connections that were found in the pool
     *
     * @return The number of invalid connections
     */
    public long getBadConnectionCount() {
        return stat.badConnectionCount;
    }

    /**
     * Getter for the number of connections that were claimed before they were returned
     *
     * @return The number of connections
     */
    public long getClaimedOverdueCount() {
        return stat.claimedOverdueCount;
    }

    /**
     * Getter for the average age of overdue connections
     *
     * @return The average age
     */
    public long getAvgOverdueUseTime() {
        return stat.claimedOverdueCount == 0 ? 0 : stat.totalOverdueCheckoutTime / stat.claimedOverdueCount;
    }

    /**
     * Getter for the average age of a connection checkout
     *
     * @return The average age
     */
    public long getAvgCheckoutTime() {
        return stat.requestCount == 0 ? 0 : stat.totalCheckoutTime / stat.requestCount;
    }

    /**
     * Returns the status of the connection pool
     *
     * @return The status
     */
    public String getStatus(String h) {
        StringBuilder buf = new StringBuilder("PoolStatus " + hashCode() + " :");
        if (h == null) {
            h = "\n";
        } else {
            h = "\n" + h;
        }
        // synchronized (stat)
        {
            String rc = String.valueOf(stat.requestCount);
            int w = rc.length();
// @formatter:off
buf.append(h).append(" ---------------------------------------------------------------------------------");
buf.append(h).append(" jdbcDriver         ").append(jdbcDriver);
buf.append(h).append(" jdbcUrl            ").append(jdbcUrl);
buf.append(h).append(" jdbcUsername       ").append(pad(jdbcUsername))               .append(" jdbcPassword             ").append((jdbcPassword == null ? "NULL" : "******"));
buf.append(h).append(" requestCount       ").append(pad(rc))                         .append(" defaultAutoCommit        ").append(jdbcDefaultAutoCommit);
buf.append(h).append("  idleConnections   ").append(pad(w, idleConnections.size()))  .append(" transactionIsolation     ").append(toTransactionIsolationName(defaultTransactionIsolation));
buf.append(h).append("  activeConnections ").append(pad(w, activeConnections.size())).append(" driverProperties         ").append(driverProperties);
buf.append(h).append("  reuseCount        ").append(pad(w, stat.reuseCount))         .append(" poolMaxActiveConnections ").append(poolMaxActive);
buf.append(h).append("  closeCount        ").append(pad(w, stat.closeCount))         .append(" poolMaxIdleConnections   ").append(poolMaxIdle);
buf.append(h).append("  eraseCount        ").append(pad(w, stat.eraseCount))         .append(" poolMaxCheckoutTime      ").append(poolMaxCheckoutTime);
buf.append(h).append("  claimedOverdue    ").append(pad(w, stat.claimedOverdueCount)).append(" poolTimeToWait           ").append(poolTimeToWait);
buf.append(h).append("  badCount          ").append(pad(w, stat.badConnectionCount)) .append(" poolPingIdleConnsAfter   ").append(poolPingIdleConnAfter);
buf.append(h).append(" avgRequestTime     ").append(pad(getAvgRequestTime()))        .append(" poolEraseIdleConnsAfter  ").append(poolEraseIdleConnAfter);
buf.append(h).append(" avgCheckoutTime    ").append(pad(getAvgCheckoutTime()))       .append(" poolPingConnsOlderThan   ").append(poolPingConnOlderThan);
buf.append(h).append(" avgOverdueUseTime  ").append(pad(getAvgOverdueUseTime()))     .append(" poolPingConnsNotUsedFor  ").append(poolPingConnNotUsedFor);
buf.append(h).append(" waitCount          ").append(pad(stat.hadToWaitCount))        .append(" poolPingEnabled          ").append(poolPingEnabled);
buf.append(h).append(" avgWaitTime        ").append(pad(getAvgWaitTime()))           .append(" poolPingQuery            ").append(poolPingQuery);
buf.append(h).append(" maxIdleCount       ").append(pad(maxIdleCount))               .append(" poolShutdownDelay        ").append(poolShutdownDelay);
buf.append(h).append(" maxActiveCount     ").append(pad(maxActiveCount))             .append(" poolCommitOnReturn       ").append(poolCommitOnReturn);
buf.append(h).append(" sql_exec_threshold ").append(pad(sql_executor_threshold))     .append(" poolLogSqlOverdueThan    ").append(poolLogSqlOverdueThan);
buf.append(h).append(" ---------------------------------------------------------------------------------");
// @formatter:on
        }
        /*-
         *  requestCount - reuseCount - claimedOverdue = activeConnections + idleConnections + closeCount + eraseCount + badCount
         */
        return buf.toString();
    }

    static final String SP16 = "                ";

    static String pad(Object o) {
        String s = String.valueOf(o);
        if (s.length() <= 16) {
            return s + SP16.substring(0, 16 - s.length());
        }
        return s;
    }

    static String pad(int w, Number o) {
        String s = String.valueOf(o);
        if (s.length() <= w && w <= 16) {
            return SP16.substring(0, w - s.length()) + s + SP16.substring(w);
        } else if (s.length() <= 16) {
            return s + SP16.substring(0, 16 - s.length());
        }
        return s;
    }

    /**
     * Closes all of the connections in the pool
     */
    public void forceCloseAll() {
        try {
            Thread.sleep(poolShutdownDelay);
        } catch (Exception e) {
        }
        // lock (POOL_LOCK) {
        Thread t = idle;
        if (t == null) {
            return;
        }
        idle = null;

        activeConnections.forEach(new With<SimplePooledConnection>() {
            @Override
            public boolean with(SimplePooledConnection conn) {
                try {
                    conn.invalidate();
                } catch (Throwable e) {
                    // ignore
                }
                return false;
            }
        });

        idleConnections.forEach(new With<SimplePooledConnection>() {
            @Override
            public boolean with(SimplePooledConnection conn) {
                try {
                    conn.invalidate();
                    cleanConn(conn);
                    closeConn(conn);
                } catch (Throwable e) {
                    // ignore
                }
                // synchronized (stat)
                {
                    stat.closeCount++;
                }
                return true;
            }
        });
        log.warn("SimpleDataSource " + hashCode() + " forcefully shutdown.");
        try {
            log.info(getStatus("iBATIS ! "));
        } catch (Throwable e) {
        }
        try {
            t.interrupt();
        } catch (Throwable e) {
        }
        notifyPool();
        // }
    }

    void closeConn(SimplePooledConnection conn) {
        try {
            if (conn.getRealConnection() != null) {
                conn.getRealConnection().close();
            }
        } catch (Exception ex) {
        }
    }

    boolean cleanConn(SimplePooledConnection conn) {
        try {
            Connection c = conn.getRealConnection();
            if (c != null) {
                if (!c.getAutoCommit()) {
                    if (poolCommitOnReturn) {
                        c.commit();
                    } else {
                        c.rollback();
                    }
                }
                return true;
            }
        } catch (Exception e) {
            log.error("Clean connection " + conn.getRealHashCode() + " error: " + e);
        }
        return false;
    }

    void pushConnection(SimplePooledConnection conn) throws SQLException {
        // lock (POOL_LOCK) {
        int closeCount = 0;
        int totalCheckoutTime = 0;
        int badConnectionCount = 0;
        int eraseCount = 0;
        try {
            boolean real = activeConnections.remove(conn, false);
            if (!real) {
                if (log.isDebugEnabled()) {
                    log.debug("Overdue connection " + conn.getRealHashCode() + " returned to pool.");
                }
            } else if (!conn.isValid()) {
                cleanConn(conn);
                closeConn(conn);
                closeCount++;
                if (log.isDebugEnabled()) {
                    log.debug("Closed connection " + conn.getRealHashCode());
                }
            } else {
                int ret = pingFree(conn, false);
                if (ret == 0) {
                    if (idleConnections.size() < poolMaxIdle) {
                        totalCheckoutTime += conn.getCheckoutTime();

                        if (cleanConn(conn)) {
                            SimplePooledConnection newConn = new SimplePooledConnection(conn.getRealConnection(), this);
                            newConn.setCreatedTimestamp(conn.getCreatedTimestamp());
                            newConn.setLastUsedTimestamp(conn.getLastUsedTimestamp());

                            long size = idleConnections.put(newConn);
                            if (size > maxIdleCount) {
                                maxIdleCount = size;
                            }
                            conn.invalidate();
                            notifyPool();
                        } else {
                            closeConn(conn);
                            badConnectionCount++;
                            if (log.isDebugEnabled()) {
                                log.debug("Bad connection " + conn.getRealHashCode() + " returned");
                            }
                        }
                    } else {
                        totalCheckoutTime += conn.getCheckoutTime();

                        cleanConn(conn);
                        closeConn(conn);
                        closeCount++;
                        if (log.isDebugEnabled()) {
                            log.debug("Closed connection " + conn.getRealHashCode());
                        }
                    }
                } else {
                    if (ret < 0) {
                        badConnectionCount++;
                        log.debug("Bad connection " + conn.getRealHashCode() + " returned");
                        closeConn(conn);
                    } else {
                        eraseCount++;
                        log.debug("Erase bad connection " + conn.getRealHashCode() + " returned");
                    }
                }
            }
            // }
        } finally {
            // synchronized (stat)
            {
                stat.closeCount += closeCount;
                stat.totalCheckoutTime += totalCheckoutTime;
                stat.badConnectionCount += badConnectionCount;
                stat.eraseCount += eraseCount;
            }
        }
    }

    void waitPool(long timeToWait) {
        poolLock.lock();
        try {
            forIdle.await(timeToWait, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
        } finally {
            poolLock.unlock();
        }
    }

    void notifyPool() {
        poolLock.lock();
        try {
            forIdle.signalAll();
        } finally {
            poolLock.unlock();
        }
    }

    SimplePooledConnection popConnection() throws SQLException {
        if (idle == null) {
            throw new SQLException("SimpleDataSource " + hashCode() + ": pool closed.");
        }
        boolean countedWait = false;
        SimplePooledConnection conn = null;
        long t = System.currentTimeMillis();
        int localBadConnectionCount = 0;

        while (conn == null) {
            // lock (POOL_LOCK) {
            long reuseCount = 0;
            long badConnectionCount = 0;
            long eraseCount = 0;
            long totalOverdueCheckoutTime = 0;
            long totalCheckoutTime = 0;
            long claimedOverdueCount = 0;
            long hadToWaitCount = 0;
            long totalWaitTime = 0;
            long requestCount = 0;
            long totalRequestTime = 0;
            try {
                conn = idleConnections.take();
                if (conn != null) {
                    // Pool has available connection
                    int ret = pingFree(conn, true);
                    if (ret == 0) {
                        reuseCount++;
                    } else {
                        if (ret < 0) {
                            log.debug("Bad connection " + conn.getRealHashCode() + " returned from the idle pool.");
                            badConnectionCount++;
                            localBadConnectionCount++;
                        } else {
                            log.debug("Erase old connection " + conn.getRealHashCode());
                            eraseCount++;
                        }
                        conn = null;
                    }
                } else {
                    // Pool does not have available connection
                    if (activeConnections.size() < poolMaxActive) {
                        // Can create new connection
                        if (useDriverProps) {
                            conn = new SimplePooledConnection(DriverManager.getConnection(jdbcUrl, driverProps), this);
                        } else {
                            conn = new SimplePooledConnection(DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword), this);
                        }

                        if (defaultTransactionIsolation != null) {
                            conn.getRealConnection().setTransactionIsolation(defaultTransactionIsolation);
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Created connection " + conn.getRealHashCode());
                        }
                    } else if (poolMaxCheckoutTime > 0) {
                        // Cannot create new connection
                        SimplePooledConnection oldest = activeConnections.peek();
                        long longestCheckoutTime = oldest != null ? oldest.getCheckoutTime() : 0L;
                        if (longestCheckoutTime > poolMaxCheckoutTime) {
                            if (activeConnections.remove(oldest, true)) {
                                // Can claim overdue connection

                                totalOverdueCheckoutTime += longestCheckoutTime;
                                totalCheckoutTime += longestCheckoutTime;

                                conn = new SimplePooledConnection(oldest.getRealConnection(), this);
                                conn.setCreatedTimestamp(oldest.getCreatedTimestamp());
                                conn.setLastUsedTimestamp(oldest.getLastUsedTimestamp());
                                oldest.invalidate();

                                int ret = pingFree(conn, true);
                                if (ret == 0 && cleanConn(conn)) {
                                    claimedOverdueCount++;
                                    if (log.isDebugEnabled()) {
                                        log.debug("Claimed overdue connection " + conn.getRealHashCode());
                                    }
                                } else {
                                    if (ret > 0) {
                                        log.debug("Erase old claimed connection " + oldest.getRealHashCode());
                                        eraseCount++;
                                    } else {
                                        closeConn(conn);
                                        log.debug("Bad connection " + conn.getRealHashCode()
                                            + " claimed from the active pool.");
                                        badConnectionCount++;
                                        localBadConnectionCount++;
                                    }
                                    conn = null;
                                }
                            } else {
                                continue;
                            }
                        }
                    }

                    if (conn == null) {
                        // Must wait
                        try {
                            if (!countedWait) {
                                hadToWaitCount++;
                                countedWait = true;
                            }
                            long wt = System.currentTimeMillis();
                            waitPool(poolTimeToWait);
                            totalWaitTime += System.currentTimeMillis() - wt;
                        } catch (Exception e) {
                        }
                    }
                }
                if (conn != null) {
                    conn.setCheckoutTimestamp(System.currentTimeMillis());
                    conn.setLastUsedTimestamp(System.currentTimeMillis());

                    if (jdbcDefaultAutoCommit != null && jdbcDefaultAutoCommit != conn.getRealConnection().getAutoCommit()) {
                        conn.getRealConnection().setAutoCommit(jdbcDefaultAutoCommit);
                    }

                    requestCount++;
                    long size = activeConnections.put(conn);
                    if (size > maxActiveCount) {
                        maxActiveCount = size;
                    }
                    t = System.currentTimeMillis() - t;
                    totalRequestTime += t;
                    if (t > poolTimeToWait) {
                        log.warn("Wait as long as " + t + " milliseconds for connection " + conn.getRealHashCode());
                        dumpThread();
                    }
                } else if (localBadConnectionCount > (poolMaxIdle + 3)) {
                    if (log.isDebugEnabled()) {
                        log.debug("SimpleDataSource: Could not get a good connection to the database.");
                    }
                    throw new SQLException("SimpleDataSource: Could not get a good connection to the database.");
                }
                // }
            } finally {
                // synchronized (stat)
                {
                    stat.reuseCount += reuseCount;
                    stat.badConnectionCount += badConnectionCount;
                    stat.eraseCount += eraseCount;
                    stat.totalOverdueCheckoutTime += totalOverdueCheckoutTime;
                    stat.totalCheckoutTime += totalCheckoutTime;
                    stat.claimedOverdueCount += claimedOverdueCount;
                    stat.hadToWaitCount += hadToWaitCount;
                    stat.totalWaitTime += totalWaitTime;
                    stat.requestCount += requestCount;
                    stat.totalRequestTime += totalRequestTime;
                }
            }
        }

        return conn;
    }

    void dumpThread() {
        if (threadDumped.compareAndSet(false, true)) {
            log.warn("iBATIS Thread dump start ...");
            Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
            for (Thread t : all.keySet()) {
                log.info(t.toString());
                StackTraceElement[] stes = all.get(t);
                for (StackTraceElement ste : stes) {
                    log.warn("\t" + ste);
                }
            }
            log.info("iBATIS Thread dump Okey!");
        }
    }

    /**
     * Method to check to see if a connection is still usable
     *
     * @param conn
     *            - the connection to check
     * @param out
     *            - if the conn will be borrow out
     * @return 0: still usable, 1: erase, -1: bad
     */
    int pingFree(SimplePooledConnection conn, boolean out) {
        try {
            if (conn.getRealConnection().isClosed()) {
                return -1;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
            }
            return -1;
        }
        if (poolEraseIdleConnAfter > 0 && conn.getAge() > poolEraseIdleConnAfter) {
            closeConn(conn);
            return 1;
        }

        if (poolPingEnabled && (poolPingConnOlderThan > 0 && conn.getAge() > poolPingConnOlderThan
            || poolPingConnNotUsedFor > 0 && conn.getTimeElapsedSinceLastUse() > poolPingConnNotUsedFor)) {

            try {
                boolean ok = pingConn(conn.getRealConnection());
                if (ok) {
                    return 0;
                }
                if (out) {
                    log.debug("Pool connection " + conn.getRealHashCode() + " is BAD by ping.");
                } else {
                    log.debug("User connection " + conn.getRealHashCode() + " is BAD by ping.");
                }
                return -1;
            } catch (Exception e) {
                closeConn(conn);
                /*
                 * not need closeCount ++, add to badConnectionCount
                 */
                return -1;
            }
        }
        return 0;
    }

    /**
     * Unwraps a pooled connection to get to the 'real' connection
     *
     * @param conn
     *            - the pooled connection to unwrap
     * @return The 'real' connection
     */
    public static Connection unwrapConnection(Connection conn) {
        if (conn instanceof SimplePooledConnection) {
            return ((SimplePooledConnection) conn).getRealConnection();
        } else {
            return conn;
        }
    }

    protected void finalize() throws Throwable {
        forceCloseAll();
    }

    /**
     * IdleCheckThread
     * <p>
     * Date: 2010-11-26, 21:29:40 +0800
     *
     * @author Song Sun
     * @version 1.0
     */
    class IdleThread extends Thread {

        IdleThread(int id) {
            setName("iBATIS Idle " + id);
            setDaemon(true);
        }

        public void run() {
            final int[] eraseCount = new int[1];
            while (idle != null && poolMaxIdle > 0 && (poolPingIdleConnAfter > 0 || poolEraseIdleConnAfter > 0)) {
                int badConnectionCount = 0;
                try {
                    Thread.sleep(30000);
                    // lock (POOL_LOCK) {
                    if (idleConnections.size() <= 0) {
                        continue;
                    }
                    if (poolEraseIdleConnAfter > 0) {
                        idleConnections.forEach(new With<SimplePooledConnection>() {
                            @Override
                            public boolean with(SimplePooledConnection spc) {
                                if (spc.getAge() > poolEraseIdleConnAfter) {
                                    try {
                                        closeConn(spc);
                                        log.debug("Erase old idle connection " + spc.getRealHashCode());
                                    } catch (Exception e) {
                                    }
                                    eraseCount[0]++;
                                    return true;
                                }
                                return false;
                            }
                        });
                    }
                    // }

                    while (poolPingIdleConnAfter > 0) {
                        SimplePooledConnection spc = idleConnections.take();
                        if (spc == null) {
                            // no idle to ping
                            break;
                        }
                        if (spc.getTimeElapsedSinceLastUse() < poolPingIdleConnAfter) {
                            // no more idle to ping
                            // lock (POOL_LOCK) {
                            idleConnections.put(spc);
                            notifyPool();
                            // }
                            break;
                        }
                        if (pingIdle(spc)) {
                            /*
                             * if ping failed, the conn colsed also. so we just return the 'good' idle.
                             */
                            // lock (POOL_LOCK) {
                            spc.setLastUsedTimestamp(System.currentTimeMillis());
                            if (idleConnections.size() < poolMaxIdle) {
                                idleConnections.put(spc);
                                notifyPool();
                            } else {
                                // the idle conn is unlucky to be closed.
                                try {
                                    closeConn(spc);
                                    log.debug("Erase extra idle connection " + spc.getRealHashCode());
                                } catch (Exception e) {
                                }
                                eraseCount[0]++;
                            }
                            // }
                        } else {
                            try {
                                closeConn(spc);
                                log.debug("Erase bad idle connection " + spc.getRealHashCode());
                            } catch (Exception e) {
                            }
                            badConnectionCount++;
                        }
                    }
                } catch (Throwable t) {
                    if (idle == null) {
                        break;
                    }
                    log.error(getName() + " error: " + t.getMessage(), t);
                } finally {
                    // synchronized (stat)
                    {
                        stat.eraseCount += eraseCount[0];
                        stat.badConnectionCount += badConnectionCount;
                    }
                    eraseCount[0] = 0;
                }
            }
        }

        boolean pingIdle(SimplePooledConnection conn) {
            try {
                if (conn.getRealConnection().isClosed()) {
                    return false;
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Idle Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
                }
                return false;
            }
            boolean ok = pingConn(conn);
            if (!ok && log.isDebugEnabled()) {
                log.debug("Idle connection " + conn.getRealHashCode() + " is BAD by ping.");
            }
            return ok;
        }
    }

    boolean pingConn(Connection realConn) {
        String sql = getRealPingQuery(realConn);
        if (sql == null || sql.isEmpty()) {
            return true;
        }
        long time = System.currentTimeMillis();
        PreparedStatement statement = null;
        try {
            statement = realConn.prepareStatement(sql);
        } catch (Exception t) {
            realPingQuery = "";
            log.warn("Preparation of ping query '" + sql + "' failed: " + t.getMessage());
            return true;
        }
        try {
            ResultSet rs = statement.executeQuery();
            rs.close();
            statement.close();
            if (!realConn.getAutoCommit()) {
                realConn.rollback();
            }

            time = System.currentTimeMillis() - time;
            if (time > 10000) {
                if (log.isDebugEnabled()) {
                    log.debug("Connection " + realConn.hashCode() + " maybe BAD, ping elapse " + time + " ms");
                }
                return false;
            }
            return true;
        } catch (Throwable t) {
            log.warn("Execution of ping query '" + sql + "' failed: " + t.getMessage());
            return false;
        }
    }

    String getRealPingQuery(Connection conn) {
        if (realPingQuery == null) {
            if (poolPingQuery == null || poolPingQuery.trim().isEmpty()) {
                String prod = null;
                try {
                    prod = conn.getMetaData().getDatabaseProductName();
                } catch (Exception e) {
                }
                prod = String.valueOf(prod).toLowerCase();
                if (prod.indexOf("oracle") >= 0) {
                    realPingQuery = "select 1 from dual";
                } else if (prod.indexOf("db2") >= 0) {
                    realPingQuery = "select 1 from sysibm.SYSDUMMY1";
                } else if (prod.indexOf("mysql") >= 0) {
                    realPingQuery = "select current_date";
                } else if (prod.indexOf("microsoft") >= 0) {
                    realPingQuery = "select getdate()";
                } else if (prod.indexOf("derby") >= 0) {
                    realPingQuery = "select 1 from sysibm.SYSDUMMY1";
                } else {
                    log.warn("Unknown Database: " + prod + ", Please set parameter: " + PROP_POOL_PING_QUERY);
                    realPingQuery = "";
                }
            } else {
                realPingQuery = poolPingQuery;
            }
        }
        return realPingQuery;
    }

    // ## Add JDBC 4
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }
        throw new SQLFeatureNotSupportedException();
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    interface With<E> {
        /**
         * delete it or not?
         */
        boolean with(E e);
    }

    static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }

    static class Que<E> {

        volatile int size = 0;

        /*-
         * Pointer to first node.
         * Invariant: (first == null && last == null) ||
         *            (first.prev == null && first.item != null)
         */
        volatile Node<E> first;

        /*-
         * Pointer to last node.
         * Invariant: (first == null && last == null) ||
         *            (last.next == null && last.item != null)
         */
        volatile Node<E> last;

        public int size() {
            return size;
        }

        public synchronized void forEach(With<E> cb) {
            for (Node<E> x = last; x != null;) {
                Node<E> n = x;
                x = x.prev;
                if (cb.with(n.item)) {
                    unlink(n);
                }
            }
        }

        public synchronized long put(E e) {
            if (e == null) {
                throw new NullPointerException();
            }
            Node<E> l = last;
            Node<E> newNode = new Node<E>(l, e, null);
            last = newNode;
            if (l == null)
                first = newNode;
            else
                l.next = newNode;
            size++;
            return size;
        }

        public synchronized boolean remove(E e, boolean isFirst) {
            if (isFirst) {
                if (first != null && e == first.item) {
                    unlink(first);
                    return true;
                }

                return false;
            }
            for (Node<E> x = first; x != null; x = x.next) {
                if (e == x.item || x.item.equals(e)) {
                    unlink(x);

                    return true;
                }
            }
            return false;
        }

        private void unlink(Node<E> x) {
            final Node<E> next = x.next;
            final Node<E> prev = x.prev;

            if (prev == null) {
                first = next;
            } else {
                prev.next = next;
                x.prev = null;
            }

            if (next == null) {
                last = prev;
            } else {
                next.prev = prev;
                x.next = null;
            }

            x.item = null;
            size--;
        }

        public synchronized E take() {
            Node<E> f = first;
            if (f == null) {
                return null;
            }
            // assert f == first && f != null;
            final E element = f.item;
            final Node<E> next = f.next;
            f.item = null;
            f.next = null; // help GC
            first = next;
            if (next == null)
                last = null;
            else
                next.prev = null;
            size--;
            return element;
        }

        public E peek() {
            Node<E> f = first;
            if (f == null) {
                return null;
            }
            return f.item;
        }
    }
}
