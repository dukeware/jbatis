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

package org.ibatis.client;

/**
 * PropertyProvider
 * <p>
 * Date: 2015-04-29,11:03:10 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
public interface PropertyProvider {

    /**
     * Get the property from iBatis.
     */
    String getGlobalProperty(String name);
}
