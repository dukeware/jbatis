package com.ibatis.sqlmap.engine.cache.memory;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.engine.cache.CacheModel;

/**
 * MemoryCache
 * <p>
 * Date: 2016-01-13
 * 
 * @author Song Sun
 * @version 1.0
 */
public class MemoryCache {
    static final ILog log = ILogFactory.getLog(MemoryCache.class);

    ConcurrentHashMap<String, LRUMap> cache = new ConcurrentHashMap<String, LRUMap>();

    MulticastorBase multicast;

    MemoryCache() {
        Properties p = Resources.getIbatisIniProperties();
        if (!"true".equals(p.getProperty("memory.mcast.enable"))) {
            return;
        }
        multicast = new MemoryCacheJGroupsBroadcastor();
        multicast.init(this);
    }

    LRUMap cache(String id) {
        LRUMap map = cache.get(id);
        if (map == null) {
            map = new LRUMap();
            cache.put(id, map);
        }
        return map;
    }

    public void setCacheSize(CacheModel cacheModel, int val) {
        cache(cacheModel.getId()).setMaxCacheSize(val);
    }

    public Object getEntry(CacheModel cacheModel, Object key) {
        return cache(cacheModel.getId()).getObject(key);
    }

    public void putEntry(CacheModel cm, MemoryCacheLevel lvl, Object key, Object value) {
        CacheEntry ce = new CacheEntry(System.currentTimeMillis() + cm.getFlushInterval(), lvl, value);
        cache(cm.getId()).put(key, ce);
    }

    public Object removeEntry(CacheModel cacheModel, Object key) {
        CacheEntry ce = cache(cacheModel.getId()).remove(key);
        return ce == null ? null : ce.get();
    }

    public void flushGroup(String id) {
        LRUMap map = cache.get(id);
        if (map != null) {
            map.clear();
            if (multicast != null) {
                multicast.multicastFlush(id);
            }
        }
    }

    static class LRUMap extends LinkedHashMap<Object, CacheEntry> {
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 4058604680659857139L;

        int maxCacheSize = 512;

        protected boolean removeEldestEntry(Entry<Object, CacheEntry> eldest) {
            return maxCacheSize > 0 && size() > maxCacheSize;
        }

        public synchronized Object getObject(Object key) {
            CacheEntry ce = super.get(key);
            if (ce != null) {
                Object value = ce.get();
                if (value == null) {
                    super.remove(key);
                    return null;
                } else {
                    super.put(key, ce);
                }
                return value;
            }
            return null;
        }

        public synchronized CacheEntry put(Object key, CacheEntry value) {
            return super.put(key, value);
        }

        public synchronized void clear() {
            super.clear();
        }

        public synchronized CacheEntry remove(Object key) {
            return super.remove(key);
        }

        public int getMaxCacheSize() {
            return maxCacheSize;
        }

        public void setMaxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
        }

    }

    /**
     * Class to implement a strong (permanent) reference.
     */
    static class CacheEntry {
        private Object object;
        private final boolean ref;
        private long expiredTime;

        /**
         * StrongReference constructor for an object
         * 
         * @param object
         *            - the Object to store
         */
        public CacheEntry(long expiredTime, MemoryCacheLevel level, Object object) {
            this.expiredTime = expiredTime;
            this.ref = level != MemoryCacheLevel.STRONG;
            if (level == MemoryCacheLevel.STRONG) {
                this.object = object;
            } else if (level == MemoryCacheLevel.SOFT) {
                this.object = new SoftReference<Object>(object);
            } else {
                this.object = new WeakReference<Object>(object);
            }
        }

        /**
         * Getter to get the object stored in the StrongReference
         * 
         * @return - the stored Object
         */
        public Object get() {
            if (expiredTime < System.currentTimeMillis()) {
                return null;
            }
            if (ref) {
                return ((Reference<?>) object).get();
            }
            return object;
        }
    }

    static final MemoryCache Instance = new MemoryCache();

    public static MemoryCache getInstance() {
        return Instance;
    }
}
