package org.ibatis.cglib;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static com.ibatis.common.Objects.uncheckedCast;

public abstract class Invoker {
    final List<Annotation> annos = new ArrayList<Annotation>();
    private String name;

    public Invoker(String name) {
        this.name = name;
    }

    public abstract String getAccessName();

    public abstract Class<?> getType();
    
    public final String getName() {
        return name;
    }

    public final <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        for (Annotation anno : annos) {
            if (annotationClass.isAssignableFrom(anno.getClass())) {
                return uncheckedCast(anno);
            }
        }
        return null;
    }

    public abstract Object invoke(Object target, Object... args) throws IllegalAccessException,
        InvocationTargetException;

    final void fixAccess(AccessibleObject ao) {
        Annotation[] annotations = ao.getAnnotations();
        if (annotations != null) {
            for (Annotation anno : annotations) {
                annos.add(anno);
            }
        }
        if (ao instanceof Field) {
            name = ((Field) ao).getName();
        }
        try {
            ao.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalArgumentException(ao.toString());
        }
    }
}
