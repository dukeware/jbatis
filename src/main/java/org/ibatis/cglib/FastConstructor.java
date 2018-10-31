package org.ibatis.cglib;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.ibatis.asm.ClassWriter;
import org.ibatis.asm.Label;
import org.ibatis.asm.MethodEmitter;
import org.ibatis.asm.Opcodes;
import org.ibatis.asm.Type;

public class FastConstructor extends FastMember implements Opcodes {
    private Constructor<?> constructor;

    @Deprecated
    public FastConstructor(Constructor<?> constructor) {
        super(constructor);
        this.constructor = constructor;
    }

    public Object newInstance(Object... args) throws InvocationTargetException {
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
    }

    @SuppressWarnings("serial")
    private static final Map<Object, FastConstructor> factorys = new LinkedHashMap<Object, FastConstructor>() {
        protected boolean removeEldestEntry(Entry<Object, FastConstructor> eldest) {
            return size() > ReflectUtil.getCatchSize();
        }
    };

    public static FastConstructor create(Constructor<?> constructor) {
        if ((constructor.getModifiers() & ACC_PRIVATE) != 0) {
            throw new IllegalArgumentException("Private constructor: " + constructor);
        }

        Object key = constructor;
        FastConstructor fc = null;
        try {
            synchronized (factorys) {
                fc = factorys.get(key);
                if (fc == null) {
                    fc = doCreate(constructor);
                    factorys.put(key, fc);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return fc;
    }

    private static Type constructorType = Type.getType(Constructor.class);

    private static FastConstructor doCreate(Constructor<?> constructor) throws Exception {
        final Class<?> target = constructor.getDeclaringClass();
        Type targetType = Type.getType(target);
        // String cn = "package." + target.getName() + ".newFastNew" + seqNo++;

        String cn = NamingPolicy.SIMPLE.getClassName(target.getName(), "new", "FC" + constructor.getParameterCount(), new Predicate() {
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

        Type superType = Type.getType(FastConstructor.class);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        cw.visit(V1_5, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, goal.getInternalName(), null,
            superType.getInternalName(), null);
        cw.visitSource("<generated>", null);

        {
            /*-
             * FastConstructor(Constructor<?> constructor)
             */
            String desc = Type.getMethodDescriptor(T_void, constructorType);
            MethodEmitter e = cw.visitMethodX(false, ACC_PUBLIC, CONSTRUCTOR_NAME, desc, null, null);
            e.start_method();
            Label start = e.mark();
            e.load_local(goal, 0);
            e.load_local(constructorType, 1);
            e.invoke_constructor(superType, desc);
            e.return_void();
            Label end = e.mark();
            e.mark_local("this", goal, start, end, 0);
            e.end_method();
        }
        Type objArrayType = T_Object.getArrayType(1);
        {
            /*-
             * Object newInstance(Object... args)
             */
            MethodEmitter e = cw.visitMethodX(false, ACC_PUBLIC | ACC_VARARGS, "newInstance",
                Type.getMethodDescriptor(T_Object, objArrayType), null, null);
            e.start_method();
            Label start = e.mark();
            
            e.new_instance(targetType);
            e.dup();
            Class<?>[] pcs = constructor.getParameterTypes();
            for (int j = 0; j < pcs.length; j++) {
                e.load_local(objArrayType, 1);
                e.aaload(j);
                Type pt = Type.getType(pcs[j]);
                if (pt.isPrimitive()) {
                    e.checkcast(pt.toReferenceType());
                    e.unbox_or_zero(pt);
                } else {
                    e.checkcast(pt);
                }
            }
            e.invoke_constructor(targetType, Type.getConstructorDescriptor(constructor));

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
            return (FastConstructor) clazz.getConstructor(Constructor.class).newInstance(constructor);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
