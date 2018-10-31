/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ibatis.spring;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.ibatis.client.Dialect;
import org.ibatis.client.SqlMapClient;
import org.ibatis.persist.criteria.CriteriaBuilder;
import org.ibatis.persist.criteria.CriteriaDelete;
import org.ibatis.persist.criteria.CriteriaQuery;
import org.ibatis.persist.criteria.CriteriaUpdate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.util.Assert;

import com.ibatis.common.ArrayMap;
import com.ibatis.sqlmap.client.SqlMapExecutor;
import com.ibatis.sqlmap.client.SqlMapSession;
import com.ibatis.sqlmap.client.event.RowHandler;

/**
 * Helper class that simplifies data access via the iBATIS {@link com.ibatis.sqlmap.client.SqlMapClient} API, converting
 * checked SQLExceptions into unchecked DataAccessExceptions, following the <code>org.springframework.dao</code>
 * exception hierarchy. Uses the same {@link org.springframework.jdbc.support.SQLExceptionTranslator} mechanism as
 * {@link org.springframework.jdbc.core.JdbcTemplate}.
 *
 * <p>
 * The main method of this class executes a callback that implements a data access action. Furthermore, this class
 * provides numerous convenience methods that mirror {@link com.ibatis.sqlmap.client.SqlMapExecutor}'s execution
 * methods.
 *
 * <p>
 * It is generally recommended to use the convenience methods on this template for plain query/insert/update/delete
 * operations. However, for more complex operations like batch updates, a custom SqlMapClientCallback must be
 * implemented, usually as anonymous inner class. For example:
 *
 * <pre class="code">
 * getSqlMapClientTemplate().execute(new SqlMapClientCallback() {
 *     public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
 *         executor.startBatch();
 *         executor.update(&quot;insertSomething&quot;, &quot;myParamValue&quot;);
 *         executor.update(&quot;insertSomethingElse&quot;, &quot;myOtherParamValue&quot;);
 *         executor.executeBatch();
 *         return null;
 *     }
 * });
 * </pre>
 *
 * The template needs a SqlMapClient to work on, passed in via the "sqlMapClient" property. A Spring context typically
 * uses a {@link SqlMapClientFactoryBean} to build the SqlMapClient. The template an additionally be configured with a
 * DataSource for fetching Connections, although this is not necessary if a DataSource is specified for the SqlMapClient
 * itself (typically through SqlMapClientFactoryBean's "dataSource" property).
 *
 * @author Song Sun
 */
public class SqlMapClientTemplate extends JdbcAccessor implements SqlMapClientOperations {

    private SqlMapClient sqlMapClient;

    /**
     * Create a new SqlMapClientTemplate.
     */
    public SqlMapClientTemplate() {
    }

    /**
     * Create a new SqlMapTemplate.
     * 
     * @param sqlMapClient
     *            iBATIS SqlMapClient that defines the mapped statements
     */
    public SqlMapClientTemplate(SqlMapClient sqlMapClient) {
        setSqlMapClient(sqlMapClient);
        afterPropertiesSet();
    }

    /**
     * Create a new SqlMapTemplate.
     * 
     * @param dataSource
     *            JDBC DataSource to obtain connections from
     * @param sqlMapClient
     *            iBATIS SqlMapClient that defines the mapped statements
     */
    public SqlMapClientTemplate(DataSource dataSource, SqlMapClient sqlMapClient) {
        setDataSource(dataSource);
        setSqlMapClient(sqlMapClient);
        afterPropertiesSet();
    }

    /**
     * Set the iBATIS Database Layer SqlMapClient that defines the mapped statements.
     */
    public void setSqlMapClient(SqlMapClient sqlMapClient) {
        this.sqlMapClient = sqlMapClient;
    }

    /**
     * Return the iBATIS Database Layer SqlMapClient that this template works with.
     */
    public SqlMapClient getSqlMapClient() {
        return this.sqlMapClient;
    }

    /**
     * If no DataSource specified, use SqlMapClient's DataSource.
     * 
     * @see com.ibatis.sqlmap.client.SqlMapClient#getDataSource()
     */
    @Override
    public DataSource getDataSource() {
        DataSource ds = super.getDataSource();
        return (ds != null ? ds : this.sqlMapClient.getDataSource());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.jdbc.support.JdbcAccessor#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() {
        if (this.sqlMapClient == null) {
            throw new IllegalArgumentException("Property 'sqlMapClient' is required");
        }
        super.afterPropertiesSet();
    }

    /**
     * Execute the given data access action on a SqlMapExecutor.
     * 
     * @param action
     *            callback object that specifies the data access action
     * @return a result object returned by the action, or <code>null</code>
     * @throws DataAccessException
     *             in case of SQL Maps errors
     */
    public <T> T execute(SqlMapClientCallback<T> action) throws DataAccessException {
        Assert.notNull(action, "Callback object must not be null");
        Assert.notNull(this.sqlMapClient, "No SqlMapClient specified");

        // We always need to use a SqlMapSession, as we need to pass a Spring-managed
        // Connection (potentially transactional) in. This shouldn't be necessary if
        // we run against a TransactionAwareDataSourceProxy underneath, but unfortunately
        // we still need it to make iBATIS batch execution work properly: If iBATIS
        // doesn't recognize an existing transaction, it automatically executes the
        // batch for every single statement...

        SqlMapSession session = this.sqlMapClient.openSession();
        if (logger.isTraceEnabled()) {
            logger.debug("Opened SqlMapSession [" + session + "] for iBATIS operation");
        }
        Connection ibatisCon = null;

        try {
            Connection springCon = null;
            DataSource dataSource = getDataSource();
            boolean transactionAware = (dataSource instanceof TransactionAwareDataSourceProxy);

            // Obtain JDBC Connection to operate on...
            try {
                ibatisCon = session.getCurrentConnection();
                if (ibatisCon == null) {
                    springCon = (transactionAware ? dataSource.getConnection()
                            : DataSourceUtils.doGetConnection(dataSource));
                    session.setUserConnection(springCon);
                    if (logger.isTraceEnabled()) {
                        logger.debug("Obtained JDBC Connection [" + springCon + "] for iBATIS operation");
                    }
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.debug("Reusing JDBC Connection [" + ibatisCon + "] for iBATIS operation");
                    }
                }
            } catch (SQLException ex) {
                throw new CannotGetJdbcConnectionException("Could not get JDBC Connection", ex);
            }

            // Execute given callback...
            try {
                return action.doInSqlMapClient(session);
            } catch (SQLException ex) {
                throw getExceptionTranslator().translate("SqlMapClient operation", null, ex);
            } finally {
                try {
                    if (springCon != null) {
                        if (transactionAware) {
                            springCon.close();
                        } else {
                            DataSourceUtils.doReleaseConnection(springCon, dataSource);
                        }
                    }
                } catch (Throwable ex) {
                    logger.debug("Could not close JDBC Connection", ex);
                }
            }

            // Processing finished - potentially session still to be closed.
        } finally {
            // Only close SqlMapSession if we know we've actually opened it
            // at the present level.
            if (ibatisCon == null) {
                session.close();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForObject(java.lang.String)
     */
    public <T> T queryForObject(String id) throws DataAccessException {
        return queryForObject(id, null);
    }
    
    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForObject(java.lang.String, java.lang.Object)
     */
    public <T> T queryForObject(final String id, final Object parameterObject) throws DataAccessException {
        return execute(new SqlMapClientCallback<T>() {
            public T doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.queryForObject(id, parameterObject);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForObject(java.lang.String, java.lang.Object, java.lang.Object)
     */
    public <T> T queryForObject(final String id, final Object parameterObject, final Object resultObject)
        throws DataAccessException {
        return execute(new SqlMapClientCallback<T>() {
            public T doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.queryForObject(id, parameterObject, resultObject);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForList(java.lang.String)
     */
    public <T> List<T> queryForList(String id) throws DataAccessException {
        return queryForList(id, null);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForList(java.lang.String, java.lang.Object)
     */
    public <T> List<T> queryForList(final String id, final Object parameterObject) throws DataAccessException {
        return execute(new SqlMapClientCallback<List<T>>() {
            public List<T> doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.queryForList(id, parameterObject);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForList(java.lang.String, int, int)
     */
    public <T> List<T> queryForList(String id, int skipResults, int maxResults) throws DataAccessException {
        return queryForList(id, null, skipResults, maxResults);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForList(java.lang.String, java.lang.Object, int, int)
     */
    public <T> List<T> queryForList(final String id, final Object parameterObject, final int skipResults,
        final int maxResults) throws DataAccessException {
        return execute(new SqlMapClientCallback<List<T>>() {
            public List<T> doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.queryForList(id, parameterObject, skipResults, maxResults);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryWithRowHandler(java.lang.String, com.ibatis.sqlmap.client.event.RowHandler)
     */
    public void queryWithRowHandler(String id, RowHandler rowHandler) throws DataAccessException {
        queryWithRowHandler(id, null, rowHandler);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryWithRowHandler(java.lang.String, java.lang.Object, com.ibatis.sqlmap.client.event.RowHandler)
     */
    public void queryWithRowHandler(final String id, final Object parameterObject, final RowHandler rowHandler)
        throws DataAccessException {
        execute(new SqlMapClientCallback<Object>() {
            public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                executor.queryWithRowHandler(id, parameterObject, rowHandler);
                return null;
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryWithRowHandler(java.lang.String, com.ibatis.sqlmap.client.event.RowHandler, java.lang.Object[])
     */
    public void queryWithRowHandler(String id, RowHandler rowHandler, Object... args) throws DataAccessException {
        queryWithRowHandler(id, toParameter(args), rowHandler);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForMap(java.lang.String, java.lang.Object, java.lang.String)
     */
    public <K, V> Map<K, V> queryForMap(final String id, final Object parameterObject, final String keyProperty)
        throws DataAccessException {
        return execute(new SqlMapClientCallback<Map<K, V>>() {
            public Map<K, V> doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.queryForMap(id, parameterObject, keyProperty);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForMap(java.lang.String, java.lang.Object, java.lang.String, java.lang.String)
     */
    public <K, V> Map<K, V> queryForMap(final String id, final Object parameterObject, final String keyProperty,
        final String valueProperty) throws DataAccessException {
        return execute(new SqlMapClientCallback<Map<K, V>>() {
            public Map<K, V> doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.queryForMap(id, parameterObject, keyProperty, valueProperty);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForMap(java.lang.String, java.lang.Object, java.lang.String, java.lang.Class, java.lang.String, java.lang.Class)
     */
    public <K, V> Map<K, V> queryForMap(final String id, final Object parameterObject, final String keyProperty,
        final Class<K> keyType, final String valueProperty, final Class<V> valueType) throws DataAccessException {
        return execute(new SqlMapClientCallback<Map<K, V>>() {
            public Map<K, V> doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.queryForMap(id, parameterObject, keyProperty, keyType, valueProperty, valueType);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#insert(java.lang.String)
     */
    public <T> T insert(String id) throws DataAccessException {
        return insert(id, null);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#insert(java.lang.String, java.lang.Object)
     */
    public <T> T insert(final String id, final Object parameterObject) throws DataAccessException {
        return execute(new SqlMapClientCallback<T>() {
            public T doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.insert(id, parameterObject);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#update(java.lang.String)
     */
    public int update(String id) throws DataAccessException {
        return update(id, null);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#update(java.lang.String, java.lang.Object)
     */
    public int update(final String id, final Object parameterObject) throws DataAccessException {
        return execute(new SqlMapClientCallback<Integer>() {
            public Integer doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.update(id, parameterObject);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#update(java.lang.String, java.lang.Object, int)
     */
    public void update(String id, Object parameterObject, int requiredRowsAffected) throws DataAccessException {

        int actualRowsAffected = update(id, parameterObject);
        if (actualRowsAffected != requiredRowsAffected) {
            throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(id, requiredRowsAffected, actualRowsAffected);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#delete(java.lang.String)
     */
    public int delete(String id) throws DataAccessException {
        return delete(id, null);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#delete(java.lang.String, java.lang.Object)
     */
    public int delete(final String id, final Object parameterObject) throws DataAccessException {
        return execute(new SqlMapClientCallback<Integer>() {
            public Integer doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.delete(id, parameterObject);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#delete(java.lang.String, java.lang.Object, int)
     */
    public void delete(String id, Object parameterObject, int requiredRowsAffected) throws DataAccessException {
        int actualRowsAffected = delete(id, parameterObject);
        if (actualRowsAffected != requiredRowsAffected) {
            throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(id, requiredRowsAffected, actualRowsAffected);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#insertArgs(java.lang.String, java.lang.Object[])
     */
    public <T> T insertArgs(String id, Object... args) throws DataAccessException {
        return insert(id, toParameter(args));
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#updateArgs(java.lang.String, java.lang.Object[])
     */
    public int updateArgs(String id, Object... args) throws DataAccessException {
        return update(id, toParameter(args));
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#deleteArgs(java.lang.String, java.lang.Object[])
     */
    public int deleteArgs(String id, Object... args) throws DataAccessException {
        return delete(id, toParameter(args));
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForListArgs(java.lang.String, java.lang.Object[])
     */
    public <T> List<T> queryForListArgs(String id, Object... args) throws DataAccessException {
        return queryForList(id, toParameter(args));
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForListArgs(int, int, java.lang.String, java.lang.Object[])
     */
    public <T> List<T> queryForListArgs(int skip, int max, String id, Object... args) throws DataAccessException {
        return queryForList(id, toParameter(args), skip, max);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForPage(java.util.List, java.lang.String, int, int)
     */
    public <T> int queryForPage(List<T> page, String id, int skip, int max) throws DataAccessException {
        return queryForPage(page, id, null, skip, max);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForPage(java.util.List, java.lang.String, java.lang.Object, int, int)
     */
    public <T> int queryForPage(final List<T> page, final String id, final Object paramObject, final int skip,
        final int max) throws DataAccessException {

        return execute(new SqlMapClientCallback<Integer>() {
            public Integer doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.queryForPage(page, id, paramObject, skip, max);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForPageArgs(java.util.List, java.lang.String, int, int, java.lang.Object[])
     */
    public <T> int queryForPageArgs(List<T> page, String id, int skip, int max, Object... args)
        throws DataAccessException {
        return queryForPage(page, id, toParameter(args), skip, max);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForMapArgs(java.lang.String, java.lang.String, java.lang.Object[])
     */
    public <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, Object... args) throws DataAccessException {
        return queryForMap(id, toParameter(args), keyProp);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForMapArgs(java.lang.String, java.lang.String, java.lang.String, java.lang.Object[])
     */
    public <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, String valueProp, Object... args)
        throws DataAccessException {
        return queryForMap(id, toParameter(args), keyProp, valueProp);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForMapArgs(java.lang.String, java.lang.String, java.lang.Class, java.lang.String, java.lang.Class, java.lang.Object[])
     */
    @Override
    public <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, Class<K> keyType, String valueProp,
        Class<V> valueType, Object... args) throws DataAccessException {
        return queryForMap(id, toParameter(args), keyProp, keyType, valueProp, valueType);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForObjectArgs(java.lang.String, java.lang.Object[])
     */
    public <T> T queryForObjectArgs(String id, Object... args) throws DataAccessException {
        return queryForObject(id, toParameter(args));
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForFirstArgs(java.lang.String, java.lang.Object[])
     */
    public <T> T queryForFirstArgs(String id, Object... args) throws DataAccessException {
        return queryForFirst(id, toParameter(args));
    }


    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForFirst(java.lang.String)
     */
    public <T> T queryForFirst(String id) throws DataAccessException {
        List<T> list = queryForList(id, 0, 1);
        if (list == null || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }

    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForFirst(java.lang.String, java.lang.Object)
     */
    public <T> T queryForFirst(String id, Object parameterObject) throws DataAccessException {
        List<T> list = queryForList(id, parameterObject, 0, 1);
        if (list == null || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }

    }

    /**
     * Get the dialect of the iBatis instance.
     */
    public Dialect getDialect() {
        return sqlMapClient.getDialect();
    }

    Map<String, Object> toParameter(Object[] args) {
        if (args == null || args.length == 0) {
            return Collections.emptyMap();
        }

        return new ArrayMap(args);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#insertEntity(java.lang.Class, java.lang.Object)
     */
    public <E> E insertEntity(final Class<E> cls, final E entity) throws DataAccessException {
        return execute(new SqlMapClientCallback<E>() {
            public E doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.insertEntity(cls, entity);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#updateEntity(java.lang.Class, java.lang.Object)
     */
    public <E, K> int updateEntity(final Class<E> cls, final E entity) throws DataAccessException {
        return execute(new SqlMapClientCallback<Integer>() {
            public Integer doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.updateEntity(cls, entity);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#deleteEntity(java.lang.Class, java.lang.Object)
     */
    public <E, K> int deleteEntity(final Class<E> cls, final K key) throws DataAccessException {
        return execute(new SqlMapClientCallback<Integer>() {
            public Integer doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.deleteEntity(cls, key);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#findEntity(java.lang.Class, java.lang.Object)
     */
    public <E, K> E findEntity(final Class<E> cls, final K key) throws DataAccessException {
        return execute(new SqlMapClientCallback<E>() {
            public E doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.findEntity(cls, key);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#executeQueryObject(org.ibatis.persist.criteria.CriteriaQuery)
     */
    @Override
    public <T> T executeQueryObject(final CriteriaQuery<T> criteriaQuery) throws DataAccessException {
        return execute(new SqlMapClientCallback<T>() {
            public T doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.executeQueryObject(criteriaQuery);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#executeQuery(org.ibatis.persist.criteria.CriteriaQuery)
     */
    @Override
    public <T> List<T> executeQuery(final CriteriaQuery<T> criteriaQuery) throws DataAccessException {
        return execute(new SqlMapClientCallback<List<T>>() {
            public List<T> doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.executeQuery(criteriaQuery);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#executeQuery(org.ibatis.persist.criteria.CriteriaQuery, int, int)
     */
    @Override
    public <T> List<T> executeQuery(final CriteriaQuery<T> criteriaQuery, final int startPosition, final int maxResult)
        throws DataAccessException {
        return execute(new SqlMapClientCallback<List<T>>() {
            public List<T> doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.executeQuery(criteriaQuery, startPosition, maxResult);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#executeQueryPage(org.ibatis.persist.criteria.CriteriaQuery, java.util.List, int, int)
     */
    @Override
    public <T> int executeQueryPage(final CriteriaQuery<T> criteriaQuery, final List<T> page, final int startPosition,
        final int maxResult) throws DataAccessException {
        return execute(new SqlMapClientCallback<Integer>() {
            public Integer doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.executeQueryPage(criteriaQuery, page, startPosition, maxResult);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#executeUpdate(org.ibatis.persist.criteria.CriteriaUpdate)
     */
    @Override
    public <T> int executeUpdate(final CriteriaUpdate<T> updateQuery) throws DataAccessException {
        return execute(new SqlMapClientCallback<Integer>() {
            public Integer doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.executeUpdate(updateQuery);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#executeDelete(org.ibatis.persist.criteria.CriteriaDelete)
     */
    @Override
    public <T> int executeDelete(final CriteriaDelete<T> deleteQuery) throws DataAccessException {
        return execute(new SqlMapClientCallback<Integer>() {
            public Integer doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.executeDelete(deleteQuery);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#getCriteriaBuilder()
     */
    @Override
    public CriteriaBuilder getCriteriaBuilder() throws DataAccessException {
        return sqlMapClient.getCriteriaBuilder();
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForResultSetArgs(java.lang.String, java.lang.Object[])
     */
    @Override
    public ResultSet queryForResultSetArgs(String id, Object... args) throws DataAccessException {
        return queryForResultSet(id, toParameter(args));
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForResultSet(java.lang.String)
     */
    @Override
    public ResultSet queryForResultSet(String id) throws DataAccessException {
        return queryForResultSet(id, null);
    }

    /*
     * (non-Javadoc)
     * @see org.ibatis.spring.SqlMapClientOperations#queryForResultSet(java.lang.String, java.lang.Object)
     */
    @Override
    public ResultSet queryForResultSet(final String id, final Object parameterObject) throws DataAccessException {
        return execute(new SqlMapClientCallback<ResultSet>() {
            public ResultSet doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                return executor.queryForResultSet(id, parameterObject);
            }
        });
    }

    @Override
    public String getGlobalProperty(String name) {
        return sqlMapClient.getGlobalProperty(name);
    }

}
