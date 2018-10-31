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

import com.ibatis.common.Objects;
import com.ibatis.common.Page;
import com.ibatis.sqlmap.client.event.RowHandler;
import com.ibatis.sqlmap.engine.cache.CacheKey;
import com.ibatis.sqlmap.engine.cache.CacheModel;
import com.ibatis.sqlmap.engine.cache.FlushListener;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.sql.Sql;
import com.ibatis.sqlmap.engine.scope.StatementScope;
import com.ibatis.sqlmap.engine.transaction.Transaction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CachingStatement extends MappedStatement {

    private MappedStatement statement;
    private CacheModel cacheModel;

    public CachingStatement(MappedStatement statement, CacheModel cacheModel) {
        this.statement = statement;
        this.cacheModel = cacheModel;
    }

    @Override
    public String getId() {
        return statement.getId();
    }

    @Override
    public StatementType getStatementType() {
        return statement.getStatementType();
    }

    @Override
    public Integer getResultSetType() {
        return statement.getResultSetType();
    }

    @Override
    public Integer getFetchSize() {
        return statement.getFetchSize();
    }

    @Override
    public ParameterMap getParameterMap() {
        return statement.getParameterMap();
    }

    @Override
    public ResultMap getResultMap() {
        return statement.getResultMap();
    }

    @Override
    public int executeUpdate(StatementScope statementScope, Transaction trans, Object parameterObject)
        throws SQLException {
        int n = statement.executeUpdate(statementScope, trans, parameterObject);
        return n;
    }

    @Override
    public <T> T executeQueryForObject(StatementScope statementScope, Transaction trans, Object parameterObject,
        Object resultObject) throws SQLException {
        CacheKey cacheKey = getCacheKey(statementScope, parameterObject);
        cacheKey.update("executeQueryForObject");
        Object old = cacheModel.getObject(cacheKey);
        if (old == CacheModel.NULL_OBJECT) {
            // This was cached, but null
            return null;
        } else if (old == null) {
            T t = statement.<T>executeQueryForObject(statementScope, trans, parameterObject, resultObject);
            cacheModel.putObject(cacheKey, t);
            return t;
        }
        return Objects.uncheckedCast(old);
    }

    @Override
    public <T> int executeQueryForPage(StatementScope statementScope, List<T> page, Transaction trans,
        Object paramObject, int skipResults, int maxResults) throws SQLException {
        CacheKey cacheKey = getCacheKey(statementScope, paramObject);
        cacheKey.update("executeQueryForPage");
        cacheKey.update(page != null);
        cacheKey.update(skipResults);
        cacheKey.update(maxResults);
        Object old = cacheModel.getObject(cacheKey);
        Page<T> pageObject = null;
        if (old instanceof Page<?>) {
            pageObject = Objects.uncheckedCast(old);
            if (page != null) {
                page.addAll(pageObject.list);
            }
            return pageObject.total;
        } else {
            ArrayList<T> p = new ArrayList<T>();
            int t = statement.executeQueryForPage(statementScope, p, trans, paramObject, skipResults, maxResults);
            if (page != null) {
                page.addAll(p);
            }
            cacheModel.putObject(cacheKey, new Page<T>(t, p));
            return t;
        }
    }

    @Override
    public <T> List<T> executeQueryForList(StatementScope statementScope, Transaction trans, Object parameterObject,
        int skipResults, int maxResults) throws SQLException {
        CacheKey cacheKey = getCacheKey(statementScope, parameterObject);
        cacheKey.update("executeQueryForList");
        cacheKey.update(skipResults);
        cacheKey.update(maxResults);
        Object old = cacheModel.getObject(cacheKey);
        if (old == CacheModel.NULL_OBJECT) {
            // The cached object was null
            return null;
        } else if (old == null) {
            List<T> list = statement.executeQueryForList(statementScope, trans, parameterObject, skipResults, maxResults);
            cacheModel.putObject(cacheKey, list);
            return list;
        }
        return Objects.<List<T>>uncheckedCast(old);
    }

    @Override
    public void executeQueryWithRowHandler(StatementScope statementScope, Transaction trans, Object parameterObject,
        RowHandler rowHandler) throws SQLException {
        statement.executeQueryWithRowHandler(statementScope, trans, parameterObject, rowHandler);
    }

    @Override
    public <K, V> void executeQueryWithMapHandler(StatementScope statementScope, Transaction trans, Object parameterObject,
        int skipResults, int maxResults, MappedRowHandler<K, V> mapHandler) throws SQLException {
        CacheKey cacheKey = getCacheKey(statementScope, parameterObject);
        cacheKey.update("executeQueryWithMapHandler");
        cacheKey.update(skipResults);
        cacheKey.update(maxResults);
        Object old = cacheModel.getObject(cacheKey);
        if (old == CacheModel.NULL_OBJECT) {
            mapHandler.setMap(null);
        } else if (old == null) {
            statement.executeQueryWithMapHandler(statementScope, trans, parameterObject, skipResults, maxResults, mapHandler);
            cacheModel.putObject(cacheKey, mapHandler.getMap());
        } else {
            mapHandler.setMap(Objects.<Map<K, V>>uncheckedCast(old));
        }
    }

    @Override
    public CacheKey getCacheKey(StatementScope statementScope, Object parameterObject) {
        CacheKey key = statement.getCacheKey(statementScope, parameterObject);
        return key;
    }

    @Override
    public int getBaseCacheKey() {
        return statement.getBaseCacheKey();
    }

    @Override
    public void setBaseCacheKey(int base) {
        statement.setBaseCacheKey(base);
    }

    @Override
    public void addExecuteListener(FlushListener listener) {
        statement.addExecuteListener(listener);
    }

    @Override
    public void notifyListeners(Object arg) {
        statement.notifyListeners(arg);
    }

    @Override
    public void initRequest(StatementScope statementScope) {
        statement.initRequest(statementScope);
    }

    @Override
    public Sql getSql() {
        return statement.getSql();
    }

    @Override
    public Class<?> getParameterClass() {
        return statement.getParameterClass();
    }

    @Override
    public Integer getTimeout() {
        return statement.getTimeout();
    }

    @Override
    public boolean hasMultipleResultMaps() {
        return statement.hasMultipleResultMaps();
    }

    @Override
    public ResultMap[] getAdditionalResultMaps() {
        return statement.getAdditionalResultMaps();
    }

}
