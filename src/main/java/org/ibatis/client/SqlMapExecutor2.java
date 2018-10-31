/*-
 * Copyright (c) 2007-2008 Owlgroup.
 * All rights reserved. 
 * SqlMapExecutor.java
 * Date: 2008-10-7
 * Author: Song Sun
 */
package org.ibatis.client;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.ibatis.sqlmap.client.event.RowHandler;

/**
 * SqlMapExecutor2 supply var-args api for users.
 * <p>
 * Date: 2008-10-7
 * 
 * @author Song Sun
 * @version 1.0
 */
public interface SqlMapExecutor2 {

    /**
     * Do insert operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#insert(String, Object)
     */
    <T> T insertArgs(String id, Object... args) throws SQLException;

    /**
     * Do update operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#update(String, Object)
     */
    int updateArgs(String id, Object... args) throws SQLException;

    /**
     * Do delete operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#delete(String, Object)
     */
    int deleteArgs(String id, Object... args) throws SQLException;

    /**
     * Do queryForList operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String, Object)
     */
    <T> List<T> queryForListArgs(String id, Object... args) throws SQLException;

    /**
     * Do queryForList operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String, Object, int, int)
     */
    <T> List<T> queryForListArgs(int skip, int max, String id, Object... args) throws SQLException;

    /**
     * Do queryForMap operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForMap(String, Object, String)
     */
    <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, Object... args) throws SQLException;

    /**
     * Do queryForMap operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForMap(String, Object, String, String)
     */
    <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, String valueProp, Object... args) throws SQLException;

    /**
     * Do queryForMap operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForMap(String, Object, String, Class, String, Class)
     */
    <K, V> Map<K, V> queryForMapArgs(String id, String keyProp, Class<K> keyType, String valueProp, Class<V> valueType,
        Object... args) throws SQLException;

    /**
     * Do queryForObject operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForObject(String, Object)
     */
    <T> T queryForObjectArgs(String id, Object... args) throws SQLException;

    /**
     * Do queryForPage operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForPage(List, String, Object, int, int)
     */
    <T> int queryForPageArgs(List<T> page, String id, int skip, int max, Object... args) throws SQLException;

    /**
     * Do queryForFirst operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String, Object)
     */
    <T> T queryForFirstArgs(String id, Object... args) throws SQLException;

    /**
     * Do queryForFirst operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String)
     */
    <T> T queryForFirst(String id) throws SQLException;

    /**
     * Do queryForFirst operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String, Object)
     */
    <T> T queryForFirst(String id, Object parameterObject) throws SQLException;

    /**
     * Do queryWithRowHandler operation by var-args paramenters as #1# to #n#
     * 
     * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryWithRowHandler(String, Object, RowHandler)
     */
    void queryWithRowHandlerArgs(String id, RowHandler rowHandler, Object... args) throws SQLException;

    /**
     * Do queryForResultSet operation by var-args paramenters as #1# to #n#
     * 
     * @see #queryForResultSet(String, Object)
     */
    ResultSet queryForResultSetArgs(String id, Object... args) throws SQLException;

    /**
     * Execute the query and return a cached result set.
     * 
     * @param id
     *            the statement id.
     * @return the cached result set
     * @see javax.sql.CachedRowSet
     */
    ResultSet queryForResultSet(String id) throws SQLException;

    /**
     * Execute the query and return a cached result set.
     * 
     * @param id
     *            the statement id.
     * @param parameterObject
     *            the parameter object.
     * @return the cached result set
     * @see javax.sql.CachedRowSet
     */
    ResultSet queryForResultSet(String id, Object parameterObject) throws SQLException;

}
