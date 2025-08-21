package space.jamestang.simpletimer.client.network;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class STClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final int MAX_FRAME_LENGTH = 1024 * 1024 * 10; // 1 MB

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new LoggingHandler(LogLevel.TRACE))
                .addLast(new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, 0, 4, 0, 4))
                .addLast(new MessageDecoder())
                .addLast(new LengthFieldPrepender(4))
                .addLast(new MessageEncoder())
                .addLast(new MessageDispatcher());
    }
}
