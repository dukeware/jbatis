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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import org.ibatis.persist.impl.ExecuteContext;
import org.ibatis.client.Cache;
import org.ibatis.client.Dialect;
import org.ibatis.persist.impl.EntityManager;

import com.ibatis.common.Objects;
import com.ibatis.common.RunStats;
import com.ibatis.common.Touchable;
import com.ibatis.common.beans.Probe;
import com.ibatis.common.beans.ProbeFactory;
import com.ibatis.common.jdbc.exception.NestedSQLException;
import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.client.BatchResult;
import com.ibatis.sqlmap.client.SqlMapException;
import com.ibatis.sqlmap.client.event.RowHandler;
import com.ibatis.sqlmap.engine.builder.xml.XmlParserState;
import com.ibatis.sqlmap.engine.cache.CacheController;
import com.ibatis.sqlmap.engine.cache.CacheModel;
import com.ibatis.sqlmap.engine.cache.CacheRoot;
import com.ibatis.sqlmap.engine.cache.CacheRoots;
import com.ibatis.sqlmap.engine.cache.FlushListener;
import com.ibatis.sqlmap.engine.cache.NoneCacheController;
import com.ibatis.sqlmap.engine.dialect.LimitOffsetPageDialect;
import com.ibatis.sqlmap.engine.dialect.PageDialect;
import com.ibatis.sqlmap.engine.dialect.OffsetFetchPageDialect;
import com.ibatis.sqlmap.engine.exchange.DataExchangeFactory;
import com.ibatis.sqlmap.engine.execution.BatchException;
import com.ibatis.sqlmap.engine.execution.DefaultSqlExecutor;
import com.ibatis.sqlmap.engine.execution.SqlExecutor;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.result.Discriminator;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.result.ResultObjectFactory;
import com.ibatis.sqlmap.engine.mapping.statement.InsertStatement;
import com.ibatis.sqlmap.engine.mapping.statement.MappedRowHandler;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;
import com.ibatis.sqlmap.engine.mapping.statement.SelectKeyStatement;
import com.ibatis.sqlmap.engine.scope.SessionScope;
import com.ibatis.sqlmap.engine.scope.StatementScope;
import com.ibatis.sqlmap.engine.transaction.Transaction;
import com.ibatis.sqlmap.engine.transaction.TransactionException;
import com.ibatis.sqlmap.engine.transaction.TransactionManager;
import com.ibatis.sqlmap.engine.transaction.TransactionState;
import com.ibatis.sqlmap.engine.transaction.user.UserProvidedTransaction;
import com.ibatis.sqlmap.engine.type.TypeHandlerFactory;

/**
 * The workhorse that really runs the SQL
 */
public class SqlMapExecutorDelegate implements Touchable {
    static final ILog log = ILogFactory.getLog(SqlMapExecutorDelegate.class);

    private static final Probe PROBE = ProbeFactory.getProbe();

    private boolean lazyLoadingEnabled = false;
    private boolean cacheModelsEnabled = true;
    private boolean enhancementEnabled = true;
    private boolean databasePagingQueryEnabled = true;
    private boolean useColumnLabel = true;
    private boolean forceMultipleResultSetSupport;

    // ## sunsong
    private Integer jdbcTypeForNull;
    private String defaultCacheModelType = "MEMORY";
    private String forceCacheModelType;

    private TransactionManager txManager;

    private HashMap<String, MappedStatement> mappedStatements;
    private final Map<String, CacheModel> cacheModels;
    private final CacheRoots cacheRoots = new CacheRoots();
    private HashMap<String, ResultMap> resultMaps;
    private HashMap<String, ParameterMap> parameterMaps;

    protected SqlExecutor sqlExecutor;
    private TypeHandlerFactory typeHandlerFactory;
    private DataExchangeFactory dataExchangeFactory;

    private ResultObjectFactory resultObjectFactory;
    private boolean statementCacheEnabled = true;

    private final EntityManager entityManager;
    final XmlParserState state;

    /**
     * Default constructor
     */
    public SqlMapExecutorDelegate(XmlParserState state) {
        mappedStatements = new HashMap<String, MappedStatement>();
        cacheModels = new HashMap<String, CacheModel>();
        resultMaps = new HashMap<String, ResultMap>();
        parameterMaps = new HashMap<String, ParameterMap>();

        sqlExecutor = new DefaultSqlExecutor(this);
        typeHandlerFactory = new TypeHandlerFactory();
        dataExchangeFactory = new DataExchangeFactory(typeHandlerFactory);
        entityManager = new EntityManager(this, state);
        this.state = state;
        RunStats.getInstance().addTouchable(this);
    }

    // ## sunsong
    public XmlParserState getState() {
        return state;
    }

    public CacheRoots getCacheRoots() {
        return cacheRoots;
    }

    public void setCustomExecutor(String sqlExecutorClass) {
        try {
            Class<?> factoryClass = Class.forName(sqlExecutorClass);
            sqlExecutor = (SqlExecutor) factoryClass.newInstance();
        } catch (Exception e) {
            throw new SqlMapException("Error instantiating " + sqlExecutorClass
                + ". Please check the class given in properties file. Cause: " + e, e);
        }
    }

    /**
     * Getter for the DataExchangeFactory
     *
     * @return - the DataExchangeFactory
     */
    public DataExchangeFactory getDataExchangeFactory() {
        return dataExchangeFactory;
    }

    /**
     * Getter for the TypeHandlerFactory
     *
     * @return - the TypeHandlerFactory
     */
    public TypeHandlerFactory getTypeHandlerFactory() {
        return typeHandlerFactory;
    }

    /**
     * Getter for the status of lazy loading
     *
     * @return - the status
     */
    public boolean isLazyLoadingEnabled() {
        return lazyLoadingEnabled;
    }

    /**
     * Turn on or off lazy loading
     *
     * @param lazyLoadingEnabled
     *            - the new state of caching
     */
    public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {
        this.lazyLoadingEnabled = lazyLoadingEnabled;
    }

    /**
     * Getter for the status of caching
     *
     * @return - the status
     */
    public boolean isCacheModelsEnabled() {
        return cacheModelsEnabled;
    }

    /**
     * Turn on or off caching
     *
     * @param cacheModelsEnabled
     *            - the new state of caching
     */
    public void setCacheModelsEnabled(boolean cacheModelsEnabled) {
        this.cacheModelsEnabled = cacheModelsEnabled;
    }

    /**
     * Getter for the status of CGLib enhancements
     *
     * @return - the status
     */
    public boolean isEnhancementEnabled() {
        return enhancementEnabled;
    }

    /**
     * Turn on or off CGLib enhancements
     *
     * @param enhancementEnabled
     *            - the new state
     */
    public void setEnhancementEnabled(boolean enhancementEnabled) {
        this.enhancementEnabled = enhancementEnabled;
    }

    public boolean isDatabasePagingQueryEnabled() {
        return databasePagingQueryEnabled;
    }

    public void setDatabasePagingQueryEnabled(boolean databasePagingQueryEnabled) {
        this.databasePagingQueryEnabled = databasePagingQueryEnabled;
    }

    public boolean isUseColumnLabel() {
        return useColumnLabel;
    }

    public void setUseColumnLabel(boolean useColumnLabel) {
        this.useColumnLabel = useColumnLabel;
    }

    public Integer getJdbcTypeForNull() {
        return jdbcTypeForNull;
    }

    public void setJdbcTypeForNull(Integer jdbcTypeForNull) {
        this.jdbcTypeForNull = jdbcTypeForNull;
    }

    public String getDefaultCacheModelType() {
        return defaultCacheModelType;
    }

    public void setDefaultCacheModelType(String defaultCacheModelType) {
        this.defaultCacheModelType = defaultCacheModelType;
    }

    public String getForceCacheModelType() {
        return forceCacheModelType;
    }

    public void setForceCacheModelType(String forceCacheModelType) {
        this.forceCacheModelType = forceCacheModelType;
    }

    /**
     * Getter for the transaction manager
     *
     * @return - the transaction manager
     */
    public TransactionManager getTxManager() {
        return txManager;
    }

    /**
     * Setter for the transaction manager
     *
     * @param txManager
     *            - the transaction manager
     */
    public void setTxManager(TransactionManager txManager) {
        this.txManager = txManager;
    }

    /**
     * Add a mapped statement
     *
     * @param ms
     *            - the mapped statement to add
     */
    public void addMappedStatement(MappedStatement ms, int base) {
        MappedStatement old = mappedStatements.get(ms.getId());
        if (old != null && (old.getBaseCacheKey() != base || old.getClass() != ms.getClass())) {
            throw new SqlMapException("There is already a statement named " + ms.getId() + " in " + old.getResource());
        }
        ms.setBaseCacheKey(base);
        mappedStatements.put(ms.getId(), ms);
    }

    /**
     * Get an iterator of the mapped statements
     *
     * @return - the set
     */
    public Set<String> getMappedStatementNames() {
        return mappedStatements.keySet();
    }

    /**
     * Get a mappedstatement by its ID
     *
     * @param id
     *            - the statement ID
     * @return - the mapped statement
     */
    public MappedStatement getMappedStatement(String id) {
        // ## sunsong - dialect
        if (state.getDialect() != null) {
            String did = id + "." + state.getDialect();
            if (mappedStatements.containsKey(did)) {
                id = did;
            }
        }

        MappedStatement ms = mappedStatements.get(id);
        if (ms == null) {
            throw new SqlMapException("There is no statement named " + id + " in this SqlMap.");
        }
        return ms;
    }

    /**
     * Add a cache model
     *
     * @param model
     *            - the model to add
     */
    public synchronized void addCacheModel(CacheModel model) {
        cacheModels.put(model.getId(), model);
    }

    /**
     * Get an iterator of the cache models
     *
     * @return - the cache models
     */
    public Set<String> getCacheModelNames() {
        return cacheModels.keySet();
    }

    /**
     * Get a cache model by ID
     *
     * @param id
     *            - the ID
     * @return - the cache model
     */
    public CacheModel getCacheModel(String id) {
        CacheModel model = (CacheModel) cacheModels.get(id);
        if (model == null) {
            throw new SqlMapException("There is no cache model named " + id + " in this SqlMap.");
        }
        return model;
    }

    /**
     * Get a cache model by ID
     *
     * @param id
     *            - the ID
     * @return - the cache model
     */
    public synchronized CacheModel findCacheModel(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        CacheModel model = (CacheModel) cacheModels.get(id);
        if (model != null) {
            return model;
        }
        if (id.indexOf('.') > 0) {
            return null;
        }
        if (!id.startsWith(".")) {
            id = "." + id;
        }
        for (String name : getCacheModelNames()) {
            if (name.endsWith(id)) {
                return (CacheModel) cacheModels.get(name);
            }
        }
        return null;
    }

    public synchronized Cache[] getCacheModels() {
        return cacheModels.values().toArray(new Cache[0]);
    }

    /**
     * Add a result map
     *
     * @param map
     *            - the result map to add
     */
    public void addResultMap(ResultMap map) {
        resultMaps.put(map.getId(), map);
    }

    /**
     * Get an iterator of the result maps
     *
     * @return - the result maps
     */
    public Set<String> getResultMapNames() {
        return resultMaps.keySet();
    }

    /**
     * Get a result map by ID
     *
     * @param id
     *            - the ID
     * @return - the result map
     */
    public ResultMap getResultMap(String id) {
        ResultMap map = (ResultMap) resultMaps.get(id);
        if (map == null) {
            throw new SqlMapException("There is no result map named " + id + " in this SqlMap.");
        }
        return map;
    }

    /**
     * Add a parameter map
     *
     * @param map
     *            - the map to add
     */
    public void addParameterMap(ParameterMap map) {
        parameterMaps.put(map.getId(), map);
    }

    /**
     * Get an iterator of all of the parameter maps
     *
     * @return - the parameter maps
     */
    public Set<String> getParameterMapNames() {
        return parameterMaps.keySet();
    }

    /**
     * Get a parameter map by ID
     *
     * @param id
     *            - the ID
     * @return - the parameter map
     */
    public ParameterMap getParameterMap(String id) {
        ParameterMap map = (ParameterMap) parameterMaps.get(id);
        if (map == null) {
            throw new SqlMapException("There is no parameter map named " + id + " in this SqlMap.");
        }
        return map;
    }

    /**
     * Flush all of the data caches
     */
    public synchronized void flushDataCache() {
        long time = System.currentTimeMillis();
        for (CacheModel cache : cacheModels.values()) {
            cache.onFlush(cache.getId(), time);
        }
        log.debug("All data caches flushed.");
    }

    /**
     * Flush a single cache by ID
     *
     * @param id
     *            - the ID
     */
    public void flushDataCache(String id) {
        CacheModel model = findCacheModel(id);
        if (model != null) {
            model.flush();
        }
    }

    // -- Basic Methods
    /**
     * Call an insert statement by ID
     *
     * @param sessionScope
     *            - the session
     * @param id
     *            - the statement ID
     * @param param
     *            - the parameter object
     * @return - the generated key (or null)
     * @throws SQLException
     *             - if the insert fails
     */
    public <T> T insert(SessionScope sessionScope, String id, Object param) throws SQLException {
        Object generatedKey = null;

        MappedStatement ms = getMappedStatement(id);
        Transaction trans = getTransaction(sessionScope);
        boolean autoStart = trans == null;

        try {
            trans = autoStartTransaction(sessionScope, autoStart, trans);

            SelectKeyStatement ks = null;
            if (ms instanceof InsertStatement) {
                ks = ((InsertStatement) ms).getSelectKeyStatement();
            }

            // Here we get the old value for the key property. We'll want it later if for some reason the
            // insert fails.
            Object oldKeyValue = null;
            String keyProperty = null;
            boolean resetKeyValueOnFailure = false;
            if (ks != null && !ks.isRunAfterSQL()) {
                keyProperty = ks.getKeyProperty();
                oldKeyValue = PROBE.getObject(param, keyProperty);
                generatedKey = executeSelectKey(sessionScope, trans, ms, param);
                resetKeyValueOnFailure = true;
            }

            StatementScope statementScope = beginStatementScope(sessionScope, ms);

            try {
                if (ks != null && ks.isRunAfterSQL() && ks.isGeneratedKeys()) {
                    generatedKey = ((InsertStatement) ms).executeInsert(statementScope, trans, param);
                } else {
                    int rows = ms.executeUpdate(statementScope, trans, param);
                    if (ks == null) {
                        generatedKey = rows;
                    }
                }
            } catch (SQLException e) {
                // uh-oh, the insert failed, so if we set the reset flag earlier, we'll put the old value
                // back...
                if (resetKeyValueOnFailure)
                    PROBE.setObject(param, keyProperty, oldKeyValue);
                // ...and still throw the exception.
                throw e;
            } finally {
                endStatementScope(statementScope);
            }

            if (ks != null && ks.isRunAfterSQL() && !ks.isGeneratedKeys()) {
                generatedKey = executeSelectKey(sessionScope, trans, ms, param);
            }

            autoCommitTransaction(sessionScope, autoStart);
        } finally {
            autoEndTransaction(sessionScope, autoStart);
        }

        return Objects.uncheckedCast(generatedKey);
    }

    private Object executeSelectKey(SessionScope sessionScope, Transaction trans, MappedStatement ms, Object param)
        throws SQLException {
        Object generatedKey = null;
        StatementScope statementScope;
        InsertStatement insert = (InsertStatement) ms;
        SelectKeyStatement selectKeyStatement = insert.getSelectKeyStatement();
        if (selectKeyStatement != null) {
            statementScope = beginStatementScope(sessionScope, selectKeyStatement);
            try {
                generatedKey = selectKeyStatement.executeQueryForObject(statementScope, trans, param, null);
                String keyProp = selectKeyStatement.getKeyProperty();
                if (keyProp != null) {
                    PROBE.setObject(param, keyProp, generatedKey);
                }
            } finally {
                endStatementScope(statementScope);
            }
        }
        return generatedKey;
    }

    /**
     * Execute an update statement
     *
     * @param sessionScope
     *            - the session scope
     * @param id
     *            - the statement ID
     * @param param
     *            - the parameter object
     * @return - the number of rows updated
     * @throws SQLException
     *             - if the update fails
     */
    public int update(SessionScope sessionScope, String id, Object param) throws SQLException {
        int rows = 0;

        MappedStatement ms = getMappedStatement(id);
        Transaction trans = getTransaction(sessionScope);
        boolean autoStart = trans == null;

        try {
            trans = autoStartTransaction(sessionScope, autoStart, trans);

            StatementScope statementScope = beginStatementScope(sessionScope, ms);
            try {
                rows = ms.executeUpdate(statementScope, trans, param);
            } finally {
                endStatementScope(statementScope);
            }

            autoCommitTransaction(sessionScope, autoStart);
        } finally {
            autoEndTransaction(sessionScope, autoStart);
        }

        return rows;
    }

    /**
     * Execute a delete statement
     *
     * @param sessionScope
     *            - the session scope
     * @param id
     *            - the statement ID
     * @param param
     *            - the parameter object
     * @return - the number of rows deleted
     * @throws SQLException
     *             - if the delete fails
     */
    public int delete(SessionScope sessionScope, String id, Object param) throws SQLException {
        return update(sessionScope, id, param);
    }

    /**
     * Execute a select for a single object
     *
     * @param sessionScope
     *            - the session scope
     * @param id
     *            - the statement ID
     * @param paramObject
     *            - the parameter object
     * @return - the result of the query
     * @throws SQLException
     *             - if the query fails
     */
    public <T> T queryForObject(SessionScope sessionScope, String id, Object paramObject) throws SQLException {
        return queryForObject(sessionScope, id, paramObject, null);
    }

    /**
     * Execute a select for a single object
     *
     * @param sessionScope
     *            - the session scope
     * @param id
     *            - the statement ID
     * @param paramObject
     *            - the parameter object
     * @param resultObject
     *            - the result object (if not supplied or null, a new object will be created)
     * @return - the result of the query
     * @throws SQLException
     *             - if the query fails
     */
    public <T> T queryForObject(SessionScope sessionScope, String id, Object paramObject, Object resultObject)
        throws SQLException {
        Object object = null;

        MappedStatement ms = getMappedStatement(id);
        Transaction trans = getTransaction(sessionScope);
        boolean autoStart = trans == null;

        try {
            trans = autoStartTransaction(sessionScope, autoStart, trans);

            StatementScope statementScope = beginStatementScope(sessionScope, ms);
            try {
                object = ms.executeQueryForObject(statementScope, trans, paramObject, resultObject);
            } finally {
                endStatementScope(statementScope);
            }

            autoCommitTransaction(sessionScope, autoStart);
        } finally {
            autoEndTransaction(sessionScope, autoStart);
        }

        return Objects.uncheckedCast(object);
    }

    /**
     * Execute a query for a list
     *
     * @param sessionScope
     *            - the session scope
     * @param id
     *            - the statement ID
     * @param paramObject
     *            - the parameter object
     * @return - the data list
     * @throws SQLException
     *             - if the query fails
     */
    public <T> List<T> queryForList(SessionScope sessionScope, String id, Object paramObject) throws SQLException {
        return queryForList(sessionScope, id, paramObject, SqlExecutor.ZERO, SqlExecutor.NO_LIMIT);
    }

    /**
     * Execute a query for a list
     *
     * @param sessionScope
     *            - the session scope
     * @param id
     *            - the statement ID
     * @param paramObject
     *            - the parameter object
     * @param skip
     *            - the number of rows to skip
     * @param max
     *            - the maximum number of rows to return
     * @return - the data list
     * @throws SQLException
     *             - if the query fails
     */
    public <T> List<T> queryForList(SessionScope sessionScope, String id, Object paramObject, int skip, int max)
        throws SQLException {
        List<T> list = null;

        MappedStatement ms = getMappedStatement(id);
        Transaction trans = getTransaction(sessionScope);
        boolean autoStart = trans == null;

        try {
            trans = autoStartTransaction(sessionScope, autoStart, trans);

            StatementScope statementScope = beginStatementScope(sessionScope, ms);
            try {
                list = ms.executeQueryForList(statementScope, trans, paramObject, skip, max);
            } finally {
                endStatementScope(statementScope);
            }

            autoCommitTransaction(sessionScope, autoStart);
        } finally {
            autoEndTransaction(sessionScope, autoStart);
        }

        return list;
    }

    /**
     * Execute a query with a row handler. The row handler is called once per row in the query results.
     *
     * @param sessionScope
     *            - the session scope
     * @param id
     *            - the statement ID
     * @param paramObject
     *            - the parameter object
     * @param rowHandler
     *            - the row handler
     * @throws SQLException
     *             - if the query fails
     */
    public void queryWithRowHandler(SessionScope sessionScope, String id, Object paramObject, RowHandler rowHandler)
        throws SQLException {

        MappedStatement ms = getMappedStatement(id);
        Transaction trans = getTransaction(sessionScope);
        boolean autoStart = trans == null;

        try {
            trans = autoStartTransaction(sessionScope, autoStart, trans);

            StatementScope statementScope = beginStatementScope(sessionScope, ms);
            try {
                ms.executeQueryWithRowHandler(statementScope, trans, paramObject, rowHandler);
            } finally {
                endStatementScope(statementScope);
            }

            autoCommitTransaction(sessionScope, autoStart);
        } finally {
            autoEndTransaction(sessionScope, autoStart);
        }

    }

    /**
     * Execute a query for a map. The map has the table key as the key, and a property from the results as the map data
     *
     * @param sessionScope
     *            - the session scope
     * @param id
     *            - the statement ID
     * @param paramObject
     *            - the parameter object
     * @param keyProp
     *            - the property for the map key
     * @param keyType
     *            - the type of property for the map key
     * @param valueProp
     *            - the property for the map data
     * @param valueType
     *            - the type of property for the map data
     * @return - the Map
     * @throws SQLException
     *             - if the query fails
     */
    public <K, V> Map<K, V> queryForMap(SessionScope sessionScope, String id, Object paramObject, String keyProp,
        Class<K> keyType, String valueProp, Class<V> valueType) throws SQLException {
        MappedStatement ms = getMappedStatement(id);
        if (ms.getResultMap() == null && valueProp != null) {
            MappedRowHandler<K, V> mapHandler = new MappedRowHandler<K, V>(getTypeHandlerFactory(), isUseColumnLabel(),
                keyProp, keyType, valueProp, valueType);
            queryWithMapHandler(sessionScope, id, paramObject, mapHandler);
            return mapHandler.getMap();
        }

        List<V> list = queryForList(sessionScope, id, paramObject);

        Map<K, V> map = new LinkedHashMap<K, V>();
        for (int i = 0, n = list.size(); i < n; i++) {
            V object = list.get(i);
            K key = Objects.uncheckedCast(PROBE.getObject(object, keyProp));
            V value = null;
            if (valueProp == null) {
                value = object;
            } else {
                value = Objects.uncheckedCast(PROBE.getObject(object, valueProp));
            }
            map.put(key, value);
        }

        return map;
    }

    <K, V> void queryWithMapHandler(SessionScope sessionScope, String id, Object paramObject,
        MappedRowHandler<K, V> mrh) throws SQLException {
        MappedStatement ms = getMappedStatement(id);
        Transaction trans = getTransaction(sessionScope);
        boolean autoStart = trans == null;

        try {
            trans = autoStartTransaction(sessionScope, autoStart, trans);

            StatementScope statementScope = beginStatementScope(sessionScope, ms);
            try {
                ms.executeQueryWithMapHandler(statementScope, trans, paramObject, SqlExecutor.ZERO,
                    SqlExecutor.NO_LIMIT, mrh);
            } finally {
                endStatementScope(statementScope);
            }

            autoCommitTransaction(sessionScope, autoStart);
        } finally {
            autoEndTransaction(sessionScope, autoStart);
        }
    }

    private String productName;
    private int majorVersion;
    private int minorVersion;

    private void initProduct(Transaction tx) {
        if (productName == null && tx != null) {
            try {
                DatabaseMetaData meta = tx.getConnection().getMetaData();
                majorVersion = meta.getDatabaseMajorVersion();
                minorVersion = meta.getDatabaseMinorVersion();
                productName = meta.getDatabaseProductName().toLowerCase();
            } catch (Exception e) {
                log.warn(e.toString());
            }
            if (productName == null) {
                productName = "";
            }
        }
    }

    // -- Transaction Control Methods
    /**
     * Start a transaction on the session
     *
     * @param sessionScope
     *            - the session
     * @throws SQLException
     *             - if the transaction could not be started
     */
    public void startTransaction(SessionScope sessionScope) throws SQLException {
        try {
            txManager.begin(sessionScope);
        } catch (TransactionException e) {
            throw new NestedSQLException("Could not start transaction.  Cause: " + e, e);
        }
    }

    /**
     * Start a transaction on the session with the specified isolation level.
     *
     * @param sessionScope
     *            - the session
     * @throws SQLException
     *             - if the transaction could not be started
     */
    public void startTransaction(SessionScope sessionScope, int transactionIsolation) throws SQLException {
        try {
            txManager.begin(sessionScope, transactionIsolation);
        } catch (TransactionException e) {
            throw new NestedSQLException("Could not start transaction.  Cause: " + e, e);
        }
    }

    /**
     * Commit the transaction on a session
     *
     * @param sessionScope
     *            - the session
     * @throws SQLException
     *             - if the transaction could not be committed
     */
    public void commitTransaction(SessionScope sessionScope) throws SQLException {
        try {
            // Auto batch execution
            if (sessionScope.isInBatch()) {
                executeBatch(sessionScope);
            }
            sqlExecutor.cleanup(sessionScope);
            txManager.commit(sessionScope);
        } catch (TransactionException e) {
            throw new NestedSQLException("Could not commit transaction.  Cause: " + e, e);
        }
    }

    /**
     * End the transaction on a session
     *
     * @param sessionScope
     *            - the session
     * @throws SQLException
     *             - if the transaction could not be ended
     */
    public void endTransaction(SessionScope sessionScope) throws SQLException {
        try {
            try {
                sqlExecutor.cleanup(sessionScope);
            } finally {
                txManager.end(sessionScope);
            }
        } catch (TransactionException e) {
            throw new NestedSQLException("Error while ending transaction.  Cause: " + e, e);
        }
    }

    /**
     * Start a batch for a session
     *
     * @param sessionScope
     *            - the session
     */
    public void startBatch(SessionScope sessionScope, int batchSize) throws SQLException {
        if (batchSize <= 0 && batchSize != -1) {
            throw new SQLException("Illegal batchSize: " + batchSize);
        }
        if (sessionScope.isInBatch()) {
            sqlExecutor.cleanup(sessionScope);
            sessionScope.setInBatch(0);
            throw new SQLException("Incorrect batch mode.\n  +-- Already in batch");
        }
        sessionScope.setInBatch(batchSize);
    }

    /**
     * Execute a batch for a session
     *
     * @param sessionScope
     *            - the session
     * @return - the number of rows impacted by the batch
     * @throws SQLException
     *             - if the batch fails
     */
    public int executeBatch(SessionScope sessionScope) throws SQLException {
        // ## auto batch tx

        // sessionScope.setInBatch(false);
        // return sqlExecutor.executeBatch(sessionScope);
        Transaction trans = getTransaction(sessionScope);
        if (trans == null) {
            sessionScope.setInBatch(0);
            sessionScope.autoBatch = false;
            return 0;
        }
        boolean autoBatch = sessionScope.autoBatch;
        int anwser = 0;
        try {
            sessionScope.setInBatch(0);
            sessionScope.autoBatch = false;
            anwser = sqlExecutor.executeBatch(sessionScope);
            if (autoBatch) {
                commitTransaction(sessionScope);
            }
        } finally {
            if (autoBatch) {
                endTransaction(sessionScope);
            }
        }
        return anwser;
    }

    /**
     * Execute a batch for a session
     *
     * @param sessionScope
     *            - the session
     * @return - a List of BatchResult objects (may be null if no batch has been initiated). There will be one
     *         BatchResult object in the list for each sub-batch executed
     * @throws SQLException
     *             if a database access error occurs, or the drive does not support batch statements
     * @throws BatchException
     *             if the driver throws BatchUpdateException
     */
    public List<BatchResult> executeBatchDetailed(SessionScope sessionScope) throws SQLException, BatchException {
        // ## auto batch tx

        Transaction trans = getTransaction(sessionScope);
        if (trans == null) {
            sessionScope.setInBatch(0);
            sessionScope.autoBatch = false;
            return Collections.emptyList();
        }
        boolean autoBatch = sessionScope.autoBatch;
        List<BatchResult> anwser = null;
        try {
            sessionScope.setInBatch(0);
            sessionScope.autoBatch = false;
            anwser = sqlExecutor.executeBatchDetailed(sessionScope);
            if (autoBatch) {
                commitTransaction(sessionScope);
            }
        } finally {
            if (autoBatch) {
                endTransaction(sessionScope);
            }
        }
        return anwser;
    }

    /**
     * Use a user-provided transaction for a session
     *
     * @param sessionScope
     *            - the session scope
     * @param userConnection
     *            - the user supplied connection
     */
    public void setUserProvidedTransaction(SessionScope sessionScope, Connection userConnection) {
        if (sessionScope.getTransactionState() == TransactionState.STATE_USER_PROVIDED) {
            sessionScope.recallTransactionState();
        }
        if (userConnection != null) {
            Connection conn = userConnection;
            sessionScope.saveTransactionState();
            sessionScope.setTransaction(new UserProvidedTransaction(conn));
            sessionScope.setTransactionState(TransactionState.STATE_USER_PROVIDED);
        } else {
            sessionScope.setTransaction(null);
            sessionScope.cleanup();
        }
    }

    /**
     * Get the DataSource for the session
     *
     * @return - the DataSource
     */
    public DataSource getDataSource() {
        DataSource ds = null;
        if (txManager != null) {
            ds = txManager.getConfig().getDataSource();
        }
        return ds;
    }

    /**
     * Getter for the SqlExecutor
     *
     * @return the SqlExecutor
     */
    public SqlExecutor getSqlExecutor() {
        return sqlExecutor;
    }

    /**
     * Get a transaction for the session
     *
     * @param sessionScope
     *            - the session
     * @return - the transaction
     */
    public Transaction getTransaction(SessionScope sessionScope) {
        Transaction tx = sessionScope.getTransaction();
        initProduct(tx);
        return tx;
    }

    // -- Protected Methods

    protected void autoEndTransaction(SessionScope sessionScope, boolean autoStart) throws SQLException {
        if (autoStart && !sessionScope.autoBatch) {
            sessionScope.getSqlMapTxMgr().endTransaction();
        }
    }

    protected void autoCommitTransaction(SessionScope sessionScope, boolean autoStart) throws SQLException {
        if (autoStart && !sessionScope.autoBatch) {
            sessionScope.getSqlMapTxMgr().commitTransaction();
        }
    }

    protected Transaction autoStartTransaction(SessionScope sessionScope, boolean autoStart, Transaction trans)
        throws SQLException {
        Transaction transaction = trans;
        if (autoStart) {
            sessionScope.getSqlMapTxMgr().startTransaction();
            transaction = getTransaction(sessionScope);
            sessionScope.autoBatch = sessionScope.isInBatch();
        }
        return transaction;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    protected StatementScope beginStatementScope(SessionScope sessionScope, MappedStatement mappedStatement) {
        StatementScope statementScope = new StatementScope(sessionScope);
        if (mappedStatement != null) { // ## sunsong - callback
            mappedStatement.initRequest(statementScope);
        }
        return statementScope;
    }

    protected void endStatementScope(StatementScope statementScope) {
    }

    protected SessionScope beginSessionScope() {
        return new SessionScope();
    }

    protected void endSessionScope(SessionScope sessionScope) {
        sessionScope.cleanup();
    }

    public ResultObjectFactory getResultObjectFactory() {
        return resultObjectFactory;
    }

    public void setResultObjectFactory(ResultObjectFactory resultObjectFactory) {
        this.resultObjectFactory = resultObjectFactory;
    }

    public boolean isStatementCacheEnabled() {
        return statementCacheEnabled;
    }

    public void setStatementCacheEnabled(boolean statementCacheEnabled) {
        this.statementCacheEnabled = statementCacheEnabled;
    }

    public boolean isForceMultipleResultSetSupport() {
        return forceMultipleResultSetSupport;
    }

    public void setForceMultipleResultSetSupport(boolean forceMultipleResultSetSupport) {
        this.forceMultipleResultSetSupport = forceMultipleResultSetSupport;
    }

    public <T> int queryForPage(SessionScope sessionScope, List<T> page, String id, Object paramObject, int skip,
        int max) throws SQLException {
        int total = -1;

        MappedStatement ms = getMappedStatement(id);
        Transaction trans = getTransaction(sessionScope);
        boolean autoStart = trans == null;

        try {
            trans = autoStartTransaction(sessionScope, autoStart, trans);

            StatementScope statementScope = beginStatementScope(sessionScope, ms);
            try {
                total = ms.executeQueryForPage(statementScope, page, trans, paramObject, skip, max);
            } finally {
                endStatementScope(statementScope);
            }

            autoCommitTransaction(sessionScope, autoStart);
        } finally {
            autoEndTransaction(sessionScope, autoStart);
        }

        return total;
    }

    public <T> T executeCallback(SessionScope sessionScope, ExecuteContext<T> callback) throws SQLException {
        Transaction trans = getTransaction(sessionScope);
        boolean autoStart = trans == null;
        T t = null;
        try {
            trans = autoStartTransaction(sessionScope, autoStart, trans);

            StatementScope statementScope = beginStatementScope(sessionScope, null);
            try {
                t = callback.execute(statementScope, trans);
            } catch (SQLException e) {
                throw new NestedSQLException(statementScope.getErrorContext().toString(), e);
            } finally {
                endStatementScope(statementScope);
            }

            autoCommitTransaction(sessionScope, autoStart);
        } finally {
            autoEndTransaction(sessionScope, autoStart);
        }
        return t;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public String getGlobalProperty(String name) {
        return state.getGlobalProps().getProperty(name);
    }

    public synchronized void finalizeSqlMapConfig() {
        wireUpCacheModels();
        bindResultMapDiscriminators();
    }

    void wireUpCacheModels() {
        // Wire Up Cache Models
        Set<String> cacheNames = getCacheModelNames();
        for (String cacheName : cacheNames) {
            CacheModel cacheModel = getCacheModel(cacheName);

            Set<String> triggerCacheRoots = cacheModel.getFlushTriggerCacheRoots();
            for (String triggerCacheRoot : triggerCacheRoots) {
                CacheRoot cacheRoot = getCacheRoots().makeRoot(triggerCacheRoot);
                cacheRoot.addFlushListener(cacheModel);
            }

            Set<String> statementNames = cacheModel.getFlushTriggerStatementNames();
            for (String statementName : statementNames) {
                MappedStatement statement = null;
                try {
                    statement = getMappedStatement(statementName);
                } catch (Exception e) {
                }
                if (statement != null) {
                    statement.addExecuteListener(cacheModel);
                } else {
                    log.warn("Could not find statement named '" + statementName
                        + "' for use as a flush trigger for the cache model named '" + cacheName + "'.");
                }
            }

            Set<String> triggerCacheNames = cacheModel.getFlushTriggerCacheNames();
            for (String triggerCacheName : triggerCacheNames) {
                CacheModel triggerCacheModel = findCacheModel(triggerCacheName);
                if (triggerCacheModel != null) {
                    triggerCacheModel.addFlushListener(cacheModel);
                } else {
                    log.warn("Could not find cache named '" + triggerCacheName
                        + "' for use as a flush trigger for the cache model named '" + cacheName + "'.");
                }
            }

            Set<Class<?>> triggerEntityClasses = cacheModel.getFlushTriggerEntityClasses();
            for (Class<?> triggerEntityClass : triggerEntityClasses) {
                Cache triggerCache = getEntityManager().findEntityCache(triggerEntityClass);
                if (triggerCache instanceof CacheModel) {
                    ((CacheModel) triggerCache).addFlushListener(cacheModel);
                }
            }
        }
    }

    void bindResultMapDiscriminators() {
        // Bind discriminators
        Set<String> names = getResultMapNames();
        for (String name : names) {
            ResultMap rm = getResultMap(name);
            Discriminator disc = rm.getDiscriminator();
            if (disc != null) {
                disc.bindSubMaps();
            }
        }
    }

    final FlushListener err = new FlushListener() {
        @Override
        public void onFlush(String id, long timestamp) {
            log.error("cache '" + id + "' flushed.", new Exception("just for stack"));
        }
    };
    final FlushListener info = new FlushListener() {
        @Override
        public void onFlush(String id, long timestamp) {
            log.info("cache '" + id + "' flushed.");
        }
    };

    @Override
    public void onTouch(File file) {
        Properties p = new Properties();
        try (InputStream is = new FileInputStream(file)) {
            p.load(is);
        } catch (Exception e) {
        }
        if (!"true".equals(p.getProperty("cache_flush_monitor"))) {
            return;
        }
        for (Cache c : getCacheModels()) {
            if (c instanceof CacheModel) {
                CacheModel cm = (CacheModel) c;
                String type = p.getProperty(cm.getId() + ".onFlush");
                cm.removeFlushListener(err);
                cm.removeFlushListener(info);
                if ("error".equalsIgnoreCase(type)) {
                    cm.addFlushListener(err);
                } else if ("info".equalsIgnoreCase(type)) {
                    cm.addFlushListener(info);
                }
            }
        }
    }

    // ## sunsong
    public String toCacheModelType(String type) {
        String forceType = getForceCacheModelType();
        if (forceType != null) {
            return forceType;
        }
        if (type == null || type.isEmpty()) {
            return getDefaultCacheModelType();
        }
        return type;
    }

    public CacheController newCacheController(String type) throws Exception {
        type = toCacheModelType(type);
        if ("None".equals(type)) {
            return NoneCacheController.INSTANCE;
        }
        type = getTypeHandlerFactory().resolveAlias(type);
        Class<?> clazz = Resources.classForName(type);
        return (CacheController) Resources.instantiate(clazz);
    }

    public PageDialect getPageDialect(String id, String sql, boolean needTotal, int skip, int max) {
        Dialect d = state.getDialect();
        if (databasePagingQueryEnabled && max > SqlExecutor.NO_LIMIT && d != null) {
            switch (d) {
            case mysql: {
                if (id == null || !id.endsWith(".mysql")) {
                    PageDialect dialect = new LimitOffsetPageDialect(d, sql, needTotal, skip, max);
                    return dialect.canHandle(productName, majorVersion, minorVersion);
                }
                break;
            }
            case postgresql: {
                if (id == null || !id.endsWith(".postgresql")) {
                    PageDialect dialect = new LimitOffsetPageDialect(d, sql, needTotal, skip, max);
                    return dialect.canHandle(productName, majorVersion, minorVersion);
                }
                break;
            }
            case db2: {
                if (id == null || !id.endsWith(".db2")) {
                    PageDialect dialect = new LimitOffsetPageDialect(d, sql, needTotal, skip, max);
                    return dialect.canHandle(productName, majorVersion, minorVersion);
                }
                break;
            }
            case sqlserver: {
                if (id == null || !id.endsWith(".sqlserver")) {
                    PageDialect dialect = new OffsetFetchPageDialect(d, sql, needTotal, skip, max);
                    return dialect.canHandle(productName, majorVersion, minorVersion);
                }
                break;
            }
            case oracle: {
                if (id == null || !id.endsWith(".oracle")) {
                    PageDialect dialect = new OffsetFetchPageDialect(d, sql, needTotal, skip, max);
                    return dialect.canHandle(productName, majorVersion, minorVersion);
                }
                break;
            }
            default:
                break;
            }
        }
        return null;
    }

}
