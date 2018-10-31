package com.ibatis.sqlmap.engine.mapping.sql.raw;

import com.ibatis.sqlmap.engine.mapping.sql.Sql;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.scope.StatementScope;

/**
 * A non-executable SQL container simply for communicating raw SQL around the framework.
 */
public class RawSql implements Sql {

    private String sql;

    public RawSql(String sql) {
        this.sql = sql;
    }

    @Override
    public String getSql(StatementScope statementScope, Object parameterObject) {
        if (statementScope != null && statementScope.getErrorContext() != null) {
            statementScope.getErrorContext().setSql(sql);
        }
        return sql;
    }

    @Override
    public ParameterMap getParameterMap(StatementScope statementScope, Object parameterObject) {
        throw new RuntimeException("Method not implemented on RawSql.");
    }

    @Override
    public ResultMap getResultMap(StatementScope statementScope, Object parameterObject) {
        throw new RuntimeException("Method not implemented on RawSql.");
    }

    @Override
    public void cleanup(StatementScope statementScope) {
        throw new RuntimeException("Method not implemented on RawSql.");
    }

    @Override
    public int hashCodex() {
        return getClass().getName().hashCode() + (sql == null ? 0 : sql.hashCode());
    }

    @Override
    public String headText() {
        return sql;
    }
}
