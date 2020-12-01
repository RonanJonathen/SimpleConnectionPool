package com.ronan.pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;

/**
 * 本示例只是简单模拟，
 * 所以，通过动态构造构造了一个Connection
 * 该Connection的代理实现仅仅是在commit()方法调用休眠100毫秒
 */
public class ConnectionDriver {
    static class ConnectionHandler implements InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("commit")) {
                TimeUnit.MICROSECONDS.sleep(100);
            }
            return null;
        }
    }

    /**
     * 创建一个Connection的代理，在commit时休眠100毫秒
     * @return
     */
    public static final Connection createConnection(){
        return (Connection) Proxy.newProxyInstance(
                ConnectionDriver.class.getClassLoader(),
                new Class[]{Connection.class},
                new ConnectionHandler());
    }
}
