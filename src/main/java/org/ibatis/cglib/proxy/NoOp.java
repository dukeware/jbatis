package org.ibatis.cglib.proxy;

public interface NoOp extends Callback {

    Callback INSTANCE = new NoOp() {
        public Object intercept(MethodProxy proxy, Object obj, Object... args) throws Throwable {
            return proxy.invokeSuper(obj, args);
        }
    };
}
