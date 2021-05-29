package com.xhy.utils;

import com.xhy.constants.ZookeeperConstants;
import lombok.Data;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.CountDownLatch;

/**
 * @author xhy
 * @date 2021/5/27 22:40
 */
@Data
public class ZKUtils {

    private static CountDownLatch countDownLatch = new CountDownLatch(1);

    private static DefaultWatch defaultWatch = new DefaultWatch();

    /**
     * 获取zk的连接
     *
     * @return
     */
    public static ZooKeeper getZK() {
        defaultWatch.setCountDownLatch(countDownLatch);
        ZooKeeper zooKeeper = null;
        try {
            zooKeeper = new ZooKeeper(buildIpAddress(), 3000, defaultWatch);
            countDownLatch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zooKeeper;
    }

    /**
     * 构建四台服务器的地址端口
     * 如：192.168.163.66:2181,192.168.163.67:2181,192.168.163.68:2181,192.168.163.69:2181/testConfig
     *
     * @return
     */
    private static String buildIpAddress() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ZookeeperConstants.NODE1_IP_ADDRESS + ZookeeperConstants.COLON + ZookeeperConstants.PORT);
        stringBuffer.append(ZookeeperConstants.SPLIT);
        stringBuffer.append(ZookeeperConstants.NODE2_IP_ADDRESS + ZookeeperConstants.COLON + ZookeeperConstants.PORT);
        stringBuffer.append(ZookeeperConstants.SPLIT);
        stringBuffer.append(ZookeeperConstants.NODE3_IP_ADDRESS + ZookeeperConstants.COLON + ZookeeperConstants.PORT);
        stringBuffer.append(ZookeeperConstants.SPLIT);
        stringBuffer.append(ZookeeperConstants.NODE4_IP_ADDRESS + ZookeeperConstants.COLON + ZookeeperConstants.PORT);
        stringBuffer.append(ZookeeperConstants.DIAGONAL_LINE);
        stringBuffer.append("testLock");
        System.out.println("zk连接地址:" + stringBuffer.toString());
        return stringBuffer.toString();
    }

}
