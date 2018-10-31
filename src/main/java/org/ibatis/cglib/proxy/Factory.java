package org.ibatis.cglib.proxy;

import java.lang.annotation.Annotation;
import java.util.List;

public interface Factory {

    Object newInstance(Callback... callbacks);

    Callback getCallback(int index);

    void setCallback(int index, Callback callback);

    Callback[] getCallbacks();

    /**
     * Internal use only.
     */
    @Deprecated
    Object $invoke(int mIdx, Object... args);

    /*-
     * entity api
     */

    List<String> $attrs();

    <T extends Annotation> T $annotation(String attr, Class<T> annotationClass);

    Class<?> $type(String attr);

    Object $get(String attr);

    void $set(String attr, Object obj);
}
