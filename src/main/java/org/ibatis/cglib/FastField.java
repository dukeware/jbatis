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

import java.lang.reflect.Field;
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
public class FastField extends FastMember implements Opcodes {

    private final Field field;

    @Deprecated
    public FastField(Field field) {
        super(field);
        this.field = field;
    }

    public Object get(Object target) throws IllegalArgumentException, IllegalAccessException {
        return field.get(target);
    }

    public void set(Object target, Object value) throws IllegalArgumentException, IllegalAccessException {
        field.set(target, value);
    }

    @SuppressWarnings("serial")
    private static final Map<Object, FastField> factorys = new LinkedHashMap<Object, FastField>() {
        protected boolean removeEldestEntry(Entry<Object, FastField> eldest) {
            return size() > ReflectUtil.getCatchSize();
        }
    };

    public static FastField create(Field field) {
        if ((field.getModifiers() & ACC_PRIVATE) != 0) {
            throw new IllegalArgumentException("Private filed: " + field);
        }

        Object key = field;
        FastField ff = null;
        try {
            synchronized (factorys) {
                ff = factorys.get(key);
                if (ff == null) {
                    ff = doCreate(field);
                    factorys.put(key, ff);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return ff;
    }

    private static FastField doCreate(Field field) throws Exception {
        final Class<?> target = field.getDeclaringClass();
        Type targetType = Type.getType(target);
        // String cn = "package." + target.getName() + "." + field.getName() + "FastField" + seqNo++;
        String cn = NamingPolicy.SIMPLE.getClassName(target.getName(), field.getName(), "FF", new Predicate() {
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

        Type superType = Type.getType(FastField.class);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        cw.visit(V1_5, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, goal.getInternalName(), null,
            superType.getInternalName(), null);
        cw.visitSource("<generated>", null);

        {
            /*-
             * FastField(Field field)
             */
            String desc = Type.getMethodDescriptor(T_void, T_Field);
            MethodEmitter e = cw.visitMethodX(false, ACC_PUBLIC, CONSTRUCTOR_NAME, desc, null, null);
            e.start_method();
            Label start = e.mark();
            e.load_local(goal, 0);
            e.load_local(T_Field, 1);
            e.invoke_constructor(superType, desc);
            e.return_void();
            Label end = e.mark();
            e.mark_local("this", goal, start, end, 0);
            e.end_method();
        }
        {
            /*-
             * Object get(Object target)
             */
            MethodEmitter e = cw.visitMethodX(false, ACC_PUBLIC, "get", Type.getMethodDescriptor(T_Object, T_Object),
                null, null);
            e.start_method();
            Label start = e.mark();
            Type rt = Type.getType(field.getType());
            if ((field.getModifiers() & ACC_STATIC) == 0) {
                e.load_local(goal, 1);
                e.checkcast(targetType);
                e.getfield(targetType, field.getName(), rt);
            } else {
                e.getstatic(targetType, field.getName(), rt);
            }

            if (rt.isPrimitive()) {
                e.box(rt);
            }
            e.return_value(T_Object);
            Label end = e.mark();
            e.mark_local("this", goal, start, end, 0);
            e.end_method();
        }
        {
            /*-
             * void set(Object target, Object value)
             */
            MethodEmitter e = cw.visitMethodX(false, ACC_PUBLIC, "set",
                Type.getMethodDescriptor(T_void, T_Object, T_Object), null, null);
            e.start_method();
            Label start = e.mark();
            if ((field.getModifiers() & ACC_FINAL) == 0) {
                Type rt = Type.getType(field.getType());
                if ((field.getModifiers() & ACC_STATIC) == 0) {
                    e.load_local(goal, 1);
                    e.checkcast(targetType);
                }

                e.load_local(T_Object, 2);
                if (rt.isPrimitive()) {
                    e.checkcast(rt.toReferenceType());
                    e.unbox_or_zero(rt);
                } else {
                    e.checkcast(rt);
                }

                if ((field.getModifiers() & ACC_STATIC) == 0) {
                    e.putfield(targetType, field.getName(), rt);
                } else {
                    e.putstatic(targetType, field.getName(), rt);
                }
            }
            e.return_void();
            Label end = e.mark();
            e.mark_local("this", goal, start, end, 0);
            e.end_method();
        }
        cw.visitEnd();

        byte[] bs = cw.toByteArray();
        // Class<?> clazz = cl.newClass(bs, 0, bs.length);
        Class<?> clazz = ReflectUtil.defineClass(cn, bs, target.getClassLoader());
        try {
            return (FastField) clazz.getConstructor(Field.class).newInstance(field);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
