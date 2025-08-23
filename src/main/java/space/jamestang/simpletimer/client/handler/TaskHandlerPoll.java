package space.jamestang.simpletimer.client.handler;

import space.jamestang.simpletimer.client.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TaskHandlerPoll {
    public static final TaskHandlerPoll INSTANCE = new TaskHandlerPoll();
    
    private static final Logger logger = LoggerFactory.getLogger(TaskHandlerPoll.class);

    private final ConcurrentMap<String, TaskTriggeredHandler> handlers = new ConcurrentHashMap<>();

    private TaskHandlerPoll() {
        // Private constructor to prevent instantiation
    }

    public void registerHandler(String topic, TaskTriggeredHandler handler) {
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic must not be null or empty");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Handler must not be null");
        }
        
        TaskTriggeredHandler oldHandler = handlers.put(topic, handler);
        if (oldHandler != null) {
            logger.warn("Replaced existing handler for topic: {}", topic);
        } else {
            logger.info("Registered handler for topic: {}", topic);
        }
    }
    
    public boolean unregisterHandler(String topic) {
        if (topic == null) {
            return false;
        }
        
        TaskTriggeredHandler removed = handlers.remove(topic);
        if (removed != null) {
            logger.info("Unregistered handler for topic: {}", topic);
            return true;
        }
        return false;
    }
    
    public boolean hasHandler(String topic) {
        return topic != null && handlers.containsKey(topic);
    }

    public void handle(Message msg){
        if (msg == null || msg.topic() == null) {
            logger.error("Message and its topic must not be null");
            return;
        }

        TaskTriggeredHandler handler = handlers.get(msg.topic());
        if (handler != null) {
            try {
                handler.handle(msg);
                logger.debug("Successfully handled message for topic: {}", msg.topic());
            } catch (Exception e) {
                logger.error("Error handling message for topic '{}': {}", msg.topic(), e.getMessage(), e);
            }
        } else {
            logger.warn("No handler registered for topic: {}. Available topics: {}", 
                       msg.topic(), handlers.keySet());
        }
    }
}
