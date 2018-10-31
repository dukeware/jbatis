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
package com.ibatis.sqlmap.engine.mapping.parameter;

import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.sqlmap.engine.cache.CacheKey;
import com.ibatis.sqlmap.engine.exchange.DataExchange;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.scope.StatementScope;
import com.ibatis.sqlmap.engine.type.CustomTypeHandler;
import com.ibatis.sqlmap.engine.type.JdbcTypeRegistry;
import com.ibatis.sqlmap.engine.type.TypeHandler;
import com.ibatis.sqlmap.engine.type.ValueHandler;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class ParameterMap {

    private static final ILog log = ILogFactory.getLog(ParameterMap.class);

    private String id;
    private Class<?> parameterClass;

    private ParameterMapping[] parameterMappings;
    private DataExchange dataExchange;

    private String resource;

    private SqlMapExecutorDelegate delegate;

    public ParameterMap(SqlMapExecutorDelegate delegate) {
        this.delegate = delegate;
    }

    public SqlMapExecutorDelegate getDelegate() {
        return delegate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Class<?> getParameterClass() {
        return parameterClass;
    }

    public void setParameterClass(Class<?> parameterClass) {
        this.parameterClass = parameterClass;
    }

    public DataExchange getDataExchange() {
        return dataExchange;
    }

    public void setDataExchange(DataExchange dataExchange) {
        this.dataExchange = dataExchange;
    }

    public ParameterMapping[] getParameterMappings() {
        return parameterMappings;
    }

    public void setParameterMappingList(List<ParameterMapping> parameterMappingList) {
        parameterMappings = parameterMappingList.toArray(new ParameterMapping[parameterMappingList.size()]);

        dataExchange = delegate.getDataExchangeFactory().getDataExchangeForClass(parameterClass);
        dataExchange.initialize(this); // ## sunsong
    }

    public int getParameterCount() {
        return this.parameterMappings.length;
    }

    /**
     * @param ps
     * @param parameters
     * @throws java.sql.SQLException
     */
    public Object[] setParameters(StatementScope statementScope, PreparedStatement ps, Object[] parameters)
        throws SQLException {
        ErrorContext errorContext = statementScope.getErrorContext();
        errorContext.setResource(this.getResource());

        if (parameterMappings != null) {
            String activity = errorContext.getActivity();
            String objectId = errorContext.getObjectId();
            errorContext.setActivity("applying a parameter map");
            errorContext.setObjectId(this.getId());
            errorContext.setConnection(getConnection(ps));
            errorContext.setMoreInfo("Check the parameter map.");
            for (int i = 0; i < parameterMappings.length; i++) {
                ParameterMapping mapping = parameterMappings[i];
                errorContext.setMoreInfo(mapping.getErrorString());
                if (mapping.isInputAllowed()) {
                    setParameter(ps, mapping, parameters, i);
                }
            }
            // ## restore the err ctx
            errorContext.setActivity(activity);
            errorContext.setObjectId(objectId);
        }
        return parameters;
    }

    static Connection getConnection(PreparedStatement ps) {
        try {
            return ps.getConnection();
        } catch (Exception e) {
            return null;
        }
    }

    public Object[] getParameterObjectValues(StatementScope statementScope, Object parameterObject) {
        if (getParameterCount() == 0) {
            return NoParameterMap.NO_DATA;
        }
        return dataExchange.getData(statementScope, this, parameterObject);
    }

    public CacheKey getCacheKey(StatementScope statementScope, Object parameterObject) {
        return dataExchange.getCacheKey(statementScope, this, parameterObject);
    }

    public void refreshParameterObjectValues(StatementScope statementScope, Object parameterObject, Object[] values) {
        dataExchange.setData(statementScope, this, parameterObject, values);
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    @SuppressWarnings("unchecked")
    protected void setParameter(PreparedStatement ps, ParameterMapping mapping, Object[] parameters, int i)
        throws SQLException {
        Object value = parameters[i];
        // Apply Null Value
        String nullValueString = mapping.getNullValue();
        if (nullValueString != null) {
            TypeHandler handler = mapping.getTypeHandler();
            if (handler.equals(value, nullValueString)) {
                value = null;
            }
        }

        // Set Parameter
        TypeHandler typeHandler = mapping.getTypeHandler();
        if (value != null) {
            if (typeHandler instanceof ValueHandler) {
                parameters[i] = ((ValueHandler) typeHandler).setParameterValue(ps, i + 1, value,
                    mapping.getJdbcTypeName());
            } else {
                typeHandler.setParameter(ps, i + 1, value, mapping.getJdbcTypeName());
            }
        } else if (typeHandler instanceof CustomTypeHandler) {
            if (typeHandler instanceof ValueHandler) {
                parameters[i] = ((ValueHandler) typeHandler).setParameterValue(ps, i + 1, value,
                    mapping.getJdbcTypeName());
            } else {
                typeHandler.setParameter(ps, i + 1, value, mapping.getJdbcTypeName());
            }
        } else {
            int jdbcType = mapping.getJdbcType();
            if (jdbcType != JdbcTypeRegistry.UNKNOWN_TYPE) {
                ps.setNull(i + 1, jdbcType);
            } else {
                Integer nullJdbcType = delegate.getJdbcTypeForNull();
                if (nullJdbcType != null) {
                    ps.setNull(i + 1, nullJdbcType);
                } else {
                    try {
                        ps.setObject(i + 1, null);
                    } catch (SQLException ex) {
                        log.error("Set jdbc null parameter error: " + ex.getMessage()
                            + ". use <settings jdbcTypeForNull='NULL|VARCHAR|OTHER' /> to fix this problem.");
                        trySetNullParameter(ps, i);
                    }
                }
            }
        }
    }

    private void trySetNullParameter(PreparedStatement ps, int i) throws SQLException {
        int sqlTypeToUseForNullValue = Types.NULL;
        DatabaseMetaData dbmd = ps.getConnection().getMetaData();
        String databaseProductName = String.valueOf(dbmd.getDatabaseProductName());
        String jdbcDriverName = String.valueOf(dbmd.getDriverName());
        if (databaseProductName.contains(("DB2")) || jdbcDriverName.contains("jConnect")
            || jdbcDriverName.contains("SQLServer") || jdbcDriverName.contains("Apache Derby")) {
            sqlTypeToUseForNullValue = Types.VARCHAR;
        }
        ps.setNull(i + 1, sqlTypeToUseForNullValue);
        delegate.setJdbcTypeForNull(sqlTypeToUseForNullValue);
    }
}
