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
package com.ibatis.sqlmap.engine.cache;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.ibatis.client.Cache;

import com.ibatis.common.ArraySet;
import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;

/**
 * Wrapper for Caches.
 */
public class CacheModel implements Cache, FlushListener {

    private static final ILog log = ILogFactory.getLog(CacheModel.class);

    private static final int MAX_OBJECT_LOG_SIZE = 32;

    private long requests = 0, hits = 0, flushs = 0;

    private String id;
    private int maxCacheSize = -1;

    private long lastFlush;
    private long flushInterval;
    private long flushIntervalSeconds;
    private Set<String> flushTriggerRoots;
    private Set<String> flushTriggerStatements;
    private Set<String> flushTriggerCaches;
    private Set<Class<?>> flushTriggerEntityClasses;

    private CacheController controller;

    private String resource;

    private Set<FlushListener> flushListeners = new ArraySet<FlushListener>();

    /**
     * Default constructor
     */
    public CacheModel() {
        this.flushInterval = -1;
        this.flushIntervalSeconds = -1;
        this.lastFlush = System.currentTimeMillis();
        this.flushTriggerRoots = new ArraySet<String>();
        this.flushTriggerStatements = new ArraySet<String>();
        this.flushTriggerCaches = new ArraySet<String>();
        this.flushTriggerEntityClasses = new ArraySet<Class<?>>();
    }

    /**
     * Getter for the cache model's id
     *
     * @return the id
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Setter for the cache model's id
     *
     * @param id
     *            - the new id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Getter for resource property
     *
     * @return the value of the resource property
     */
    public String getResource() {
        return resource;
    }

    /**
     * Setter for resource property
     *
     * @param resource
     *            - the new value
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    public void setMaxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * Sets up the controller for the cache model
     */
    public void setCacheController(CacheController controller) {
        this.controller = controller;
    }
    public CacheController getCacheController() {
        return this.controller;
    }
    /**
     * Getter for flushInterval property
     *
     * @return The flushInterval (in milliseconds)
     */
    public long getFlushInterval() {
        return flushInterval;
    }

    /**
     * Getter for flushInterval property
     *
     * @return The flushInterval (in milliseconds)
     */
    public long getFlushIntervalSeconds() {
        return flushIntervalSeconds;
    }

    /**
     * Setter for flushInterval property
     *
     * @param flushInterval
     *            The new flushInterval (in milliseconds)
     */
    public void setFlushInterval(long flushInterval) {
        this.flushInterval = flushInterval;
        this.flushIntervalSeconds = flushInterval / 1000;
    }

    public void addFlushTriggerRoot(String name) {
        flushTriggerRoots.add(name);
    }

    /**
     * Adds a flushTriggerStatment. When a flushTriggerStatment is executed, the cache is flushed (cleared).
     *
     * @param statementName
     *            The statement to add.
     */
    public void addFlushTriggerStatement(String statementName) {
        flushTriggerStatements.add(statementName);
    }

    /**
     * Adds a flushTriggerCache. When a flushTriggerCache is flushed, the cache is flushed (cleared).
     *
     * @param cacheId
     *            The cache to add.
     */
    public void addFlushTriggerCache(String cacheId) {
        flushTriggerCaches.add(cacheId);
    }

    public void addFlushTriggerEntityClass(Class<?> clazz) {
        flushTriggerEntityClasses.add(clazz);
    }

    public Set<String> getFlushTriggerCacheRoots() {
        return flushTriggerRoots;
    }

    /**
     * Gets an Set containing all flushTriggerStatment objects for this cache.
     */
    public Set<String> getFlushTriggerStatementNames() {
        return flushTriggerStatements;
    }

    /**
     * Gets an Set containing all flushTriggerCache objects for this cache.
     */
    public Set<String> getFlushTriggerCacheNames() {
        return flushTriggerCaches;
    }

    public Set<Class<?>> getFlushTriggerEntityClasses() {
        return flushTriggerEntityClasses;
    }

    /**
     * Returns statistical information about the cache.
     *
     * @return the number of cache hits divided by the total requests
     */
    public double getHitRatio() {
        return (double) hits / (double) requests;
    }

    /**
     * Configures the cache
     *
     * @param props
     */
    public void configure(Properties props) {
        controller.setProperties(this, props);
    }

    /**
     * Clears the cache
     */
    @Override
    public void flush() {
        doFlush(System.currentTimeMillis());
    }

    synchronized void doFlush(long timestamp) {
        controller.flush(this);
        lastFlush = timestamp;
        flushs++;
        if (log.isTraceEnabled()) {
            log("flushed", false, null);
        }
        for (FlushListener fl : flushListeners) {
            fl.onFlush(id, timestamp);
        }
    }

    @Override
    public void onFlush(String id, long timestamp) {
        if (lastFlush < timestamp) {
            doFlush(timestamp);
        }
    }

    /**
     * Get an object out of the cache. A side effect of this method is that is may clear the cache if it has not been
     * cleared in the flushInterval.
     *
     * @param key
     *            The key of the object to be returned
     * @return The cached object (or null)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getObject(Object key) {
        T value = null;
        synchronized (this) {
            long curr = System.currentTimeMillis();
            if (flushInterval > 0 && curr - lastFlush > flushInterval) {
                controller.flush(this);
                lastFlush = curr;
                flushs++;
            }

            value = (T) controller.getObject(this, key);
            requests++;
            if (value != null) {
                hits++;
            }
            if (log.isTraceEnabled()) {
                if (value != null) {
                    log("retrieved #" + key, false, null);
                } else {
                    log("cache miss #" + key, false, null);
                }
            }
        }
        return value;
    }

    /**
     * Add an object to the cache
     *
     * @param key
     *            The key of the object to be cached
     * @param value
     *            The object to be cached
     */
    @Override
    public void putObject(Object key, Object value) {
        if (null == value)
            value = NULL_OBJECT;
        synchronized (this) {
            controller.putObject(this, key, value);
            if (log.isTraceEnabled()) {
                log("stored object #" + key, true, value);
            }
        }
    }

    /**
     * Get the maximum size of an object in the log output.
     *
     * @return Maximum size of a logged object in the output
     */
    protected int getMaxObjectLogSize() {
        return MAX_OBJECT_LOG_SIZE;
    }

    /**
     * Log a cache action. Since this method is pretty heavy weight, it's best to enclose it with a log.isDebugEnabled()
     * when called.
     *
     * @param action
     *            String to output
     * @param addValue
     *            Add the value being cached to the log
     * @param cacheValue
     *            The value being logged
     */
    protected void log(String action, boolean addValue, Object cacheValue) {
        StringBuilder output = new StringBuilder("Cache '");
        output.append(getId());
        output.append("': ");
        output.append(action);
        if (addValue) {
            String cacheObjectStr = (cacheValue == null ? "null" : toStr(cacheValue));
            output.append(" '");
            if (cacheObjectStr.length() < getMaxObjectLogSize()) {
                output.append(cacheObjectStr.toString());
            } else {
                output.append(cacheObjectStr.substring(0, getMaxObjectLogSize()));
                output.append("...");
            }
            output.append("'");
        }
        log.trace(output.toString());
    }

    String toStr(Object o) {
        if (o instanceof List) {
            List<?> list = (List<?>) o;
            if (list.isEmpty()) {
                return "[]";
            } else {
                return "[ " + list.size() + " ) " + list.get(0)+ "]";
            }
        } else if (o instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) o;
            if (map.isEmpty()) {
                return "{}";
            } else {
                return "{ " + map.size() + " ) " + map.entrySet().iterator().next() + "}";
            }
        }
        return o.toString();
    }

    public void setControllerProperties(Properties cacheProps) {
        controller.setProperties(this, cacheProps);
    }

    @Override
    public long getRequests() {
        return requests;
    }

    @Override
    public long getHits() {
        return hits;
    }

    @Override
    public long getFlushs() {
        return flushs;
    }

    @Override
    public long getPeriodMillis() {
        return System.currentTimeMillis() - lastFlush;
    }

    // ## sunsong
    @Override
    public String toString() {
        return "CacheModel [" + id + ", " + hits + " / " + requests + ", " + getPeriodMillis() / 1000L + "]";
    }

    public synchronized void addFlushListener(FlushListener listener) {
        flushListeners.add(listener);
    }
    
    public synchronized void removeFlushListener(FlushListener listener) {
        flushListeners.remove(listener);
    }
}
