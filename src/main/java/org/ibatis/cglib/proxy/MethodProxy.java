package org.ibatis.cglib.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface MethodProxy {

    Object invokeSuper(Object obj, Object... args) throws Throwable;

    String getName();

    Method getMethod();

    <T extends Annotation> T getAnnotation(Class<T> annotationClass);
}
