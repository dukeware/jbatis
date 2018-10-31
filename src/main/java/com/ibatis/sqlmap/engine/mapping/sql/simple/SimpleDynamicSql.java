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
package com.ibatis.sqlmap.engine.mapping.sql.simple;

import com.ibatis.common.Objects;
import com.ibatis.common.beans.Probe;
import com.ibatis.common.beans.ProbeFactory;
import com.ibatis.sqlmap.client.SqlMapException;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.sql.Sql;
import com.ibatis.sqlmap.engine.scope.StatementScope;

public class SimpleDynamicSql implements Sql {

    private static final Probe PROBE = ProbeFactory.getProbe();

    private static final char ELEMENT_TOKEN_CHAR = '$';

    private String sqlStatement;

    private SqlMapExecutorDelegate delegate;

    public SimpleDynamicSql(SqlMapExecutorDelegate delegate, String sqlStatement) {
        this.delegate = delegate;
        this.sqlStatement = sqlStatement;
    }

    @Override
    public String getSql(StatementScope statementScope, Object parameterObject) {
        String sql = statementScope.getSimpleDynamicSql();
        if (sql == null) {
            sql = processDynamicElements(statementScope, sqlStatement, parameterObject);
            statementScope.setSimpleDynamicSql(sql);
        }
        return sql;
    }

    @Override
    public ParameterMap getParameterMap(StatementScope statementScope, Object parameterObject) {
        return statementScope.getParameterMap();
    }

    @Override
    public ResultMap getResultMap(StatementScope statementScope, Object parameterObject) {
        return statementScope.getResultMap();
    }

    @Override
    public void cleanup(StatementScope statementScope) {
    }

    public static boolean isSimpleDynamicSql(String sql) {
        if (sql != null && sql.indexOf(ELEMENT_TOKEN_CHAR) >= 0) {
            if (sql.indexOf('\'') < 0) {
                return true;
            }

            boolean inQuote = false;
            for (char c : sql.toCharArray()) {
                if (inQuote) {
                    if (c == '\'') {
                        inQuote = false;
                    }
                } else {
                    if (c == ELEMENT_TOKEN_CHAR) {
                        return true;
                    } else if (c == '\'') {
                        inQuote = true;
                    }
                }
            }
        }
        return false;
    }

    private String processDynamicElements(StatementScope statementScope, String sql, Object parameterObject) {
        StringBuilder buf = new StringBuilder(sql);
        StringBuilder sqlPeer = new StringBuilder();

        {
            // ## sunsong
            int startIndex = 0;
            int endIndex = -1;
            while (startIndex >= 0 && startIndex < buf.length()) {
                startIndex = Objects.indexOf(buf, ELEMENT_TOKEN_CHAR, endIndex + 1);
                if (startIndex > endIndex + 1) {
                    sqlPeer.append(buf.substring(endIndex + 1, startIndex));
                }
                if (startIndex >= 0) {
                    endIndex = Objects.indexOf(buf, ELEMENT_TOKEN_CHAR, startIndex + 1);
                    if (endIndex < 0) {
                        throw new SqlMapException(
                            "Unterminated dynamic element in near '" + buf.substring(startIndex) + "'");
                    }

                    String token = buf.substring(startIndex + 1, endIndex);

                    Object value = null;
                    if (token.isEmpty()) { // $$ -> $
                        buf.deleteCharAt(endIndex);
                        sqlPeer.append(ELEMENT_TOKEN_CHAR);
                        endIndex--;
                    } else {
                        if (token.startsWith("@")) {
                            // $@propertyName$ -> propertyValue
                            value = delegate.getGlobalProperty(token.substring(1));
                        } else if (parameterObject != null) {
                            // $propertyName$ -> propertyValue
                            if (delegate.getTypeHandlerFactory().hasTypeHandler(parameterObject.getClass())) {
                                value = parameterObject;
                            } else {
                                value = PROBE.getObject(parameterObject, token);
                            }
                        }

                        String str = value != null ? String.valueOf(value) : "";
                        buf.replace(startIndex, endIndex + 1, str);
                        endIndex = startIndex + str.length() - 1;

                        if (token.startsWith("@")) {
                            sqlPeer.append(token);
                        } else {
                            sqlPeer.append(str);
                        }
                    }
                } else {
                    sqlPeer.append(buf.substring(endIndex + 1));
                }
            }
        }

        if (statementScope != null && statementScope.getErrorContext() != null) {
            statementScope.getErrorContext().setSql(sqlPeer.toString());
        }

        return buf.toString();
    }

    @Override
    public int hashCodex() {
        return getClass().getName().hashCode() + (sqlStatement == null ? 0 : sqlStatement.hashCode());
    }

    @Override
    public String headText() {
        return sqlStatement;
    }
}
