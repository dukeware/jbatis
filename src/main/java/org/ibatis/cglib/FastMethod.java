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

package org.ibatis.cglib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.ibatis.asm.ClassWriter;
import org.ibatis.asm.Label;
import org.ibatis.asm.MethodEmitter;
import org.ibatis.asm.Opcodes;
import org.ibatis.asm.Type;

/**
 * FastMethod
 * <p>
 * Date: 2015-06-17,16:41:24 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
public class FastMethod extends FastMember implements Opcodes {

    private final Method method;

    @Deprecated
    public FastMethod(Method method) {
        super(method);
        this.method = method;
    }

    public Object invoke(Object target, Object... args)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return method.invoke(target, args);
    }

    @SuppressWarnings("serial")
    private static final Map<Object, FastMethod> factorys = new LinkedHashMap<Object, FastMethod>() {
        protected boolean removeEldestEntry(Entry<Object, FastMethod> eldest) {
            return size() > ReflectUtil.getCatchSize();
        }
    };

    public static FastMethod create(Method method) {
        if ((method.getModifiers() & ACC_PRIVATE) != 0) {
            throw new IllegalArgumentException("Private method: " + method);
        }
        if ((method.getModifiers() & ACC_ABSTRACT) != 0) {
            throw new IllegalArgumentException("Abstract method: " + method);
        }

        Object key = method;
        FastMethod fm = null;
        try {
            synchronized (factorys) {
                fm = factorys.get(key);
                if (fm == null) {
                    fm = doCreate(method);
                    factorys.put(key, fm);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return fm;
    }

    private static FastMethod doCreate(Method method) throws Exception {
        final Class<?> target = method.getDeclaringClass();
        Type targetType = Type.getType(target);
        // String cn = "package." + target.getName() + "." + method.getName() + "FastMethod" + seqNo++;
        String cn = NamingPolicy.SIMPLE.getClassName(target.getName(), method.getName(),
            "FM" + method.getParameterTypes().length, new Predicate() {
                public boolean evaluate(Object arg) {
                    try {
                        target.getClassLoader().loadClass((String) arg);
                    } catch (Exception e) {
                        return false;
                    }
                    return true;
                }
            });
        Type goal = Type.getObjectType(cn.replace('.', '/'));

        Type superType = Type.getType(FastMethod.class);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        cw.visit(V1_5, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, goal.getInternalName(), null,
            superType.getInternalName(), null);
        cw.visitSource("<generated>", null);

        {
            /*-
             * FastMethod(Method method)
             */
            String desc = Type.getMethodDescriptor(T_void, T_Method);
            MethodEmitter e = cw.visitMethodX(false, ACC_PUBLIC, CONSTRUCTOR_NAME, desc, null, null);
            e.start_method();
            Label start = e.mark();
            e.load_local(goal, 0);
            e.load_local(T_Method, 1);
            e.invoke_constructor(superType, desc);
            e.return_void();
            Label end = e.mark();
            e.mark_local("this", goal, start, end, 0);
            e.end_method();
        }
        Type objArrayType = T_Object.getArrayType(1);
        {
            /*-
             * Object invoke(Object target, Object... args)
             */
            MethodEmitter e = cw.visitMethodX(false, ACC_PUBLIC | ACC_VARARGS, "invoke",
                Type.getMethodDescriptor(T_Object, T_Object, objArrayType), null, null);
            e.start_method();
            Label start = e.mark();
            if ((method.getModifiers() & ACC_STATIC) == 0) {
                e.load_local(goal, 1);
                e.checkcast(targetType);
            }

            Class<?>[] pcs = method.getParameterTypes();
            for (int j = 0; j < pcs.length; j++) {
                e.load_local(objArrayType, 2);
                e.aaload(j);
                Type pt = Type.getType(pcs[j]);
                if (pt.isPrimitive()) {
                    e.checkcast(pt.toReferenceType());
                    e.unbox_or_zero(pt);
                } else {
                    e.checkcast(pt);
                }
            }

            e.invoke_method(method);
            Type rt = Type.getType(method.getReturnType());
            if (rt.isVoid()) {
                e.aconst_null();
            } else if (rt.isPrimitive()) {
                e.box(rt);
            }
            e.return_value(T_Object);
            Label end = e.mark();
            e.mark_local("this", goal, start, end, 0);
            e.end_method();
        }
        cw.visitEnd();

        byte[] bs = cw.toByteArray();
        // Class<?> clazz = cl.newClass(bs, 0, bs.length);
        Class<?> clazz = ReflectUtil.defineClass(cn, bs, target.getClassLoader());
        try {
            return (FastMethod) clazz.getConstructor(Method.class).newInstance(method);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
