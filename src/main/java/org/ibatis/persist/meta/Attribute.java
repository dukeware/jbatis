/*-
 * Copyright 2010-2013 Owl Group
 * All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 */

package org.ibatis.persist.meta;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import org.ibatis.cglib.Invoker;

/**
 * PropertyAccessor
 * 
 * @param <E>
 *            The type containing the represented attribute
 * @param <P>
 *            The type of the represented attribute
 * 
 * @author Song Sun
 * @since iBatis Persistence 1.0
 */
public class Attribute<E, P> {
    final String name;
    final String column;
    final Class<?> type;
    final Invoker getter;
    final Invoker setter;

    public Attribute(String name, String column, Class<?> type, Invoker getter, Invoker setter) {
        this.name = name;
        this.column = column;
        this.type = type;
        this.getter = getter;
        this.setter = setter;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        T t = getter.getAnnotation(annotationClass);
        if (t == null) {
            t = setter.getAnnotation(annotationClass);
        }
        return t;
    }

    public String getName() {
        return name;
    }

    public String getColumn() {
        return column;
    }

    public Class<?> getType() {
        return type;
    }

    public Object getValue(E entity) {
        try {
            return getter.invoke(entity);
        } catch (RuntimeException e) {
            throw e;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setValue(E entity, P val) {
        try {
            setter.invoke(entity, val);
        } catch (RuntimeException e) {
            throw e;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
