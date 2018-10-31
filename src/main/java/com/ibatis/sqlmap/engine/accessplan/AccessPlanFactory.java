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

import java.util.Map;

import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;

/**
 * Factory to get an accesss plan appropriate for an object
 */
public class AccessPlanFactory {
    static final ILog log = ILogFactory.getLog(AccessPlanFactory.class.getPackage().getName());

    private static boolean bytecodeEnhancementEnabled = true;

    private AccessPlanFactory() {
    }

    /**
     * Creates an access plan for working with a bean
     *
     * @param clazz
     * @param propertyNames
     * @return An access plan
     */
    public static AccessPlan getAccessPlan(Class<?> clazz, String[] propertyNames) {
        AccessPlan plan;

        boolean complex = false;

        if (clazz == null || propertyNames == null) {
            complex = true;
        } else {
            for (int i = 0; i < propertyNames.length; i++) {
                String pn = propertyNames[i];
                if (pn.indexOf('[') >= 0 || pn.indexOf('.') >= 0) {
                    complex = true;
                    break;
                }
            }
        }

        if (complex) {
            plan = new ComplexAccessPlan(clazz, propertyNames);
        } else if (Map.class.isAssignableFrom(clazz)) {
            plan = new MapAccessPlan(clazz, propertyNames);
        } else {
            // Possibly causes bug 945746 --but the bug is unconfirmed (can't be reproduced)
            if (bytecodeEnhancementEnabled) {
                try {
                    // ## bulk with fields
                    plan = new EnhancedPropertyAccessPlanX(clazz, propertyNames);
                } catch (Throwable t) {
                    log.warn("EnhancedAccessPlan failed -> " + t.getMessage());
                    try {
                        plan = new PropertyAccessPlan(clazz, propertyNames);
                    } catch (Throwable t2) {
                        plan = new ComplexAccessPlan(clazz, propertyNames);
                    }
                }
            } else {
                try {
                    plan = new PropertyAccessPlan(clazz, propertyNames);
                } catch (Throwable t) {
                    plan = new ComplexAccessPlan(clazz, propertyNames);
                }
            }
        }
        return plan;
    }

    /**
     * Tells whether or not bytecode enhancement (CGLIB, etc) is enabled
     *
     * @return true if bytecode enhancement is enabled
     */
    public static boolean isBytecodeEnhancementEnabled() {
        return bytecodeEnhancementEnabled;
    }

    /**
     * Turns on or off bytecode enhancement (CGLIB, etc)
     *
     * @param bytecodeEnhancementEnabled
     *            - the switch
     */
    public static void setBytecodeEnhancementEnabled(boolean bytecodeEnhancementEnabled) {
        // log.info("bytecodeEnhancementEnabled = " + bytecodeEnhancementEnabled);
        AccessPlanFactory.bytecodeEnhancementEnabled = bytecodeEnhancementEnabled;
    }

}
