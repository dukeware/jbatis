package org.ibatis.cglib;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;

public class MethodInvoker extends Invoker {
    private static final ILog log = ILogFactory.getLog(Invoker.class);

    private Method method;
    private FastMethod fastMethod;
    boolean beSetter;

    public MethodInvoker(String name, Method method, boolean beSetter) {
        super(name);
        this.method = method;
        this.beSetter = beSetter;
        fixAccess(method);
    }

    @SuppressWarnings("deprecation")
    public Object invoke(Object target, Object... args) throws IllegalAccessException, InvocationTargetException {
        if (fastMethod == null) {
            synchronized (this) {
                try {
                    fastMethod = FastMethod.create(method);
                } catch (Throwable e) {
                    log.warn("Failed to create fast method for " + method + ", " + e);
                    System.err.println("Failed to create fast method for " + method + ", " + e);
                }
                if (fastMethod == null) {
                    fastMethod = new FastMethod(method);
                }
            }
        }
        try {
            return fastMethod.invoke(target, args);
        } catch (IllegalArgumentException e) {
            // ## ignore the primitive null value
            if (args != null && args.length == 1 && args[0] == null) {
                return null;
            }
            throw e;
        }
    }

    public Method getMethod() {
        return method;
    }

    public String getAccessName() {
        return method.getName() + "()";
    }

    public AccessibleObject getAccessibleObject() {
        return method;
    }

    @Override
    public Class<?> getType() {
        if (beSetter) {
            return method.getParameterTypes()[0];
        }
        return method.getReturnType();
    }
}
