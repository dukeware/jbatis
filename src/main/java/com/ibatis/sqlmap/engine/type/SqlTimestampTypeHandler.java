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
 * SQL timestamp implementation of TypeHandler
 */
public class SqlTimestampTypeHandler extends BaseTypeHandler<java.sql.Timestamp> {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public void setParameter(PreparedStatement ps, int i, java.sql.Timestamp parameter, String jdbcType) throws SQLException {
        ps.setTimestamp(i, parameter);
    }

    public java.sql.Timestamp getResult(ResultSet rs, String columnName) throws SQLException {
        java.sql.Timestamp sqlTimestamp = rs.getTimestamp(columnName);
        if (rs.wasNull()) {
            return null;
        } else {
            return sqlTimestamp;
        }
    }

    public java.sql.Timestamp getResult(ResultSet rs, int columnIndex) throws SQLException {
        java.sql.Timestamp sqlTimestamp = rs.getTimestamp(columnIndex);
        if (rs.wasNull()) {
            return null;
        } else {
            return sqlTimestamp;
        }
    }

    public java.sql.Timestamp getResult(CallableStatement cs, int columnIndex) throws SQLException {
        java.sql.Timestamp sqlTimestamp = cs.getTimestamp(columnIndex);
        if (cs.wasNull()) {
            return null;
        } else {
            return sqlTimestamp;
        }
    }

    public java.sql.Timestamp valueOf(Object s) {
        if (s == null) {
            return null;
        } else if (s instanceof Date) {
            return new java.sql.Timestamp(((Date) s).getTime());
        }
        return new java.sql.Timestamp(SimpleDateFormatter.format(DATE_FORMAT, s.toString()).getTime());
    }

}
