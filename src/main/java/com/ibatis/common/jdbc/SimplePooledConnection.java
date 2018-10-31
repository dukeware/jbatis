package com.ibatis.common.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * SimplePooledConnection
 * 
 * @author Song Sun
 * @version 1.0
 */
class SimplePooledConnection implements Connection {

    private SimpleDataSource dataSource;
    private Connection realConnection;
    private long checkoutTimestamp;
    private long createdTimestamp;
    private long lastUsedTimestamp;
    int logOverdue;
    int logThreshold;

    private boolean valid;
    private boolean closed;

    /**
     * Constructor for SimplePooledConnection that uses the Connection and SimpleDataSource passed in
     *
     * @param connection
     *            - the connection that is to be presented as a pooled connection
     * @param dataSource
     *            - the dataSource that the connection is from
     */
    SimplePooledConnection(Connection connection, SimpleDataSource dataSource) {
        this.realConnection = connection;
        this.dataSource = dataSource;
        this.createdTimestamp = System.currentTimeMillis();
        this.lastUsedTimestamp = System.currentTimeMillis();
        this.valid = true;
        this.logOverdue = dataSource.poolLogSqlOverdueThan;
        this.logThreshold = dataSource.sql_executor_threshold;

        // proxyConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), IFACES, this);
    }

    private boolean needWrap() {
        return logOverdue > 0;
    }
    
    /**
     * Invalidates the connection
     */
    void invalidate() {
        valid = false;
    }

    /**
     * Method to see if the connection is usable
     *
     * @return True if the connection is usable
     */
    boolean isValid() {
        return valid;
    }

    /**
     * Getter for the *real* connection that this wraps
     * 
     * @return The connection
     */
    Connection getRealConnection() {
        return realConnection;
    }

    /**
     * Getter for the proxy for the connection
     * 
     * @return The proxy
     */
    public Connection open() {
        closed = false;
        return this; // proxyConnection;
    }

    /**
     * Gets the hashcode of the real connection (or 0 if it is null)
     *
     * @return The hashcode of the real connection (or 0 if it is null)
     */
    int getRealHashCode() {
        if (realConnection == null) {
            return 0;
        } else {
            return realConnection.hashCode();
        }
    }

    /**
     * Getter for the time that the connection was created
     * 
     * @return The creation timestamp
     */
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    /**
     * Setter for the time that the connection was created
     * 
     * @param createdTimestamp
     *            - the timestamp
     */
    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    /**
     * Getter for the time that the connection was last used
     * 
     * @return - the timestamp
     */
    public long getLastUsedTimestamp() {
        return lastUsedTimestamp;
    }

    /**
     * Setter for the time that the connection was last used
     * 
     * @param lastUsedTimestamp
     *            - the timestamp
     */
    public void setLastUsedTimestamp(long lastUsedTimestamp) {
        this.lastUsedTimestamp = lastUsedTimestamp;
    }

    /**
     * Getter for the time since this connection was last used
     * 
     * @return - the time since the last use
     */
    public long getTimeElapsedSinceLastUse() {
        return System.currentTimeMillis() - lastUsedTimestamp;
    }

    /**
     * Getter for the age of the connection
     * 
     * @return the age
     */
    long getAge() {
        return System.currentTimeMillis() - createdTimestamp;
    }

    /**
     * Getter for the timestamp that this connection was checked out
     * 
     * @return the timestamp
     */
    public long getCheckoutTimestamp() {
        return checkoutTimestamp;
    }

    /**
     * Setter for the timestamp that this connection was checked out
     * 
     * @param timestamp
     *            the timestamp
     */
    public void setCheckoutTimestamp(long timestamp) {
        this.checkoutTimestamp = timestamp;
    }

    /**
     * Getter for the time that this connection has been checked out
     * 
     * @return the time
     */
    public long getCheckoutTime() {
        return System.currentTimeMillis() - checkoutTimestamp;
    }

    private Connection getValidConnection() throws SQLException {
        if (!valid) {
            throw new SQLException("Connection " + getRealHashCode() + " is invalid.");
        }
        return realConnection;
    }

    public Statement createStatement() throws SQLException {
        if (needWrap()) {
            return new SimpleStatement(this, getValidConnection().createStatement(), null);
        }
        return getValidConnection().createStatement();
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        if (needWrap()) {
            return new SimplePreparedStatement(this, getValidConnection().prepareStatement(sql), sql);
        }
        return getValidConnection().prepareStatement(sql);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        if (needWrap()) {
            return new SimpleCallableStatement(this, getValidConnection().prepareCall(sql), sql);
        }
        return getValidConnection().prepareCall(sql);
    }

    public String nativeSQL(String sql) throws SQLException {
        return getValidConnection().nativeSQL(sql);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        Connection conn = getValidConnection();
        if (conn.getAutoCommit() != autoCommit) {
            conn.setAutoCommit(autoCommit);
        }
    }

    public boolean getAutoCommit() throws SQLException {
        return getValidConnection().getAutoCommit();
    }

    public void commit() throws SQLException {
        getValidConnection().commit();
    }

    public void rollback() throws SQLException {
        getValidConnection().rollback();
    }

    public void close() throws SQLException {
        closed = true;
        dataSource.pushConnection(this);
    }

    public boolean isClosed() throws SQLException {
        return !valid || closed || getValidConnection().isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return getValidConnection().getMetaData();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        getValidConnection().setReadOnly(readOnly);
    }

    public boolean isReadOnly() throws SQLException {
        return getValidConnection().isReadOnly();
    }

    public void setCatalog(String catalog) throws SQLException {
        getValidConnection().setCatalog(catalog);
    }

    public String getCatalog() throws SQLException {
        return getValidConnection().getCatalog();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        // getValidConnection().setTransactionIsolation(level);
    }

    public int getTransactionIsolation() throws SQLException {
        return getValidConnection().getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return getValidConnection().getWarnings();
    }

    public void clearWarnings() throws SQLException {
        getValidConnection().clearWarnings();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        if (needWrap()) {
            return new SimpleStatement(this, getValidConnection().createStatement(resultSetType, resultSetConcurrency),
                null);
        }
        return getValidConnection().createStatement(resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException {
        if (needWrap()) {
            return new SimplePreparedStatement(this, getValidConnection().prepareStatement(sql, resultSetType,
                resultSetConcurrency), sql);
        }
        return getValidConnection().prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (needWrap()) {
            return new SimpleCallableStatement(this, getValidConnection().prepareCall(sql, resultSetType,
                resultSetConcurrency), sql);
        }
        return getValidConnection().prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return getValidConnection().getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        getValidConnection().setTypeMap(map);
    }

    // **********************************
    // JDK 1.4 JDBC 3.0 Methods below
    // **********************************

    public void setHoldability(int holdability) throws SQLException {
        getValidConnection().setHoldability(holdability);
    }

    public int getHoldability() throws SQLException {
        return getValidConnection().getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        return getValidConnection().setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return getValidConnection().setSavepoint(name);
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        getValidConnection().rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        getValidConnection().releaseSavepoint(savepoint);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
        if (needWrap()) {
            return new SimpleStatement(this, getValidConnection().createStatement(resultSetType, resultSetConcurrency,
                resultSetHoldability), null);
        }
        return getValidConnection().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
        int resultSetHoldability) throws SQLException {
        if (needWrap()) {
            return new SimplePreparedStatement(this, getValidConnection().prepareStatement(sql, resultSetType,
                resultSetConcurrency, resultSetHoldability), sql);
        }
        return getValidConnection().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
        int resultSetHoldability) throws SQLException {
        if (needWrap()) {
            return new SimpleCallableStatement(this, getValidConnection().prepareCall(sql, resultSetType,
                resultSetConcurrency, resultSetHoldability), sql);
        }
        return getValidConnection().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        if (needWrap()) {
            return new SimplePreparedStatement(this, getValidConnection().prepareStatement(sql, autoGeneratedKeys), sql);
        }
        return getValidConnection().prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int columnIndexes[]) throws SQLException {
        if (needWrap()) {
            return new SimplePreparedStatement(this, getValidConnection().prepareStatement(sql, columnIndexes), sql);
        }
        return getValidConnection().prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String columnNames[]) throws SQLException {
        if (needWrap()) {
            return new SimplePreparedStatement(this, getValidConnection().prepareStatement(sql, columnNames), sql);
        }
        return getValidConnection().prepareStatement(sql, columnNames);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return getValidConnection().unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return getValidConnection().isWrapperFor(iface);
    }

    public Clob createClob() throws SQLException {
        return getValidConnection().createClob();
    }

    public Blob createBlob() throws SQLException {
        return getValidConnection().createBlob();
    }

    public NClob createNClob() throws SQLException {
        return getValidConnection().createNClob();
    }

    public SQLXML createSQLXML() throws SQLException {
        return getValidConnection().createSQLXML();
    }

    public boolean isValid(int timeout) throws SQLException {
        return valid && realConnection != null && getValidConnection().isValid(timeout);
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        try {
            getValidConnection().setClientInfo(name, value);
        } catch (SQLClientInfoException e) {
            throw e;
        } catch (SQLException e) {
            throw new SQLClientInfoException(e.getMessage(), null);
        }
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        try {
            getValidConnection().setClientInfo(properties);
        } catch (SQLClientInfoException e) {
            throw e;
        } catch (SQLException e) {
            throw new SQLClientInfoException(e.getMessage(), null);
        }
    }

    public String getClientInfo(String name) throws SQLException {
        return getValidConnection().getClientInfo(name);
    }

    public Properties getClientInfo() throws SQLException {
        return getValidConnection().getClientInfo();
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return getValidConnection().createArrayOf(typeName, elements);
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return getValidConnection().createStruct(typeName, attributes);
    }

    public void setSchema(String schema) throws SQLException {
        getValidConnection().setSchema(schema);
    }

    public String getSchema() throws SQLException {
        return getValidConnection().getSchema();
    }

    public void abort(Executor executor) throws SQLException {
        getValidConnection().abort(executor);
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        getValidConnection().setNetworkTimeout(executor, milliseconds);
    }

    public int getNetworkTimeout() throws SQLException {
        return getValidConnection().getNetworkTimeout();
    }

    @Override
    public String toString() {
        return "iBATIS Connection " + getRealHashCode();
    }

}