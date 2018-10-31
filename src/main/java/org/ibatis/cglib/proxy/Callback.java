package org.ibatis.cglib.proxy;


public interface Callback {
    Object intercept(MethodProxy proxy, Object obj, Object... args) throws Throwable;
}
