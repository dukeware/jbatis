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

package org.ibatis.spring.support;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * DaoSupport
 * <p>
 * Date: 2014-10-14,14:13:28 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
public abstract class AbstractDaoSupport<E> extends SqlMapClientDaoSupport {
    private Class<E> entityClass;

    public AbstractDaoSupport() {
        entityClass = findEntityClass(getClass().getGenericSuperclass());
    }

    @SuppressWarnings("unchecked")
    Class<E> findEntityClass(Type t) {
        if (t instanceof ParameterizedType) {
            Type pt = ((ParameterizedType) t).getActualTypeArguments()[0];
            if (pt instanceof Class<?>) {
                return (Class<E>) pt;
            }
        } else if (t instanceof Class<?>) {
            return findEntityClass(((Class<?>) t).getGenericSuperclass());
        }
        return null;
    }

    protected Class<E> getEntityClass() {
        return entityClass;
    }

    protected void setEntityClass(Class<E> entityClass) {
        this.entityClass = entityClass;
    }

    public E saveEntity(E entity) {
        return getSqlMapClientTemplate().insertEntity(getEntityClass(), entity);
    }

    /**
     * Update the entity.
     * 
     * @param entity
     *            the entity object
     * @return 1 if successful.
     */
    public int updateEntity(E entity) {
        return getSqlMapClientTemplate().updateEntity(getEntityClass(), entity);
    }

    /**
     * Delete the entity.
     * 
     * @param key
     *            the primary key of the entity.
     * @return 1 if successful.
     */
    public int deleteEntity(Object key) {
        return getSqlMapClientTemplate().deleteEntity(getEntityClass(), key);
    }

    /**
     * Find the entity.
     * 
     * @param key
     *            the primary key of the entity.
     * @return the entity object or <code>null</code> if not exists.
     */
    public E findEntity(Object key) {
        return getSqlMapClientTemplate().findEntity(getEntityClass(), key);
    }
}
