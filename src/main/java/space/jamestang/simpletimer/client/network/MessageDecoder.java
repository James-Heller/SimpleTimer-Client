package space.jamestang.simpletimer.client.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MessageDecoder extends ByteToMessageDecoder {

    private final Logger logger = LoggerFactory.getLogger(MessageDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        logger.debug("Received data of length: {}", in.readableBytes());

        if (in.readableBytes() < 4){
            logger.warn("Not enough data to read the magic number, expected at least 4 bytes, but got: {}", in.readableBytes());
            return; // Not enough data to read the magic number
        }
        int magic = in.readInt();
        if (magic != 0x7355608) {
            logger.warn("Invalid magic number: {}, expected: 0x7355608", Integer.toHexString(magic));
            ctx.close(); // Close the connection if the magic number is invalid
            return;
        }

        int version = in.readInt();
        int type = in.readInt();
        int topicLength = in.readInt();
        byte[] topicBytes = new byte[topicLength];
        in.readBytes(topicBytes);
        int delay = in.readInt();
        int payloadLength = in.readableBytes();
        if (payloadLength < 0) {
            logger.warn("Payload length is negative: {}", payloadLength);
            ctx.close(); // Close the connection if the payload length is invalid
        }

        byte[] payload = new byte[payloadLength];
        in.readBytes(payload);

        Message message = new Message(magic, version, type, topicLength, new String(topicBytes), delay, payload);
        logger.debug("Decoded message: {}", message);
        out.add(message);
    }
}
