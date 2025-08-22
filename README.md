# SimpleTimer-Client

SimpleTimer-Client 是 [SimpleTimer](https://github.com/James-Heller/SimpleTimer) 的官方 Java 客户端库。它用于与 SimpleTimer 服务端进行通信，实现定时任务的分发与处理。

## 特性
- 通过 Netty 实现高性能网络通信
- 支持定时任务的注册、分发与回调
- 简单易用的 API
- 可扩展的任务处理机制

## 安装

### 克隆仓库
```
git clone https://github.com/James-Heller/SimpleTimer-Client.git
```

### 构建项目
使用 Gradle 构建：
```
cd SimpleTimer-Client
./gradlew build
```

## 使用示例
```java
//构建一个客户端
var client = new STClient("localhost", 8080);

//注册一个用于处理某一话题的任务处理器，在定时结束后会调用该处理器
TaskHandlerPoll.INSTANCE.registerHandler("test-topic", msg -> {
    System.out.println("Received message on topic: " + msg.topic());
    System.out.println("Message payload: " + new String(msg.payload()));
});

//启动客户端并开始调度任务
/**
 *这里的 "test-topic" 是任务的主题，5000 是延迟时间（毫秒），
 * "Hello, World!" 是任务的负载。所有的负载都会被转换为字节数组发送到服务端。
 * 如果你对性能没有极致的要求，可以简单的使用 Jackson/FastJSON将数据转为JSON Bytes。
 * 定时器并不会对数据的格式做任何处理。
 * 
 */
client.start();
client.schedule("test-topic", 5000, "Hello, World!"::getBytes);
```

## 依赖
- Java 21+
- Netty

## 贡献
欢迎提交 issue 或 pull request。

## 协议
本项目采用 MIT 协议，详见 LICENSE 文件。

