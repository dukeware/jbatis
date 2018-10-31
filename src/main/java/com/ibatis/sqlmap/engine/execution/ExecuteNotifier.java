/*-
 * Copyright 2009-2017 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.engine.execution;

/**
 * ExecuteNotifier
 * <p>
 * Date: 2017-12-04
 * 
 * @author Song Sun
 * @version 1.0
 */
public interface ExecuteNotifier {

    String getId();

    /**
     * @param arg
     *            a SessionScope or timestamp object
     */
    void notifyListeners(Object arg);
}
