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
package com.ibatis.sqlmap.engine.mapping.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ibatis.client.TooManyResultException;

import com.ibatis.common.jdbc.exception.NestedSQLException;
import com.ibatis.sqlmap.client.event.PageHandler;
import com.ibatis.sqlmap.client.event.RowHandler;
import com.ibatis.sqlmap.engine.cache.CacheKey;
import com.ibatis.sqlmap.engine.cache.FlushListener;
import com.ibatis.sqlmap.engine.execution.Batch;
import com.ibatis.sqlmap.engine.execution.ExecuteNotifier;
import com.ibatis.sqlmap.engine.execution.SqlExecutor;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.sql.Sql;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.scope.SessionScope;
import com.ibatis.sqlmap.engine.scope.StatementScope;
import com.ibatis.sqlmap.engine.transaction.Transaction;
import com.ibatis.sqlmap.engine.transaction.TransactionException;

public class MappedStatement implements ExecuteNotifier {
    private String id;
    private Integer resultSetType;
    private Integer fetchSize;
    private ResultMap resultMap;
    private ParameterMap parameterMap;
    private Class<?> parameterClass;
    private Sql sql;
    private int baseCacheKey;
    private SqlMapExecutorDelegate delegate;
    private Integer timeout;
    private ResultMap[] additionalResultMaps = new ResultMap[0];
    private List<FlushListener> executeListeners = new ArrayList<FlushListener>();
    private Map<Class<?>, FlushListener> flushEntitys = new HashMap<Class<?>, FlushListener>();
    private Map<String, FlushListener> flushCacheModels = new HashMap<String, FlushListener>();
    private List<String> flushCacheRoots = new ArrayList<String>();
    private String resource;
    boolean canBatch;

    public StatementType getStatementType() {
        return StatementType.UNKNOWN;
    }

    public int executeUpdate(StatementScope statementScope, Transaction trans, Object parameterObject)
        throws SQLException {
        ErrorContext errorContext = statementScope.getErrorContext();
        errorContext.setActivity("preparing the mapped statement for execution");
        errorContext.setObjectId(this.getId());
        errorContext.setResource(this.getResource());

        statementScope.getSession().setCommitRequired(true);

        try {
            parameterObject = validateParameter(parameterObject);

            Sql sql = getSql();

            errorContext.setMoreInfo("Check the parameter map.");
            ParameterMap parameterMap = sql.getParameterMap(statementScope, parameterObject);

            errorContext.setMoreInfo("Check the result map.");
            ResultMap resultMap = sql.getResultMap(statementScope, parameterObject);

            statementScope.setResultMap(resultMap);
            statementScope.setParameterMap(parameterMap);

            int rows = 0;

            errorContext.setMoreInfo("Check the parameter map.");
            Object[] parameters = parameterMap.getParameterObjectValues(statementScope, parameterObject);

            errorContext.setMoreInfo("Check the SQL statement.");
            String sqlString = sql.getSql(statementScope, parameterObject);

            errorContext.setActivity("executing mapped statement");
            errorContext.setMoreInfo("Check the statement or the result map.");
            errorContext.setConnection(trans.getConnection());
            rows = sqlExecuteUpdate(statementScope, trans.getConnection(), sqlString, parameters);

            errorContext.setMoreInfo("Check the output parameters.");
            if (parameterObject != null) {
                postProcessParameterObject(statementScope, parameterObject, parameters);
            }

            // errorContext.reset();
            sql.cleanup(statementScope);
            notifyListeners(statementScope.getSession());
            return rows;
        } catch (SQLException e) {
            errorContext.setCause(e);
            throw new NestedSQLException(errorContext.toString(), e.getSQLState(), e.getErrorCode(), e);
        } catch (Exception e) {
            errorContext.setCause(e);
            throw new NestedSQLException(errorContext.toString(), e);
        }
    }

    public <T> T executeQueryForObject(StatementScope statementScope, Transaction trans, Object parameterObject,
        Object resultObject) throws SQLException {
        try {
            T object = null;

            DefaultRowHandler rowHandler = new DefaultRowHandler();
            executeQueryWithCallback(statementScope, trans.getConnection(), parameterObject, resultObject, rowHandler,
                SqlExecutor.ZERO, SqlExecutor.TWO);
            List<T> list = rowHandler.getList();

            if (list.size() > 1) {
                ErrorContext errorContext = statementScope.getErrorContext();
                errorContext.setActivity(null);
                errorContext.setMoreInfo("executeQueryForObject returned too many results.");

                throw new NestedSQLException(errorContext.toString(), new TooManyResultException(list.size()));
            } else if (list.size() > 0) {
                object = list.get(0);
            }

            return object;
        } catch (TransactionException e) {
            throw new NestedSQLException("Error getting Connection from Transaction.  Cause: " + e, e);
        }
    }

    public <T> List<T> executeQueryForList(StatementScope statementScope, Transaction trans, Object parameterObject,
        int skipResults, int maxResults) throws SQLException {
        try {
            DefaultRowHandler rowHandler = new DefaultRowHandler();
            executeQueryWithCallback(statementScope, trans.getConnection(), parameterObject, null, rowHandler,
                skipResults, maxResults);
            return rowHandler.getList();
        } catch (TransactionException e) {
            throw new NestedSQLException("Error getting Connection from Transaction.  Cause: " + e, e);
        }
    }

    public void executeQueryWithRowHandler(StatementScope statementScope, Transaction trans, Object parameterObject,
        RowHandler rowHandler) throws SQLException {
        try {
            executeQueryWithCallback(statementScope, trans.getConnection(), parameterObject, null, rowHandler,
                SqlExecutor.ZERO, SqlExecutor.NO_LIMIT);
        } catch (TransactionException e) {
            throw new NestedSQLException("Error getting Connection from Transaction.  Cause: " + e, e);
        }
    }

    //
    // PROTECTED METHODS
    //

    protected void executeQueryWithCallback(StatementScope statementScope, Connection conn, Object parameterObject,
        Object resultObject, RowHandler rowHandler, int skipResults, int maxResults) throws SQLException {
        ErrorContext errorContext = statementScope.getErrorContext();
        errorContext.setActivity("preparing the mapped statement for execution");
        errorContext.setObjectId(this.getId());
        errorContext.setConnection(conn);
        errorContext.setResource(this.getResource());

        try {
            parameterObject = validateParameter(parameterObject);

            Sql sql = getSql();

            errorContext.setMoreInfo("Check the parameter map.");
            ParameterMap parameterMap = sql.getParameterMap(statementScope, parameterObject);

            errorContext.setMoreInfo("Check the result map.");
            ResultMap resultMap = sql.getResultMap(statementScope, parameterObject);

            statementScope.setResultMap(resultMap);
            statementScope.setParameterMap(parameterMap);

            errorContext.setMoreInfo("Check the parameter map.");
            Object[] parameters = parameterMap.getParameterObjectValues(statementScope, parameterObject);

            errorContext.setMoreInfo("Check the SQL statement.");
            String sqlString = sql.getSql(statementScope, parameterObject);

            errorContext.setActivity("executing mapped statement");
            errorContext.setMoreInfo("Check the SQL statement or the result map.");
            RowHandlerCallback callback = new RowHandlerCallback(resultMap, resultObject, rowHandler);
            sqlExecuteQuery(statementScope, conn, sqlString, parameters, skipResults, maxResults, callback);

            errorContext.setMoreInfo("Check the output parameters.");
            if (parameterObject != null) {
                postProcessParameterObject(statementScope, parameterObject, parameters);
            }

            // errorContext.reset();
            sql.cleanup(statementScope);
            notifyListeners(null);
        } catch (SQLException e) {
            errorContext.setCause(e);
            throw new NestedSQLException(errorContext.toString(), e.getSQLState(), e.getErrorCode(), e);
        } catch (Exception e) {
            errorContext.setCause(e);
            throw new NestedSQLException(errorContext.toString(), e);
        }
    }

    protected void postProcessParameterObject(StatementScope statementScope, Object parameterObject,
        Object[] parameters) {
    }

    protected int sqlExecuteUpdate(StatementScope statementScope, Connection conn, String sqlString,
        Object[] parameters) throws SQLException {
        if (isCanBatch() && statementScope.getSession().isInBatch()) {
            return getSqlExecutor().addBatch(statementScope, conn, sqlString, parameters);
        } else {
            return getSqlExecutor().executeUpdate(getId(), statementScope, conn, sqlString, parameters);
        }
    }

    protected void sqlExecuteQuery(StatementScope statementScope, Connection conn, String sqlString,
        Object[] parameters, int skipResults, int maxResults, RowHandlerCallback callback) throws SQLException {
        if (callback.getRowHandler() instanceof PageHandler) {
            getSqlExecutor().executeQueryPage(getId(), statementScope, conn, sqlString, parameters, skipResults,
                maxResults, callback, ((PageHandler) callback.getRowHandler()));
        } else {
            getSqlExecutor().executeQuery(getId(), statementScope, conn, sqlString, parameters, skipResults, maxResults,
                callback);
        }
    }

    protected Object validateParameter(Object param) throws SQLException {
        Object newParam = param;
        Class<?> parameterClass = getParameterClass();
        if (newParam != null && parameterClass != null) {
            if (!parameterClass.isAssignableFrom(newParam.getClass())) {
                throw new SQLException("Invalid parameter object type.  Expected '" + parameterClass.getName()
                    + "' but found '" + newParam.getClass().getName() + "'.");
            }
        }
        return newParam;
    }

    @Override
    public String getId() {
        return id;
    }

    public Integer getResultSetType() {
        return resultSetType;
    }

    public void setResultSetType(Integer resultSetType) {
        this.resultSetType = resultSetType;
    }

    public Integer getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(Integer fetchSize) {
        this.fetchSize = fetchSize;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Sql getSql() {
        return sql;
    }

    public void setSql(Sql sql) {
        this.sql = sql;
    }

    public ResultMap getResultMap() {
        return resultMap;
    }

    public void setResultMap(ResultMap resultMap) {
        this.resultMap = resultMap;
    }

    public ParameterMap getParameterMap() {
        return parameterMap;
    }

    public void setParameterMap(ParameterMap parameterMap) {
        this.parameterMap = parameterMap;
    }

    public Class<?> getParameterClass() {
        return parameterClass;
    }

    public void setParameterClass(Class<?> parameterClass) {
        this.parameterClass = parameterClass;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public boolean isCanBatch() {
        return canBatch;
    }

    public void setCanBatch(boolean canBatch) {
        this.canBatch = canBatch;
    }

    public CacheKey getCacheKey(StatementScope statementScope, Object parameterObject) {
        Sql sql = statementScope.getSql();
        ParameterMap pmap = sql.getParameterMap(statementScope, parameterObject);
        CacheKey cacheKey = pmap.getCacheKey(statementScope, parameterObject);
        cacheKey.update(id);
        cacheKey.update(baseCacheKey);
        cacheKey.update(sql.getSql(statementScope, parameterObject)); // Fixes bug 953001
        return cacheKey;
    }

    public int getBaseCacheKey() {
        return baseCacheKey;
    }

    public void setBaseCacheKey(int base) {
        this.baseCacheKey = base;
    }

    public synchronized void addExecuteListener(FlushListener listener) {
        if (!executeListeners.contains(listener)) {
            executeListeners.add(listener);
        }
    }

    @Override
    public synchronized void notifyListeners(Object arg) {
        if (arg instanceof SessionScope) {
            Batch batch = ((SessionScope) arg).getBatch();
            if (batch != null && !batch.isCleanup()) {
                if (batch.hasNotifier(this)) {
                    return;
                }
            }
        }

        long timestamp = arg instanceof Long ? ((Long) arg).longValue() : System.currentTimeMillis();

        for (String name : flushCacheRoots) {
            getDelegate().getCacheRoots().flushRoots(timestamp, name);
        }

        for (FlushListener flushListener : executeListeners) {
            flushListener.onFlush(getId(), timestamp);
        }

        for (Map.Entry<Class<?>, FlushListener> fe : flushEntitys.entrySet()) {
            FlushListener cache = fe.getValue();
            if (cache != null) {
                cache.onFlush(getId(), timestamp);
            } else {
                cache = getDelegate().getEntityManager().findEntityCache(fe.getKey());
                if (cache != null) {
                    fe.setValue(cache);
                    cache.onFlush(getId(), timestamp);
                }
            }
        }

        for (Map.Entry<String, FlushListener> me : flushCacheModels.entrySet()) {
            FlushListener cache = me.getValue();
            if (cache != null) {
                cache.onFlush(getId(), timestamp);
            } else {
                cache = getDelegate().findCacheModel(me.getKey());
                if (cache != null) {
                    me.setValue(cache);
                    cache.onFlush(getId(), timestamp);
                }
            }
        }
    }

    public SqlExecutor getSqlExecutor() {
        return delegate.getSqlExecutor();
    }

    public SqlMapExecutorDelegate getDelegate() {
        return delegate;
    }

    public void setDelegate(SqlMapExecutorDelegate delegate) {
        this.delegate = delegate;
    }

    public void initRequest(StatementScope statementScope) {
        statementScope.setStatement(this);
        statementScope.setParameterMap(parameterMap);
        statementScope.setResultMap(resultMap);
        statementScope.setSql(sql);
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public void addResultMap(ResultMap resultMap) {
        ResultMap[] resultMaps = Arrays.copyOf(additionalResultMaps, additionalResultMaps.length + 1);
        resultMaps[additionalResultMaps.length] = resultMap;
        additionalResultMaps = resultMaps;
    }

    public boolean hasMultipleResultMaps() {
        return additionalResultMaps.length > 0;
    }

    public ResultMap[] getAdditionalResultMaps() {
        return additionalResultMaps;
    }

    public <T> int executeQueryForPage(StatementScope statementScope, List<T> page, Transaction trans,
        Object paramObject, int skipResults, int maxResults) throws SQLException {
        try {
            PageHandler rowHandler = new PageHandler(page);
            executeQueryWithCallback(statementScope, trans.getConnection(), paramObject, null, rowHandler, skipResults,
                maxResults);
            return rowHandler.getTotal();
        } catch (TransactionException e) {
            throw new NestedSQLException("Error getting Connection from Transaction.  Cause: " + e, e);
        }
    }

    public <K, V> void executeQueryWithMapHandler(StatementScope statementScope, Transaction trans,
        Object parameterObject, int skipResults, int maxResults, MappedRowHandler<K, V> mapHandler)
        throws SQLException {
        try {
            executeQueryWithCallback(statementScope, trans.getConnection(), parameterObject, null, mapHandler,
                skipResults, maxResults);
        } catch (TransactionException e) {
            throw new NestedSQLException("Error getting Connection from Transaction.  Cause: " + e, e);
        }
    }

    public synchronized void addFlushEntityCache(Class<?> entityClass) {
        flushEntitys.put(entityClass, null);
    }

    public synchronized void addFlushCacheModel(String cacheModel) {
        flushCacheModels.put(cacheModel, null);
    }

    public synchronized void addFlushCacheRoot(String name) {
        if (!flushCacheRoots.contains(name)) {
            flushCacheRoots.add(name);
        }
    }

    public void checkSql(ErrorContext ec) {
    }
}
