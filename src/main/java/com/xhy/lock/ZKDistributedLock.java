package com.xhy.lock;

import com.xhy.utils.LogUtils;
import com.xhy.utils.ZKUtils;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * zk的分布式锁
 *
 * @author xhy
 * @date 2021/5/28 22:45
 */
public class ZKDistributedLock {

    ZooKeeper zk;

    @Before
    public void beforeZKConn() {
        zk = ZKUtils.getZK();
    }

    @After
    public void close() {
        try {
            zk.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDistributedLock() {
        //模拟高并发,启动十个线程同时访问
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                ZKWatchCallBack watchCallBack = new ZKWatchCallBack();
                watchCallBack.setZk(zk);
                watchCallBack.setThreadName(Thread.currentThread().getName());
                watchCallBack.tryLock();
                //doing
                LogUtils.info("【zk分布式锁】threadName="+Thread.currentThread().getName()+"执行程序...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                watchCallBack.unLock();
            }).start();
        }

        while (true){

        }
    }
}
