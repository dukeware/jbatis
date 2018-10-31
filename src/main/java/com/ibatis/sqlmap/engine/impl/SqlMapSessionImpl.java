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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.ibatis.persist.criteria.CriteriaBuilder;
import org.ibatis.persist.criteria.CriteriaDelete;
import org.ibatis.persist.criteria.CriteriaQuery;
import org.ibatis.persist.criteria.CriteriaUpdate;
import org.ibatis.persist.meta.EntityType;

import com.ibatis.common.ArrayMap;
import com.ibatis.common.jdbc.exception.NestedSQLException;
import com.ibatis.sqlmap.client.BatchResult;
import com.ibatis.sqlmap.client.SqlMapSession;
import com.ibatis.sqlmap.client.event.RowHandler;
import com.ibatis.sqlmap.client.event.RowSetHandler;
import com.ibatis.sqlmap.engine.execution.BatchException;
import com.ibatis.sqlmap.engine.execution.SqlExecutor;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;
import com.ibatis.sqlmap.engine.scope.SessionScope;
import com.ibatis.sqlmap.engine.transaction.Transaction;
import com.ibatis.sqlmap.engine.transaction.TransactionException;

/**
 * Implementation of SqlMapSession
 */
public class SqlMapSessionImpl implements SqlMapSession {

    protected SqlMapExecutorDelegate delegate;
    protected SessionScope sessionScope;
    protected boolean closed;

    /**
     * Constructor
     *
     * @param client
     *            - the client that will use the session
     */
    public SqlMapSessionImpl(SqlMapClientImpl client) {
        this.delegate = client.getDelegate();
        this.sessionScope = this.delegate.beginSessionScope();
        this.sessionScope.setSqlMapClient(client);
        this.sessionScope.setSqlMapExecutor(client);
        this.sessionScope.setSqlMapTxMgr(client);
        this.closed = false;
    }

    public SessionScope getSessionScope() {
        return sessionScope;
    }

    /**
     * Start the session
     */
    public void open() {
        sessionScope.setSqlMapTxMgr(this);
    }

    /**
     * Getter to tell if the session is still open
     *
     * @return - the status of the session
     */
    public boolean isClosed() {
        return closed;
    }

    public void close() {
        if (delegate != null && sessionScope != null)
            delegate.endSessionScope(sessionScope);
        if (sessionScope != null)
            sessionScope = null;
        if (delegate != null)
            delegate = null;
        if (!closed)
            closed = true;
    }

    public <T> T insert(String id, Object param) throws SQLException {
        return delegate.insert(sessionScope, id, param);
    }

    public <T> T insert(String id) throws SQLException {
        return insert(id, null);
    }

    public int update(String id, Object param) throws SQLException {
        return delegate.update(sessionScope, id, param);
    }

    public int update(String id) throws SQLException {
        return update(id, null);
    }

    public int delete(String id, Object param) throws SQLException {
        return delegate.delete(sessionScope, id, param);
    }

    public int delete(String id) throws SQLException {
        return delete(id, null);
    }

    public <T> T queryForObject(String id, Object paramObject) throws SQLException {
        return delegate.queryForObject(sessionScope, id, paramObject);
    }

    public <T> T queryForObject(String id) throws SQLException {
        return queryForObject(id, null);
    }

    public <T> T queryForObject(String id, Object paramObject, Object resultObject) throws SQLException {
        return delegate.queryForObject(sessionScope, id, paramObject, resultObject);
    }

    public <T> List<T> queryForList(String id, Object paramObject) throws SQLException {
        return delegate.queryForList(sessionScope, id, paramObject);
    }

    public <T> List<T> queryForList(String id) throws SQLException {
        return queryForList(id, null);
    }

    public <T> List<T> queryForList(String id, Object paramObject, int skip, int max) throws SQLException {
        return delegate.queryForList(sessionScope, id, paramObject, skip, max);
    }

    public <T> List<T> queryForList(String id, int skip, int max) throws SQLException {
        return queryForList(id, null, skip, max);
    }

    public <K, V> Map<K, V> queryForMap(String id, Object paramObject, String keyProp) throws SQLException {
        return delegate.queryForMap(sessionScope, id, paramObject, keyProp, null, null, null);
    }

    public <K, V> Map<K, V> queryForMap(String id, Object paramObject, String keyProp, String valueProp)
        throws SQLException {
        return delegate.queryForMap(sessionScope, id, paramObject, keyProp, null, valueProp, null);
    }

    public <K, V> Map<K, V> queryForMap(String id, Object paramObject, String keyProp, Class<K> keyType,
        String valueProp, Class<V> valueType) throws SQLException {
        return delegate.queryForMap(sessionScope, id, paramObject, keyProp, keyType, valueProp, valueType);
    }

    public void queryWithRowHandler(String id, Object paramObject, RowHandler rowHandler) throws SQLException {
        delegate.queryWithRowHandler(sessionScope, id, paramObject, rowHandler);
    }

    public void queryWithRowHandler(String id, RowHandler rowHandler) throws SQLException {
        queryWithRowHandler(id, null, rowHandler);
    }

    public void startTransaction() throws SQLException {
        delegate.startTransaction(sessionScope);
    }

    public void startTransaction(int transactionIsolation) throws SQLException {
        delegate.startTransaction(sessionScope, transactionIsolation);
    }

    public void commitTransaction() throws SQLException {
        delegate.commitTransaction(sessionScope);
    }

    public void endTransaction() throws SQLException {
        delegate.endTransaction(sessionScope);
    }

    public void startBatch() throws SQLException {
        delegate.startBatch(sessionScope, -1);
    }
    
    public void startBatch(int batchSize) throws SQLException {
        delegate.startBatch(sessionScope, batchSize);
    }

    public int executeBatch() throws SQLException {
        return delegate.executeBatch(sessionScope);
    }

    public List<BatchResult> executeBatchDetailed() throws SQLException, BatchException {
        return delegate.executeBatchDetailed(sessionScope);
    }

    public void setUserConnection(Connection connection) throws SQLException {
        delegate.setUserProvidedTransaction(sessionScope, connection);
    }

    public Connection getCurrentConnection() throws SQLException {
        try {
            Connection conn = null;
            Transaction trans = delegate.getTransaction(sessionScope);
            if (trans != null) {
                conn = trans.getConnection();
            }
            return conn;
        } catch (TransactionException e) {
            throw new NestedSQLException("Error getting Connection from Transaction.  Cause: " + e, e);
        }
    }

    public DataSource getDataSource() {
        return delegate.getDataSource();
    }

    /**
     * Gets a mapped statement by ID
     *
     * @param id
     *            - the ID
     * @return - the mapped statement
     */
    public MappedStatement getMappedStatement(String id) {
        return delegate.getMappedStatement(id);
    }

    /**
     * Get the status of lazy loading
     *
     * @return - the status
     */
    public boolean isLazyLoadingEnabled() {
        return delegate.isLazyLoadingEnabled();
    }

    /**
     * Get the status of CGLib enhancements
     *
     * @return - the status
     */
    public boolean isEnhancementEnabled() {
        return delegate.isEnhancementEnabled();
    }

    /**
     * Get the SQL executor
     *
     * @return - the executor
     */
    public SqlExecutor getSqlExecutor() {
        return delegate.getSqlExecutor();
    }

    /**
     * Get the delegate
     *
     * @return - the delegate
     */
    public SqlMapExecutorDelegate getDelegate() {
        return delegate;
    }

    public <T> int queryForPage(List<T> page, String id, Object paramObject, int skip, int max) throws SQLException {
        return delegate.queryForPage(sessionScope, page, id, paramObject, skip, max);
    }

    public <T> int queryForPage(List<T> page, String id, int skip, int max) throws SQLException {
        return delegate.queryForPage(sessionScope, page, id, null, skip, max);
    }

    private Map<String, Object> toParameter(Object[] args) {
        if (args == null || args.length == 0) {
            return Collections.emptyMap();
        }

        return new ArrayMap(args);
    }

    public <T> int queryForPageArgs(List<T> page, String id, int skip, int max, Object... args) throws SQLException {
        return delegate.queryForPage(sessionScope, page, id, toParameter(args), skip, max);
    }

    public <E> E insertEntity(Class<E> cls, E entity) throws SQLException {
        return delegate.getEntityManager().insertEntity(sessionScope, cls, entity);
    }

    public <E, K> int updateEntity(Class<E> cls, E entity) throws SQLException {
        return delegate.getEntityManager().updateEntity(sessionScope, cls, entity);
    }

    public <E, K> int deleteEntity(Class<E> cls, K key) throws SQLException {
        return delegate.getEntityManager().deleteEntity(sessionScope, cls, key);
    }

    public <E, K> E findEntity(Class<E> cls, K key) throws SQLException {
        return delegate.getEntityManager().findEntity(sessionScope, cls, key);
    }

    @Override
    public <E> EntityType<E> initEntityClass(Class<E> entityClass) {
        return delegate.getEntityManager().initEntityClass(entityClass);
    }

    @Override
    public <T> T executeQueryObject(CriteriaQuery<T> criteriaQuery) {
        return delegate.getEntityManager().executeQueryObject(sessionScope, criteriaQuery);
    }

    @Override
    public <T> List<T> executeQuery(CriteriaQuery<T> criteriaQuery) {
        return delegate.getEntityManager().executeQuery(sessionScope, criteriaQuery);
    }

    @Override
    public <T> List<T> executeQuery(CriteriaQuery<T> criteriaQuery, int startPosition, int maxResult) {
        return delegate.getEntityManager().executeQuery(sessionScope, criteriaQuery, startPosition, maxResult);
    }

    @Override
    public <T> int executeQueryPage(CriteriaQuery<T> criteriaQuery, List<T> page, int startPosition, int maxResult) {
        return delegate.getEntityManager().executeQueryPage(sessionScope, criteriaQuery, page, startPosition, maxResult);
    }

    @Override
    public <T> int executeUpdate(CriteriaUpdate<T> updateQuery) {
        return delegate.getEntityManager().executeUpdate(sessionScope, updateQuery);
    }

    @Override
    public <T> int executeDelete(CriteriaDelete<T> deleteQuery) {
        return delegate.getEntityManager().executeDelete(sessionScope, deleteQuery);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return delegate.getEntityManager().getCriteriaBuilder();
    }

    @Override
    public ResultSet queryForResultSet(String id, Object paramObject) throws SQLException {
        RowSetHandler rsh = new RowSetHandler();
        delegate.queryWithRowHandler(sessionScope, id, paramObject, rsh);
        return rsh.getRowSet();
    }

    @Override
    public <T> T insertArgs(String id, Object... args) throws SQLException {
        return delegate.insert(sessionScope, id, toParameter(args));
    }

    @Override
    public int updateArgs(String id, Object... args) throws SQLException {
        return delegate.update(sessionScope, id, toParameter(args));
    }

    @Override
    public int deleteArgs(String id, Object... args) throws SQLException {
        return delegate.delete(sessionScope, id, toParameter(args));
    }

    @Override
    public <T> List<T> queryForListArgs(String id, Object... args) throws SQLException {
        return delegate.queryForList(sessionScope, id, toParameter(args));
    }

    @Override
    public <T> List<T> queryForListArgs(int skip, int max, String id, Object... args) throws SQLException {
        return delegate.queryForList(sessionScope, id, toParameter(args), skip, max);
    }

    @Override
    public <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, Object... args) throws SQLException {
        return delegate.queryForMap(sessionScope, id, toParameter(args), keyProp, null, null, null);
    }

    @Override
    public <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, String valueProp, Object... args)
        throws SQLException {
        return delegate.queryForMap(sessionScope, id, toParameter(args), keyProp, null, valueProp, null);
    }

    @Override
    public <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, Class<K> keyType, String valueProp,
        Class<V> valueType, Object... args) throws SQLException {
        return delegate.queryForMap(sessionScope, id, toParameter(args), keyProp, keyType, valueProp, valueType);
    }

    @Override
    public <T> T queryForObjectArgs(String id, Object... args) throws SQLException {
        return delegate.queryForObject(sessionScope, id, toParameter(args));
    }

    @Override
    public <T> T queryForFirstArgs(String id, Object... args) throws SQLException {
        return queryForFirst(id, toParameter(args));
    }

    @Override
    public <T> T queryForFirst(String id) throws SQLException {
        return queryForFirst(id, null);
    }

    @Override
    public <T> T queryForFirst(String id, Object parameterObject) throws SQLException {
        List<T> list = delegate.queryForList(sessionScope, id, parameterObject, SqlExecutor.ZERO, SqlExecutor.ONE);
        if (list == null || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    @Override
    public void queryWithRowHandlerArgs(String id, RowHandler rowHandler, Object... args) throws SQLException {
        delegate.queryWithRowHandler(sessionScope, id, toParameter(args), rowHandler);
    }

    @Override
    public ResultSet queryForResultSetArgs(String id, Object... args) throws SQLException {
        return queryForResultSet(id, toParameter(args));
    }

    @Override
    public ResultSet queryForResultSet(String id) throws SQLException {
        return queryForResultSet(id, null);
    }
}
