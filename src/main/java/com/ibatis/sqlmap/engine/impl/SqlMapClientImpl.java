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
package com.ibatis.sqlmap.engine.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.ibatis.client.Cache;
import org.ibatis.client.Dialect;
import org.ibatis.persist.criteria.CriteriaBuilder;
import org.ibatis.persist.criteria.CriteriaDelete;
import org.ibatis.persist.criteria.CriteriaQuery;
import org.ibatis.persist.criteria.CriteriaUpdate;
import org.ibatis.persist.meta.EntityType;

import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.sqlmap.client.BatchResult;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapException;
import com.ibatis.sqlmap.client.SqlMapSession;
import com.ibatis.sqlmap.client.event.RowHandler;
import com.ibatis.sqlmap.engine.execution.BatchException;
import com.ibatis.sqlmap.engine.execution.SqlExecutor;
import com.ibatis.sqlmap.engine.mapping.result.ResultObjectFactory;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;

/**
 * Implementation of ExtendedSqlMapClient
 */
public class SqlMapClientImpl implements SqlMapClient, ExtendedSqlMapClient, org.ibatis.client.SqlMapClient {

    static final ILog log = ILogFactory.getLog(SqlMapClientImpl.class);

    /**
     * Delegate for SQL execution
     */
    public SqlMapExecutorDelegate delegate;

    protected ThreadLocal<SqlMapSessionImpl> localSqlMapSession = new ThreadLocal<SqlMapSessionImpl>();

    /**
     * Constructor to supply a delegate
     *
     * @param delegate
     *            - the delegate
     */
    public SqlMapClientImpl(SqlMapExecutorDelegate delegate) {
        this.delegate = delegate;
    }

    public <T> T insert(String id, Object param) throws SQLException {
        return getLocalSqlMapSession().insert(id, param);
    }

    public <T> T insert(String id) throws SQLException {
        return getLocalSqlMapSession().insert(id);
    }

    public int update(String id, Object param) throws SQLException {
        return getLocalSqlMapSession().update(id, param);
    }

    public int update(String id) throws SQLException {
        return getLocalSqlMapSession().update(id);
    }

    public int delete(String id, Object param) throws SQLException {
        return getLocalSqlMapSession().delete(id, param);
    }

    public int delete(String id) throws SQLException {
        return getLocalSqlMapSession().delete(id);
    }

    public <T> T queryForObject(String id, Object paramObject) throws SQLException {
        return getLocalSqlMapSession().queryForObject(id, paramObject);
    }

    public <T> T queryForObject(String id) throws SQLException {
        return getLocalSqlMapSession().queryForObject(id);
    }

    public <T> T queryForObject(String id, Object paramObject, Object resultObject) throws SQLException {
        return getLocalSqlMapSession().queryForObject(id, paramObject, resultObject);
    }

    public <T> List<T> queryForList(String id, Object paramObject) throws SQLException {
        return getLocalSqlMapSession().queryForList(id, paramObject);
    }

    public <T> List<T> queryForList(String id) throws SQLException {
        return getLocalSqlMapSession().queryForList(id);
    }

    public <T> List<T> queryForList(String id, Object paramObject, int skip, int max) throws SQLException {
        return getLocalSqlMapSession().queryForList(id, paramObject, skip, max);
    }

    public <T> List<T> queryForList(String id, int skip, int max) throws SQLException {
        return getLocalSqlMapSession().queryForList(id, skip, max);
    }

    public <K, V> Map<K, V> queryForMap(String id, Object paramObject, String keyProp) throws SQLException {
        return getLocalSqlMapSession().queryForMap(id, paramObject, keyProp);
    }

    public <K, V> Map<K, V> queryForMap(String id, Object paramObject, String keyProp, String valueProp)
        throws SQLException {
        return getLocalSqlMapSession().queryForMap(id, paramObject, keyProp, valueProp);
    }

    public <K, V> Map<K, V> queryForMap(String id, Object paramObject, String keyProp, Class<K> keyType,
        String valueProp, Class<V> valueType) throws SQLException {
        return getLocalSqlMapSession().queryForMap(id, paramObject, keyProp, keyType, valueProp, valueType);
    }

    public void queryWithRowHandler(String id, Object paramObject, RowHandler rowHandler) throws SQLException {
        getLocalSqlMapSession().queryWithRowHandler(id, paramObject, rowHandler);
    }

    public void queryWithRowHandler(String id, RowHandler rowHandler) throws SQLException {
        getLocalSqlMapSession().queryWithRowHandler(id, rowHandler);
    }

    public void startTransaction() throws SQLException {
        getLocalSqlMapSession().startTransaction();
    }

    public void startTransaction(int transactionIsolation) throws SQLException {
        getLocalSqlMapSession().startTransaction(transactionIsolation);
    }

    public void commitTransaction() throws SQLException {
        getLocalSqlMapSession().commitTransaction();
    }

    public void endTransaction() throws SQLException {
        try {
            getLocalSqlMapSession().endTransaction();
        } finally {
            getLocalSqlMapSession().close();
            localSqlMapSession.remove();
        }
    }

    public void startBatch() throws SQLException {
        getLocalSqlMapSession().startBatch(-1);
    }

    public void startBatch(int batchSize) throws SQLException {
        getLocalSqlMapSession().startBatch(batchSize);
    }

    public int executeBatch() throws SQLException {
        return getLocalSqlMapSession().executeBatch();
    }

    public List<BatchResult> executeBatchDetailed() throws SQLException, BatchException {
        return getLocalSqlMapSession().executeBatchDetailed();
    }

    public void setUserConnection(Connection connection) throws SQLException {
        try {
            getLocalSqlMapSession().setUserConnection(connection);
        } finally {
            if (connection == null) {
                getLocalSqlMapSession().close();
            }
        }
    }

    public Connection getCurrentConnection() throws SQLException {
        return getLocalSqlMapSession().getCurrentConnection();
    }

    public DataSource getDataSource() {
        return delegate.getDataSource();
    }

    public MappedStatement getMappedStatement(String id) {
        return delegate.getMappedStatement(id);
    }

    public boolean isLazyLoadingEnabled() {
        return delegate.isLazyLoadingEnabled();
    }

    public boolean isEnhancementEnabled() {
        return delegate.isEnhancementEnabled();
    }

    public SqlExecutor getSqlExecutor() {
        return delegate.getSqlExecutor();
    }

    public SqlMapExecutorDelegate getDelegate() {
        return delegate;
    }

    public SqlMapSession openSession() {
        SqlMapSessionImpl sqlMapSession = new SqlMapSessionImpl(this);
        sqlMapSession.open();
        return sqlMapSession;
    }

    public SqlMapSession openSession(Connection conn) {
        try {
            SqlMapSessionImpl sqlMapSession = new SqlMapSessionImpl(this);
            sqlMapSession.open();
            sqlMapSession.setUserConnection(conn);
            return sqlMapSession;
        } catch (SQLException e) {
            throw new SqlMapException("Error setting user provided connection.  Cause: " + e, e);
        }
    }

    public void flushDataCache() {
        delegate.flushDataCache();
    }

    public void flushDataCache(String cacheId) {
        delegate.flushDataCache(cacheId);
    }

    public void flushEntityCache(Class<?> entityClass) {
        delegate.getEntityManager().flushEntityCache(entityClass);
    }

    protected SqlMapSessionImpl getLocalSqlMapSession() {
        SqlMapSessionImpl sqlMapSession = localSqlMapSession.get();
        if (sqlMapSession == null || sqlMapSession.isClosed()) {
            sqlMapSession = new SqlMapSessionImpl(this);
            localSqlMapSession.set(sqlMapSession);
        }
        return sqlMapSession;
    }

    public ResultObjectFactory getResultObjectFactory() {
        return delegate.getResultObjectFactory();
    }

    public <T> int queryForPage(List<T> page, String id, Object paramObject, int skip, int max) throws SQLException {
        return getLocalSqlMapSession().queryForPage(page, id, paramObject, skip, max);
    }

    public <T> int queryForPage(List<T> page, String id, int skip, int max) throws SQLException {
        return getLocalSqlMapSession().queryForPage(page, id, skip, max);
    }

    public <E> E insertEntity(Class<E> cls, E entity) throws SQLException {
        return getLocalSqlMapSession().insertEntity(cls, entity);
    }

    public <E, K> int updateEntity(Class<E> cls, E entity) throws SQLException {
        return getLocalSqlMapSession().updateEntity(cls, entity);
    }

    public <E, K> int deleteEntity(Class<E> cls, K key) throws SQLException {
        return getLocalSqlMapSession().deleteEntity(cls, key);
    }

    public <E, K> E findEntity(Class<E> cls, K key) throws SQLException {
        return getLocalSqlMapSession().findEntity(cls, key);
    }

    @Override
    public <E> EntityType<E> initEntityClass(Class<E> entityClass) {
        return getLocalSqlMapSession().initEntityClass(entityClass);
    }

    @Override
    public <T> T executeQueryObject(CriteriaQuery<T> criteriaQuery) {
        return getLocalSqlMapSession().executeQueryObject(criteriaQuery);
    }

    @Override
    public <T> List<T> executeQuery(CriteriaQuery<T> criteriaQuery) {
        return getLocalSqlMapSession().executeQuery(criteriaQuery);
    }

    @Override
    public <T> List<T> executeQuery(CriteriaQuery<T> criteriaQuery, int startPosition, int maxResult) {
        return getLocalSqlMapSession().executeQuery(criteriaQuery, startPosition, maxResult);
    }

    @Override
    public <T> int executeQueryPage(CriteriaQuery<T> criteriaQuery, List<T> page, int startPosition, int maxResult) {
        return getLocalSqlMapSession().executeQueryPage(criteriaQuery, page, startPosition, maxResult);
    }

    @Override
    public <T> int executeUpdate(CriteriaUpdate<T> updateQuery) {
        return getLocalSqlMapSession().executeUpdate(updateQuery);
    }

    @Override
    public <T> int executeDelete(CriteriaDelete<T> deleteQuery) {
        return getLocalSqlMapSession().executeDelete(deleteQuery);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return getLocalSqlMapSession().getCriteriaBuilder();
    }

    @Override
    public ResultSet queryForResultSet(String id, Object parameterObject) throws SQLException {
        return getLocalSqlMapSession().queryForResultSet(id, parameterObject);
    }

    @Override
    public <T> T insertArgs(String id, Object... args) throws SQLException {
        return getLocalSqlMapSession().insertArgs(id, args);
    }

    @Override
    public int updateArgs(String id, Object... args) throws SQLException {
        return getLocalSqlMapSession().updateArgs(id, args);
    }

    @Override
    public int deleteArgs(String id, Object... args) throws SQLException {
        return getLocalSqlMapSession().deleteArgs(id, args);
    }

    @Override
    public <T> List<T> queryForListArgs(String id, Object... args) throws SQLException {
        return getLocalSqlMapSession().queryForListArgs(id, args);
    }

    @Override
    public <T> List<T> queryForListArgs(int skip, int max, String id, Object... args) throws SQLException {
        return getLocalSqlMapSession().queryForListArgs(skip, max, id, args);
    }

    @Override
    public <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, Object... args) throws SQLException {
        return getLocalSqlMapSession().queryForMapArgs(id, keyProp, args);
    }

    @Override
    public <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, String valueProp, Object... args)
        throws SQLException {
        return getLocalSqlMapSession().queryForMapArgs(id, keyProp, valueProp, args);
    }

    @Override
    public <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, Class<K> keyType, String valueProp,
        Class<V> valueType, Object... args) throws SQLException {
        return getLocalSqlMapSession().queryForMapArgs(id, keyProp, keyType, valueProp, valueType, args);
    }

    @Override
    public <T> T queryForObjectArgs(String id, Object... args) throws SQLException {
        return getLocalSqlMapSession().queryForObjectArgs(id, args);
    }

    @Override
    public <T> int queryForPageArgs(List<T> page, String id, int skip, int max, Object... args) throws SQLException {
        return getLocalSqlMapSession().queryForPageArgs(page, id, skip, max, args);
    }

    @Override
    public <T> T queryForFirstArgs(String id, Object... args) throws SQLException {
        return getLocalSqlMapSession().queryForFirstArgs(id, args);
    }

    @Override
    public <T> T queryForFirst(String id) throws SQLException {
        return getLocalSqlMapSession().queryForFirst(id);
    }

    @Override
    public <T> T queryForFirst(String id, Object parameterObject) throws SQLException {
        return getLocalSqlMapSession().queryForFirst(id, parameterObject);
    }

    @Override
    public void queryWithRowHandlerArgs(String id, RowHandler rowHandler, Object... args) throws SQLException {
        getLocalSqlMapSession().queryWithRowHandlerArgs(id, rowHandler, args);
    }

    @Override
    public ResultSet queryForResultSetArgs(String id, Object... args) throws SQLException {
        return getLocalSqlMapSession().queryForResultSetArgs(id, args);
    }

    @Override
    public ResultSet queryForResultSet(String id) throws SQLException {
        return getLocalSqlMapSession().queryForResultSet(id);
    }

    @Override
    public String getGlobalProperty(String name) {
        return getDelegate().getState().getGlobalProps().getProperty(name);
    }

    @Override
    public Dialect getDialect() {
        return getDelegate().getState().getDialect();
    }

    public void setRealDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DataSource getRealDataSource() throws SQLException {
        if (dataSource != null)
            return dataSource;
        return getDataSource().unwrap(DataSource.class);
    }

    @Override
    public Cache getCache(String id) {
        return getDelegate().findCacheModel(id);
    }
    
    DataSource dataSource;

}
