package com.ibatis.common.logging;

public interface ILog {

    boolean isTraceEnabled();

    boolean isDebugEnabled();

    void error(String s, Throwable e);

    void error(String s);

    public void trace(String s);

    public void debug(String s);

    public void info(String s);

    public void warn(String s);

}
