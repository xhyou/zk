package com.xhy.zookeeper;

import com.xhy.constants.ZookeeperConstants;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.proto.WatcherEvent;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * watch的监听只发生在读事件中
 * zk是有session的,没有连接池的概念
 */
public class ZookeeperConn {
    public static void main(String[] args) throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        ZooKeeper zookeeper = getZookeeper(buildIpAddress(), 3000, countDownLatch);
        //等待zk的正常连接
        waitUntilConnected(zookeeper, countDownLatch);
        ZooKeeper.States zkState = zookeeper.getState();
        System.out.println("zk current state:" + zkState);
        //创建EPHEMERAL(临时)节点,随session的断开而销毁
        String path = syncCreateNode(zookeeper, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        System.out.println(path);
        //获取数据并且监听事件
        String data = getSyncAndWatchData(path, true, zookeeper);
        System.out.println("返回的数据:" + data);
        //第一次修改数据,触发回调
        Stat stat = setSimpleData(path, zookeeper, 0);
        System.out.println("元数据信息czXid=" + stat.getCzxid());
        //第二次修改数据,触发回调
        Stat secondStat = setSimpleData(path, zookeeper, stat.getVersion());
        System.out.println("元数据信息=" + secondStat.toString());
        //异步回调

        System.out.println("========zk执行异步回调获取数据开始======");
        getAsyncDataAndWatch(path, zookeeper);
        System.out.println("========zk执行异步回调获取数据结束======");

        System.out.println("========zk执行异步回调创建数据开始======");
        asyncNodeByStringCallback(zookeeper, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        System.out.println("========zk执行异步回调创建数据结束======");
        Thread.sleep(222222);
    }

    /**
     * 简单的数据修改
     *
     * @param path
     * @param zookeeper
     * @throws Exception
     */
    public static Stat setSimpleData(String path, ZooKeeper zookeeper, int version) throws Exception {
        Stat stat = zookeeper.setData(path, "modifyZk".getBytes(), version);
        return stat;
    }


    /**
     * 异步回调并且监听
     *
     * @param path
     * @param zookeeper
     * @return
     * @throws Exception
     */
    public static void getAsyncDataAndWatch(final String path, final ZooKeeper zookeeper) throws Exception {
        zookeeper.getData(path, true, new AsyncCallback.DataCallback() {
            /**
             *
             * @param rc   返回的状态码
             * @param path 路径
             * @param ctx  内容
             * @param data 返回的内容
             * @param stat 元数据
             */
            @Override
            public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
                System.out.println("异步调用获取数据返回的状态码:" + rc + ",返回的路径:" + path + ",自定义的内容:" + ctx + ",返回的数据" + new String(data) + "返回的元数据信息:" + stat.getCzxid());
            }
        }, "hello");
    }

    /**
     * stat 放节点的元数据:如事务id
     *
     * @param path
     * @param isWatch
     * @param zookeeper
     * @return
     */
    public static String getSyncAndWatchData(final String path, boolean isWatch, final ZooKeeper zookeeper) throws Exception {
        final Stat stat = new Stat();
        if (!isWatch) {
            byte[] data = zookeeper.getData(path, false, stat);
            return new String(data);
        }
        Watcher watcher = new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println("节点监听事件event:" + event);
                //添加重复注册
                try {
                    zookeeper.getData(path, this, stat);
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        byte[] data = zookeeper.getData(path, watcher, stat);
        return new String(data);
    }

    /**
     * 同步创建节点，根据不同的类型创建节点
     * 如临时节点还是持久节点
     *
     * @param zookeeper
     * @param acl        OPEN_ACL_UNSAFE 无权限控制
     * @param createMode
     * @return
     */
    public static String syncCreateNode(ZooKeeper zookeeper, List<ACL> acl, CreateMode createMode) throws KeeperException, InterruptedException {
        String path = zookeeper.create("/node01", "zookeeper01".getBytes(), acl, createMode);
        return path;
    }

    /**
     * 异步创建节点
     *
     * @param zookeeper
     * @param acl
     * @param createMode
     * @throws Exception
     */
    public static void asyncNodeByCreate2Callback(ZooKeeper zookeeper, List<ACL> acl, CreateMode createMode) throws Exception {
        zookeeper.create("/node2", "zookeeper02".getBytes(), acl, createMode, new AsyncCallback.Create2Callback() {
            @Override
            public void processResult(int rc, String path, Object ctx, String name, Stat stat) {
                System.out.println("异步调用Create2Callback创建数据返回的状态码:" + rc + ",返回的路径:" + path + ",自定义的内容:" + ctx + ",返回的元数据:" + stat);

            }
        }, "Create2Callback");
    }

    /**
     * 异步创建节点
     *
     * @param zookeeper
     * @param acl
     * @param createMode
     * @throws Exception
     */
    public static void asyncNodeByStringCallback(final ZooKeeper zookeeper, List<ACL> acl, CreateMode createMode) throws Exception {
        zookeeper.create("/node2", "zookeeper02".getBytes(), acl, createMode, new AsyncCallback.StringCallback() {

            @Override
            public void processResult(int rc, String path, Object ctx, String name) {
                switch (KeeperException.Code.get(rc)) {
                    case OK:
                        System.out.println("异步调用StringCallback创建数据返回的状态码:" + rc + ",返回的路径:" + path + ",自定义的内容:" + ctx);
                        break;
                    case CONNECTIONLOSS:
                        // 连接丢失，重新发布命令
                        try {
                            zookeeper.create(path, ctx.toString().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                        } catch (KeeperException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return;
                    default:
                        KeeperException e = KeeperException.create(KeeperException.Code.get(rc), path);

                }
            }
        }, "zookeeper02".getBytes());
    }

    /**
     * 连接zk
     *
     * @param address        zk的连接地址
     * @param sessionTimeOut 超时时间,超过多久zk连接session断开
     * @param countDownLatch
     * @return
     */
    public static ZooKeeper getZookeeper(String address, int sessionTimeOut, final CountDownLatch countDownLatch) throws Exception {
        ZooKeeper zooKeeper = new ZooKeeper(address, sessionTimeOut, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println("zk监听:" + event);
                System.out.println("zk的路径:" + event.getPath());
                switch (event.getState()) {
                    case Unknown:
                        System.out.println("Unknown");
                        break;
                    case Disconnected:
                        System.out.println("Disconnected");
                        break;
                    case NoSyncConnected:
                        System.out.println("NoSyncConnected");
                        break;
                    case SyncConnected:
                        //zk连接同步进行
                        countDownLatch.countDown();
                        System.out.println("SyncConnected");
                        break;
                    case AuthFailed:
                        System.out.println("AuthFailed");
                        break;
                    case ConnectedReadOnly:
                        System.out.println("ConnectedReadOnly");
                        break;
                    case SaslAuthenticated:
                        System.out.println("SaslAuthenticated");
                        break;
                    case Expired:
                        System.out.println("Expired");
                        break;
                    case Closed:
                        System.out.println("Closed");
                        break;
                }
                switch (event.getType()) {
                    case None:
                        System.out.println("None");
                        break;
                    case NodeCreated:
                        System.out.println("NodeCreated");
                        break;
                    case NodeDeleted:
                        System.out.println("NodeDeleted");
                        break;
                    case NodeDataChanged:
                        System.out.println("NodeDataChanged");
                        break;
                    case NodeChildrenChanged:
                        System.out.println("NodeChildrenChanged");
                        break;
                    case DataWatchRemoved:
                        System.out.println("DataWatchRemoved");
                        break;
                    case ChildWatchRemoved:
                        System.out.println("ChildWatchRemoved");
                        break;
                    case PersistentWatchRemoved:
                        System.out.println("PersistentWatchRemoved");
                        break;
                }
                WatcherEvent wrapper = event.getWrapper();
                System.out.println("zk Wrapper:" + wrapper);
            }
        });
        return zooKeeper;
    }

    /**
     * 检验是否连接上zk
     *
     * @param zooKeeper
     * @param connectedLatch
     */
    public static void waitUntilConnected(ZooKeeper zooKeeper, CountDownLatch connectedLatch) {
        if (ZooKeeper.States.CONNECTING == zooKeeper.getState()) {
            try {
                connectedLatch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * 构建四台服务器的地址端口
     * 如：192.168.163.66:2181,192.168.163.67:2181,192.168.163.68:2181,192.168.163.69:2181
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
        System.out.println("zk连接地址:" + stringBuffer.toString());
        return stringBuffer.toString();
    }
}
