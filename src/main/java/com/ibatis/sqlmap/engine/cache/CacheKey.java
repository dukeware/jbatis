/*
 *  Copyright 2004 Clinton Begin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.ibatis.sqlmap.engine.cache;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Hash value generator for cache keys
 */
public class CacheKey implements Cloneable, Serializable {

    private static final long serialVersionUID = 4723868979183352847L;
    private static final int DEFAULT_MULTIPLYER = 37;
    private static final int DEFAULT_HASHCODE = 17;

    private int hashcode;
    private long checksum;
    private int count;
    private List<Object> paramList = new ArrayList<Object>();

    /**
     * Default constructor
     */
    public CacheKey() {
        hashcode = DEFAULT_HASHCODE;
        count = 0;
    }

    /**
     * Updates this object with new information based on an object
     *
     * @param object
     *            - the object
     * @return the cachekey
     */
    public CacheKey update(Object object) {
        if (object instanceof Class<?>) {
            object = object.toString();
        }
        int baseHashCode = object == null ? DEFAULT_HASHCODE : object.hashCode();

        count++;
        checksum += baseHashCode;
        baseHashCode *= count;

        hashcode = DEFAULT_MULTIPLYER * hashcode + baseHashCode;

        paramList.add(object);

        return this;
    }

    public CacheKey update(Object[] objects) {
        if (objects == null) {
            update((Object) null);
        } else {
            update(Arrays.asList(objects).toString());
        }
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof CacheKey))
            return false;

        final CacheKey cacheKey = (CacheKey) object;

        if (hashcode != cacheKey.hashcode)
            return false;
        if (checksum != cacheKey.checksum)
            return false;
        if (count != cacheKey.count)
            return false;

        for (int i = 0; i < paramList.size(); i++) {
            Object thisParam = paramList.get(i);
            Object thatParam = cacheKey.paramList.get(i);
            if (thisParam == null) {
                if (thatParam != null)
                    return false;
            } else {
                if (!thisParam.equals(thatParam))
                    return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    public String toStr() {
        return Integer.toHexString(hashcode);
    }

    @Override
    public String toString() {
        StringBuilder returnValue = new StringBuilder().append(hashcode).append('|').append(checksum);
        for (int i = 0; i < paramList.size(); i++) {
            String str = String.valueOf(paramList.get(i));
            if (str.length() > 72) {
                str = str.substring(0, 30) + "..." + str.substring(str.length() - 30);
            }
            returnValue.append('|').append(str);
        }

        return returnValue.toString();
    }

    @Override
    public CacheKey clone() throws CloneNotSupportedException {
        CacheKey ck = (CacheKey) super.clone();
        ck.paramList = new ArrayList<Object>(paramList);
        return ck;
    }
}
