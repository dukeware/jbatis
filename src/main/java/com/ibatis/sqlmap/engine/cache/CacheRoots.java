/*-
 * Copyright 2015 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.engine.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CacheRoots
 * <p>
 * Date: 2016-02-18
 * 
 * @author Song Sun
 * @version 1.0
 */
public class CacheRoots {

    Map<String, CacheRoot> roots = new ConcurrentHashMap<String, CacheRoot>();

    public void flushRoots(long timestamp, String name) {
        CacheRoot root = roots.get(name.toLowerCase().trim());
        if (root != null) {
            root.rootChanged(timestamp);
        } else {
            root = new CacheRoot(name, false);
            roots.put(name, root);
            root.rootChanged(timestamp);
        }
    }

    public void flushRoots(long timestamp, String... names) {
        for (String name : names) {
            flushRoots(timestamp, name);
        }
    }

    public void flushRoots(String name) {
        flushRoots(System.currentTimeMillis(), name);
    }

    public void flushRoots(String... names) {
        flushRoots(System.currentTimeMillis(), names);
    }

    public CacheRoot makeRoot(String name) {
        name = name.toLowerCase().trim();
        CacheRoot root = roots.get(name);
        if (root == null) {
            root = new CacheRoot(name, true);
            roots.put(name, root);
        } else {
            root.real = true;
        }
        return root;
    }

    public void reset(boolean all) {
        Map<String, CacheRoot> old = roots;
        if (all) {
            roots = new ConcurrentHashMap<String, CacheRoot>();
        }
        for (CacheRoot root : old.values()) {
            root.clear();
        }
    }

    public CacheRoot[] getRoots() {
        return roots.values().toArray(new CacheRoot[0]);
    }

}
