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

package com.ibatis.sqlmap.client.event;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;

/**
 * RowSetHandler
 * <p>
 * Date: 2015-04-01,12:57:32 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
public class RowSetHandler implements RowHandler {
    CachedRowSet rsi;

    public void handleResultSet(ResultSet rset) throws SQLException {
        rsi = RowSetProvider.newFactory().createCachedRowSet();
        rsi.populate(rset);
    }

    public ResultSet getRowSet() {
        return rsi;
    }

    @Override
    public void handleRow(Object valueObject) throws SQLException {
    }

    @Override
    public Integer getRows() {
        return rsi == null ? null : rsi.size();
    }
}
