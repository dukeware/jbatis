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
 * Short implementation of TypeHandler
 */
public class ShortTypeHandler extends BaseTypeHandler<Short> {
    
    static final Short _ZERO = (short) 0;
    static final Short _ONE = (short) 1;

    public void setParameter(PreparedStatement ps, int i, Short parameter, String jdbcType) throws SQLException {
        ps.setShort(i, parameter.shortValue());
    }

    public Short getResult(ResultSet rs, String columnName) throws SQLException {
        short s = rs.getShort(columnName);
        if (rs.wasNull()) {
            return null;
        } else {
            return Short.valueOf(s);
        }
    }

    public Short getResult(ResultSet rs, int columnIndex) throws SQLException {
        short s = rs.getShort(columnIndex);
        if (rs.wasNull()) {
            return null;
        } else {
            return Short.valueOf(s);
        }
    }

    public Short getResult(CallableStatement cs, int columnIndex) throws SQLException {
        short s = cs.getShort(columnIndex);
        if (cs.wasNull()) {
            return null;
        } else {
            return Short.valueOf(s);
        }
    }

    public Short valueOf(Object s) {
        if (s == null) {
            return null;
        } else if (s instanceof Boolean) {
            return (short) (Boolean.TRUE.equals(s) ? _ONE : _ZERO);
        }
        return Short.valueOf(s.toString());
    }

}
