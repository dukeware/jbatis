package org.ibatis.cglib;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;

public class GetFieldInvoker extends Invoker {
    private static final ILog log = ILogFactory.getLog(Invoker.class);

    private Field field;
    FastField fastField;

    public GetFieldInvoker(Field field) {
        super(field.getName());
        this.field = field;
        fixAccess(field);
    }

    @SuppressWarnings("deprecation")
    public Object invoke(Object target, Object... args) throws IllegalAccessException, InvocationTargetException {
        if (fastField == null) {
            synchronized (this) {
                try {
                    fastField = FastField.create(field);
                } catch (Throwable e) {
                    log.warn("Failed to create fast field for " + field + ", " + e);
                    System.err.println("Failed to create fast field for " + field + ", " + e);
                }

                if (fastField == null) {
                    fastField = new FastField(field);
                }
            }
        }
        return fastField.get(target);
    }

    public String getAccessName() {
        return field.getName();
    }

    public Field getField() {
        return field;
    }

    @Override
    public Class<?> getType() {
        return field.getType();
    }
}
