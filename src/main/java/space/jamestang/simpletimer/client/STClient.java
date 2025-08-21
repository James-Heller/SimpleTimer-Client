package space.jamestang.simpletimer.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.nio.NioIoHandler;
import space.jamestang.simpletimer.client.network.Message;
import space.jamestang.simpletimer.client.network.STClientChannelInitializer;
import space.jamestang.simpletimer.client.utils.Serializer;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class STClient {
    private final IoHandlerFactory ioThreadFactory = NioIoHandler.newFactory();
    private final MultiThreadIoEventLoopGroup eventLoop = new MultiThreadIoEventLoopGroup(ioThreadFactory);
    private final Bootstrap client = new Bootstrap();
    private Channel channel;
    private final Logger logger = LoggerFactory.getLogger(STClient.class);
    private final String host;
    private final int port;
    private final String topic;


    public STClient(String host, int port, String topic) {
        client.group(eventLoop)
                .channel(NioSocketChannel.class)
                .handler(new STClientChannelInitializer())
                .option(ChannelOption.SO_KEEPALIVE, true);

        this.host = host;
        this.port = port;
        this.topic = topic;

        hookupShutdownHook();
    }

    /**
     * Starts the STClient and connects it to the specified host and port.
     */
    public void start() {

        client.connect(this.host, this.port).addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                channel = future.channel();
                logger.info("STClient connected successfully to {}:{}", this.host, this.port);

                channel.eventLoop().scheduleAtFixedRate(this::doHeartbeat, 0, 20, TimeUnit.SECONDS);
            } else {
                logger.error("Failed to connect STClient: {}", future.cause().getMessage());
                eventLoop.shutdownGracefully();
            }
        });
    }


    public ChannelFuture scheduleAsync(long delay, Object payload) {
        Objects.requireNonNull(channel, "Channel is not initialized. Please start the client first.");

        if (delay < 1000) {
            throw new IllegalArgumentException("Delay must be at least 1000 milliseconds");
        }

        var bytes = Serializer.INSTANCE.toBytes(payload);
        return channel.writeAndFlush(bytes);
    }

    public boolean schedule(long delay, Object payload) {
        try {
            var future = scheduleAsync(delay, payload);
            future.sync();
            return future.isSuccess();
        } catch (InterruptedException e) {
            logger.error("Scheduling interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt(); // Restore the interrupted status
            return false;
        }
    }



    private void doHeartbeat(){
        var ping = Message.createPING(this.topic);
        var result = channel.writeAndFlush(ping);
        if (!result.isSuccess()){
            logger.error("Failed to sent ping message with cause: {}", result.cause().getMessage());
        }
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

    public static void main(String[] args) throws IOException {
        var instance = new STClient("localhost", 8080, "test-topic");
        instance.start();
    }

}
