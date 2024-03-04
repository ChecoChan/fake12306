# 项目介绍
12306 铁路购票服务是与大家生活和出行相关的关键系统，包括会员、购票、订单、支付和网关等服务。

12306 项目中包含了缓存、消息队列、分库分表、设计模式等代码，通过这些代码可以全面了解分布式系统的核心知识点。

# 如何使用

## 克隆项目

```shell
git clone https://github.com/ChecoChan/fake12306.git
```
检查项目 SDK 的版本是否为 JDK 17，如果不是请选择电脑上的 JDK 版本。

```shell
mvn clean install
```
测试是否具备运行环境

## 安装中间件及数据库初始化

### Redis Latest

```shell
docker run -p 6379:6379 --name redis  -d redis redis-server --requirepass "123456"
```

### RocketMQ 4.5.1

- 安装 NameServer
  ```shell
  docker run -d -p 9876:9876 --name rmqnamesrv foxiswho/rocketmq:server-4.5.1
  ```
- 安装 Brocker
  - 新建配置目录
    ```shell
    mkdir -p ${HOME}/docker/software/rocketmq/conf
    ```
  - 新建配置文件 broker.conf
    ```
    brokerClusterName = DefaultCluster
    brokerName = broker-a
    brokerId = 0
    deleteWhen = 04
    fileReservedTime = 48
    brokerRole = ASYNC_MASTER
    flushDiskType = ASYNC_FLUSH
    # 此处为本地ip, 如果部署服务器, 需要填写服务器外网ip
    brokerIP1 = xx.xx.xx.xx
    ```
  - 创建容器
    ```shell
    docker run -d \
    -p 10911:10911 \
    -p 10909:10909 \
    --name rmqbroker \
    --link rmqnamesrv:namesrv \
    -v ${HOME}/docker/software/rocketmq/conf/broker.conf:/etc/rocketmq/broker.conf \
    -e "NAMESRV_ADDR=namesrv:9876" \
    -e "JAVA_OPTS=-Duser.home=/opt" \
    -e "JAVA_OPT_EXT=-server -Xms512m -Xmx512m" \
    foxiswho/rocketmq:broker-4.5.1
    ```
  - 安装 rocketmq 控制台
    ```shell
    docker pull pangliang/rocketmq-console-ng
    docker run -d \
    --link rmqnamesrv:namesrv \
    -e "JAVA_OPTS=-Drocketmq.config.namesrvAddr=namesrv:9876 -Drocketmq.config.isVIPChannel=false" \
    --name rmqconsole \
    -p 8088:8080 \
    -t pangliang/rocketmq-console-ng
    ```

运行成功，稍等几秒启动时间，浏览器输入 localhost:8088 查看

### Nacos 2.1.2

```shell
docker run \
-d -p 8848:8848 \
-p 9848:9848 \
--name nacos2 \
-e MODE=standalone \
-e TIME_ZONE='Asia/Shanghai' \
nacos/nacos-server:v2.1.2
```

### MySQL 5.7.36
- Windows、Linux 以及 Mac M1 以下电脑
    ```shell
    docker run --name mysql \
    -p 3306:3306 \
    -e MYSQL_ROOT_HOST='%' \
    -e MYSQL_ROOT_PASSWORD=root \
    -d mysql:5.7.36
    ```
- Mac M1 及以上电脑
    ```shell
    docker run --name mysql \
    --platform=linux/amd64 \
    -p 3306:3306 \
    -e MYSQL_ROOT_HOST='%' \
    -e MYSQL_ROOT_PASSWORD=root \
    -d amd64/mysql:5.7.36
    ```
  
### 数据库初始化
- SpringBoot 聚合模式
  - MySQL 数据库中创建新的 DB，名称为 12306
    ```sql
    CREATE DATABASE /*!32312 IF NOT EXISTS*/ `12306` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;
    ```
  - 创建好数据库后，进入 12306 数据库中，导入项目中下述 SQL 语句
    ```
    resources/db/12306-springboot.sql
    resources/data/12306-springboot.sql
    ```
- SpringCloud 分布式模式
  - MySQL 数据库中创建多个12306 业务相关 DB
    ```sql
    CREATE DATABASE /*!32312 IF NOT EXISTS*/ `12306_ticket` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

    CREATE DATABASE /*!32312 IF NOT EXISTS*/ `12306_order_0` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;
    CREATE DATABASE /*!32312 IF NOT EXISTS*/ `12306_order_1` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

    CREATE DATABASE /*!32312 IF NOT EXISTS*/ `12306_pay_0` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;
    CREATE DATABASE /*!32312 IF NOT EXISTS*/ `12306_pay_1` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

    CREATE DATABASE /*!32312 IF NOT EXISTS*/ `12306_user_0` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;
    CREATE DATABASE /*!32312 IF NOT EXISTS*/ `12306_user_1` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;
    ```
  - 12306_ticket 数据库导入项目中下述建表 SQL 语句
    ```
    resources/db/12306-springcloud-ticket.sql
    resources/data/12306-springcloud-ticket.sql
    ```
  - 进入 12306_user_0 数据库导入项目中下述建表 SQL 语句，该 SQL 文件包含了 12306_user_0 和 1 两个数据库的数据
    ```
    resources/db/12306-springcloud-user.sql
    resources/data/12306-springcloud-user.sql
    ```
  - 进入 12306_pay_0 数据库导入项目中下述建表 SQL 语句，该 SQL 文件包含了 12306_pay_0 和 1 两个数据库的数据
    ```
    resources/db/12306-springcloud-pay.sql
    ```
  - 进入 12306_order_0 数据库导入项目中下述建表 SQL 语句，该 SQL 文件包含了 12306_order_0 和 1 两个数据库的数据
    ```
    resources/db/12306-springcloud-order.sql
    ```

## 启动后端项目
### SpringBoot 模式
启动 Aggregation Service 和 GateWay Service 即可。
### SpringCloud 模式
配置中默认是 SpringBoot 单体模式，如果是以分布式方式启动，需要修改网关服务、订单服务、支付服务、购票服务、用户服务的 application.yaml 配置文件中的属性。
将 `spring.profiles.active: aggregation` 改为 `spring.profiles.active: dev`。然后依次启动各个微服务即可。

## 启动前端项目
- 安装 Node.js
- 进入 fake12306/console-vue 目录执行通过终端工具依次执行下述命令
  ```shell
  npm install -g yarn
  yarn install
  yarn serve
  ```
首页查询车次信息是不涉及登录的，但是如果想体验购票，需要用户登录操作，默认用户名和密码：admin/admin123456
  
