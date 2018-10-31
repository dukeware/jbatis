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

/**
 * Long implementation of TypeHandler
 */
public class LongTypeHandler extends BaseTypeHandler<Long> {
    
    static final Long _ZERO = 0L;
    static final Long _ONE = 1L;

    public void setParameter(PreparedStatement ps, int i, Long parameter, String jdbcType) throws SQLException {
        ps.setLong(i, parameter.longValue());
    }

    public Long getResult(ResultSet rs, String columnName) throws SQLException {
        long l = rs.getLong(columnName);
        if (rs.wasNull()) {
            return null;
        } else {
            return Long.valueOf(l);
        }
    }

    public Long getResult(ResultSet rs, int columnIndex) throws SQLException {
        long l = rs.getLong(columnIndex);
        if (rs.wasNull()) {
            return null;
        } else {
            return Long.valueOf(l);
        }
    }

    public Long getResult(CallableStatement cs, int columnIndex) throws SQLException {
        long l = cs.getLong(columnIndex);
        if (cs.wasNull()) {
            return null;
        } else {
            return Long.valueOf(l);
        }
    }

    public Long valueOf(Object s) {
        if (s == null) {
            return null;
        } else if (s instanceof Boolean) {
            return Boolean.TRUE.equals(s) ? _ONE : _ZERO;
        }
        return Long.valueOf(s.toString());
    }

}
