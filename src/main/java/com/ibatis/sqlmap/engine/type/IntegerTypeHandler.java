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
 * Integer Decimal implementation of TypeHandler
 */
public class IntegerTypeHandler extends BaseTypeHandler<Integer> {

    static final Integer _ZERO = 0;
    static final Integer _ONE = 1;

    public void setParameter(PreparedStatement ps, int i, Integer parameter, String jdbcType) throws SQLException {
        ps.setInt(i, parameter.intValue());
    }

    public Integer getResult(ResultSet rs, String columnName) throws SQLException {
        int i = rs.getInt(columnName);
        if (rs.wasNull()) {
            return null;
        } else {
            return Integer.valueOf(i);
        }
    }

    public Integer getResult(ResultSet rs, int columnIndex) throws SQLException {
        int i = rs.getInt(columnIndex);
        if (rs.wasNull()) {
            return null;
        } else {
            return Integer.valueOf(i);
        }
    }

    public Integer getResult(CallableStatement cs, int columnIndex) throws SQLException {
        int i = cs.getInt(columnIndex);
        if (cs.wasNull()) {
            return null;
        } else {
            return Integer.valueOf(i);
        }
    }

    public Integer valueOf(Object s) {
        if (s == null) {
            return null;
        } else if (s instanceof Boolean) {
            return Boolean.TRUE.equals(s) ? _ONE : _ZERO;
        }
        return Integer.valueOf(s.toString());
    }

}
