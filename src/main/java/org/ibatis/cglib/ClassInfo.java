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
package org.ibatis.cglib;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a cached set of class definition information that allows for easy mapping between property
 * names and getter/setter methods.
 */
public class ClassInfo {

    private static boolean cacheEnabled = true;
    private static final Map<Class<?>, ClassInfo> CLASS_INFO_MAP = new HashMap<Class<?>, ClassInfo>();
    private static final Map<Class<?>, ClassInfo> CLASS_INFO_MAP_STRICT = new HashMap<Class<?>, ClassInfo>();

    private String className;
    private boolean strict;
    private Map<String, Invoker> setMethods = new LinkedHashMap<String, Invoker>();
    private Map<String, Invoker> getMethods = new LinkedHashMap<String, Invoker>();
    private Map<String, Class<?>> setTypes = new LinkedHashMap<String, Class<?>>();
    private Map<String, Class<?>> getTypes = new LinkedHashMap<String, Class<?>>();
    private Constructor<?> defaultConstructor;
    private List<String> propertyNames = new ArrayList<String>();

    private ClassInfo(Class<?> clazz, boolean strict) {
        if (clazz.getEnclosingClass() != null && !Modifier.isStatic(clazz.getModifiers())) {
            throw new RuntimeException("Not top-level or static class: " + clazz.getName());
        }
        if (!Modifier.isPublic(clazz.getModifiers())) {
            throw new RuntimeException("Not public class: " + clazz.getName());
        }
        this.strict = strict;
        className = clazz.getName();
        addDefaultConstructor(clazz);
        addGetMethods(clazz);
        addSetMethods(clazz);
        addFields(clazz);
        if (strict) {
            Set<String> set = new LinkedHashSet<String>();
            set.addAll(getMethods.keySet());
            set.retainAll(setMethods.keySet());
            HashMap<String, Invoker> _setMethods = new LinkedHashMap<String, Invoker>();
            HashMap<String, Invoker> _getMethods = new LinkedHashMap<String, Invoker>();
            HashMap<String, Class<?>> _setTypes = new LinkedHashMap<String, Class<?>>();
            HashMap<String, Class<?>> _getTypes = new LinkedHashMap<String, Class<?>>();
            for (String key : set) {
                if (getTypes.get(key) != setTypes.get(key)) {
                    continue;
                }
                _getMethods.put(key, getMethods.get(key));
                _setMethods.put(key, setMethods.get(key));
                _getTypes.put(key, getTypes.get(key));
                _setTypes.put(key, setTypes.get(key));
            }
            getMethods = _getMethods;
            setMethods = _setMethods;
            getTypes = _getTypes;
            setTypes = _setTypes;
            Set<String> attrs = new HashSet<String>();
            for (String key : _getMethods.keySet()) {
                attrs.add(_getMethods.get(key).getName());
            }
            propertyNames.addAll(attrs);
        } else {
            Set<String> attrs = new HashSet<String>();
            for (String key : getMethods.keySet()) {
                attrs.add(getMethods.get(key).getName());
            }
            for (String key : setMethods.keySet()) {
                attrs.add(setMethods.get(key).getName());
            }
            propertyNames.addAll(attrs);
        }
        Collections.sort(propertyNames, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.toLowerCase(Locale.ENGLISH).compareTo(o2.toLowerCase(Locale.ENGLISH));
            }
        });
    }

    public List<String> getPropertyNames() {
        return Collections.unmodifiableList(propertyNames);
    }

    private void addDefaultConstructor(Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            this.defaultConstructor = constructor;
        } catch (Exception e) {
        }
    }

    private void addGetMethods(Class<?> cls) {
        Method[] methods = getClassMethods(cls);
        out: for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            for (Class<?> e : method.getExceptionTypes()) {
                if (Exception.class.isAssignableFrom(e) && !RuntimeException.class.isAssignableFrom(e)) {
                    continue out;
                }
            }
            String name = method.getName();
            if (name.startsWith("get") && name.length() > 3) {
                if (method.getParameterTypes().length == 0) {
                    name = name.substring(3);
                    addGetMethod(dropCase(name), method);
                }
            } else if (name.startsWith("is") && name.length() > 2) {
                if (method.getParameterTypes().length == 0
                    && (method.getReturnType() == Boolean.class || method.getReturnType() == boolean.class)) {
                    name = name.substring(2);
                    addGetMethod(dropCase(name), method);
                }
            }
        }
    }

    void addGetMethod(String name, Method method) {
        MethodInvoker mi = new MethodInvoker(name, method, false);
        Class<?> rt = method.getReturnType();
        // getMethods.put(name, mi);
        // getTypes.put(name, rt);
        {
            getMethods.put(name.toLowerCase(Locale.ENGLISH), mi);
            getTypes.put(name.toLowerCase(Locale.ENGLISH), rt);
        }
    }

    private void addSetMethods(Class<?> cls) {
        Method[] methods = getClassMethods(cls);
        out: for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            for (Class<?> e : method.getExceptionTypes()) {
                if (Exception.class.isAssignableFrom(e) && !RuntimeException.class.isAssignableFrom(e)) {
                    continue out;
                }
            }
            String name = method.getName();
            if (name.startsWith("set") && name.length() > 3) {
                if (method.getParameterTypes().length == 1) {
                    name = name.substring(3);
                    addSetMethod(dropCase(name), method);
                }
            }
        }
    }

    void addSetMethod(String name, Method method) {
        MethodInvoker mi = new MethodInvoker(name, method, true);
        Class<?> pt = method.getParameterTypes()[0];
        // setMethods.put(name, mi);
        // setTypes.put(name, pt);
        {
            setMethods.put(name.toLowerCase(Locale.ENGLISH), mi);
            setTypes.put(name.toLowerCase(Locale.ENGLISH), pt);
        }
    }

    private void addFields(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            if (strict && Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Invoker setter = setMethods.get(field.getName().toLowerCase(Locale.ENGLISH));
            if (setter == null) {
                addSetField(field);
            } else {
                setter.fixAccess(field);
            }

            Invoker getter = getMethods.get(field.getName().toLowerCase(Locale.ENGLISH));
            if (getter == null) {
                addGetField(field);
            } else {
                getter.fixAccess(field);
            }
        }
        if (clazz.getSuperclass() != null) {
            addFields(clazz.getSuperclass());
        }
    }

    void addSetField(Field field) {
        // setMethods.put(field.getName(), new SetFieldInvoker(field));
        // setTypes.put(field.getName(), field.getType());
        if (Modifier.isPublic(field.getModifiers())) {
            setMethods.put(field.getName().toLowerCase(Locale.ENGLISH), new SetFieldInvoker(field));
            setTypes.put(field.getName().toLowerCase(Locale.ENGLISH), field.getType());
        }
    }

    void addGetField(Field field) {
        // getMethods.put(field.getName(), new GetFieldInvoker(field));
        // getTypes.put(field.getName(), field.getType());
        if (Modifier.isPublic(field.getModifiers())) {

            getMethods.put(field.getName().toLowerCase(Locale.ENGLISH), new GetFieldInvoker(field));
            getTypes.put(field.getName().toLowerCase(Locale.ENGLISH), field.getType());
        }
    }

    /**
     * This method returns an array containing all methods declared in this class and any superclass. We use this
     * method, instead of the simpler Class.getMethods(), because we want to look for private methods as well.
     *
     * @param cls
     *            The class
     * @return An array containing all methods in this class
     */
    private Method[] getClassMethods(Class<?> cls) {
        Map<String, Method> methodSet = new LinkedHashMap<String, Method>();
        Class<?> currentClass = cls;
        while (currentClass != null) {
            addUniqueMethods(methodSet, currentClass.getDeclaredMethods());

            // we also need to look for interface methods -
            // because the class may be abstract
            Class<?>[] interfaces = currentClass.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                addUniqueMethods(methodSet, interfaces[i].getMethods());
            }

            currentClass = currentClass.getSuperclass();
        }

        Collection<?> methods = methodSet.values();

        return (Method[]) methods.toArray(new Method[methods.size()]);
    }

    private void addUniqueMethods(Map<String, Method> mmap, Method[] methods) {
        for (Method m : methods) {
            if (strict && Modifier.isStatic(m.getModifiers())) {
                continue;
            }

            String name = m.getName();
            if ((name.startsWith("get") || name.startsWith("is") || name.startsWith("set")) && !m.isBridge()
                && Modifier.isPublic(m.getModifiers())) {
                if (!mmap.containsKey(name.toLowerCase(Locale.ENGLISH))) {
                    mmap.put(name.toLowerCase(Locale.ENGLISH), m);
                }
            }
        }
    }

    /**
     * Gets the name of the class the instance provides information for
     *
     * @return The class name
     */
    public String getClassName() {
        return className;
    }

    public Object instantiateClass() {
        if (defaultConstructor != null) {
            try {
                return defaultConstructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating class. Cause: " + e, e);
            }
        } else {
            throw new RuntimeException("Error instantiating class. No default constructor for class " + className);
        }
    }

    public Invoker getSetInvoker(String propertyName) {
        Invoker method = (Invoker) setMethods.get(propertyName.toLowerCase(Locale.ENGLISH));
        if (method == null) {
            throw new RuntimeException("No WRITEABLE property named '" + propertyName + "' in class '" + className + "'");
        }
        return method;
    }

    public Invoker getGetInvoker(String propertyName) {
        Invoker method = (Invoker) getMethods.get(propertyName.toLowerCase(Locale.ENGLISH));
        if (method == null) {
            throw new RuntimeException("No READABLE property named '" + propertyName + "' in class '" + className + "'");
        }
        return method;
    }

    /**
     * Gets the type for a property setter
     *
     * @param propertyName
     *            - the name of the property
     * @return The Class of the propery setter
     */
    public Class<?> getSetterType(String propertyName) {
        Class<?> clazz = (Class<?>) setTypes.get(propertyName.toLowerCase(Locale.ENGLISH));
        if (clazz == null) {
            throw new RuntimeException("No WRITEABLE property named '" + propertyName + "' in class '" + className + "'");
        }
        return clazz;
    }

    /**
     * Gets the type for a property getter
     *
     * @param propertyName
     *            - the name of the property
     * @return The Class of the propery getter
     */
    public Class<?> getGetterType(String propertyName) {
        Class<?> clazz = getTypes.get(propertyName.toLowerCase(Locale.ENGLISH));
        if (clazz == null) {
            throw new RuntimeException("No READABLE property named '" + propertyName + "' in class '" + className + "'");
        }
        return clazz;
    }

    /**
     * Check to see if a class has a writeable property by name
     *
     * @param propertyName
     *            - the name of the property to check
     * @return True if the object has a writeable property by the name
     */
    public boolean hasWritableProperty(String propertyName) {
        return setMethods.containsKey(propertyName.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Check to see if a class has a readable property by name
     *
     * @param propertyName
     *            - the name of the property to check
     * @return True if the object has a readable property by the name
     */
    public boolean hasReadableProperty(String propertyName) {
        return getMethods.containsKey(propertyName.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Gets an instance of ClassInfo for the specified class.
     *
     * @param clazz
     *            The class for which to lookup the method cache.
     * @return The method cache for the class
     */
    public static ClassInfo getInstance(Class<?> clazz) {
        return getInstance(clazz, false);
    }

    public static ClassInfo getInstance(Class<?> clazz, boolean strict) {
        if (cacheEnabled) {
            ClassInfo cached = null;
            if (strict) {
                synchronized (CLASS_INFO_MAP_STRICT) {
                    cached = (ClassInfo) CLASS_INFO_MAP_STRICT.get(clazz);
                    if (cached == null) {
                        cached = new ClassInfo(clazz, strict);
                        CLASS_INFO_MAP_STRICT.put(clazz, cached);
                    }
                }
            } else {
                synchronized (CLASS_INFO_MAP) {
                    cached = (ClassInfo) CLASS_INFO_MAP.get(clazz);
                    if (cached == null) {
                        cached = new ClassInfo(clazz, strict);
                        CLASS_INFO_MAP.put(clazz, cached);
                    }
                }
            }
            return cached;
        } else {
            return new ClassInfo(clazz, strict);
        }
    }

    public static void setCacheEnabled(boolean cacheEnabled) {
        ClassInfo.cacheEnabled = cacheEnabled;
    }

    /**
     * Examines a Throwable object and gets it's root cause
     *
     * @param t
     *            - the exception to examine
     * @return The root cause
     */
    public static Throwable unwrapThrowable(Throwable t) {
        Throwable t2 = t;
        while (true) {
            if (t2 instanceof InvocationTargetException) {
                t2 = ((InvocationTargetException) t).getTargetException();
            } else if (t2 instanceof UndeclaredThrowableException) {
                t2 = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
            } else {
                return t2;
            }
        }
    }

    public static String dropCase(String name) {
        int firstLower = -1;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLowerCase(c)) {
                firstLower = i;
                break;
            }
        }
        if (firstLower == -1) {
            // empty or all upper case char
            return name.toLowerCase(Locale.ENGLISH);
        } else if (firstLower == 0) {
            // start with lower case char
            return name;
        } else if (firstLower == 1) {
            return name.substring(0, firstLower).toLowerCase(Locale.ENGLISH) + name.substring(firstLower);
        } else {
            return name.substring(0, firstLower - 1).toLowerCase(Locale.ENGLISH) + name.substring(firstLower - 1);
        }
    }

}
