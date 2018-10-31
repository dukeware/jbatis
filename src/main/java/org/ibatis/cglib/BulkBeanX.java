/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ibatis.cglib;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.ibatis.asm.ClassWriter;
import org.ibatis.asm.Label;
import org.ibatis.asm.MethodEmitter;
import org.ibatis.asm.Opcodes;
import org.ibatis.asm.Type;

/**
 * @author Song Sun
 */
public abstract class BulkBeanX implements Opcodes {

    final int size;

    protected BulkBeanX(int size) {
        this.size = size;
    }

    public abstract void getPropertyValues(Object bean, Object[] values);

    public abstract void setPropertyValues(Object bean, Object[] values);

    public Object[] getPropertyValues(Object bean) {
        Object[] values = new Object[size];
        getPropertyValues(bean, values);
        return values;
    }

    public static BulkBeanX create(final Class<?> target, Object key, String[] propertyNames, Method[] getters, Method[] setters,
        Field[] fields) throws Exception {
        // CLoader cl = new CLoader(target);
        Type beanType = Type.getType(target);
        // String cn = "package." + target.getName() + "BulkBean" + seqNo++;
        String cn = NamingPolicy.SIMPLE.getClassName(target.getName(), "BulkBean", key, new Predicate() {
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

        Type BULK_BEAN = Type.getType(BulkBeanX.class);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        cw.visit(V1_5, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, goal.getInternalName(), null,
            BULK_BEAN.getInternalName(), null);
        cw.visitSource("<generated>", null);

        {
            MethodEmitter e = cw.visitMethodX(false, ACC_PUBLIC, "<init>", "()V", null, null);
            e.start_method();
            Label start = e.mark();
            e.load_local(goal, 0);
            e.push(propertyNames.length);
            e.invoke_constructor(BULK_BEAN, "(I)V");
            e.return_void();
            Label end = e.mark();
            e.mark_local("this", goal, start, end, 0);
            e.end_method();
        }
        {

            MethodEmitter e = cw.visitMethodX(false, ACC_PUBLIC, "getPropertyValues",
                "(Ljava/lang/Object;[Ljava/lang/Object;)V", null, null);
            e.start_method();
            Label start = e.mark();
            e.load_local(T_Object, 1);
            e.checkcast(beanType);
            e.store_local(beanType, 3);
            Label bean = e.mark();
            Type oarray = T_Object.getArrayType(1);
            for (int i = 0; i < propertyNames.length; i++) {
                if (fields[i] != null) {
                    Field f = fields[i];
                    Type owner = Type.getType(f.getDeclaringClass());
                    Type type = Type.getType(f.getType());
                    e.load_local(oarray, 2);
                    e.push(i);
                    if ((f.getModifiers() & ACC_STATIC) == 0) {
                        e.load_local(beanType, 3);
                        e.getfield(owner, f.getName(), type);
                    } else {
                        e.getstatic(owner, f.getName(), type);
                    }
                    e.box(type);
                    e.aastore();
                } else {
                    Method m = getters[i];
                    e.load_local(oarray, 2);
                    e.push(i);
                    if ((m.getModifiers() & ACC_STATIC) == 0) {
                        e.load_local(beanType, 3);
                    }
                    e.invoke_method(m);
                    e.box(Type.getType(m.getReturnType()));
                    e.aastore();
                }
            }
            e.return_value(T_void);
            Label end = e.mark();
            e.mark_local("this", goal, start, end, 0);
            e.mark_local("obj", T_Object, start, end, 1);
            e.mark_local("values", oarray, start, end, 2);
            e.mark_local("bean", beanType, bean, end, 3);
            e.end_method();
        }
        {
            MethodEmitter e = cw.visitMethodX(false, ACC_PUBLIC, "setPropertyValues",
                "(Ljava/lang/Object;[Ljava/lang/Object;)V", null, null);
            e.start_method();
            Label start = e.mark();
            e.load_local(T_Object, 1);
            e.checkcast(beanType);
            e.store_local(beanType, 3);
            Type oarray = T_Object.getArrayType(1);

            for (int i = 0; i < propertyNames.length; i++) {
                if (fields[i] != null) {
                    Field f = fields[i];
                    Type owner = Type.getType(f.getDeclaringClass());
                    Type type = Type.getType(f.getType());

                    if ((f.getModifiers() & ACC_STATIC) == 0) {
                        e.load_local(beanType, 3);
                    }

                    e.load_local(oarray, 2);
                    e.aaload(i);
                    if (type.isPrimitive()) {
                        e.checkcast(type.toReferenceType());
                        // ## unbox the null value
                        e.unbox_or_zero(type);
                    } else {
                        e.checkcast(type);
                    }

                    if ((f.getModifiers() & ACC_STATIC) == 0) {
                        e.putfield(owner, f.getName(), type);
                    } else {
                        e.putstatic(owner, f.getName(), type);
                    }
                } else {
                    Method m = setters[i];
                    Type atype = Type.getType(m.getParameterTypes()[0]);
                    if ((m.getModifiers() & ACC_STATIC) == 0) {
                        e.load_local(beanType, 3);
                    }

                    e.load_local(oarray, 2);
                    e.aaload(i);
                    if (atype.isPrimitive()) {
                        e.checkcast(atype.toReferenceType());
                        // ## unbox the null value
                        e.unbox_or_zero(atype);
                    } else {
                        e.checkcast(atype);
                    }
                    e.invoke_method(m);
                }
            }
            e.return_value(T_void);
            Label end = e.mark();
            e.mark_local("this", goal, start, end, 0);
            e.mark_local("obj", T_Object, start, end, 1);
            e.mark_local("values", oarray, start, end, 2);
            e.mark_local("bean", beanType, start, end, 2);
            e.end_method();
        }
        cw.visitEnd();

        byte[] bs = cw.toByteArray();
        /*-
        OutputStream os = null;
        try {
            File file = new File("f:/" + cn + ".class");
            file.getParentFile().mkdirs();
            os = new BufferedOutputStream(new FileOutputStream(file));
            os.write(bs);
        } catch (Exception e) {
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }
        //*/
        // Class<?> clazz = cl.newClass(bs, 0, bs.length);
        Class<?> clazz = ReflectUtil.defineClass(cn, bs, target.getClassLoader());
        /*-
        synchronized (beans) {
            BulkBeanX bx = (BulkBeanX) beans.get(clazz);
            if (bx != null) {
                return bx;
            }
        }
         */
        return (BulkBeanX) clazz.getConstructor().newInstance();
    }
}
