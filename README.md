# SimpleTimer-Client

SimpleTimer-Client 是 [SimpleTimer](https://github.com/James-Heller/SimpleTimer) 的官方 Java 客户端库。用于与 SimpleTimer 服务端进行高性能定时任务通信，支持任务分发、回调和自动重连。

## 特性
- 基于 Netty 实现高性能异步网络通信
- 支持定时任务注册、分发、回调
- 自动重连与心跳机制，连接更可靠
- 简单易用且可扩展的 API
- 线程安全的处理器注册与分发
- 灵活的配置系统，适合生产环境

## 安装

### 克隆仓库
```shell
git clone https://github.com/James-Heller/SimpleTimer-Client.git
```

### 构建项目
使用 Gradle 构建：
```shell
cd SimpleTimer-Client
./gradlew build
```

## 依赖
- Java 21+
- Netty 4.2.4
- SLF4J + Logback

## 快速开始

### 基本用法
```java
STClient client = new STClient("localhost", 8080);
TaskHandlerPoll.INSTANCE.registerHandler("user-notification", msg -> {
    System.out.println("用户通知: " + new String(msg.payload()));
});
client.start();
while (!client.isConnected()) { Thread.sleep(100); }
client.schedule("user-notification", 5000, () -> "您的订单已确认".getBytes());
Runtime.getRuntime().addShutdownHook(new Thread(client::shutdown));
```

### 高级配置
```java
STClientConfig config = STClientConfig.builder("localhost", 8080)
    .maxReconnectAttempts(5)
    .initialReconnectDelay(2000)
    .maxReconnectDelay(30000)
    .heartbeatInterval(15)
    .autoReconnect(true)
    .build();
STClient client = new STClient(config);
```

### 异步任务调度
```java
client.scheduleAsync("async-task", 10000, () -> "异步数据".getBytes())
    .addListener(future -> {
        if (future.isSuccess()) {
            System.out.println("异步任务调度成功");
        } else {
            System.err.println("异步任务调度失败: " + future.cause().getMessage());
        }
    });
```

### 处理器管理
```java
TaskHandlerPoll.INSTANCE.registerHandler("topic", handler);
TaskHandlerPoll.INSTANCE.unregisterHandler("topic");
boolean exists = TaskHandlerPoll.INSTANCE.hasHandler("topic");
```

## 主要优化亮点
- 自动重连与心跳机制，连接更稳定
- 线程安全的处理器注册与分发
- 灵活的配置系统，支持生产环境参数
- 优雅关闭与资源管理
- 完善的错误处理与日志

## 贡献
欢迎提交 issue 或 pull request。

## 协议
本项目采用 MIT 协议，详见 LICENSE 文件。

