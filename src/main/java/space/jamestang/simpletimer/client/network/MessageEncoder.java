package space.jamestang.simpletimer.client.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {

        out.writeInt(msg.magic()); // Write magic number
        out.writeInt(msg.version()); // Write version
        out.writeInt(msg.type()); // Write type
        out.writeInt(msg.topicLength()); // Write topic length
        out.writeBytes(msg.topic().getBytes()); // Write topic bytes
        out.writeLong(msg.delay()); // Write delay
        out.writeBytes(msg.payload()); // Write payload bytes

    }
}
