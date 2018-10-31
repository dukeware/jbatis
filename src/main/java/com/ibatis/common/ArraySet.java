/*-
 * Copyright 2009-2019 Owl Group
 * All rights reserved.
 */
package com.ibatis.common;

import java.util.ArrayList;
import java.util.Set;

/**
 * ArraySet for internal use only
 * <p>
 * Date: 2018-05-31
 * 
 * @author Song Sun
 * @version 1.0
 */
public class ArraySet<E> extends ArrayList<E> implements Set<E> {

    private static final long serialVersionUID = 1L;

    public ArraySet() {
    }

    @Override
    public boolean add(E e) {
        if (contains(e)) {
            return false;
        }
        return super.add(e);
    }
}
