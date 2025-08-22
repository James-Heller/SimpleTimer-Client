package space.jamestang.simpletimer.client.handler;

import space.jamestang.simpletimer.client.network.Message;

import java.util.HashMap;

public class TaskHandlerPoll {
    public static final TaskHandlerPoll INSTANCE = new TaskHandlerPoll();

    private final HashMap<String, TaskTriggeredHandler> handlers = new HashMap<>();

    private TaskHandlerPoll() {
        // Private constructor to prevent instantiation
    }

    public void registerHandler(String topic, TaskTriggeredHandler handler) {
        if (topic == null || handler == null) {
            throw new IllegalArgumentException("Topic and handler must not be null");
        }
        handlers.put(topic, handler);
    }

    public void handle(Message msg){
        if (msg == null || msg.topic() == null) {
            throw new IllegalArgumentException("Message and its topic must not be null");
        }

        TaskTriggeredHandler handler = handlers.get(msg.topic());
        if (handler != null) {
            handler.handle(msg);
        } else {
            throw new IllegalStateException("No handler registered for topic: " + msg.topic());
        }
    }
}
