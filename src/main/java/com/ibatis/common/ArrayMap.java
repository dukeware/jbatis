/*-
 * Copyright 2009-2017 Owl Group
 * All rights reserved.
 */
package com.ibatis.common;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * ArrayMap for internal use only
 * <p>
 * Date: 2017-12-30
 * 
 * @author Song Sun
 * @version 1.0
 */
public class ArrayMap implements Map<String, Object>, Serializable {

    private static final long serialVersionUID = -1L;

    Object[] array;

    public ArrayMap(Object... args) {
        this.array = args;
    }

    @Override
    public int size() {
        if (array != null) {
            return array.length;
        }
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return array == null || array.length == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof String) {
            int idx = toIndex((String) key);
            return array != null && idx >= 0 && idx < array.length;
        }

        return false;
    }

    int toIndex(String key) {
        if (key != null) {
            if (key.length() == 1) {
                char i = key.charAt(0);
                if (i <= '9') {
                    return i - '1';
                }
            }
            try {
                return Integer.parseInt(key) - 1;
            } catch (NumberFormatException e) {
            }
        }
        return -1;
    }

    @Override
    public boolean containsValue(Object value) {
        if (array != null) {
            for (Object o : array) {
                if (o == value) {
                    return true;
                }
                if (o != null && o.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Object get(Object key) {
        if (key instanceof String) {
            int idx = toIndex((String) key);
            if (array != null && idx >= 0 && idx < array.length) {
                return array[idx];
            }
        }
        return null;
    }

    @Override
    public Object put(String key, Object value) {
        int idx = toIndex(key);
        if (array != null && idx >= 0 && idx < array.length) {
            Object old = array[idx];
            array[idx] = value;
            return old;
        }
        return null;
    }

    @Override
    public Object remove(Object key) {
        if (key instanceof String) {
            return put((String) key, null);
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        if (m != null) {
            for (String key : m.keySet()) {
                put(key, m.get(key));
            }
        }
    }

    @Override
    public void clear() {
        array = null;
    }

    @Override
    public Set<String> keySet() {
        if (array == null) {
            return Collections.emptySet();
        }
        Set<String> set = new ArraySet<String>();
        for (int i = 0; i < array.length; i++) {
            set.add(String.valueOf(i + 1));
        }
        return Collections.unmodifiableSet(set);
    }

    @Override
    public Collection<Object> values() {
        if (array == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(array);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        if (array == null) {
            return Collections.emptySet();
        }
        Set<Entry<String, Object>> set = new ArraySet<Entry<String, Object>>();
        for (int i = 0; i < array.length; i++) {
            set.add(new MapEntry(String.valueOf(i + 1), array[i]));
        }
        return Collections.unmodifiableSet(set);
    }

    static class MapEntry implements Entry<String, Object> {
        String key;
        Object val;

        public MapEntry(String key, Object val) {
            this.key = key;
            this.val = val;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return val;
        }

        @Override
        public Object setValue(Object value) {
            Object val = this.val;
            this.val = value;
            return val;
        }
    }
}
