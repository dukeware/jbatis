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

import java.sql.SQLException;
import java.util.List;

import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.sqlmap.engine.mapping.result.AutoResultMap;
import com.ibatis.sqlmap.engine.mapping.sql.Sql;
import com.ibatis.sqlmap.engine.mapping.sql.SqlChild;
import com.ibatis.sqlmap.engine.mapping.sql.SqlText;
import com.ibatis.sqlmap.engine.mapping.sql.dynamic.DynamicSql;
import com.ibatis.sqlmap.engine.mapping.sql.dynamic.elements.SqlTag;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.scope.StatementScope;
import com.ibatis.sqlmap.engine.transaction.Transaction;

public class SelectStatement extends MappedStatement {
    static final ILog log = ILogFactory.getLog(SelectStatement.class);

    @Override
    public StatementType getStatementType() {
        return StatementType.SELECT;
    }

    @Override
    public int executeUpdate(StatementScope statementScope, Transaction trans, Object parameterObject)
        throws SQLException {
        throw new SQLException("Select statements cannot be executed as an update.");
    }

    @Override
    public void checkSql(ErrorContext ec) {
        Sql sql = getSql();
        String txt = sql.headText();
        if (txt != null && !txt.contains("select") && !txt.contains("SELECT")) {
            ec.setObjectId(getId());
            ec.setDebugInfo("Maybe not select clause.");
            log.warn(ec.toStr());
        }
        if (sql instanceof DynamicSql) {
            if (getResultMap() instanceof AutoResultMap && !getResultMap().isAllowRemapping()) {
                DynamicSql dsql = (DynamicSql) sql;
                List<SqlChild> list = dsql.getChildren();
                boolean tag = false;
                for (SqlChild sc : list) {
                    if (sc instanceof SqlTag) {
                        tag = true;
                    }
                    if (sc instanceof SqlText) {
                        String s = ((SqlText) sc).getText();
                        if (s.contains(" FROM ") || s.contains(" from ")) {
                            if (tag) {
                                ec.setObjectId(getId());
                                ec.setDebugInfo("Maybe should set remapResults = true");
                                log.warn(ec.toStr());
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

}
