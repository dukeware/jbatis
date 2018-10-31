package org.ibatis.cglib.proxy;

import java.lang.reflect.Method;

public interface CallbackFilter {
    int accept(Method method);
}
