/*-
 * Copyright 2016 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.engine.cache.memory;

import java.io.Closeable;

/**
 * MulticastorBase
 * <p>
 * Date: 2016-01-13
 * 
 * @author Song Sun
 * @version 1.0
 */
public abstract class MulticastorBase implements Closeable {

    public abstract void init(MemoryCache memoryCache);

    public abstract void multicastFlush(String cacheId);

    public abstract void onMulticastFlush(String cacheId);
}
