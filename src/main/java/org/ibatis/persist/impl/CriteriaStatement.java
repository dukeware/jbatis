/*-
 * Copyright 2012 Owl Group
 * All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 */

package org.ibatis.persist.impl;

import org.ibatis.persist.meta.EntityType;

import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;

/**
 * CriteriaStatement
 * <p>
 * Date: 2015-06-26,09:51:28 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
public interface CriteriaStatement {
    
    void prepare();
    
    boolean isQuery();

    String getSql();

    ParameterInfo<?>[] getParameterInfos();

    Class<?> getResultType();

    ParameterMap makeParameterMap(SqlMapExecutorDelegate delegate);

    void flushCache(SqlMapExecutorDelegate delegate);
    
    EntityType<?> getQueryCacheType();
}
