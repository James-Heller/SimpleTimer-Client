package space.jamestang.simpletimer.client.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.jamestang.simpletimer.client.handler.TaskHandlerPoll;

public class MessageDispatcher extends ChannelInboundHandlerAdapter {

    private final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof Message)){
            logger.warn("Received unexpected message type: {}", msg.getClass().getSimpleName());
            return;
        }

        try {
            var response = dispatch((Message) msg);
            if (response != null){
                ctx.writeAndFlush(response);
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage(), e);
            // 不关闭连接，只记录错误
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception caught in message dispatcher: {}", cause.getMessage(), cause);
        // 根据异常类型决定是否关闭连接
        if (cause instanceof java.io.IOException) {
            logger.info("IO exception, closing channel");
            ctx.close();
        }
    }


    /**
     * Dispatches the message to the appropriate handler based on its type.
     * @param message the message to dispatch
     * @return the acknowledgment message or null if no handler is found
     */
    private Message dispatch(Message message){
        if (message == null) {
            logger.warn("Received null message");
            return null;
        }

        logger.debug("Dispatching message: type={}, topic={}", message.type(), message.topic());

        try {
            switch (message.type()){
                case MessageType.PONG -> {
                    logger.trace("Received PONG message: {}", message.topic());
                    return null;
                }

                case MessageType.TASK_RECEIVED -> {
                    logger.debug("Timer has received the task for topic: {}", message.topic());
                    return null;
                }

                case MessageType.TASK_TRIGGERED -> {
                    logger.info("Received TASK_TRIGGERED message for topic: {}", message.topic());
                    try {
                        TaskHandlerPoll.INSTANCE.handle(message);
                    } catch (Exception e) {
                        logger.error("Error handling triggered task for topic '{}': {}", message.topic(), e.getMessage(), e);
                    }
                    return null;
                }

                default -> {
                    logger.warn("Unknown message type: {}", message.type());
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Error dispatching message: {}", e.getMessage(), e);
            return null;
        }
    }
}
