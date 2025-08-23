import space.jamestang.simpletimer.client.STClient;
import space.jamestang.simpletimer.client.STClientConfig;
import space.jamestang.simpletimer.client.handler.TaskHandlerPoll;

/**
 * 使用示例和最佳实践
 */
public class STClientExample {
    
    public static void main(String[] args) {
        // 基本使用方式
        basicUsage();
        
        // 高级配置使用方式
        // advancedUsage();
    }
    
    /**
     * 基本使用示例
     */
    public static void basicUsage() {
        // 创建客户端 - 使用默认配置
        STClient client = new STClient("localhost", 8080);
        
        // 注册消息处理器
        TaskHandlerPoll.INSTANCE.registerHandler("user-notification", msg -> {
            System.out.println("用户通知: " + new String(msg.payload()));
            // 处理业务逻辑...
        });
        
        TaskHandlerPoll.INSTANCE.registerHandler("order-timeout", msg -> {
            System.out.println("订单超时处理: " + msg.topic());
            // 处理订单超时逻辑...
        });
        
        // 启动客户端
        client.start();
        
        // 等待连接建立
        while (!client.isConnected()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 调度任务
        if (client.isConnected()) {
            // 5秒后发送用户通知
            boolean success = client.schedule("user-notification", 5000,
                    "您的订单已确认"::getBytes);
            
            if (success) {
                System.out.println("任务调度成功");
            } else {
                System.out.println("任务调度失败");
            }
        }
        
        // 程序结束时关闭客户端
        Runtime.getRuntime().addShutdownHook(new Thread(client::shutdown));
    }
    
    /**
     * 高级配置使用示例
     */
    public static void advancedUsage() {
        // 自定义配置
        STClientConfig config = STClientConfig.builder("localhost", 8080)
                .maxReconnectAttempts(5)          // 最大重连次数
                .initialReconnectDelay(2000)      // 初始重连延迟2秒
                .maxReconnectDelay(30000)         // 最大重连延迟30秒
                .heartbeatInterval(15)            // 心跳间隔15秒
                .autoReconnect(true)              // 启用自动重连
                .build();
        
        // 使用配置创建客户端
        STClient client = new STClient(config);
        
        // 注册错误处理器
        TaskHandlerPoll.INSTANCE.registerHandler("error-topic", msg -> {
            try {
                // 业务处理逻辑
                processMessage(msg.payload());
            } catch (Exception e) {
                System.err.println("处理消息时发生错误: " + e.getMessage());
                // 记录错误日志或进行其他错误处理
            }
        });
        
        // 启动客户端
        client.start();
        
        // 异步调度任务
        client.scheduleAsync("async-task", 10000, () -> {
            // 可以是复杂的数据序列化
            return createComplexPayload();
        }).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("异步任务调度成功");
            } else {
                System.err.println("异步任务调度失败: " + future.cause().getMessage());
            }
        });
    }
    
    private static void processMessage(byte[] payload) {
        // 模拟业务处理
        String message = new String(payload);
        System.out.println("处理消息: " + message);
        
        // 可能抛出异常的业务逻辑
        if (message.contains("error")) {
            throw new RuntimeException("业务处理异常");
        }
    }
    
    private static byte[] createComplexPayload() {
        // 示例：创建JSON格式的复杂数据
        String json = "{\"userId\":12345,\"action\":\"login\",\"timestamp\":" + System.currentTimeMillis() + "}";
        return json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
