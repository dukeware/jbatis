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
package com.ibatis.sqlmap.engine.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * SQL time implementation of TypeHandler
 */
public class SqlTimeTypeHandler extends BaseTypeHandler<java.sql.Time> {

    // private static final String DATE_FORMAT = "HH:mm:ss";

    public void setParameter(PreparedStatement ps, int i, java.sql.Time parameter, String jdbcType) throws SQLException {
        ps.setTime(i, parameter);
    }

    public java.sql.Time getResult(ResultSet rs, String columnName) throws SQLException {
        java.sql.Time sqlTime = rs.getTime(columnName);
        if (rs.wasNull()) {
            return null;
        } else {
            return sqlTime;
        }
    }

    public java.sql.Time getResult(ResultSet rs, int columnIndex) throws SQLException {
        java.sql.Time sqlTime = rs.getTime(columnIndex);
        if (rs.wasNull()) {
            return null;
        } else {
            return sqlTime;
        }
    }

    public java.sql.Time getResult(CallableStatement cs, int columnIndex) throws SQLException {
        java.sql.Time sqlTime = cs.getTime(columnIndex);
        if (cs.wasNull()) {
            return null;
        } else {
            return sqlTime;
        }
    }

    public java.sql.Time valueOf(Object s) {
        if (s == null) {
            return null;
        } else if (s instanceof Date) {
            return new java.sql.Time(((Date) s).getTime());
        }
        return java.sql.Time.valueOf(s.toString());
    }

}
