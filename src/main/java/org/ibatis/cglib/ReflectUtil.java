/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package org.ibatis.cglib;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ibatis.cglib.proxy.Callback;
import org.ibatis.cglib.proxy.CallbackFilter;
import org.ibatis.cglib.proxy.NoOp;

import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;

/**
 * ReflectUtils
 * 
 * @author Song Sun
 * @version 1.0
 */
public class ReflectUtil {
    private static final ILog log = ILogFactory.getLog(Invoker.class);
    private static int catchSize = 2048;
    private static Method DEFINE_CLASS;
    private static final ProtectionDomain PROTECTION_DOMAIN;

    public static int getCatchSize() {
        return catchSize;
    }

    public static void setCatchSize(String cs) {
        try {
            int size = Integer.parseInt(cs);
            if (size > 100) {
                catchSize = size;
            }
        } catch (Exception e) {
            log.warn("bad cache size: " + cs);
        }
    }

    static {
        PROTECTION_DOMAIN = AccessController.doPrivileged(new PrivilegedAction<ProtectionDomain>() {
            public ProtectionDomain run() {
                return ReflectUtil.class.getProtectionDomain();
            }
        });

        AccessController.doPrivileged(new PrivilegedAction<Method>() {
            public Method run() {
                try {
                    Class<?> loader = Class.forName("java.lang.ClassLoader");
                    DEFINE_CLASS = loader.getDeclaredMethod("defineClass",
                        new Class[] { String.class, byte[].class, Integer.TYPE, Integer.TYPE, ProtectionDomain.class });
                    DEFINE_CLASS.setAccessible(true);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
    }

    private ReflectUtil() {
    }

    public static Map<String, Field> loadFields(Class<?> clazz) {
        Map<String, Field> map = new LinkedHashMap<String, Field>();
        while (clazz != null && !getPackageName(clazz).startsWith("java.")) {
            Field[] fs = clazz.getDeclaredFields();
            for (Field f : fs) {
                int m = f.getModifiers();
                if (Modifier.isStatic(m) || Modifier.isFinal(m)) {
                    continue;
                }
                if (!map.containsKey(f.getName())) {
                    map.put(f.getName().toLowerCase(), f);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return map;
    }

    public static LinkedHashMap<Method, Integer> filterMethods(List<Method> ms, CallbackFilter filter,
        Callback[] callbacks) {
        LinkedHashMap<Method, Integer> map = new LinkedHashMap<Method, Integer>();
        for (Method m : ms) {
            int mod = m.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || m.getDeclaringClass() == Object.class) {
                continue;
            }
            int idx = filter == null ? 0 : filter.accept(m);
            if (idx >= 0 && idx < callbacks.length) {
                Callback cb = callbacks[idx];
                if (cb != NoOp.INSTANCE) {
                    map.put(m, idx);
                    continue;
                }
            }
            map.put(m, -1);
        }
        return map;
    }

    public static Map<Method, String> mapPropertyName(List<Method> ms) {
        Map<Method, String> map = new LinkedHashMap<Method, String>();
        for (Method m : ms) {
            int mod = m.getModifiers();
            if (Modifier.isStatic(mod)) {
                continue;
            }
            String n = m.getName();
            if (m.getParameterTypes().length == 0) {
                if (n.startsWith("get") && n.length() > 3 && m.getReturnType() != void.class) {
                    map.put(m, ClassInfo.dropCase(n.substring(3)));
                } else if (n.startsWith("is") && n.length() > 2
                    && (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)) {
                    map.put(m, ClassInfo.dropCase(n.substring(2)));
                }
            } else if (m.getParameterTypes().length == 1 && n.startsWith("set") && m.getReturnType() == void.class) {
                map.put(m, ClassInfo.dropCase(n.substring(3)));
            }
        }
        return map;
    }

    public static String getPackageName(Class<?> type) {
        String className = type.getName();
        int idx = className.lastIndexOf('.');
        return (idx < 0) ? "" : className.substring(0, idx);
    }

    public static List<Method> loadMethods(Class<?> type) {
        String pkg = getPackageName(type);
        List<Method> list = new ArrayList<Method>();
        Set<String> mkeys = new HashSet<String>();
        StringBuilder buf = new StringBuilder();
        while (type != null && !getPackageName(type).startsWith("java.")) {
            for (Method m : type.getDeclaredMethods()) {
                int mod = m.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isAbstract(mod)) {
                    continue;
                }
                String mpkg = getPackageName(m.getDeclaringClass());
                if (Modifier.isPrivate(mod)
                    || (!pkg.equals(mpkg) && !Modifier.isPublic((mod)) && !Modifier.isProtected((mod)))) {
                    continue;
                }
                buf.setLength(0);
                buf.append(m.getName());
                buf.append("|");
                for (Class<?> pt : m.getParameterTypes()) {
                    buf.append(pt.getName());
                    buf.append("|");
                }
                buf.append(m.getReturnType().getName());
                if (mkeys.add(buf.toString())) {
                    list.add(m);
                }
            }
            type = type.getSuperclass();
        }

        return list;
    }

    public static Class<?> defineClass(String className, byte[] bs, ClassLoader loader) throws Exception {
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        if (loader == null) {
            loader = ReflectUtil.class.getClassLoader();
        }
        Class<?> c = (Class<?>) DEFINE_CLASS.invoke(loader, className, bs, 0, bs.length, PROTECTION_DOMAIN);
        Class.forName(className, true, loader);
        return c;
    }

    public static boolean canAccess(Class<?> superclass, AccessibleObject ao) {
        String pkg = getPackageName(superclass);
        if (ao instanceof Field) {
            Field f = (Field) ao;
            int mod = f.getModifiers();
            String mpkg = getPackageName(f.getDeclaringClass());
            return !Modifier.isPrivate(mod)
                && (pkg.equals(mpkg) || Modifier.isPublic((mod)) || Modifier.isProtected((mod)));
        } else if (ao instanceof Method) {
            Method m = (Method) ao;
            int mod = m.getModifiers();
            String mpkg = getPackageName(m.getDeclaringClass());
            return !Modifier.isPrivate(mod)
                && (pkg.equals(mpkg) || Modifier.isPublic((mod)) || Modifier.isProtected((mod)));
        }
        return false;
    }
}
