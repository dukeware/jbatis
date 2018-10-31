/*-
 * Copyright (c) 2007-2008 Owlgroup.
 * All rights reserved. 
 * SqlMapClient2.java
 * Date: 2008-10-7
 * Author: Song Sun
 */
package org.ibatis.client;

import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * SqlMapClient is the externally provided API interface for users.
 * <p>
 * Date: 2008-10-7
 * 
 * @author Song Sun
 * @version 1.0
 */
public interface SqlMapClient extends com.ibatis.sqlmap.client.SqlMapClient, PropertyProvider {

    /**
     * Get the database dialect.
     */
    Dialect getDialect();

    /**
     * Get the data source object really used.
     */
    DataSource getRealDataSource() throws SQLException;

    /**
     * Get the cache object by its id.
     */
    Cache getCache(String id);

}
