package org.ibatis.persist.impl.util;

import org.ibatis.cglib.ClassInfo;
import org.ibatis.cglib.proxy.Enhancer;
import org.ibatis.cglib.proxy.MethodInterceptor;
import org.ibatis.cglib.proxy.MethodProxy;

public class GetterInterceptor implements MethodInterceptor {

    static final GetterInterceptor INSTANCE = new GetterInterceptor();
    static final ThreadLocal<String> attributeHolder = new ThreadLocal<String>();

    @SuppressWarnings("unchecked")
    public static <X> X create(Class<X> clazz) {
        return (X) Enhancer.create(clazz, new GetterFilter(0), INSTANCE);
    }

    public static String take() {
        String attr = attributeHolder.get();
        if (attr == null) {
            throw new IllegalStateException("no entity attr gotten.");
        }
        attributeHolder.set(null);
        return attr;
    }

    @Override
    public Object intercept(MethodProxy proxy, Object obj, Object... args) throws Throwable {
        String name = proxy.getName();
        if (name.startsWith("get")) {
            name = name.substring(3);
        } else if (name.startsWith("is")) {
            name = name.substring(2);
        }
        attributeHolder.set(ClassInfo.dropCase(name));
        return null;
    }

}
