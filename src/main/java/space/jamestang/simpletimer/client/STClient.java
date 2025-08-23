package space.jamestang.simpletimer.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.nio.NioIoHandler;
import space.jamestang.simpletimer.client.network.Message;
import space.jamestang.simpletimer.client.network.STClientChannelInitializer;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class STClient {
    private final IoHandlerFactory ioThreadFactory = NioIoHandler.newFactory();
    private final MultiThreadIoEventLoopGroup eventLoop = new MultiThreadIoEventLoopGroup(ioThreadFactory);
    private final Bootstrap client = new Bootstrap();
    private volatile Channel channel;
    private ScheduledFuture<?> heartbeatTask;
    private ScheduledFuture<?> reconnectTask;
    private final Logger logger = LoggerFactory.getLogger(STClient.class);
    
    // 配置参数
    private final STClientConfig config;
    
    // 连接状态管理
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);


    public STClient(String host, int port) {
        this(STClientConfig.builder(host, port).build());
    }
    
    public STClient(STClientConfig config) {
        this.config = config;
        client.group(eventLoop)
                .channel(NioSocketChannel.class)
                .handler(new STClientChannelInitializer())
                .option(ChannelOption.SO_KEEPALIVE, true);

        hookupShutdownHook();
    }

    /**
     * Starts the STClient and connects it to the specified host and port.
     */
    public void start() {
        if (isShuttingDown.get()) {
            logger.warn("Cannot start client - shutdown in progress");
            return;
        }
        
        logger.info("Starting STClient, attempting to connect to {}:{}", config.getHost(), config.getPort());
        connect();
    }
    
    /**
     * Attempts to connect to the server with retry logic
     */
    private void connect() {
        if (isShuttingDown.get()) {
            return;
        }
        
        ChannelFuture connectFuture = client.connect(config.getHost(), config.getPort());
        connectFuture.addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                handleConnectionSuccess(future.channel());
            } else {
                handleConnectionFailure(future.cause());
            }
        });
    }
    
    /**
     * Handles successful connection
     */
    private void handleConnectionSuccess(Channel newChannel) {
        this.channel = newChannel;
        isConnected.set(true);
        reconnectAttempts.set(0);
        
        logger.info("STClient connected successfully to {}:{}", config.getHost(), config.getPort());
        
        // 启动心跳任务
        startHeartbeat();
        
        // 添加通道关闭监听器
        channel.closeFuture().addListener((ChannelFuture closeFuture) -> handleConnectionLost());
    }
    
    /**
     * Handles connection failure
     */
    private void handleConnectionFailure(Throwable cause) {
        isConnected.set(false);
        int attempts = reconnectAttempts.incrementAndGet();
        
        logger.error("Failed to connect STClient to {}:{} (attempt {}), cause: {}",
                config.getHost(), config.getPort(), attempts, cause.getMessage());
        
        if (attempts >= config.getMaxReconnectAttempts()) {
            logger.error("Max reconnection attempts ({}) reached. Giving up.", config.getMaxReconnectAttempts());
            shutdown();
            return;
        }
        
        if (config.isAutoReconnect()) {
            scheduleReconnect();
        }
    }
    
    /**
     * Handles connection lost
     */
    private void handleConnectionLost() {
        if (isShuttingDown.get()) {
            return;
        }
        
        isConnected.set(false);
        logger.warn("Connection lost, attempting to reconnect...");
        
        stopHeartbeat();
        scheduleReconnect();
    }
    
    /**
     * Schedules a reconnection attempt with exponential backoff
     */
    private void scheduleReconnect() {
        if (isShuttingDown.get()) {
            return;
        }
        
        long delay = Math.min(config.getInitialReconnectDelay() * (1L << Math.min(reconnectAttempts.get() - 1, 6)), 
                             config.getMaxReconnectDelay());
        
        logger.info("Scheduling reconnection attempt in {} ms", delay);
        
        reconnectTask = eventLoop.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Starts the heartbeat task
     */
    private void startHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel(false);
        }
        
        heartbeatTask = channel.eventLoop().scheduleAtFixedRate(
                this::doHeartbeat,
                1,
                config.getHeartbeatInterval(),
                TimeUnit.SECONDS
        );
        logger.info("Heartbeat task scheduled successfully");
    }
    
    /**
     * Stops the heartbeat task
     */
    private void stopHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel(false);
            logger.debug("Heartbeat task stopped");
        }
    }


    public ChannelFuture scheduleAsync(String topic, long delay, Supplier<byte[]> payloadTransformer) {
        // 参数校验
        Objects.requireNonNull(topic, "Topic cannot be null");
        Objects.requireNonNull(payloadTransformer, "PayloadTransformer cannot be null");
        
        if (topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be empty");
        }
        
        if (delay < 1000) {
            throw new IllegalArgumentException("Delay must be at least 1000 milliseconds");
        }
        
        // 连接状态检查
        if (!isConnected.get() || channel == null || !channel.isActive()) {
            throw new IllegalStateException("Client is not connected. Please ensure the client is started and connected.");
        }
        
        byte[] businessBytes;
        try {
            businessBytes = payloadTransformer.get();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to generate payload: " + e.getMessage(), e);
        }
        
        if (businessBytes == null || businessBytes.length == 0) {
            throw new IllegalArgumentException("Payload must not be null or empty");
        }
        
        var bytes = Message.createSchedule(topic, delay, businessBytes);
        return channel.writeAndFlush(bytes);
    }

    public boolean schedule(String topic, long delay, Supplier<byte[]> payloadTransformer) {
        try {
            var future = scheduleAsync(topic, delay, payloadTransformer);
            future.sync();
            return future.isSuccess();
        } catch (InterruptedException e) {
            logger.error("Scheduling interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt(); // Restore the interrupted status
            return false;
        } catch (Exception e) {
            logger.error("Failed to schedule task: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查客户端是否已连接
     */
    public boolean isConnected() {
        return isConnected.get() && channel != null && channel.isActive();
    }
    
    /**
     * 优雅关闭客户端
     */
    public void shutdown() {
        if (isShuttingDown.compareAndSet(false, true)) {
            logger.info("Shutting down STClient...");
            
            // 取消重连任务
            if (reconnectTask != null && !reconnectTask.isCancelled()) {
                reconnectTask.cancel(false);
            }
            
            // 停止心跳
            stopHeartbeat();
            
            // 关闭连接
            if (channel != null && channel.isActive()) {
                channel.close().addListener(future -> {
                    eventLoop.shutdownGracefully();
                    logger.info("STClient shutdown complete.");
                });
            } else {
                eventLoop.shutdownGracefully();
                logger.info("STClient shutdown complete.");
            }
        }
    }


    private void doHeartbeat() {
        if (!isConnected.get() || channel == null || !channel.isActive()) {
            logger.debug("Channel is not active, skipping heartbeat");
            return;
        }

        logger.debug("Sending heartbeat...");
        var ping = Message.createPING("CLIENT-PING");
        var result = channel.writeAndFlush(ping);
        result.addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                logger.debug("Heartbeat sent successfully");
            } else {
                logger.error("Failed to send ping message with cause: {}", future.cause().getMessage());
                // 心跳失败可能意味着连接有问题，触发重连
                if (!isShuttingDown.get()) {
                    logger.warn("Heartbeat failed, connection may be lost");
                    handleConnectionLost();
                }
            }
        });
    }


    private void hookupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!isShuttingDown.get()) {
                shutdown();
            }
        }));
    }


//    public static void main(String[] args) throws InterruptedException {
//        var instance = new STClient("localhost", 8080);
//        TaskHandlerPoll.INSTANCE.registerHandler("test-topic", msg -> {
//            System.out.println("Received message on topic: " + msg.topic());
//            System.out.println("Message payload: " + new String(msg.payload()));
//        });
//
//        instance.start();
//        Thread.sleep(3000);
//
//        instance.schedule("test-topic", 5000, "Hello, World!"::getBytes);
//    }

}
