package com.xhy.test;

import com.xhy.config.MyConf;
import com.xhy.utils.LogUtils;
import com.xhy.utils.WatchCallBack;
import com.xhy.utils.ZKUtils;
import lombok.Data;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

/**
 * @author xhy
 * @date 2021/5/27 23:15
 */
@Data
public class ZookeeperConnTest {

    ZooKeeper zk;

    /**
     * 构建空的配置类
     */
    private MyConf myConf = new MyConf();

    /**
     * 连接zk测试
     */
    @Before
    public void testZKConn() {
        zk = ZKUtils.getZK();
        System.out.println(zk.getState());
    }

    @After
    public void closeZkConn() {
        try {
            LogUtils.warn("zk close......");
            zk.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testZk() throws InterruptedException {
        WatchCallBack watchCallBack = new WatchCallBack();
        watchCallBack.setZookeeper(zk);
        watchCallBack.setMyConf(myConf);
        watchCallBack.waitResult();

        while (true) {
            if (Objects.nonNull(myConf.getNodeData())) {
                LogUtils.info(myConf.getNodeData());
            } else {
                LogUtils.warn("无配置......");
                watchCallBack.waitResult();
            }
            Thread.sleep(2000);
        }
    }

}
