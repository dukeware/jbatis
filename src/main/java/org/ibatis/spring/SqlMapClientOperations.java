/*
 * Copyright 2002-2008 the original author or authors.
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

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import org.ibatis.client.PropertyProvider;
import org.ibatis.persist.criteria.CriteriaBuilder;
import org.ibatis.persist.criteria.CriteriaDelete;
import org.ibatis.persist.criteria.CriteriaQuery;
import org.ibatis.persist.criteria.CriteriaUpdate;
import org.springframework.dao.DataAccessException;

import com.ibatis.sqlmap.client.event.RowHandler;

/**
 * Interface that specifies a basic set of iBATIS SqlMapClient operations, implemented by {@link SqlMapClientTemplate}.
 */
public interface SqlMapClientOperations extends PropertyProvider {

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForObject(String)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    <T> T queryForObject(String statementName) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForObject(String, Object)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    <T> T queryForObject(String statementName, Object parameterObject) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForObject(String, Object, Object)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    <T> T queryForObject(String statementName, Object parameterObject, Object resultObject) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    <T> List<T> queryForList(String statementName) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String, Object)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    <T> List<T> queryForList(String statementName, Object parameterObject) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String, int, int)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    <T> List<T> queryForList(String statementName, int skipResults, int maxResults) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String, Object, int, int)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    <T> List<T> queryForList(String statementName, Object parameterObject, int skipResults, int maxResults)
        throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryWithRowHandler(String, RowHandler)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    void queryWithRowHandler(String statementName, RowHandler rowHandler) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryWithRowHandler(String, Object, RowHandler)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    void queryWithRowHandler(String statementName, Object parameterObject, RowHandler rowHandler)
        throws DataAccessException;

    /**
     * Do queryWithRowHandler operation by var-args paramenters as #1# to #n#
     * 
     * @see #queryWithRowHandler(String, Object, RowHandler)
     */
    void queryWithRowHandler(String statementName, RowHandler rowHandler, Object... args) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForMap(String, Object, String)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    <K, V> Map<K, V> queryForMap(String statementName, Object parameterObject, String keyProperty)
        throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForMap(String, Object, String, String)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    <K, V> Map<K, V> queryForMap(String statementName, Object parameterObject, String keyProperty, String valueProperty)
        throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForMap(String, Object, String, Class, String, Class)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    <K, V> Map<K, V> queryForMap(String statementName, Object parameterObject, String keyProperty, Class<K> keyType,
        String valueProperty, Class<V> valueType) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#insert(String)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    <T> T insert(String statementName) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#insert(String, Object)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    <T> T insert(String statementName, Object parameterObject) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#update(String)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    int update(String statementName) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#update(String, Object)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    int update(String statementName, Object parameterObject) throws DataAccessException;

    /**
     * Convenience method provided by Spring: execute an update operation with an automatic check that the update
     * affected the given required number of rows.
     * 
     * @param statementName
     *            the name of the mapped statement
     * @param parameterObject
     *            the parameter object
     * @param requiredRowsAffected
     *            the number of rows that the update is required to affect
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    void update(String statementName, Object parameterObject, int requiredRowsAffected) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#delete(String)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    int delete(String statementName) throws DataAccessException;

    /**
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#delete(String, Object)
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    int delete(String statementName, Object parameterObject) throws DataAccessException;

    /**
     * Convenience method provided by Spring: execute a delete operation with an automatic check that the delete
     * affected the given required number of rows.
     * 
     * @param statementName
     *            the name of the mapped statement
     * @param parameterObject
     *            the parameter object
     * @param requiredRowsAffected
     *            the number of rows that the delete is required to affect
     * @throws org.springframework.dao.DataAccessException
     *             in case of errors
     */
    void delete(String statementName, Object parameterObject, int requiredRowsAffected) throws DataAccessException;

    /**
     * Do insert operation by var-args paramenters as #1# to #n#
     * 
     * @see #insert(String, Object)
     */
    <T> T insertArgs(String id, Object... args) throws DataAccessException;

    /**
     * Do update operation by var-args paramenters as #1# to #n#
     * 
     * @see #update(String, Object)
     */
    int updateArgs(String id, Object... args) throws DataAccessException;

    /**
     * Do delete operation by var-args paramenters as #1# to #n#
     * 
     * @see #delete(String, Object)
     */
    int deleteArgs(String id, Object... args) throws DataAccessException;

    /**
     * Do queryForList operation by var-args paramenters as #1# to #n#
     * 
     * @see #queryForList(String, Object)
     */
    <T> List<T> queryForListArgs(String id, Object... args) throws DataAccessException;

    /**
     * Do queryForList operation by var-args paramenters as #1# to #n#
     * 
     * @see #queryForList(String, Object, int, int)
     */
    <T> List<T> queryForListArgs(int skip, int max, String id, Object... args) throws DataAccessException;

    /**
     * Do queryForMap operation by var-args paramenters as #1# to #n#
     * 
     * @see #queryForMap(String, Object, String)
     */
    <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, Object... args) throws DataAccessException;

    /**
     * Do queryForMap operation by var-args paramenters as #1# to #n#
     * 
     * @see #queryForMap(String, Object, String, String)
     */
    <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, String valueProp, Object... args)
        throws DataAccessException;

    /**
     * Do queryForMap operation by var-args paramenters as #1# to #n#
     * 
     * @see #queryForMap(String, Object, String, Class, String, Class)
     */
    <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, Class<K> keyType, String valueProp, Class<V> valueType,
        Object... args) throws DataAccessException;

    /**
     * Do queryForObject operation by var-args paramenters as #1# to #n#
     * 
     * @see #queryForObject(String, Object)
     */
    <T> T queryForObjectArgs(String id, Object... args) throws DataAccessException;

    /**
     * Do queryForFirst operation by var-args paramenters as #1# to #n#
     * 
     * @see #queryForFirst(String, Object)
     */
    <T> T queryForFirstArgs(String id, Object... args) throws DataAccessException;

    /**
     * Do query and return first row as result object.
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String)
     */
    <T> T queryForFirst(String id) throws DataAccessException;

    /**
     * Do query and return first row as result object.
     * 
     * @param parameterObject
     *            the parameter for query
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String, Object)
     */
    <T> T queryForFirst(String id, Object parameterObject) throws DataAccessException;

    /**
     * Executes a mapped SQL SELECT statement that returns data to populate a number of result objects within a certain
     * range into the page argument and return the total number.
     * 
     * @param page
     *            page container, input/output parameter
     * @param id
     *            The name of the statement to execute.
     * @param parameterObject
     *            The parameter object (e.g. JavaBean, Map, XML etc.).
     * @param skip
     *            The number of results to ignore.
     * @param max
     *            The maximum number of results to return.
     * @return total number
     * @throws java.sql.DataAccessException
     *             If an error occurs.
     */
    <T> int queryForPage(List<T> page, String id, Object paramObject, int skip, int max) throws DataAccessException;

    /**
     * Executes a mapped SQL SELECT statement that returns data to populate a number of result objects within a certain
     * range into the page argument and return the total number.
     * 
     * @param page
     *            page container, input/output parameter
     * @param id
     *            The name of the statement to execute.
     * @param skip
     *            The number of results to ignore.
     * @param max
     *            The maximum number of results to return.
     * @return total number
     * @throws java.sql.DataAccessException
     *             If an error occurs.
     */
    <T> int queryForPage(List<T> page, String id, int skip, int max) throws DataAccessException;

    /**
     * Executes a mapped SQL SELECT statement that returns data to populate a number of result objects within a certain
     * range into the page argument and return the total number.
     * 
     * @param page
     *            page container, input/output parameter
     * @param id
     *            The name of the statement to execute.
     * @param skip
     *            The number of results to ignore.
     * @param max
     *            The maximum number of results to return.
     * @param args
     *            The parameter objects #1# to #n#
     * @return total number
     * @throws java.sql.DataAccessException
     *             If an error occurs.
     */
    <T> int queryForPageArgs(List<T> page, String id, int skip, int max, Object... args) throws DataAccessException;

    /**
     * Do queryForResultSet operation by var-args paramenters as #1# to #n#
     * 
     * @see #queryForResultSet(String, Object)
     */
    ResultSet queryForResultSetArgs(String id, Object... args) throws DataAccessException;

    /**
     * Do query and return a cached result set.
     * 
     * @see javax.sql.RowSet
     */
    ResultSet queryForResultSet(String id) throws DataAccessException;

    /**
     * Do query and return a cached result set.
     * 
     * @param parameterObject
     *            the parameter for query
     * @see javax.sql.RowSet
     */
    ResultSet queryForResultSet(String id, Object parameterObject) throws DataAccessException;

    /**
     * Insert an entity object.
     * 
     * @see org.ibatis.persist.EntityManager#insertEntity(Class, Object)
     */
    <E> E insertEntity(Class<E> cls, E entity) throws DataAccessException;

    /**
     * Update an entity object.
     * 
     * @see org.ibatis.persist.EntityManager#updateEntity(Class, Object)
     */
    <E, K> int updateEntity(Class<E> cls, E entity) throws DataAccessException;

    /**
     * Update an entity object by its key.
     * 
     * @see org.ibatis.persist.EntityManager#deleteEntity(Class, Object)
     */
    <E, K> int deleteEntity(Class<E> cls, K key) throws DataAccessException;

    /**
     * Find an entity object by its key.
     * 
     * @see org.ibatis.persist.EntityManager#findEntity(Class, Object)
     */
    <E, K> E findEntity(Class<E> cls, K key) throws DataAccessException;

    /**
     * Query the first object by the CriteriaQuery object.
     * 
     * @param criteriaQuery
     *            the CriteriaQuery object.
     * @return the first result object or null.
     */
    public <T> T executeQueryObject(CriteriaQuery<T> criteriaQuery) throws DataAccessException;

    /**
     * Query the object list by the CriteriaQuery object.
     * 
     * @param criteriaQuery
     *            the CriteriaQuery object.
     * @return the object list or empty list.
     */
    public <T> List<T> executeQuery(CriteriaQuery<T> criteriaQuery) throws DataAccessException;

    /**
     * Query the object list by the CriteriaQuery object.
     * 
     * @param criteriaQuery
     *            the CriteriaQuery object.
     * @param startPosition
     *            position of the first result, numbered from 0
     * @param maxResult
     *            maximum number of results to retrieve
     * @return the object list or empty list.
     */
    public <T> List<T> executeQuery(CriteriaQuery<T> criteriaQuery, int startPosition, int maxResult)
        throws DataAccessException;

    /**
     * Query the object list by the CriteriaQuery object and fill into page
     * 
     * @param criteriaQuery
     *            the CriteriaQuery object.
     * @param page
     *            the page container
     * @param startPosition
     *            position of the first result, numbered from 0
     * @param maxResult
     *            maximum number of results to retrieve
     * @return the total rows
     */
    public <T> int executeQueryPage(CriteriaQuery<T> criteriaQuery, List<T> page, int startPosition, int maxResult)
        throws DataAccessException;

    /**
     * Update entities by the CriteriaUpdate object.
     * 
     * @param updateQuery
     *            the CriteriaUpdate object.
     * @return the count of rows updated.
     */
    public <T> int executeUpdate(CriteriaUpdate<T> updateQuery) throws DataAccessException;

    /**
     * Delete entities by the CriteriaUpdate object.
     * 
     * @param deleteQuery
     *            the CriteriaDelete object.
     * @return the count of rows deleted.
     */
    public <T> int executeDelete(CriteriaDelete<T> deleteQuery) throws DataAccessException;

    /**
     * Return an instance of <code>CriteriaBuilder</code> for the creation of <code>CriteriaQuery</code>,
     * <code>CriteriaUpdate</code>or <code>CriteriaDelete</code> objects.
     * 
     * @return CriteriaBuilder instance
     */
    public CriteriaBuilder getCriteriaBuilder() throws DataAccessException;

}
