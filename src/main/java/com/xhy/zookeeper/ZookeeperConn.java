package com.xhy.zookeeper;

import com.xhy.utils.ZKUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * watch的监听只发生在读事件中
 * zk是有session的,没有连接池的概念
 */
public class ZookeeperConn {
    public static void main(String[] args) throws Exception {
        ZooKeeper zookeeper = ZKUtils.getZK();
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
        zookeeper.getData(path, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println("节点监听事件event:" + event);
                //添加重复注册
                try {
                    zookeeper.getData(path, this, stat);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, stat);
        return "data";
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
}
