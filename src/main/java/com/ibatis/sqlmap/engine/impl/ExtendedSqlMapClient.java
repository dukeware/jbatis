package com.ibatis.sqlmap.engine.impl;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.engine.execution.SqlExecutor;
import com.ibatis.sqlmap.engine.mapping.result.ResultObjectFactory;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;

/**
 * this class is uneccessary and should be removed as soon as possible. Currently spring integration depends on it.
 */
public interface ExtendedSqlMapClient extends SqlMapClient {

    MappedStatement getMappedStatement(String id);

    boolean isLazyLoadingEnabled();

    boolean isEnhancementEnabled();

    SqlExecutor getSqlExecutor();

    SqlMapExecutorDelegate getDelegate();

    ResultObjectFactory getResultObjectFactory();

}
