package com.ibatis.common.logging.nologging;

import com.ibatis.common.logging.ILog;

public class NoLoggingImpl implements ILog {

    public NoLoggingImpl(String clazz) {
    }

    public boolean isDebugEnabled() {
        return false;
    }

    public boolean isTraceEnabled() {
        return false;
    }

    public void error(String s, Throwable e) {
    }

    public void error(String s) {
    }

    public void trace(String s) {
    }

    public void debug(String s) {
    }

    public void info(String s) {
    }

    public void warn(String s) {
    }

}
