package com.xhy.lock;

import com.xhy.utils.LogUtils;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * @author xhy
 * @date 2021/5/28 23:01
 */
@Data
public class ZKWatchCallBack implements Watcher, AsyncCallback.StringCallback, AsyncCallback.ChildrenCallback, AsyncCallback.StatCallback {

    private ZooKeeper zk;

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private String threadName;

    private String pathName;

    public void tryLock() {
        try {
            //创建节点
            LogUtils.info("【zk分布式锁】开始创建节点threadName=" + threadName);
            zk.create("/lock", threadName.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL, this, "ctx");
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void unLock() {
        try {
            LogUtils.info("【zk分布式锁】删除节点开始pathName=" + pathName);
            zk.delete(pathName, -1);
            LogUtils.info("【zk分布式锁】删除节点成功pathName=" + pathName);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    /**
     * watch监控
     *
     * @param event
     */
    @Override
    public void process(WatchedEvent event) {
        Event.EventType type = event.getType();
        switch (type) {
            case None:
                break;
            case NodeCreated:
                break;
            case NodeDeleted:
                //获取所有的子节点
                zk.getChildren("/", false, this, "ctx");
                break;
            case NodeDataChanged:
                break;
            case NodeChildrenChanged:
                break;
            case DataWatchRemoved:
                break;
            case ChildWatchRemoved:
                break;
            case PersistentWatchRemoved:
                break;
        }
    }

    /**
     * 创建结果的最终状态
     *
     * @param rc
     * @param path
     * @param ctx
     * @param name
     */
    @Override
    public void processResult(int rc, String path, Object ctx, String name) {
        LogUtils.info("【zk分布式锁】新建临时序列节点创建开始......");
        if (Objects.nonNull(name)) {
            LogUtils.info("【zk分布式锁】新建临时序列节点创建成功,threadName=" + threadName + ",节点的路径path=" + path + ",路径名称name=" + name);
            pathName = name;
            //获取所有的子节点
            zk.getChildren("/", false, this, "ctx");

        }
    }

    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children) {
        if (CollectionUtils.isEmpty(children)) {
            LogUtils.warn("【zk分布式锁】path=" + path + "目录下无子节点");
            throw new NullPointerException();
        }
        LogUtils.info("【zk分布式锁】获取path=" + path + ",路径下所有节点开始");
        //为每个线程的节点children升序排序
        Collections.sort(children);
        //判断当前线程的路径是否是children的第一个如果是释放锁
        int i = children.indexOf(pathName.substring(1));
        if (i == 0) {
            //做可重入锁
            try {
                zk.setData("/",threadName.getBytes(),-1);
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            countDownLatch.countDown();
        } else {
            //监听前一个节点
            zk.exists("/" + children.get(i - 1), this, this, "ctx");
        }
    }


    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        //如果客户端挂掉处理
    }
}
