package space.jamestang.simpletimer.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.nio.NioIoHandler;
import space.jamestang.simpletimer.client.handler.TaskHandlerPoll;
import space.jamestang.simpletimer.client.network.Message;
import space.jamestang.simpletimer.client.network.STClientChannelInitializer;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class STClient {
    private final IoHandlerFactory ioThreadFactory = NioIoHandler.newFactory();
    private final MultiThreadIoEventLoopGroup eventLoop = new MultiThreadIoEventLoopGroup(ioThreadFactory);
    private final Bootstrap client = new Bootstrap();
    private Channel channel;
    private ScheduledFuture<?> heartbeatTask;
    private final Logger logger = LoggerFactory.getLogger(STClient.class);
    private final String host;
    private final int port;


    public STClient(String host, int port) {
        client.group(eventLoop)
                .channel(NioSocketChannel.class)
                .handler(new STClientChannelInitializer())
                .option(ChannelOption.SO_KEEPALIVE, true);

        this.host = host;
        this.port = port;

        hookupShutdownHook();
    }

    /**
     * Starts the STClient and connects it to the specified host and port.
     */
    public void start() {
        logger.info("Starting STClient, attempting to connect to {}:{}", this.host, this.port);

        ChannelFuture connectFuture = client.connect(this.host, this.port);
        connectFuture.addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                channel = future.channel();
                logger.info("STClient connected successfully to {}:{}", this.host, this.port);

                // 启动心跳任务 - 初始延迟1秒，然后每20秒执行一次
                heartbeatTask = channel.eventLoop().scheduleAtFixedRate(
                        this::doHeartbeat,
                        1,
                        20,
                        TimeUnit.SECONDS
                );
                logger.info("Heartbeat task scheduled successfully");

                // 添加通道关闭监听器
                channel.closeFuture().addListener((ChannelFuture closeFuture) -> {
                    logger.warn("Channel closed, stopping heartbeat");
                    if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
                        heartbeatTask.cancel(false);
                    }
                });

            } else {
                logger.error("Failed to connect STClient to {}:{}, cause: {}",
                        this.host, this.port, future.cause().getMessage());
                eventLoop.shutdownGracefully();
            }
        });

        // 等待连接完成
        try {
            connectFuture.sync();
        } catch (InterruptedException e) {
            logger.error("Connection interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }


    public ChannelFuture scheduleAsync(String topic, long delay, Supplier<byte[]> payloadTransformer) {
        Objects.requireNonNull(channel, "Channel is not initialized. Please start the client first.");

        if (delay < 1000) {
            throw new IllegalArgumentException("Delay must be at least 1000 milliseconds");
        }
        var businessBytes = payloadTransformer.get();
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
        }
    }


    private void doHeartbeat() {
        if (channel == null || !channel.isActive()) {
            logger.warn("Channel is not active, skipping heartbeat");
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
            }
        });
    }


    private void hookupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down STClient...");
            try {
                eventLoop.shutdownGracefully();
                logger.info("STClient shutdown complete.");
            } catch (Exception e) {
                logger.error("Error during shutdown: {}", e.getMessage());
            }
        }));
    }


    public static void main(String[] args) throws InterruptedException {
        var instance = new STClient("localhost", 8080);
        TaskHandlerPoll.INSTANCE.registerHandler("test-topic", msg -> {
            System.out.println("Received message on topic: " + msg.topic());
            System.out.println("Message payload: " + new String(msg.payload()));
        });

        instance.start();
        Thread.sleep(3000);

        instance.schedule("test-topic", 5000, "Hello, World!"::getBytes);
    }

}
