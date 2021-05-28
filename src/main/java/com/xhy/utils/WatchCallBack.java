package com.xhy.utils;

import com.xhy.config.MyConf;
import lombok.Data;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * @author xhy
 * @date 2021/5/28 9:44
 */
@Data
public class WatchCallBack implements Watcher, AsyncCallback.StatCallback, AsyncCallback.DataCallback {

    private ZooKeeper zookeeper;

    private MyConf myConf;

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public void waitResult() throws InterruptedException {
        //1.使用回调的方式先判断目录是否存在,如果存在在回调中获取。
        LogUtils.warn("当前的countDownLatch数量是:" + countDownLatch.getCount());
        zookeeper.exists("/appConf", this, this, "test");
        countDownLatch.await();
    }

    /**
     * watch的监听事件
     *
     * @param event
     */
    @Override
    public void process(WatchedEvent event) {
        LogUtils.info("获取事件的监听开始......");
        Event.EventType type = event.getType();
        switch (type) {
            case None:
                LogUtils.info("none.....");
                break;
            case NodeCreated:
                LogUtils.info("获取事件的监听,节点数据新增......");
                zookeeper.getData("/appConf", this, this, "stat");
                break;
            case NodeDeleted:
                LogUtils.info("获取事件的监听,节点数据删除......");
                myConf.setNodeData(null);
                countDownLatch = new CountDownLatch(1);
                break;
            case NodeDataChanged:
                LogUtils.info("获取事件的监听,节点数据变更......");
                zookeeper.getData("/appConf", this, this, "stat");
                break;
            case NodeChildrenChanged:
                LogUtils.info("NodeChildrenChanged.....");
                break;
            case DataWatchRemoved:
                LogUtils.info("DataWatchRemoved.....");
                break;
            case ChildWatchRemoved:
                LogUtils.info("ChildWatchRemoved.....");
                break;
            case PersistentWatchRemoved:
                LogUtils.info("PersistentWatchRemoved.....");
                break;
        }
    }

    /**
     * 获取状态结果callBack事件监听
     *
     * @param rc
     * @param path
     * @param ctx
     * @param stat
     */
    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        LogUtils.info("获取状态结果的回调开始......");
        //如果节点存在
        if (Objects.nonNull(stat)) {
            zookeeper.getData(path, this, this, "stat");
        }
    }

    /**
     * 获取数据的callBack
     *
     * @param rc
     * @param path
     * @param ctx
     * @param data
     * @param stat
     */
    @Override
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
        LogUtils.info("获取数据的callBack开始......");
        if (Objects.nonNull(data)) {
            String nodeData = new String(data);
            //设置节点数据
            myConf.setNodeData(nodeData);
            countDownLatch.countDown();
        }
    }

}
