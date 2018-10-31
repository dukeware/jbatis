/*-
 * Copyright 2015 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.engine.cache;

import java.util.Set;

import com.ibatis.common.ArraySet;

/**
 * CacheRoot
 * <p>
 * Date: 2016-02-18
 * 
 * @author Song Sun
 * @version 1.0
 */
public final class CacheRoot implements FlushListener {
    final String id;
    long lastFlush = 0L;
    long flushCount = 0L;
    boolean real;

    CacheRoot(String id, boolean real) {
        this.id = id;
        this.real = real;
    }

    Set<FlushListener> flushListeners = new ArraySet<FlushListener>();

    public void rootChanged(long time) {
        if (time > lastFlush) {
            lastFlush = time;
            flushCount++;
            synchronized (flushListeners) {
                for (FlushListener f : flushListeners) {
                    f.onFlush(id, time);
                }
            }
        }
    }

    public void addFlushListener(FlushListener l) {
        synchronized (flushListeners) {
            flushListeners.add(l);
        }
    }

    public void clear() {
        synchronized (flushListeners) {
            flushListeners.clear();
        }
    }

    @Override
    public void onFlush(String id, long t) {
        rootChanged(t);
    }

    public long getLastFlush() {
        return lastFlush;
    }

    public long getFlushCount() {
        return flushCount;
    }

    public String getId() {
        return id;
    }

    public boolean isReal() {
        return real;
    }
}
