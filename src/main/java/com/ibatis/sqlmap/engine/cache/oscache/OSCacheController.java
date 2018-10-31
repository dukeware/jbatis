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
package com.ibatis.sqlmap.engine.cache.oscache;

import com.ibatis.sqlmap.engine.cache.CacheController;
import com.ibatis.sqlmap.engine.cache.CacheModel;
import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;

import java.util.Properties;

/**
 * Cache implementation for using OSCache with iBATIS
 */
public class OSCacheController implements CacheController {

    static class OSCache {
        static final GeneralCacheAdministrator CACHE = new GeneralCacheAdministrator();
    }

    @Override
    public void flush(CacheModel cacheModel) {
        OSCache.CACHE.flushGroup(cacheModel.getId());
    }

    @Override
    public Object getObject(CacheModel cacheModel, Object key) {
        String keyString = key.toString();
        try {
            int refreshPeriod = (int) (cacheModel.getFlushIntervalSeconds());
            return OSCache.CACHE.getFromCache(keyString, refreshPeriod);
        } catch (NeedsRefreshException e) {
            OSCache.CACHE.cancelUpdate(keyString);
            return null;
        }
    }

    @Override
    public Object removeObject(CacheModel cacheModel, Object key) {
        Object result;
        String keyString = key.toString();
        try {
            int refreshPeriod = (int) (cacheModel.getFlushIntervalSeconds());
            Object value = OSCache.CACHE.getFromCache(keyString, refreshPeriod);
            if (value != null) {
                OSCache.CACHE.flushEntry(keyString);
            }
            result = value;
        } catch (NeedsRefreshException e) {
            try {
                OSCache.CACHE.flushEntry(keyString);
            } finally {
                OSCache.CACHE.cancelUpdate(keyString);
                result = null;
            }
        }
        return result;
    }

    @Override
    public void putObject(CacheModel cacheModel, Object key, Object object) {
        String keyString = key.toString();
        OSCache.CACHE.putInCache(keyString, object, new String[] { cacheModel.getId() });
    }

    @Override
    public void setProperties(CacheModel cacheModel, Properties props) {
    }

}
