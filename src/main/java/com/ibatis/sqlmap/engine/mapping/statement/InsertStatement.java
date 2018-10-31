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
package com.ibatis.sqlmap.engine.mapping.statement;

import com.ibatis.common.beans.ProbeFactory;
import com.ibatis.common.jdbc.exception.NestedSQLException;
import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.sqlmap.client.event.RowHandler;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.sql.Sql;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.scope.StatementScope;
import com.ibatis.sqlmap.engine.transaction.Transaction;

import java.sql.SQLException;
import java.util.List;

public class InsertStatement extends MappedStatement {
    static final ILog log = ILogFactory.getLog(InsertStatement.class);

    private SelectKeyStatement selectKeyStatement;

    @Override
    public StatementType getStatementType() {
        return StatementType.INSERT;
    }

    @Override
    public <T> T executeQueryForObject(StatementScope statementScope, Transaction trans, Object parameterObject,
        Object resultObject) throws SQLException {
        throw new SQLException("Insert statements cannot be executed as a query.");
    }

    @Override
    public <T> int executeQueryForPage(StatementScope statementScope, List<T> page, Transaction trans,
        Object paramObject, int skipResults, int maxResults) throws SQLException {
        throw new SQLException("Insert statements cannot be executed as a query.");
    }

    @Override
    public <T> List<T> executeQueryForList(StatementScope statementScope, Transaction trans, Object parameterObject,
        int skipResults, int maxResults) throws SQLException {
        throw new SQLException("Insert statements cannot be executed as a query.");
    }

    @Override
    public void executeQueryWithRowHandler(StatementScope statementScope, Transaction trans, Object parameterObject,
        RowHandler rowHandler) throws SQLException {
        throw new SQLException("Update statements cannot be executed as a query.");
    }

    public SelectKeyStatement getSelectKeyStatement() {
        return selectKeyStatement;
    }

    public void setSelectKeyStatement(SelectKeyStatement selectKeyStatement) {
        this.selectKeyStatement = selectKeyStatement;
    }

    public Object executeInsert(StatementScope statementScope, Transaction trans, Object parameterObject)
        throws SQLException {
        ErrorContext errorContext = statementScope.getErrorContext();
        errorContext.setActivity("preparing the mapped statement for execution");
        errorContext.setObjectId(this.getId());
        errorContext.setResource(this.getResource());

        statementScope.getSession().setCommitRequired(true);

        try {
            parameterObject = validateParameter(parameterObject);

            Sql sql = getSql();

            errorContext.setMoreInfo("Check the parameter map.");
            ParameterMap parameterMap = sql.getParameterMap(statementScope, parameterObject);

            errorContext.setMoreInfo("Check the result map.");
            ResultMap resultMap = sql.getResultMap(statementScope, parameterObject);

            statementScope.setResultMap(resultMap);
            statementScope.setParameterMap(parameterMap);

            errorContext.setMoreInfo("Check the parameter map.");
            Object[] parameters = parameterMap.getParameterObjectValues(statementScope, parameterObject);

            errorContext.setMoreInfo("Check the SQL statement.");
            String sqlString = sql.getSql(statementScope, parameterObject);

            errorContext.setActivity("executing mapped statement");
            errorContext.setMoreInfo("Check the statement or the result map.");
            errorContext.setConnection(trans.getConnection());
            Object generatedKey = getSqlExecutor().executeInsert(getId(), statementScope, trans.getConnection(), sqlString,
                parameters, selectKeyStatement.getResultMap());

            errorContext.setMoreInfo("Set back the generatedKey.");
            String keyProp = selectKeyStatement.getKeyProperty();
            if (keyProp != null) {
                ProbeFactory.getProbe().setObject(parameterObject, keyProp, generatedKey);
            }

            errorContext.setMoreInfo("Check the output parameters.");
            if (parameterObject != null) {
                postProcessParameterObject(statementScope, parameterObject, parameters);
            }

            // errorContext.reset();
            sql.cleanup(statementScope);
            notifyListeners(null);
            return generatedKey;
        } catch (SQLException e) {
            errorContext.setCause(e);
            throw new NestedSQLException(errorContext.toString(), e.getSQLState(), e.getErrorCode(), e);
        } catch (Exception e) {
            errorContext.setCause(e);
            throw new NestedSQLException(errorContext.toString(), e);
        }
    }

    @Override
    public boolean isCanBatch() {
        return canBatch && (getSelectKeyStatement() == null || !getSelectKeyStatement().isRunAfterSQL());
    }

    @Override
    public void checkSql(ErrorContext ec) {
        Sql sql = getSql();
        String txt = sql.headText();
        if (txt != null && !txt.contains("into") && !txt.contains("INTO")) {
            ec.setObjectId(getId());
            ec.setDebugInfo("Maybe not insert/replace clause.");
            log.warn(ec.toStr());
        }
    }
}
