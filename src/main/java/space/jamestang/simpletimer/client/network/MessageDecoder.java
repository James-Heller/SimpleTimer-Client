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

        // 标记读取位置，以便在数据不完整时重置
        in.markReaderIndex();
        
        try {
            // 检查最小消息长度 (magic + version + type + topicLength + delay = 24 bytes)
            if (in.readableBytes() < 24) {
                logger.debug("Not enough data for minimum message header, expected at least 24 bytes, but got: {}", in.readableBytes());
                in.resetReaderIndex();
                return;
            }
            
            int magic = in.readInt();
            if (magic != 0x7355608) {
                logger.error("Invalid magic number: 0x{}, expected: 0x7355608. Closing connection.", Integer.toHexString(magic));
                ctx.close();
                return;
            }

            int version = in.readInt();
            int type = in.readInt();
            int topicLength = in.readInt();
            
            // 验证topic长度的合理性
            if (topicLength < 0 || topicLength > 1024) { // 限制topic最大长度为1KB
                logger.error("Invalid topic length: {}, must be between 0 and 1024. Closing connection.", topicLength);
                ctx.close();
                return;
            }
            
            // 检查是否有足够的数据读取topic和delay
            if (in.readableBytes() < topicLength + 8) { // 8 bytes for delay
                logger.debug("Not enough data for topic and delay, expected: {}, available: {}", 
                           topicLength + 8, in.readableBytes());
                in.resetReaderIndex();
                return;
            }
            
            byte[] topicBytes = new byte[topicLength];
            in.readBytes(topicBytes);
            long delay = in.readLong();
            
            // 读取剩余的payload
            int payloadLength = in.readableBytes();
            if (payloadLength < 0) {
                logger.error("Negative payload length: {}. Closing connection.", payloadLength);
                ctx.close();
                return;
            }

            byte[] payload = new byte[payloadLength];
            in.readBytes(payload);

            String topic = new String(topicBytes, java.nio.charset.StandardCharsets.UTF_8);
            Message message = new Message(magic, version, type, topicLength, topic, delay, payload);
            logger.debug("Decoded message: {}", message);
            out.add(message);
            
        } catch (Exception e) {
            logger.error("Error decoding message: {}", e.getMessage(), e);
            in.resetReaderIndex();
            ctx.close();
        }
    }
}
