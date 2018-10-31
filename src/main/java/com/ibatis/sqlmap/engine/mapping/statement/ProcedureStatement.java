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

import java.sql.Connection;
import java.sql.SQLException;

import com.ibatis.sqlmap.engine.scope.StatementScope;

public class ProcedureStatement extends MappedStatement {

    @Override
    protected void postProcessParameterObject(StatementScope statementScope, Object parameterObject, Object[] parameters) {
        statementScope.getParameterMap().refreshParameterObjectValues(statementScope, parameterObject, parameters);
    }

    @Override
    protected int sqlExecuteUpdate(StatementScope statementScope, Connection conn, String sqlString, Object[] parameters)
        throws SQLException {
        if (isCanBatch() && statementScope.getSession().isInBatch()) {
            return getSqlExecutor().addBatch(statementScope, conn, sqlString, parameters);
        } else {
            return getSqlExecutor().executeUpdateProcedure(getId(), statementScope, conn, sqlString.trim(), parameters);
        }
    }

    @Override
    protected void sqlExecuteQuery(StatementScope statementScope, Connection conn, String sqlString,
        Object[] parameters, int skipResults, int maxResults, RowHandlerCallback callback) throws SQLException {
        getSqlExecutor().executeQueryProcedure(getId(), statementScope, conn, sqlString.trim(), parameters, skipResults,
            maxResults, callback);
    }

    @Override
    public StatementType getStatementType() {
        return StatementType.PROCEDURE;
    }
}
