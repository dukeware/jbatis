package com.ibatis.common.logging.jdk14;

import java.util.logging.Logger;
import java.util.logging.Level;

public class Jdk14LoggingImpl implements com.ibatis.common.logging.ILog {

    private Logger log;

    public Jdk14LoggingImpl(String clazz) {
        log = Logger.getLogger(clazz);
    }

    public boolean isDebugEnabled() {
        return log.isLoggable(Level.FINE);
    }

    public boolean isTraceEnabled() {
        return log.isLoggable(Level.ALL);
    }

    public void error(String s, Throwable e) {
        log.log(Level.SEVERE, s, e);
    }

    public void error(String s) {
        log.log(Level.SEVERE, s);
    }

    public void trace(String s) {
        log.log(Level.FINEST, s);
    }

    public void debug(String s) {
        log.log(Level.FINE, s);
    }

    public void info(String s) {
        log.log(Level.INFO, s);
    }

    public void warn(String s) {
        log.log(Level.WARNING, s);
    }

}
