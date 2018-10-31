/*-
 * Copyright 2009-2017 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.engine.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ValueHandler
 * <p>
 * Date: 2017-11-16
 * 
 * @author Song Sun
 * @version 1.0
 */
public interface ValueHandler<X> {

    public Object setParameterValue(PreparedStatement ps, int i, X parameter, String jdbcType) throws SQLException;
}
