/*-
 * Copyright 2009-2017 Owl Group
 * All rights reserved.
 */
package org.ibatis.client;

/**
 * Dialect
 * <p>
 * Date: 2017-12-29
 * 
 * @author Song Sun
 * @version 1.0
 */
public enum Dialect {
    mysql,
    oracle,
    sqlserver,
    postgresql,
    db2;

    public static Dialect forName(String dialect) {
        try {
            return valueOf(dialect.toLowerCase());
        } catch (Exception e) {
        }
        return null;
    }
}
