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
package com.ibatis.sqlmap.engine.cache.memory;

import java.util.Properties;

import com.ibatis.sqlmap.engine.cache.CacheController;
import com.ibatis.sqlmap.engine.cache.CacheModel;

/**
 * Memory-based implementation of CacheController
 */
public class MemoryCacheController implements CacheController {

    private MemoryCacheLevel referenceType = MemoryCacheLevel.STRONG;

    /**
     * Configures the cache
     *
     * @param props
     *            Optionally can contain properties [reference-type=WEAK|SOFT|STRONG]
     */
    @Override
    public void setProperties(CacheModel cacheModel, Properties props) {
        String refType = props.getProperty("reference-type");
        if (refType == null) {
            refType = props.getProperty("referenceType");
        }
        if (refType != null) {
            referenceType = MemoryCacheLevel.getByReferenceType(refType);
        }
        String mcs = props.getProperty("memory.cache.size");
        if (mcs != null) {
            try {
                int val = Integer.parseInt(mcs);
                MemoryCache.getInstance().setCacheSize(cacheModel, val);
            } catch (Exception e) {
            }
        }
    }

    public MemoryCacheLevel getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(MemoryCacheLevel referenceType) {
        this.referenceType = referenceType;
    }

    /**
     * Add an object to the cache
     *
     * @param cacheModel
     *            The cacheModel
     * @param key
     *            The key of the object to be cached
     * @param value
     *            The object to be cached
     */
    @Override
    public void putObject(CacheModel cacheModel, Object key, Object value) {
        MemoryCache.getInstance().putEntry(cacheModel, referenceType, key, value);
    }

    /**
     * Get an object out of the cache.
     *
     * @param cacheModel
     *            The cache model
     * @param key
     *            The key of the object to be returned
     * @return The cached object (or null)
     */
    @Override
    public Object getObject(CacheModel cacheModel, Object key) {
        return MemoryCache.getInstance().getEntry(cacheModel, key);
    }

    @Override
    public Object removeObject(CacheModel cacheModel, Object key) {
        return MemoryCache.getInstance().removeEntry(cacheModel, key);
    }

    /**
     * Flushes the cache.
     *
     * @param cacheModel
     *            The cache model
     */
    @Override
    public void flush(CacheModel cacheModel) {
        MemoryCache.getInstance().flushGroup(cacheModel.getId());
    }

}
