package org.ibatis.cglib.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

public final class MProxy implements MethodProxy {
    AccessibleObject[] accessibleObjects;
    private final Method method;
    private final int mIdx;

    public MProxy(int mIdx, Method method, AccessibleObject... ao) {
        this.method = method;
        this.accessibleObjects = ao;
        this.mIdx = mIdx;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        for (AccessibleObject ao : accessibleObjects) {
            T t = ao.getAnnotation(annotationClass);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public Object invokeSuper(Object obj, Object... args) throws Throwable {
        return ((Factory) obj).$invoke(mIdx, args);
    }

    @Override
    public String getName() {
        return method.getName();
    }

    public Method getMethod() {
        return method;
    }
}
