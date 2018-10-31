/*-
 * Copyright 2012 Owl Group
 * All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 */

package org.ibatis.persist.impl;

/**
 * ParameterInfo
 * <p>
 * Date: 2015-06-26,10:22:21 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
public interface ParameterInfo<T> {
    
    Object None = new String("nul");

    T getParameterValue();

    void setParameterValue(T value);

    Class<T> getParameterType();

    boolean isBound();
}
