package com.xhy.utils;

import lombok.Data;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.concurrent.CountDownLatch;

/**
 * @author xhy
 * @date 2021/5/27 23:01
 */
@Data
public class DefaultWatch implements Watcher {

    CountDownLatch countDownLatch;

    @Override
    public void process(WatchedEvent event) {
        LogUtils.info("zk默认的监听事件:" + event);
        Event.KeeperState state = event.getState();
        switch (state) {
            case Unknown:
                break;
            case Disconnected:
                break;
            case NoSyncConnected:
                break;
            case SyncConnected:
                LogUtils.info("zk连接成功");
                countDownLatch.countDown();
                break;
            case AuthFailed:
                break;
            case ConnectedReadOnly:
                break;
            case SaslAuthenticated:
                break;
            case Expired:
                break;
            case Closed:
                break;
        }
    }
}
