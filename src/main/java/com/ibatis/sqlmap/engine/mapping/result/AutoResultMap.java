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
package com.ibatis.sqlmap.engine.mapping.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibatis.common.beans.Probe;
import com.ibatis.common.beans.ProbeFactory;
import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.sqlmap.client.SqlMapException;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.scope.StatementScope;

/**
 * An automatic result map for simple stuff
 */
public class AutoResultMap extends ResultMap {
    private static final ILog log = ILogFactory.getLog(AutoResultMap.class);

    /**
     * Constructor to pass in the SqlMapExecutorDelegate
     *
     * @param delegate
     *            - the delegate
     */
    public AutoResultMap(SqlMapExecutorDelegate delegate, boolean allowRemapping) {
        super(delegate);
        this.allowRemapping = allowRemapping;
    }

    @Override
    public synchronized Object[] getResults(StatementScope statementScope, ResultSet rs) throws SQLException {
        if (allowRemapping || getResultMappings() == null) {
            initialize(rs);
        }
        return super.getResults(statementScope, rs);
    }

    @Override
    public Object setResultObjectValues(StatementScope statementScope, Object resultObject, Object[] values) {
        // synchronization is only needed when remapping is enabled
        if (allowRemapping) {
            synchronized (this) {
                return super.setResultObjectValues(statementScope, resultObject, values);
            }
        }
        return super.setResultObjectValues(statementScope, resultObject, values);
    }

    public void initialize(ResultSet rs) {
        if (getResultClass() == null) {
            throw new SqlMapException("The automatic ResultMap named " + this.getId()
                + " had a null result class (not allowed).");
        } else if (Map.class.isAssignableFrom(getResultClass())) {
            initializeMapResults(rs);
        } else if (getDelegate().getTypeHandlerFactory().getTypeHandler(getResultClass()) != null) {
            initializePrimitiveResults(rs);
        } else {
            initializeBeanResults(rs);
        }
    }

    private void initializeBeanResults(ResultSet rs) {
        try {
            List<ResultMapping> resultMappingList = new ArrayList<ResultMapping>();
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 0, n = rsmd.getColumnCount(); i < n; i++) {
                String columnName = getColumnIdentifier(rsmd, i + 1);
                Class<?> type = null;
                Probe p = ProbeFactory.getProbe(this.getResultClass());
                try {
                    type = p.getPropertyTypeForSetter(this.getResultClass(), columnName);
                } catch (Exception e) {
                    log.warn("Failed to get type for " + getResultClass() + "#" + columnName + ", " + e);
                }
                if (type != null) {
                    ResultMapping resultMapping = new ResultMapping();
                    resultMapping.setPropertyName(columnName);
                    resultMapping.setColumnName(columnName);
                    resultMapping.setColumnIndex(i + 1);
                    resultMapping.setTypeHandler(getDelegate().getTypeHandlerFactory().getTypeHandler(type));
                    // map SQL to JDBC type
                    resultMappingList.add(resultMapping);
                }
            }
            setResultMappingList(resultMappingList);

        } catch (SQLException e) {
            throw new RuntimeException("Error automapping columns. Cause: " + e);
        }

    }

    private void initializeMapResults(ResultSet rs) {
        try {
            List<ResultMapping> resultMappingList = new ArrayList<ResultMapping>();
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 0, n = rsmd.getColumnCount(); i < n; i++) {
                String columnName = getColumnIdentifier(rsmd, i + 1);
                ResultMapping resultMapping = new ResultMapping();
                resultMapping.setPropertyName(columnName);
                resultMapping.setColumnName(columnName);
                resultMapping.setColumnIndex(i + 1);
                resultMapping.setTypeHandler(getDelegate().getTypeHandlerFactory().getTypeHandler(Object.class));
                resultMappingList.add(resultMapping);
            }

            setResultMappingList(resultMappingList);

        } catch (SQLException e) {
            throw new RuntimeException("Error automapping columns. Cause: " + e);
        }
    }

    private void initializePrimitiveResults(ResultSet rs) {
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            String columnName = getColumnIdentifier(rsmd, 1);
            ResultMapping resultMapping = new ResultMapping();
            resultMapping.setPropertyName(columnName);
            resultMapping.setColumnName(columnName);
            resultMapping.setColumnIndex(1);
            resultMapping.setTypeHandler(getDelegate().getTypeHandlerFactory().getTypeHandler(getResultClass()));

            List<ResultMapping> resultMappingList = new ArrayList<ResultMapping>();
            resultMappingList.add(resultMapping);

            setResultMappingList(resultMappingList);

        } catch (SQLException e) {
            throw new RuntimeException("Error automapping columns. Cause: " + e);
        }
    }

    private String getColumnIdentifier(ResultSetMetaData rsmd, int i) throws SQLException {
        if (delegate.isUseColumnLabel()) {
            return rsmd.getColumnLabel(i);
        } else {
            return rsmd.getColumnName(i);
        }
    }

}
