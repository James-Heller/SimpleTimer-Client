package space.jamestang.simpletimer.client.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDispatcher extends ChannelInboundHandlerAdapter {

    private final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof Message)){
            return;
        }

        var response = dispatch((Message) msg);
        if (response != null){
            ctx.writeAndFlush(response);
        }
    }


    /**
     * Dispatches the message to the appropriate handler based on its type.
     * @param message the message to dispatch
     * @return the acknowledgment message or null if no handler is found
     */
    private Message dispatch(Message message){

        switch (message.type()){
            case MessageType.PONG -> {
                logger.trace("Received PONG message: {}", message);
                return null;
            }

            case MessageType.TASK_RECEIVED -> {
                logger.debug("Timer has received the task {}", message);
                return null;
            }

            case MessageType.TASK_TRIGGERED -> {
                logger.info("Received TASK_TRIGGERED message: {}", message);
                return null;
            }


            default -> throw new IllegalArgumentException("Unknown message type: " + message.type());
        }
    }
}
