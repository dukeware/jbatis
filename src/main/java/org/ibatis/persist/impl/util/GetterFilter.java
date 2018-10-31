package org.ibatis.persist.impl.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.ibatis.cglib.proxy.CallbackFilter;

public class GetterFilter implements CallbackFilter {

    private final int index;

    public GetterFilter(int index) {
        this.index = index;
    }

    @Override
    public int accept(Method method) {
        if (!Modifier.isStatic(method.getModifiers()) && method.getReturnType() != void.class
            && method.getParameterTypes().length == 0) {
            String mn = method.getName();
            if (mn.startsWith("get") && mn.length() > 3 || mn.startsWith("is") && mn.length() > 2) {
                return index;
            }
        }
        return -1;
    }

}
