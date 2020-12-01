package com.ronan.pool;

import java.sql.Connection;
import java.util.LinkedList;

/**
 * 采用超时等待构造一个简单的数据库连接池
 * 示例中，模拟从连接池获取、使用和释放连接的过程
 * 客户端获取连接的过程被设定为等待超时的模式，如果在1000毫秒内没有获取到可用连接，会返回客户端一个null
 * 设定连接池大小为10，通过调节客户端的线程数来模拟无法获取连接的场景
 */
public class ConnectionPool {
    private LinkedList<Connection> pool = new LinkedList<Connection>();

    /**
     * 初始化连接的最大上限，通过一个双向队列来维护连接
     * @param initialSize
     */
    public ConnectionPool(int initialSize) {
        if (initialSize > 0) {
            for (int i = 0; i < initialSize; i++) {
                pool.addLast(ConnectionDriver.createConnection());
            }
        }
    }

    /**
     * 连接使用完成后，将连接放回连接池
     * @param connection
     */
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            synchronized (pool) {
                // 连接释放后需要进行通知，
                // 这样其他消费者才能够感知到连接池已经归还了一个连接
                pool.addLast(connection);
                pool.notifyAll();
            }
        }
    }

    /**
     * 指定在多少毫秒内超时获取连接
     * @param mills 在mills内无法获取到连接，将会返回null
     * @return
     * @throws InterruptedException
     */
    public Connection fetchConnection(long mills) throws InterruptedException {
        synchronized (pool) {
            // 完全超时
            if (mills <= 0) {
                while (pool.isEmpty()) {
                    pool.wait();
                }

                return pool.removeFirst();
            } else {
                long future = System.currentTimeMillis() + mills;
                long remaining = mills;

                while(pool.isEmpty() && remaining > 0) {
                    pool.wait(remaining);
                    remaining = future - System.currentTimeMillis();
                }

                Connection result = null;

                if (!pool.isEmpty()) {
                    result = pool.removeFirst();
                }

                return result;
            }
        }
    }
}
