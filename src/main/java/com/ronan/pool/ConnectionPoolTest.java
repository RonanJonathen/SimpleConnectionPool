package com.ronan.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionPoolTest {
    // 创建连接池，最大连接数为10
    static ConnectionPool pool = new ConnectionPool(10);

    // 保证所有ConectionRunner能够同时开始
    static CountDownLatch start = new CountDownLatch(1);
    static CountDownLatch end;

    public static void main(String[] args) throws Exception{
        // 并发线程数量，可以修改线程数量进行观察
        int threadCount = 1000;
        end = new CountDownLatch(threadCount);

        int count = 20;
        AtomicInteger got = new AtomicInteger();
        AtomicInteger notGot = new AtomicInteger();

        // 10个线程同时运行，获取连接池中的连接
        // 通过调节线程数量观察未获取到连接的情况
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(
                    new ConnectionRunner(count, got, notGot),
                    "ConnectionRunnerThread");
            thread.start();
        }

        start.countDown();
        end.await();
        System.out.println("total invoke: " + (threadCount * count));
        System.out.println("got connection: " + got);
        System.out.println("not got connection: " + notGot);

    }

    static class ConnectionRunner implements Runnable {
        int count;
        AtomicInteger got;
        AtomicInteger notGot;
        public ConnectionRunner(int count, AtomicInteger got, AtomicInteger notGot) {
            this.count = count;
            this.got = got;
            this.notGot = notGot;
        }

        @Override
        public void run() {
            try {
                start.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while (count > 0) {
                try {
                    // 从线程池中获取连接，如果1000ms内无法获取到连接，则返回null
                    // 分别统计连接获取的数量got和为获取到的数量notGot
                    Connection connection = pool.fetchConnection(1000);

                    if (connection != null) {
                        try {
                            connection.createStatement();
                            connection.commit();
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        } finally {
                            pool.releaseConnection(connection);
                            got.incrementAndGet();
                        }
                    } else {
                        notGot.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    count--;
                }
            }

            end.countDown();
        }
    }
}












