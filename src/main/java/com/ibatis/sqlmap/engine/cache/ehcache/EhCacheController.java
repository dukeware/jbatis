package com.ibatis.sqlmap.engine.cache.ehcache;

import java.net.URL;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import com.ibatis.sqlmap.engine.cache.CacheController;
import com.ibatis.sqlmap.engine.cache.CacheModel;

/**
 * EhCache Implementation of the {@link com.ibatis.sqlmap.engine.cache.CacheController} interface to be able to use
 * EhCache as a cache implementation in iBatis.
 */
public class EhCacheController implements CacheController {

    /** The EhCache CacheManager. */
    private static volatile CacheManager cacheManager;
    private static final AtomicInteger refCounter = new AtomicInteger();

    /**
     * Flush a cache model.
     * 
     * @param cacheModel
     *            - the model to flush.
     */
    @Override
    public void flush(CacheModel cacheModel) {
        getCache(cacheModel).removeAll();
    }

    /**
     * Get an object from a cache model.
     * 
     * @param cacheModel
     *            - the model.
     * @param key
     *            - the key to the object.
     * @return the object if in the cache, or null(?).
     */
    @Override
    public Object getObject(CacheModel cacheModel, Object key) {
        Object result = null;
        Element element = getCache(cacheModel).get(key);
        if (element != null) {
            result = element.getObjectValue();
        }
        return result;

    }

    /**
     * Put an object into a cache model.
     * 
     * @param cacheModel
     *            - the model to add the object to.
     * @param key
     *            - the key to the object.
     * @param object
     *            - the object to add.
     */
    @Override
    public void putObject(CacheModel cacheModel, Object key, Object object) {
        getCache(cacheModel).put(new Element(key, object));
    }

    /**
     * Remove an object from a cache model.
     * 
     * @param cacheModel
     *            - the model to remove the object from.
     * @param key
     *            - the key to the object.
     * @return the removed object(?).
     */
    @Override
    public Object removeObject(CacheModel cacheModel, Object key) {
        Object result = this.getObject(cacheModel, key);
        getCache(cacheModel).remove(key);
        return result;
    }

    /**
     * Configure a cache controller. Initialize the EH Cache Manager as a singleton.
     * 
     * @param props
     *            - the properties object continaing configuration information.
     */
    @Override
    public void setProperties(CacheModel cacheModel, Properties props) {
        refCounter.incrementAndGet();
        if (cacheManager == null) {
            String configLocation = props.getProperty("ehcacheConfigLocaion");
            if (configLocation == null) {
                configLocation = "/ehcache.xml";
            }
            URL url = getClass().getResource(configLocation);
            if (url != null) {
                cacheManager = CacheManager.create(url);
            } else {
                cacheManager = CacheManager.create();
            }
        }
    }

    /**
     * Gets an EH Cache based on an iBatis cache Model.
     * 
     * @param cacheModel
     *            - the cache model.
     * @return the EH Cache.
     */
    private Cache getCache(CacheModel cacheModel) {
        String cacheName = cacheModel.getId();
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            cacheManager.addCache(cacheName);
            cache = cacheManager.getCache(cacheName);
        }
        return cache;
    }

    /**
     * Shut down the EH Cache CacheManager.
     */
    @Override
    public void finalize() {
        if (refCounter.decrementAndGet() < 1) {
            if (cacheManager != null) {
                cacheManager.shutdown();
            }
            cacheManager = null;
        }
    }
}
