/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package org.ibatis.cglib;

/**
 * NamingPolicy
 * <p>
 * Date: 2015-06-17,15:50:48 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
public interface NamingPolicy {
    String getClassName(String prefix, String source, Object key, Predicate names);

    NamingPolicy INSTANCE = new NamingPolicy() {
        @Override
        public String getClassName(String prefix, String source, Object key, Predicate names) {
            if (prefix == null) {
                prefix = "org.ibatis.cglib.Null";
            } else if (prefix.startsWith("java.")) {
                prefix = "_" + prefix;
            }
            String base = prefix + "_" + source.substring(source.lastIndexOf('.') + 1) + "ByJBati_"
                + Integer.toHexString(key.hashCode());
            String attempt = base;
            int index = 2;
            while (names.evaluate(attempt))
                attempt = base + index++;
            return attempt;
        }
    };

    NamingPolicy SIMPLE = new NamingPolicy() {
        @Override
        public String getClassName(String prefix, String member, Object key, Predicate names) {
            String base = prefix + "_" + member + "_" + key;
            String attempt = base;
            int index = 2;
            while (names.evaluate(attempt))
                attempt = base + index++;
            return attempt;
        }
    };
}
