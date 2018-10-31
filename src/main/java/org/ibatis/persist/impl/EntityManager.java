/*-
 * Copyright 2012 Owl Group
 * All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 */

package org.ibatis.persist.impl;

import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ibatis.client.Cache;
import org.ibatis.client.SqlExecutor;
import org.ibatis.persist.PersistenceException;
import org.ibatis.persist.criteria.CriteriaBuilder;
import org.ibatis.persist.criteria.CriteriaDelete;
import org.ibatis.persist.criteria.CriteriaQuery;
import org.ibatis.persist.criteria.CriteriaUpdate;
import org.ibatis.persist.meta.EntityType;

import com.ibatis.common.Objects;
import com.ibatis.common.Page;
import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.sqlmap.client.event.PageHandler;
import com.ibatis.sqlmap.engine.builder.xml.SqlMapParser;
import com.ibatis.sqlmap.engine.builder.xml.XmlParserState;
import com.ibatis.sqlmap.engine.cache.CacheKey;
import com.ibatis.sqlmap.engine.cache.CacheModel;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.result.AutoResultMap;
import com.ibatis.sqlmap.engine.mapping.statement.DefaultRowHandler;
import com.ibatis.sqlmap.engine.mapping.statement.RowHandlerCallback;
import com.ibatis.sqlmap.engine.scope.SessionScope;
import com.ibatis.sqlmap.engine.scope.StatementScope;
import com.ibatis.sqlmap.engine.transaction.Transaction;

/**
 * EntityManagerImpl
 * <p>
 * Date: 2014-10-30,13:08:38 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
@SuppressWarnings("unchecked")
public class EntityManager {
    static final ILog log = ILogFactory.getLog(EntityManager.class);

    SqlMapExecutorDelegate delegate;
    XmlParserState state;
    CriteriaBuilderImpl criteriaBuilderImpl;

    public EntityManager(SqlMapExecutorDelegate delegate, XmlParserState state) {
        this.delegate = delegate;
        this.state = state;
    }

    public SqlMapExecutorDelegate getDelegate() {
        return delegate;
    }

    public <E> E insertEntity(SessionScope sessionScope, Class<E> cls, E entity) throws SQLException {
        EntityType<E> ep = initEntityClass(cls);
        if (ep == null || ep.isFailed()) {
            throw new RuntimeException(ep.getErrorMessage());
        }
        String statId = ep.getInsertStatementId();
        delegate.insert(sessionScope, statId, ep.getInsertParameter(entity));
        return entity;
    }

    public <E, K> int updateEntity(SessionScope sessionScope, Class<E> cls, E entity) throws SQLException {
        EntityType<E> ep = initEntityClass(cls);
        if (ep == null || ep.isFailed()) {
            throw new RuntimeException(ep.getErrorMessage());
        }
        String statId = ep.getUpdateStatementId();
        return delegate.update(sessionScope, statId, ep.getUpdateParameter(entity));
    }

    public <E, K> int deleteEntity(SessionScope sessionScope, Class<E> cls, K key) throws SQLException {
        EntityType<E> ep = initEntityClass(cls);
        if (ep.isFailed()) {
            throw new RuntimeException(ep.getErrorMessage());
        }
        String statId = ep.getDeleteStatementId();
        return delegate.update(sessionScope, statId, ep.getDeleteParameter(key));
    }

    public <E, K> E findEntity(SessionScope sessionScope, Class<E> cls, K key) throws SQLException {
        EntityType<E> ep = initEntityClass(cls);
        if (ep.isFailed()) {
            throw new RuntimeException(ep.getErrorMessage());
        }
        String statId = ep.getFindStatementId();
        return delegate.<E>queryForObject(sessionScope, statId, ep.getFindParameter(key));
    }

    public <T> T executeQueryObject(final SessionScope sessionScope, final CriteriaQuery<T> criteriaQuery) {
        List<T> list = executeQuery(sessionScope, criteriaQuery, SqlExecutor.ZERO, SqlExecutor.TWO);
        T t = null;
        if (list.size() > 1) {
            throw new PersistenceException("executeQueryObject returned too many results.");
        } else if (list.size() > 0) {
            t = list.get(0);
        }
        return t;
    }

    public <T> List<T> executeQuery(final SessionScope sessionScope, final CriteriaQuery<T> criteriaQuery) {
        return executeQuery(sessionScope, criteriaQuery, SqlExecutor.ZERO, SqlExecutor.NO_LIMIT);
    }

    public <T> List<T> executeQuery(final SessionScope sessionScope, final CriteriaQuery<T> criteriaQuery,
        final int skip, final int max) {
        ExecuteContext<List<T>> e = new ExecuteContext<List<T>>() {
            public List<T> execute(StatementScope statementScope, Transaction trans) throws SQLException {
                CriteriaStatement exec = (CriteriaStatement) criteriaQuery;
                exec.prepare();
                String sql = exec.getSql();
                Class<T> resultClass = (Class<T>) exec.getResultType();

                ParameterInfo<?>[] params = exec.getParameterInfos();
                Object[] parameters = new Object[params.length];
                for (int i = 0; params != null && i < params.length; i++) {
                    parameters[i] = params[i].getParameterValue();
                }

                EntityType<?> cacheType = exec.getQueryCacheType();
                CacheModel cm = null;
                CacheKey key = null;
                if (cacheType != null) {
                    key = new CacheKey();
                    key.update("executeCriteriaQuery");
                    key.update(skip);
                    key.update(max);
                    key.update(cacheType.getJavaType());
                    key.update(sql);
                    key.update(resultClass);
                    for (Object arg : parameters) {
                        key.update(arg);
                    }
                    cm = delegate.findCacheModel(cacheType.getEntityCacheModelId());
                }

                if (cm != null) {
                    Object old = cm.getObject(key);
                    if (old instanceof List<?>) {
                        return Objects.<List<T>>uncheckedCast(old);
                    }
                }

                ParameterMap pm = exec.makeParameterMap(delegate);
                AutoResultMap arm = new AutoResultMap(getDelegate(), false);
                arm.setResultClass(resultClass);
                DefaultRowHandler rowHandler = new DefaultRowHandler();
                RowHandlerCallback callback = new RowHandlerCallback(arm, null, rowHandler);
                statementScope.setParameterMap(pm);
                statementScope.setResultMap(arm);

                if (cm != null) {
                    delegate.getSqlExecutor().executeQuery(null, statementScope, trans.getConnection(), sql, parameters,
                        skip, max, callback);
                    List<T> list = rowHandler.getList();
                    cm.putObject(key, list);
                    return list;
                } else {
                    delegate.getSqlExecutor().executeQuery(null, statementScope, trans.getConnection(), sql, parameters,
                        skip, max, callback);
                    List<T> list = rowHandler.getList();
                    return list;
                }
            }
        };
        try {
            return delegate.executeCallback(sessionScope, e);
        } catch (SQLException ex) {
            throw new PersistenceException(ex.getMessage(), ex);
        }
    }

    public <T> int executeQueryPage(final SessionScope sessionScope, final CriteriaQuery<T> criteriaQuery,
        final List<T> page, final int skip, final int max) {
        ExecuteContext<Integer> e = new ExecuteContext<Integer>() {
            public Integer execute(StatementScope statementScope, Transaction trans) throws SQLException {
                CriteriaStatement exec = (CriteriaStatement) criteriaQuery;
                exec.prepare();
                String sql = exec.getSql();
                Class<T> resultClass = (Class<T>) exec.getResultType();
                ParameterInfo<?>[] params = exec.getParameterInfos();
                Object[] parameters = new Object[params.length];
                for (int i = 0; params != null && i < params.length; i++) {
                    parameters[i] = params[i].getParameterValue();
                }

                EntityType<?> cacheType = exec.getQueryCacheType();
                CacheModel cm = null;
                CacheKey key = null;
                if (cacheType != null) {
                    key = new CacheKey();
                    key.update("executeCriteriaQueryPage");
                    key.update(page != null);
                    key.update(skip);
                    key.update(max);
                    key.update(cacheType.getJavaType());
                    key.update(sql);
                    key.update(resultClass);
                    for (Object arg : parameters) {
                        key.update(arg);
                    }
                    cm = delegate.findCacheModel(cacheType.getEntityCacheModelId());
                }

                if (cm != null) {
                    Object old = cm.getObject(key);
                    if (old instanceof Page<?>) {
                        Page<T> pageObject = Objects.uncheckedCast(old);
                        if (page != null) {
                            page.addAll(pageObject.list);
                        }
                        return pageObject.total;
                    }
                }
                ParameterMap pm = exec.makeParameterMap(delegate);
                AutoResultMap arm = new AutoResultMap(getDelegate(), false);
                arm.setResultClass(resultClass);
                statementScope.setParameterMap(pm);
                statementScope.setResultMap(arm);

                if (cm != null) {
                    ArrayList<T> p = new ArrayList<T>();
                    PageHandler pageHandler = new PageHandler(p);
                    RowHandlerCallback callback = new RowHandlerCallback(arm, null, pageHandler);
                    delegate.getSqlExecutor().executeQueryPage(null, statementScope, trans.getConnection(), sql,
                        parameters, skip, max, callback, pageHandler);
                    int total = pageHandler.getTotal();
                    cm.putObject(key, new Page<T>(total, p));
                    if (page != null) {
                        page.addAll(p);
                    }
                    return total;
                } else {
                    PageHandler pageHandler = new PageHandler(page);
                    RowHandlerCallback callback = new RowHandlerCallback(arm, null, pageHandler);
                    delegate.getSqlExecutor().executeQueryPage(null, statementScope, trans.getConnection(), sql,
                        parameters, skip, max, callback, pageHandler);
                    return pageHandler.handleTotal(0);
                }
            }
        };
        try {
            return delegate.executeCallback(sessionScope, e);
        } catch (SQLException ex) {
            throw new PersistenceException(ex.getMessage(), ex);
        }
    }

    public <T> int executeUpdate(final SessionScope sessionScope, final CriteriaUpdate<T> updateQuery) {
        ExecuteContext<Integer> e = new ExecuteContext<Integer>() {
            public Integer execute(StatementScope statementScope, Transaction trans) throws SQLException {
                CriteriaStatement exec = (CriteriaStatement) updateQuery;
                exec.prepare();
                String sql = exec.getSql();

                ParameterInfo<?>[] params = exec.getParameterInfos();
                Object[] parameters = new Object[params.length];
                for (int i = 0; params != null && i < params.length; i++) {
                    parameters[i] = params[i].getParameterValue();
                }
                ParameterMap pm = exec.makeParameterMap(delegate);
                statementScope.setParameterMap(pm);

                int ret = delegate.getSqlExecutor().executeUpdate(null, statementScope, trans.getConnection(), sql,
                    parameters);
                exec.flushCache(delegate);
                return ret;
            }
        };
        try {
            return delegate.executeCallback(sessionScope, e);
        } catch (SQLException ex) {
            throw new PersistenceException(ex.getMessage(), ex);
        }
    }

    public <T> int executeDelete(final SessionScope sessionScope, final CriteriaDelete<T> deleteQuery) {
        ExecuteContext<Integer> e = new ExecuteContext<Integer>() {
            public Integer execute(StatementScope statementScope, Transaction trans) throws SQLException {
                CriteriaStatement exec = (CriteriaStatement) deleteQuery;
                exec.prepare();
                String sql = exec.getSql();

                ParameterInfo<?>[] params = exec.getParameterInfos();
                Object[] parameters = new Object[params.length];
                for (int i = 0; params != null && i < params.length; i++) {
                    parameters[i] = params[i].getParameterValue();
                }
                ParameterMap pm = exec.makeParameterMap(delegate);
                statementScope.setParameterMap(pm);

                int ret = delegate.getSqlExecutor().executeUpdate(null, statementScope, trans.getConnection(), sql,
                    parameters);
                exec.flushCache(delegate);
                return ret;
            }
        };
        try {
            return delegate.executeCallback(sessionScope, e);
        } catch (SQLException ex) {
            throw new PersistenceException(ex.getMessage(), ex);
        }
    }

    public CriteriaBuilder getCriteriaBuilder() {
        if (criteriaBuilderImpl == null) {
            criteriaBuilderImpl = new CriteriaBuilderImpl(this);
        }
        return criteriaBuilderImpl;
    }

    private Map<Class<?>, EntityType<?>> entityClassMap = new LinkedHashMap<Class<?>, EntityType<?>>();

    public <E> EntityType<E> initEntityClass(Class<E> cls) {
        EntityType<E> ep = Objects.uncheckedCast(entityClassMap.get(cls));
        if (ep == null) {
            synchronized (entityClassMap) {
                ep = Objects.uncheckedCast(entityClassMap.get(cls));
                if (ep == null) {
                    EntityTypeImpl<E> epi = new EntityTypeImpl<E>(cls, this);
                    if (!epi.isFailed()) {
                        long time = System.currentTimeMillis();
                        SqlMapParser mapParser = new SqlMapParser(state, false);
                        String xml = epi.buildSqlMapXml();
                        try {
                            state.getConfig().getErrorContext().setResource(epi.getResourceLocation());
                            ILog log = ILogFactory.getLog(delegate.getSqlExecutor().getClass());
                            mapParser.parse(epi.getResourceLocation(), new StringReader(xml));
                            state.getConfig().finalizeSqlMapConfig();

                            log.info("Load entity " + cls + " elapsed time " + (System.currentTimeMillis() -  time));
                        } catch (Exception ex) {
                            log.error(ex.getMessage(), ex);
                            if (delegate.getSqlExecutor().isDebugSql()) {
                                log.info(xml);
                            }
                            epi.setError(ex);
                        }
                    }
                    ep = epi;
                    entityClassMap.put(cls, ep);
                }
            }
        }
        return ep;
    }

    public Cache findEntityCache(Class<?> entityClass) {
        EntityType<?> ep = entityClassMap.get(entityClass);
        if (ep != null && !ep.isFailed() && ep.isCacheable()) {
            return delegate.findCacheModel(ep.getEntityCacheModelId());
        }
        return null;
    }

    public void flushEntityCache(Class<?> entityClass) {
        Cache cache = findEntityCache(entityClass);
        if (cache != null) {
            cache.flush();
        }
    }
}
