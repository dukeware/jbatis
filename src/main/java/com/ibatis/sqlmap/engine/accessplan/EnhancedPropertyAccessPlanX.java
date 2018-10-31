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
package com.ibatis.sqlmap.engine.accessplan;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.ibatis.cglib.*;

import com.ibatis.common.beans.ProbeException;

/**
 * Enhanced PropertyAccessPlan (for working with beans using CG Lib)
 */
public class EnhancedPropertyAccessPlanX extends BaseAccessPlan {

    private BulkBeanX bulkBean;

    private static final Map<Object, BulkBeanX> beans = new LinkedHashMap<Object, BulkBeanX>() {
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 8323514817332962624L;

        @Override
        protected boolean removeEldestEntry(Entry<Object, BulkBeanX> eldest) {
            return size() > ReflectUtil.getCatchSize();
        }
    };

    public EnhancedPropertyAccessPlanX(Class<?> clazz, String[] propertyNames) throws Exception {
        super(clazz, propertyNames);
        synchronized (beans) {
            bulkBean = beans.get(this);
            if (bulkBean == null) {
                bulkBean = createBulkBean(clazz, Integer.toHexString(hashCode()), propertyNames);
                beans.put(this, bulkBean);
            }
        }
    }

    @Override
    public int hashCode() {
        int hashCode = clazz.hashCode();
        if (propertyNames != null) {
            for (String s : propertyNames) {
                hashCode = 31 * hashCode + (s == null ? 0 : s.hashCode());
            }
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof EnhancedPropertyAccessPlanX) {
            EnhancedPropertyAccessPlanX epx = (EnhancedPropertyAccessPlanX) o;
            if (clazz != epx.clazz) {
                return false;
            }
            if (propertyNames == epx.propertyNames) {
                return true;
            }
            if (propertyNames == null || epx.propertyNames == null) {
                return false;
            }
            if (propertyNames.length != epx.propertyNames.length) {
                return false;
            }
            for (int i = propertyNames.length - 1; i > 0; i--) {
                Object o1 = propertyNames[i];
                Object o2 = epx.propertyNames[i];
                if (!(o1 == null ? o2 == null : o1.equals(o2)))
                    return false;
            }
            return true;
        }

        return false;
    }

    BulkBeanX createBulkBean(Class<?> clazz, Object key, String[] propertyNames) throws Exception {
        Method[] getterNames = new Method[propertyNames.length];
        Method[] setterNames = new Method[propertyNames.length];
        Field[] names = new Field[propertyNames.length];
        Invoker[] getters = getGetters(propertyNames);
        Invoker[] setters = getSetters(propertyNames);
        for (int i = 0; i < propertyNames.length; i++) {
            if (getters[i] == null) {
                throw new ProbeException("No READABLE property '" + propertyNames[i] + "' in class '" + clazz.getName()
                    + "'");
            }
            if (setters[i] == null) {
                throw new ProbeException("No WRITEABLE property '" + propertyNames[i] + "' found in class '"
                    + clazz.getName() + "'");
            }
            if (getters[i] instanceof MethodInvoker) {
                getterNames[i] = ((MethodInvoker) getters[i]).getMethod();
            } else if (getters[i] instanceof GetFieldInvoker) {
                names[i] = ((GetFieldInvoker) getters[i]).getField();
            }

            if (setters[i] instanceof MethodInvoker) {
                setterNames[i] = ((MethodInvoker) setters[i]).getMethod();
            } else if (setters[i] instanceof SetFieldInvoker) {
                names[i] = ((SetFieldInvoker) setters[i]).getField();
            }
        }
        return BulkBeanX.create(clazz, key, propertyNames, getterNames, setterNames, names);
    }

    @Override
    public void setProperties(Object object, Object[] values) {
        bulkBean.setPropertyValues(object, values);
    }

    @Override
    public Object[] getProperties(Object object) {
        return bulkBean.getPropertyValues(object);
    }

}
