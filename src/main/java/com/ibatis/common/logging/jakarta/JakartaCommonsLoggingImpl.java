package com.ibatis.common.logging.jakarta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JakartaCommonsLoggingImpl implements com.ibatis.common.logging.ILog {

    private Log log;

    public JakartaCommonsLoggingImpl(String clazz) {
        log = LogFactory.getLog(clazz);
    }

    public boolean isDebugEnabled() {
        if (log == null)
            return false;
        return log.isDebugEnabled();
    }

    public void error(String s, Throwable e) {
        if (log != null)
            log.error(s, e);
    }

    public void error(String s) {
        if (log != null)
            log.error(s);
    }

    public void trace(String s) {
        if (log != null)
            log.trace(s);
    }

    public void debug(String s) {
        if (log != null)
            log.debug(s);
    }

    public void info(String s) {
        if (log != null)
            log.info(s);
    }

    public void warn(String s) {
        if (log != null)
            log.warn(s);
    }

    public boolean isTraceEnabled() {
        if (log == null)
            return false;
        return log.isTraceEnabled();
    }

}
