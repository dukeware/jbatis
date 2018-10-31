/*-
 * Copyright 2015 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.engine.cache;

import java.util.Properties;

/**
 * NoneCacheController
 * <p>
 * Date: 2016-12-27
 * 
 * @author Song Sun
 * @version 1.0
 */
public class NoneCacheController implements CacheController {
    
    public static final NoneCacheController INSTANCE = new NoneCacheController();

    private NoneCacheController() {
    }
    
    @Override
    public void flush(CacheModel cacheModel) {
    }

    @Override
    public Object getObject(CacheModel cacheModel, Object key) {
        return null;
    }

    @Override
    public Object removeObject(CacheModel cacheModel, Object key) {
        return null;
    }

    @Override
    public void putObject(CacheModel cacheModel, Object key, Object object) {
    }

    @Override
    public void setProperties(CacheModel cacheModel, Properties props) {
    }

}
