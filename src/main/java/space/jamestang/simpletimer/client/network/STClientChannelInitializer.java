package space.jamestang.simpletimer.client.network;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class STClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final int MAX_FRAME_LENGTH = 1024 * 1024 * 10; // 10 MB
    private static final boolean ENABLE_LOGGING = Boolean.parseBoolean(
        System.getProperty("st.client.logging.enabled", "false"));

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        var pipeline = ch.pipeline();
        
        // 可选的日志处理器 - 仅在调试时启用
        if (ENABLE_LOGGING) {
            pipeline.addLast("logging", new LoggingHandler(LogLevel.DEBUG));
        }
        
        // 帧解码器 - 处理粘包/拆包
        pipeline.addLast("frameDecoder", 
            new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, 0, 4, 0, 4));
        
        // 消息解码器
        pipeline.addLast("messageDecoder", new MessageDecoder());
        
        // 帧编码器
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        
        // 消息编码器
        pipeline.addLast("messageEncoder", new MessageEncoder());
        
        // 消息分发器
        pipeline.addLast("messageDispatcher", new MessageDispatcher());
    }
}
