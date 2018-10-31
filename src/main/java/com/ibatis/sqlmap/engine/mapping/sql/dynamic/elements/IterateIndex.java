/*-
 * Copyright 2009-2017 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.engine.mapping.sql.dynamic.elements;

/**
 * IterateIndex
 * <p>
 * Date: 2017-11-28
 * 
 * @author Song Sun
 * @version 1.0
 */
public class IterateIndex {

    private static final Object Null = "<Null>";

    private int processIndex;
    private String processString;
    private String processKey;
    private Object processValue = Null;

    public int getProcessIndex() {
        return processIndex;
    }

    public void setProcessIndex(int processIndex) {
        this.processIndex = processIndex;
    }

    public String getProcessString() {
        return processString;
    }

    public void setProcessString(String processString) {
        this.processString = processString;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public Object getProcessValue() {
        return processValue;
    }

    public void setProcessValue(Object processValue) {
        this.processValue = processValue;
    }

    @Override
    public String toString() {
        return "IterateIndex [processIndex=" + processIndex + ", processString=" + processString + "]";
    }

    public boolean hasProcessValue() {
        return processValue != Null;
    }
}
