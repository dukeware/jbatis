/*-
 * Copyright 2009-2017 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.engine.dialect;

import com.ibatis.sqlmap.engine.scope.ErrorContext;

/**
 * PageDialect
 * <p>
 * Date: 2018-01-01
 * 
 * @author Song Sun
 * @version 1.0
 */
public interface PageDialect {

    PageDialect canHandle(String productNameLowerCase, int majorVersion, int minorVersion);

    String getPageSql(ErrorContext ec);

    String getCountSql(ErrorContext ec);
}
