package org.ibatis.cglib.proxy;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ibatis.asm.Case;
import org.ibatis.asm.ClassWriter;
import org.ibatis.asm.FieldVisitor;
import org.ibatis.asm.Label;
import org.ibatis.asm.MethodEmitter;
import org.ibatis.asm.Opcodes;
import org.ibatis.asm.Type;
import org.ibatis.cglib.NamingPolicy;
import org.ibatis.cglib.Predicate;
import org.ibatis.cglib.ReflectUtil;

/**
 * Enhancer
 * <p>
 * Date: 2015-06-15,08:49:51 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
public class Enhancer implements Opcodes {
    private final static boolean debug = false;

    Class<?> superclass;
    CallbackFilter filter;
    Callback[] callbacks = {};
    boolean entity;
    NamingPolicy namingPolicy;

    /**
     * Create enhanced sub-class of the type.
     */
    public static Object create(Class<?> type, CallbackFilter filter, Callback... callback) {
        Enhancer e = new Enhancer();
        e.setSuperclass(type);
        e.setCallback(callback);
        e.setCallbackFilter(filter);
        return e.create(false);
    }

    public Object create() {
        return create(false);
    }

    public Object create(boolean entity) {
        this.entity = entity;
        Object key = toKey();
        try {
            Class<? extends Factory> clazz = null;
            synchronized (factorys) {
                clazz = factorys.get(key);
                if (clazz == null) {
                    clazz = doCreate(key);
                    factorys.put(key, clazz);
                }
            }
            Factory f = clazz.newInstance();
            for (int i = 0; i < callbacks.length; i++) {
                f.setCallback(i, callbacks[i]);
            }
            return f;
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Object toKey() {
        List<Object> key = new ArrayList<Object>();
        key.add(superclass);
        key.add(filter);
        for (Callback cb : callbacks) {
            key.add(cb);
        }
        key.add(entity);
        return key;
    }

    public void setCallbackFilter(CallbackFilter filter) {
        this.filter = filter;
    }

    public void setCallback(Callback... callbacks) {
        for (Callback cb : callbacks) {
            if (cb == null) {
                throw new IllegalArgumentException("Null or empty callbacks");
            }
        }
        this.callbacks = callbacks;
    }

    public void setSuperclass(Class<?> superclass) {
        int mod = superclass.getModifiers();
        if (Modifier.isFinal(mod) || Modifier.isAbstract(mod)) {
            throw new IllegalArgumentException("Cannot inherit " + superclass);
        }

        if (Factory.class.isAssignableFrom(superclass)) {
            throw new IllegalArgumentException(superclass + " is a Factory");
        }

        try {
            if (Modifier.isPrivate(superclass.getDeclaredConstructor().getModifiers())) {
                throw new IllegalArgumentException(superclass + " has no default Constructor");
            }
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(superclass + " has no default Constructor");
        }
        this.superclass = superclass;
    }

    public void setNamingPolicy(NamingPolicy namingPolicy) {
        this.namingPolicy = namingPolicy;
    }

    public static boolean isEnhanced(Class<?> type) {
        return type.isSynthetic() && Factory.class.isAssignableFrom(type);
    }

    public static boolean isGenerated(Class<?> type) {
        if (isEnhanced(type)) {
            return true;
        }
        for (Class<?> iface : type.getInterfaces()) {
            if (iface.getName().endsWith(".cglib.proxy.Factory")) {
                return true;
            }
        }
        return false;
    }

    private static final Map<Object, Class<? extends Factory>> factorys = new LinkedHashMap<Object, Class<? extends Factory>>() {
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 4291658538184450258L;

        protected boolean removeEldestEntry(Entry<Object, Class<? extends Factory>> eldest) {
            return size() > ReflectUtil.getCatchSize();
        }
    };

    private static final Type proxyType = Type.getType(MethodProxy.class);
    private static final Type proxyImplType = Type.getType(MProxy.class);
    private static final Type callbackType = Type.getType(Callback.class);
    private static final Type callbackArrayType = callbackType.getArrayType(1);
    private static final Type factoryType = Type.getType(Factory.class);
    private static final Type strArrayType = T_String.getArrayType(1);
    private static final Type classArrayType = T_Class.getArrayType(1);
    private static final Type objectArrayType = T_Object.getArrayType(1);
    private static final Type aoType = Type.getType(AccessibleObject.class);
    private static final Type aoArrayType = aoType.getArrayType(1);

    @SuppressWarnings("unchecked")
    private Class<? extends Factory> doCreate(Object key) throws Exception {
        List<Method> allMethods = ReflectUtil.loadMethods(superclass);

        final LinkedHashMap<Method, Integer> methods = ReflectUtil.filterMethods(allMethods, filter, callbacks);
        final LinkedHashMap<Method, MProxy> allProxys = new LinkedHashMap<Method, MProxy>();
        final List<Method> proxys = new ArrayList<Method>();
        for (Method m : methods.keySet()) {
            int mIdx = methods.get(m);
            MProxy proxy = new MProxy(mIdx, m, m);
            allProxys.put(m, proxy);
            if (mIdx >= 0) {
                proxys.add(m);
            }
        }
        final List<String> attrs = new ArrayList<String>();
        final Map<String, String> as = new LinkedHashMap<String, String>();
        final Map<String, AccessibleObject[]> aos = new LinkedHashMap<String, AccessibleObject[]>();
        boolean advanced = this.entity;
        if (advanced) {
            Map<String, Field> fields = ReflectUtil.loadFields(superclass);
            Map<Method, String> propNames = ReflectUtil.mapPropertyName(allMethods);
            for (Method m : proxys) {
                String attr = propNames.get(m);
                if (attr != null) {
                    MProxy proxy = allProxys.get(m);
                    List<AccessibleObject> ao = new ArrayList<AccessibleObject>();
                    if (m.getAnnotations().length > 0) {
                        ao.add(m);
                    }
                    Field f = fields.get(attr.toLowerCase());
                    if (f != null && f.getAnnotations().length > 0) {
                        ao.add(f);
                    }

                    for (Method other : propNames.keySet()) {
                        if (m == other) {
                            continue;
                        }
                        String oattr = propNames.get(other);
                        if (oattr != null && attr.equalsIgnoreCase(oattr) && other.getAnnotations().length > 0) {
                            ao.add(other);
                        }
                    }
                    proxy.accessibleObjects = ao.toArray(new AccessibleObject[ao.size()]);
                }
            }
            for (Method m : propNames.keySet()) {
                String n = propNames.get(m);
                String ln = n.toLowerCase();
                if (as.put(ln, n) == null) {
                    attrs.add(ln);
                }
                AccessibleObject[] ao = aos.get(ln);
                if (ao == null) {
                    ao = new AccessibleObject[3];
                    aos.put(ln, ao);
                }
                if (m.getName().startsWith("set")) {
                    ao[2] = m;
                } else {
                    ao[1] = m;
                }
                Field f = fields.get(ln);
                ao[0] = f;
            }
        }

        Type beanType = Type.getType(superclass);
        NamingPolicy np = namingPolicy;
        if (np == null) {
            np = NamingPolicy.INSTANCE;
        }
        String cn = np.getClassName(superclass.getName(), getClass().getName(), key, new Predicate() {
            public boolean evaluate(Object arg) {
                try {
                    superclass.getClassLoader().loadClass((String) arg);
                } catch (Exception e) {
                    return false;
                }
                return true;
            }
        });
        final Type goal = Type.getObjectType(cn.replace('.', '/'));

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        cw.visit(V1_5, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, goal.getInternalName(), null,
            beanType.getInternalName(), new String[] { factoryType.getInternalName() });
        cw.visitSource("<generated>", null);
        /*-
         * public class $cn extends $superclass implements Factory, Invocation
         */
        {
            if (advanced) {
                /*-
                 * private static final Map[String, int] $attrIndex;
                 */
                {
                    FieldVisitor fv = cw.visitField(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, "$attrIndex",
                        T_Map.getDescriptor(), null, null);
                    fv.visitEnd();
                }
                /*-
                 * private static final String[] $attrs;
                 */
                {
                    FieldVisitor fv = cw.visitField(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, "$attrs",
                        strArrayType.getDescriptor(), null, null);
                    fv.visitEnd();
                }
            }

            int idx = 0;
            for (Method m : proxys) {
                idx++;
                /*-
                 * private static final MethodProxy $ImethodName;
                 */
                {
                    FieldVisitor fv = cw.visitField(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, "$" + idx + m.getName(),
                        proxyType.getDescriptor(), null, null);
                    fv.visitEnd();
                }
            }
        }
        {
            /*-
             * static {
             *     Class<?>[] $1;
             *     AccessibleObject[] $2;
             *     
             *     $ImethodName = new MProxy(mIdx, method, AccessibleObject[]);
             * }
             */
            MethodEmitter e = cw.visitMethodX(debug, ACC_STATIC, "<clinit>", "()V", null, null);
            e.start_method();
            int idx = 0;
            for (Method m : proxys) {
                idx++;
                MProxy proxy = allProxys.get(m);
                {
                    e.new_instance(proxyImplType);
                    e.dup();
                    e.push(idx);
                    {
                        e.push(m.getDeclaringClass());
                        e.push(m.getName());
                        Class<?>[] pcs = m.getParameterTypes();
                        e.push(pcs.length);
                        e.newarray(T_Class);
                        e.store_local(classArrayType, 1);

                        for (int j = 0; j < pcs.length; j++) {
                            e.load_local(classArrayType, 1);
                            e.push(j);
                            e.push(Type.getType(pcs[j]));
                            e.aastore();
                        }
                        e.load_local(classArrayType, 1);
                        e.invoke_virtual(T_Class, "getDeclaredMethod",
                            Type.getMethodDescriptor(T_Method, T_String, classArrayType));
                    }
                    {
                        e.push(proxy.accessibleObjects.length);
                        e.newarray(aoType);
                        e.store_local(aoArrayType, 2);
                        for (int i = 0; i < proxy.accessibleObjects.length; i++) {
                            AccessibleObject ao = proxy.accessibleObjects[i];
                            e.load_local(aoArrayType, 2);
                            e.push(i);
                            if (ao instanceof Field) {
                                Field aof = (Field) ao;
                                e.push(aof.getDeclaringClass());
                                e.push(aof.getName());
                                e.invoke_virtual(T_Class, "getDeclaredField",
                                    Type.getMethodDescriptor(T_Field, T_String));
                            } else {
                                Method aom = (Method) ao;
                                e.push(aom.getDeclaringClass());
                                e.push(aom.getName());
                                Class<?>[] pcs = aom.getParameterTypes();
                                e.push(pcs.length);
                                e.newarray(T_Class);
                                e.store_local(classArrayType, 1);

                                for (int j = 0; j < pcs.length; j++) {
                                    e.load_local(classArrayType, 1);
                                    e.push(j);
                                    e.push(Type.getType(pcs[j]));
                                    e.aastore();
                                }
                                e.load_local(classArrayType, 1);
                                e.invoke_virtual(T_Class, "getDeclaredMethod",
                                    Type.getMethodDescriptor(T_Method, T_String, classArrayType));
                            }
                            e.aastore();
                        }
                        e.load_local(aoArrayType, 2);
                    }
                    e.invoke_constructor(proxyImplType, Type.getMethodDescriptor(T_void, T_int, T_Method, aoArrayType));
                    e.putstatic(goal, "$" + idx + m.getName(), proxyType);
                }
            }
            if (advanced) {
                {
                    /*-
                     * $attrIndex = new LinkedHashMap();
                     * for-each attrs:
                     *     $attrIndex.put(attr.toLowerCase(), I);
                     */
                    Type mapType = Type.getType(LinkedHashMap.class);
                    String desc = Type.getMethodDescriptor(T_Object, T_Object, T_Object);
                    e.new_instance(mapType);
                    e.dup();
                    e.invoke_constructor(mapType);
                    e.putstatic(goal, "$attrIndex", T_Map);

                    for (int i = 0; i < attrs.size(); i++) {
                        e.getstatic(goal, "$attrIndex", T_Map);
                        e.push(attrs.get(i));
                        e.push(i + 1);
                        e.box(T_int);
                        e.invoke_interface(T_Map, "put", desc);
                        e.pop();
                    }
                }
                {
                    /*-
                     * $attrs = new String[SIZE];
                     * for-each attrs:
                     *     $attrs[I] = "attrI";
                     */
                    e.push(attrs.size());
                    e.newarray(T_String);

                    for (int i = 0; i < attrs.size(); i++) {
                        e.dup();
                        e.push(i);
                        e.push(as.get(attrs.get(i)));
                        e.aastore();
                    }
                    e.putstatic(goal, "$attrs", strArrayType);
                }
            }
            e.return_void();
            e.end_method();
        }

        {
            /*-
             * private final Callback[] $callbacks = new Callback[SIZE];
             */

            FieldVisitor fv = cw.visitField(ACC_PRIVATE | ACC_FINAL, "$callbacks", callbackArrayType.getDescriptor(),
                null, null);
            fv.visitEnd();

            MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, "<init>", "()V", null, null);
            e.start_method();
            Label start = e.mark();
            e.load_local(goal, 0);
            e.invoke_constructor(beanType, "()V");

            e.load_local(goal, 0);
            e.push(callbacks.length);
            e.newarray(callbackType);
            e.putfield(goal, "$callbacks", callbackArrayType);

            e.return_void();
            Label end = e.mark();
            e.mark_local("this", goal, start, end, 0);
            e.end_method();
        }

        {
            /*-
             * public final Object newInstance(Callback... callbacks) {
             *     Factory ret = new goal();
             *     System.arraycopy(callbacks, 0, $callbacks, 0, SIZE);
             *     return ret;
             * }
             */
            {
                MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC | ACC_FINAL | ACC_VARARGS, "newInstance",
                    Type.getMethodDescriptor(T_Object, callbackArrayType), null, null);
                e.start_method();
                Label start = e.mark();
                e.new_instance(goal);
                e.dup();
                e.invoke_constructor(goal, "()V");
                e.store_local(goal, 2);

                e.load_local(callbackArrayType, 1);
                e.push(0);
                e.load_local(goal, 2);
                e.getfield(goal, "$callbacks", callbackArrayType);
                e.push(0);
                e.push(callbacks.length);
                e.invoke_static(Type.getType(System.class), "arraycopy",
                    Type.getMethodDescriptor(T_void, T_Object, T_int, T_Object, T_int, T_int));

                e.load_local(goal, 2);
                e.return_value(goal);
                Label end = e.mark();
                e.mark_local("this", goal, start, end, 0);
                e.end_method();
            }
            /*-
             * public final Callback getCallback(int index) {
             *     return $callbacks[index];
             * }
             */
            {
                MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC | ACC_FINAL, "getCallback",
                    Type.getMethodDescriptor(callbackType, T_int), null, null);
                e.start_method();
                Label start = e.mark();
                e.load_local(goal, 0);
                e.getfield(goal, "$callbacks", callbackArrayType);
                e.load_local(T_int, 1);
                e.aaload();
                e.return_value(callbackType);
                Label end = e.mark();
                e.mark_local("this", goal, start, end, 0);
                e.end_method();
            }
            /*-
             * public final void setCallback(int index, Callback callback) {
             *     $callbacks[index] = callback;
             * }
             */
            {
                MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC | ACC_FINAL, "setCallback",
                    Type.getMethodDescriptor(T_void, T_int, callbackType), null, null);
                e.start_method();
                Label start = e.mark();
                e.load_local(goal, 0);
                e.getfield(goal, "$callbacks", callbackArrayType);
                e.load_local(T_int, 1);
                e.load_local(callbackType, 2);
                e.aastore();
                e.return_void();
                Label end = e.mark();
                e.mark_local("this", goal, start, end, 0);
                e.end_method();
            }
            /*-
             * public final Callback[] getCallbacks() {
             *     return $callbacks;
             * }
             */

            {
                MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC | ACC_FINAL, "getCallbacks",
                    Type.getMethodDescriptor(callbackArrayType), null, null);
                e.start_method();
                Label start = e.mark();
                e.load_local(goal, 0);
                e.getfield(goal, "$callbacks", callbackArrayType);
                e.return_value(callbackArrayType);
                Label end = e.mark();
                e.mark_local("this", goal, start, end, 0);
                e.end_method();
            }

            if (advanced) {
                {
                    /*-
                     * public List<String> $attrs() {
                     *     return Collections.unmodifiableList(Arrays.asList($attrs));
                     * }
                     */
                    MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, "$attrs", Type.getMethodDescriptor(T_List),
                        null, null);
                    e.start_method();
                    Label start = e.mark();
                    e.getstatic(goal, "$attrs", strArrayType);
                    e.invoke_static(Type.getType(Arrays.class), "asList",
                        Type.getMethodDescriptor(T_List, objectArrayType));
                    e.invoke_static(Type.getType(Collections.class), "unmodifiableList",
                        Type.getMethodDescriptor(T_List, T_List));
                    e.return_value(T_List);
                    Label end = e.mark();
                    e.mark_local("this", goal, start, end, 0);
                    e.end_method();
                }
                {
                    /*-
                     * public Class<?> $type(String attr) {
                     *     int idx = (Integer) $attrIndex.get(attr.toLowerCase());
                     *     switch (idx.intValue) {
                     *         case I :
                     *             return Xxx.class;
                     *         default:
                     *             return null;
                     *     }
                     * }
                     */
                    MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, "$type",
                        Type.getMethodDescriptor(T_Class, T_String), null, null);
                    e.start_method();
                    Label start = e.mark();

                    e.getstatic(goal, "$attrIndex", T_Map);
                    e.load_local(T_String, 1);
                    e.invoke_virtual(T_String, "toLowerCase", Type.getMethodDescriptor(T_String));
                    e.invoke_interface(T_Map, "get", Type.getMethodDescriptor(T_Object, T_Object));
                    e.checkcast(T_Integer);
                    e.unbox_or_zero(T_int);

                    int[] keys = new int[attrs.size()];
                    for (int i = 0; i < keys.length; i++) {
                        keys[i] = i + 1;
                    }
                    e.process_switch(keys, new Case() {
                        @Override
                        public void processCase(MethodEmitter ce, int key, Label end) {
                            String attr = attrs.get(key - 1);
                            AccessibleObject[] ao = aos.get(attr);
                            if (ao[1] != null) {
                                Method m = (Method) ao[1];
                                Class<?> clazz = m.getReturnType();
                                ce.push(clazz);
                            } else if (ao[2] != null) {
                                Method m = (Method) ao[2];
                                Class<?> clazz = m.getParameterTypes()[0];
                                ce.push(clazz);
                            } else if (ao[0] != null) {
                                Field f = (Field) ao[0];
                                Class<?> clazz = f.getType();
                                ce.push(clazz);
                            } else {
                                ce.aconst_null();
                            }
                            ce.return_value(T_Class);
                        }

                        @Override
                        public void processDefault(MethodEmitter ce) {
                            ce.aconst_null();
                            ce.return_value(T_Class);
                        }
                    }, true);

                    Label end = e.mark();
                    e.mark_local("this", goal, start, end, 0);
                    e.end_method();
                }
                {
                    /*-
                     * public Object $get(String attr) {
                     *     int idx = (Integer) $attrIndex.get(attr.toLowerCase());
                     *     switch (idx.intValue) {
                     *         case I :
                     *             return super.getXxx();
                     *         default:
                     *             return null;
                     *     }
                     * }
                     */
                    MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, "$get",
                        Type.getMethodDescriptor(T_Object, T_String), null, null);
                    e.start_method();
                    Label start = e.mark();

                    e.getstatic(goal, "$attrIndex", T_Map);
                    e.load_local(T_String, 1);
                    e.invoke_virtual(T_String, "toLowerCase", Type.getMethodDescriptor(T_String));
                    e.invoke_interface(T_Map, "get", Type.getMethodDescriptor(T_Object, T_Object));
                    e.checkcast(T_Integer);
                    e.unbox_or_zero(T_int);
                    int[] keys = new int[attrs.size()];
                    for (int i = 0; i < keys.length; i++) {
                        keys[i] = i + 1;
                    }
                    e.process_switch(keys, new Case() {
                        @Override
                        public void processCase(MethodEmitter ce, int key, Label end) {
                            String attr = attrs.get(key - 1);
                            AccessibleObject[] ao = aos.get(attr);
                            if (ao[1] != null) {
                                Method m = (Method) ao[1];
                                ce.load_this();
                                ce.super_invoke(Type.getType(m.getDeclaringClass()), m.getName(),
                                    Type.getMethodDescriptor(m));
                                Type rt = Type.getType(m.getReturnType());
                                if (rt.isPrimitive()) {
                                    ce.box(rt);
                                }
                            } else if (ReflectUtil.canAccess(superclass, ao[0])) {
                                Field f = (Field) ao[0];
                                f.setAccessible(true);
                                Type ft = Type.getType(f.getType());
                                ce.load_this();
                                ce.getfield(Type.getType(f.getDeclaringClass()), f.getName(), ft);
                                if (ft.isPrimitive()) {
                                    ce.box(ft);
                                }
                            } else {
                                ce.aconst_null();
                            }
                            ce.return_value(T_Object);
                        }

                        @Override
                        public void processDefault(MethodEmitter ce) {
                            ce.aconst_null();
                            ce.return_value(T_Object);
                        }
                    }, true);

                    Label end = e.mark();
                    e.mark_local("this", goal, start, end, 0);
                    e.end_method();
                }

                {
                    /*-
                     * public void $set(String attr, Object obj) {
                     *     int idx = (Integer) $attrIndex.get(attr.toLowerCase());
                     *     switch (idx.intValue) {
                     *         case I :
                     *             super.setXxx(obj);
                     *             return;
                     *         default:
                     *             return;
                     *     }
                     * }
                     */

                    MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, "$set",
                        Type.getMethodDescriptor(T_void, T_String, T_Object), null, null);
                    e.start_method();
                    Label start = e.mark();

                    e.getstatic(goal, "$attrIndex", T_Map);
                    e.load_local(T_String, 1);
                    e.invoke_virtual(T_String, "toLowerCase", Type.getMethodDescriptor(T_String));
                    e.invoke_interface(T_Map, "get", Type.getMethodDescriptor(T_Object, T_Object));
                    e.checkcast(T_Integer);
                    e.unbox_or_zero(T_int);
                    int[] keys = new int[attrs.size()];
                    for (int i = 0; i < keys.length; i++) {
                        keys[i] = i + 1;
                    }
                    e.process_switch(keys, new Case() {
                        @Override
                        public void processCase(MethodEmitter ce, int key, Label end) {
                            String attr = attrs.get(key - 1);
                            AccessibleObject[] ao = aos.get(attr);
                            if (ao[2] != null) {
                                Method m = (Method) ao[2];
                                ce.load_this();
                                ce.load_local(T_Object, 2);
                                Type pt = Type.getType(m.getParameterTypes()[0]);
                                if (pt.isPrimitive()) {
                                    ce.checkcast(pt.toReferenceType());
                                    ce.unbox_or_zero(pt);
                                } else {
                                    ce.checkcast(pt);
                                }
                                ce.super_invoke(Type.getType(m.getDeclaringClass()), m.getName(),
                                    Type.getMethodDescriptor(m));
                            } else if (ReflectUtil.canAccess(superclass, ao[0])) {
                                Field f = (Field) ao[0];
                                Type ft = Type.getType(f.getType());
                                ce.load_this();
                                ce.load_local(T_Object, 2);
                                if (ft.isPrimitive()) {
                                    ce.checkcast(ft.toReferenceType());
                                    ce.unbox_or_zero(ft);
                                } else {
                                    ce.checkcast(ft);
                                }
                                ce.putfield(Type.getType(f.getDeclaringClass()), f.getName(), ft);
                            }
                            ce.return_void();
                        }

                        @Override
                        public void processDefault(MethodEmitter ce) {
                            ce.return_void();
                        }
                    }, true);

                    Label end = e.mark();
                    e.mark_local("this", goal, start, end, 0);
                    e.end_method();
                }
                {
                    /*-
                     * public <T extends Annotation> T $annotation(String attr, Class<T> type) {
                     *     int idx = (Integer) $attrIndex.get(attr.toLowerCase());
                     *     switch (idx.intValue) {
                     *         case I :
                     *             super.setXxx(obj);
                     *             return;
                     *         default:
                     *             return;
                     *     }
                     * }
                     */
                    MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, "$annotation",
                        Type.getMethodDescriptor(T_Annotation, T_String, T_Class),
                        "<T::Ljava/lang/annotation/Annotation;>(Ljava/lang/String;Ljava/lang/Class<TT;>;)TT;", null);
                    e.start_method();
                    Label start = e.mark();

                    e.getstatic(goal, "$attrIndex", T_Map);
                    e.load_local(T_String, 1);
                    e.invoke_virtual(T_String, "toLowerCase", Type.getMethodDescriptor(T_String));
                    e.invoke_interface(T_Map, "get", Type.getMethodDescriptor(T_Object, T_Object));
                    e.checkcast(T_Integer);
                    e.unbox_or_zero(T_int);
                    int[] keys = new int[attrs.size()];
                    for (int i = 0; i < keys.length; i++) {
                        keys[i] = i + 1;
                    }
                    e.process_switch(keys, new Case() {
                        @Override
                        public void processCase(MethodEmitter ce, int key, Label end) {
                            String attr = attrs.get(key - 1);
                            AccessibleObject[] ao = aos.get(attr);
                            for (AccessibleObject a : ao) {
                                if (a != null && a.getAnnotations().length > 0) {
                                    if (a instanceof Field) {
                                        Field f = (Field) a;
                                        ce.push(f.getDeclaringClass());
                                        ce.push(f.getName());
                                        ce.invoke_virtual(T_Class, "getDeclaredField",
                                            Type.getMethodDescriptor(T_Field, T_String));
                                        ce.load_local(T_Class, 2);
                                        ce.invoke_virtual(T_Field, "getAnnotation",
                                            Type.getMethodDescriptor(T_Annotation, T_Class));
                                        ce.store_local(T_Annotation, 3);
                                        ce.load_local(T_Annotation, 3);
                                        Label nullAnnotation = ce.make_label();
                                        ce.ifnull(nullAnnotation);
                                        ce.load_local(T_Annotation, 3);
                                        ce.return_value(T_Annotation);
                                        ce.mark(nullAnnotation);
                                    } else {
                                        Method m = (Method) a;
                                        ce.push(m.getDeclaringClass());
                                        ce.push(m.getName());
                                        Class<?>[] pcs = m.getParameterTypes();
                                        ce.push(pcs.length);
                                        ce.newarray(T_Class);
                                        ce.store_local(classArrayType, 4);

                                        for (int j = 0; j < pcs.length; j++) {
                                            ce.load_local(classArrayType, 4);
                                            ce.push(j);
                                            ce.push(Type.getType(pcs[j]));
                                            ce.aastore();
                                        }
                                        ce.load_local(classArrayType, 4);
                                        ce.invoke_virtual(T_Class, "getDeclaredMethod",
                                            Type.getMethodDescriptor(T_Method, T_String, classArrayType));
                                        ce.load_local(T_Class, 2);
                                        ce.invoke_virtual(T_Method, "getAnnotation",
                                            Type.getMethodDescriptor(T_Annotation, T_Class));
                                        ce.store_local(T_Annotation, 3);
                                        ce.load_local(T_Annotation, 3);
                                        Label nullAnnotation = ce.make_label();
                                        ce.ifnull(nullAnnotation);
                                        ce.load_local(T_Annotation, 3);
                                        ce.return_value(T_Annotation);
                                        ce.mark(nullAnnotation);
                                    }
                                }
                            }
                            ce.aconst_null();
                            ce.return_value(T_Annotation);
                        }

                        @Override
                        public void processDefault(MethodEmitter ce) {
                            ce.aconst_null();
                            ce.return_value(T_Annotation);
                        }
                    }, true);

                    Label end = e.mark();
                    e.mark_local("this", goal, start, end, 0);
                    e.end_method();
                }
            } else {
                {
                    /*-
                     * public List<String> $attrs() {
                     *     return null;
                     * }
                     */
                    MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, "$attrs", Type.getMethodDescriptor(T_List),
                        null, null);
                    e.start_method();
                    Label start = e.mark();
                    e.invoke_static(Type.getType(Collections.class), "emptyList", Type.getMethodDescriptor(T_List));
                    e.return_value(T_List);
                    Label end = e.mark();
                    e.mark_local("this", goal, start, end, 0);
                    e.end_method();
                }
                {
                    /*-
                     * public Class<?> $type(String attr) {
                     *     return null;
                     * }
                     */
                    MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, "$type",
                        Type.getMethodDescriptor(T_Class, T_String), null, null);
                    e.start_method();
                    Label start = e.mark();
                    e.aconst_null();
                    e.return_value(T_Class);
                    Label end = e.mark();
                    e.mark_local("this", goal, start, end, 0);
                    e.end_method();
                }

                {
                    /*-
                     * public Object $get(String attr) {
                     *     return null;
                     * }
                     */
                    MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, "$get",
                        Type.getMethodDescriptor(T_Object, T_String), null, null);
                    e.start_method();
                    Label start = e.mark();
                    e.aconst_null();
                    e.return_value(T_Object);
                    Label end = e.mark();
                    e.mark_local("this", goal, start, end, 0);
                    e.end_method();
                }

                {
                    /*-
                     * public void $set(String attr, Object) {
                     * }
                     */
                    MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, "$set",
                        Type.getMethodDescriptor(T_void, T_String, T_Object), null, null);
                    e.start_method();
                    Label start = e.mark();
                    e.return_void();
                    Label end = e.mark();
                    e.mark_local("this", goal, start, end, 0);
                    e.end_method();
                }
                {
                    /*-
                     * public <T extends Annotation> T $annotation(String attr, Class<T> type) {
                     *     return null;
                     * }
                     */
                    MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, "$annotation",
                        Type.getMethodDescriptor(T_Annotation, T_String, T_Class),
                        "<T::Ljava/lang/annotation/Annotation;>(Ljava/lang/String;Ljava/lang/Class<TT;>;)TT;", null);
                    e.start_method();
                    Label start = e.mark();
                    e.aconst_null();
                    e.return_value(T_Annotation);
                    Label end = e.mark();
                    e.mark_local("this", goal, start, end, 0);
                    e.end_method();
                }
            }
        }
        {
            /*-
             * methods
             */
            int idx = 0;
            for (Method m : proxys) {
                idx++;
                String desc = Type.getMethodDescriptor(m);
                /*-
                 * public T <method>(...) {
                 *     if ($callbacks == null || $callbacks[I] == null) {
                 *         [return] super.<method>(...);
                 *     } else {
                 *         [return] cb.intercept($I<method>, this, ...);
                 *     }
                 * }
                 */
                {
                    MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, m.getName(), desc, null, null);
                    e.start_method();
                    Label start = e.mark();
                    Label nullInterceptor = e.make_label();
                    e.load_local(goal, 0);
                    e.getfield(goal, "$callbacks", callbackArrayType);
                    e.dup();
                    e.ifnull(nullInterceptor);
                    
                    int callbackIdx = methods.get(m);
                    e.push(callbackIdx);
                    e.aaload();
                    e.dup();
                    e.ifnull(nullInterceptor);
                    {
                        e.getstatic(goal, "$" + idx + m.getName(), proxyType);
                        e.load_local(goal, 0);
                        e.push(m.getParameterTypes().length);
                        e.newarray(T_Object);

                        int aIdx = 0, varIdx = 1;
                        for (Class<?> pc : m.getParameterTypes()) {
                            e.dup();
                            e.push(aIdx++);
                            Type tc = Type.getType(pc);
                            e.load_local(tc, varIdx);
                            varIdx += tc.getSize();
                            e.box(tc);
                            e.aastore();
                        }
                        e.invoke_interface(callbackType, "intercept",
                            Type.getMethodDescriptor(T_Object, proxyType, T_Object, objectArrayType));
                        Type rt = Type.getType(m.getReturnType());
                        if (rt.isVoid()) {
                            e.pop();
                        } else if (rt.isPrimitive()) {
                            e.checkcast(rt.toReferenceType());
                            e.unbox_or_zero(rt);
                        } else {
                            e.checkcast(rt);
                        }
                        e.return_value(rt);
                    }
                    e.mark(nullInterceptor);
                    {
                        e.pop();
                        e.load_local(goal, 0);
                        int varIdx = 1;
                        for (Class<?> pc : m.getParameterTypes()) {
                            Type tc = Type.getType(pc);
                            e.load_local(tc, varIdx);
                            varIdx += tc.getSize();
                        }
                        e.super_invoke(Type.getType(m.getDeclaringClass()), m.getName(), desc);
                        e.return_value(Type.getType(m.getReturnType()));
                    }
                    Label end = e.mark();
                    e.mark_local("this", goal, start, end, 0);
                    e.end_method();
                }
            }
        }

        {
            /*-
             * impl Factory.$invoke(int, Object...) {
             *     switch(int) {
             *         case I: {
             *             return super.<method>(...);
             *         }
             *         default: return null;
             *     }
             * }
             */
            MethodEmitter e = cw.visitMethodX(debug, ACC_PUBLIC, "$invoke",
                Type.getMethodDescriptor(T_Object, T_int, T_Object.getArrayType(1)), null, null);
            e.start_method();
            Label start = e.mark();
            int[] keys = new int[proxys.size()];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = i + 1;
            }
            e.load_local(T_int, 1);
            e.process_switch(keys, new Case() {
                public void processCase(MethodEmitter ce, int key, Label end) {
                    Method m = proxys.get(key - 1);
                    ce.load_local(goal, 0);

                    Class<?>[] pcs = m.getParameterTypes();
                    for (int j = 0; j < pcs.length; j++) {
                        ce.load_local(objectArrayType, 2);
                        ce.aaload(j);
                        Type pt = Type.getType(pcs[j]);
                        if (pt.isPrimitive()) {
                            ce.checkcast(pt.toReferenceType());
                            ce.unbox_or_zero(pt);
                        } else {
                            ce.checkcast(pt);
                        }
                    }

                    ce.super_invoke(Type.getType(m.getDeclaringClass()), m.getName(), Type.getMethodDescriptor(m));

                    Type rt = Type.getType(m.getReturnType());
                    if (rt.isVoid()) {
                        ce.aconst_null();
                    } else if (rt.isPrimitive()) {
                        ce.box(rt);
                    }
                    ce.return_value(T_Object);
                }

                public void processDefault(MethodEmitter ce) {
                    ce.aconst_null();
                    ce.return_value(T_Object);
                }
            }, true);
            Label end = e.mark();
            e.mark_local("this", goal, start, end, 0);
            e.end_method();
        }
        cw.visitEnd();

        byte[] bs = cw.toByteArray();
        if (debug) {
            OutputStream os = null;
            try {
                File file = new File(cn + ".class");
                System.out.println(file.getAbsolutePath());
                // file.getParentFile().mkdirs();
                os = new BufferedOutputStream(new FileOutputStream(file));
                os.write(bs);
                os.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        Class<?> clazz = ReflectUtil.defineClass(cn, bs, superclass.getClassLoader());
        return (Class<? extends Factory>) clazz;
    }

}
