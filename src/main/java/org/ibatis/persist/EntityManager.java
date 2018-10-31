package org.ibatis.persist;

import java.sql.SQLException;
import java.util.List;

import org.ibatis.persist.criteria.CriteriaBuilder;
import org.ibatis.persist.criteria.CriteriaQuery;
import org.ibatis.persist.criteria.CriteriaUpdate;
import org.ibatis.persist.meta.EntityType;
import org.ibatis.persist.criteria.CriteriaDelete;

/**
 * EntityManager
 * <p>
 * Date: 2014-10-23,13:05:21 +0800
 * 
 * @see CriteriaQuery
 * @author Song Sun
 * @version 1.0
 * 
 * @since iBatis Persistence 1.0
 */
public interface EntityManager {

    /**
     * Init the entity class
     * 
     * @param entityClass
     *            the entity class object with annotation {@link Entity}
     * 
     * @throws IllegalArgumentException
     */
    public <E> EntityType<E> initEntityClass(Class<E> entityClass);

    /**
     * Insert an entity object and return the entity object maybe filled with its key.
     * 
     * @param cls
     *            the entity class object with annotation {@link Entity}
     * @param entity
     *            the entity object
     * @return the entity itself.
     * @throws SQLException
     */
    <E> E insertEntity(Class<E> cls, E entity) throws SQLException;

    /**
     * Update an entity object with primary keys.
     * 
     * @param cls
     *            the entity class object with annotation {@link Entity}
     * @param entity
     *            the entity object
     * @param key
     *            the key of the entity.
     * @return the count of rows updated.
     * @throws SQLException
     */
    <E, K> int updateEntity(Class<E> cls, E entity) throws SQLException;

    /**
     * Delete an entity by primary keys.
     * 
     * @param cls
     *            the entity class object with annotation {@link Entity}
     * @param key
     *            the keys of the entity.
     * @return the count of rows deleted.
     * @throws SQLException
     */
    <E, K> int deleteEntity(Class<E> cls, K key) throws SQLException;

    /**
     * Find an entity object by primary keys.
     * 
     * @param cls
     *            the entity class object with annotation {@link Entity}
     * @param keys
     *            the keys of the entity.
     * @return the count of rows updated.
     * @throws SQLException
     */
    <E, K> E findEntity(Class<E> cls, K key) throws SQLException;

    /**
     * Query the first object by the CriteriaQuery object.
     * 
     * @param criteriaQuery
     *            the CriteriaQuery object.
     * @return the first result object or null.
     */
    public <T> T executeQueryObject(CriteriaQuery<T> criteriaQuery);

    /**
     * Query the object list by the CriteriaQuery object.
     * 
     * @param criteriaQuery
     *            the CriteriaQuery object.
     * @return the object list or empty list.
     */
    public <T> List<T> executeQuery(CriteriaQuery<T> criteriaQuery);

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
    public <T> List<T> executeQuery(CriteriaQuery<T> criteriaQuery, int startPosition, int maxResult);

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
    public <T> int executeQueryPage(CriteriaQuery<T> criteriaQuery, List<T> page, int startPosition, int maxResult);

    /**
     * Update entities by the CriteriaUpdate object.
     * 
     * @param updateQuery
     *            the CriteriaUpdate object.
     * @return the count of rows updated.
     */
    public <T> int executeUpdate(CriteriaUpdate<T> updateQuery);

    /**
     * Delete entities by the CriteriaUpdate object.
     * 
     * @param deleteQuery
     *            the CriteriaDelete object.
     * @return the count of rows deleted.
     */
    public <T> int executeDelete(CriteriaDelete<T> deleteQuery);

    /**
     * Return an instance of <code>CriteriaBuilder</code> for the creation of <code>CriteriaQuery</code>,
     * <code>CriteriaUpdate</code>or <code>CriteriaDelete</code> objects.
     * 
     * @return CriteriaBuilder instance
     */
    public CriteriaBuilder getCriteriaBuilder();

}
