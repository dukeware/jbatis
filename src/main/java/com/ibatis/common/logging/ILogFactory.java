package com.ibatis.common.logging;

import java.lang.reflect.Constructor;

import com.ibatis.common.resources.Resources;

public class ILogFactory {

    private static Constructor<?> logConstructor;

    static {
        tryImplementation("org.apache.commons.logging.LogFactory",
            "com.ibatis.common.logging.jakarta.JakartaCommonsLoggingImpl");
        tryImplementation("java.util.logging.Logger", "com.ibatis.common.logging.jdk14.Jdk14LoggingImpl");
        tryImplementation("java.lang.Object", "com.ibatis.common.logging.nologging.NoLoggingImpl");
    }

    private static void tryImplementation(String testClassName, String implClassName) {
        if (logConstructor == null) {
            try {
                Resources.classForName(testClassName);
                Class<?> implClass = Resources.classForName(implClassName);
                logConstructor = implClass.getConstructor(new Class[] { String.class });
            } catch (Throwable t) {
            }
        }
    }

    public static ILog getLog(Class<?> aClass) {
        try {
            return (ILog) logConstructor.newInstance(new Object[] { aClass.getName() });
        } catch (Throwable t) {
            throw new RuntimeException("Error creating logger for class " + aClass + ".  Cause: " + t, t);
        }
    }

    public static ILog getLog(String name) {
        try {
            return (ILog) logConstructor.newInstance(new Object[] { name });
        } catch (Throwable t) {
            throw new RuntimeException("Error creating logger for name " + name + ".  Cause: " + t, t);
        }
    }

    /**
     * This method will switch the logging implementation to Log4J if Log4J is available on the classpath. This is
     * useful in situations where you want to use Log4J to log iBATIS activity but commons logging is on the classpath.
     * Note that this method is only effective for log classes obtained after calling this method. If you intend to use
     * this method you should call it before calling any other iBATIS method.
     *
     */
    public static synchronized void selectLog4JLogging() {
        try {
            Resources.classForName("org.apache.log4j.Logger");
            Class<?> implClass = Resources.classForName("com.ibatis.common.logging.log4j.Log4jImpl");
            logConstructor = implClass.getConstructor(new Class[] { Class.class });
        } catch (Throwable t) {
        }
    }

    /**
     * This method will switch the logging implementation to Java native logging if you are running in JRE 1.4 or above.
     * This is useful in situations where you want to use Java native logging to log iBATIS activity but commons logging
     * or Log4J is on the classpath. Note that this method is only effective for log classes obtained after calling this
     * method. If you intend to use this method you should call it before calling any other iBATIS method.
     */
    public static synchronized void selectJavaLogging() {
        try {
            Resources.classForName("java.util.logging.Logger");
            Class<?> implClass = Resources.classForName("com.ibatis.common.logging.jdk14.Jdk14LoggingImpl");
            logConstructor = implClass.getConstructor(new Class[] { Class.class });
        } catch (Throwable t) {
        }
    }
}
