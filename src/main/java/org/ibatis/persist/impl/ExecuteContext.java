package org.ibatis.persist.impl;

import java.sql.SQLException;

import com.ibatis.sqlmap.engine.scope.StatementScope;
import com.ibatis.sqlmap.engine.transaction.Transaction;

public interface ExecuteContext<T> {

    T execute(StatementScope scope, Transaction tx) throws SQLException;

}
