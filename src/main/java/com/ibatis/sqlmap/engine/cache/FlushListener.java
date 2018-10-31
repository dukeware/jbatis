/*-
 * Copyright 2015 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.engine.cache;

/**
 * FlushListener
 * <p>
 * Date: 2016-01-19
 * 
 * @author Song Sun
 * @version 1.0
 */
public interface FlushListener {
    
    void onFlush(String id, long timestamp);

}
