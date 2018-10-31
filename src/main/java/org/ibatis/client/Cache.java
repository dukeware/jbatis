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

package org.ibatis.client;

import com.ibatis.sqlmap.engine.cache.FlushListener;

/**
 * Cache
 * <p>
 * Date: 2015-08-10,15:41:15 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
public interface Cache extends FlushListener {
    enum Nul {
        Null
    }
    /**
     * This is used to represent null objects that are returned from the cache so that they can be cached, too.
     */
    Object NULL_OBJECT = Nul.Null;

    String getId();

    void flush();

    <T> T getObject(Object key);

    <T> void putObject(Object key, T value);
    

    long getRequests();

    long getHits();

    long getFlushs();

    long getPeriodMillis();

}
