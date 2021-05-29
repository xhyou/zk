**存储位置**:

zookeeper保存在内存中,日志保存在磁盘中.

**特点**:

写只能发生在leader身上,如果leader挂掉了,大概200ms恢复

**连接**:

每个客户端与service连接必须通过session(描述client是谁,目的是做事件通知)

**结构:**

文件结构,使用节点存储数据,数据允许最大1M(保证对外提供数据快)

节点(持久节点,临时节点(session),序列节点)

![image-20210523201903704](C:\Users\xuehy\AppData\Roaming\Typora\typora-user-images\image-20210523201903704.png)

**特性**

顺序一致性:客户端更新按发送顺序应用

原子性:要么全更新成功或者失败,没有部分结果

统一视图:无论客户端连接到哪个服务器,客户端都将看到相同的服务视图

可靠性:一旦更新了应用,所有客户端都能覆盖更新

及时性:最终一致性(保证最终的结果都是最新的)



**安装环境**

```shell
准备node1~node4节点
安装jdk并且设置环境变量 /etc/profile
安装zookeeper并且设置环境变量 /opt/zk
export ZOOKEEPER_HOME=/opt/zk/zookeeper-3.4.6
export PATH=$ZOOKEEPER_HOME/bin
cd zookeeper/conf
cp zoo.sem*.cfg zoo.cfg
vi zoo.cfg
   datadir = /var/zk
   server1=node1:2888:3888
   server2=node2:2888:3888
   server3=node3:2888:3888
   server4=node4:2888:3888
mkdir -p /var/zk
echo 1 > /var/zk/myid
cd /opt && scp -r /zk node2:`pwd`
相继创建node3 node4
按顺序启动node1~4
zkSever.sh start-foreground

-- 查看端口
netstat natp | egrep '(2888|3888)' -- 2888 leader接受write请求 3888 选主投票用的
```

**连接**

```shell
zkCli.sh --默认连接本机客户端
create path data --创建节点
create /ooxx "data"
create -e /ooxx "data" --创建临时节点
create -s /ooxx "data" --创建序列节点(应用场景:分布式锁)
get /ooxx --获取节点内容
get -s /node1 -- 获取节点属性
set /ooxx "hello" --设置节点
```

![image-20210524220508339](C:\Users\xuehy\AppData\Roaming\Typora\typora-user-images\image-20210524220508339.png)

![image-20210524220914736](C:\Users\xuehy\AppData\Roaming\Typora\typora-user-images\image-20210524220914736.png)

**paxios**

> 提供一致性的算法

**zab**

> 原子广播协议 :(原子) 要么成功要么失败,(广播)分布式多节点全部知道。



**选举过程**

![image-20210526213644059](C:\Users\xuehy\AppData\Roaming\Typora\typora-user-images\image-20210526213644059.png)

**watch**

> Znode发生变化（Znode本身的增加，删除，修改，以及子Znode的变化）可以通过Watch机制通知到客户端。

![image-20210527171036838](C:\Users\xuehy\AppData\Roaming\Typora\typora-user-images\image-20210527171036838.png)

**分布式锁**
> 在同一时间只能有一个人获取一把锁.