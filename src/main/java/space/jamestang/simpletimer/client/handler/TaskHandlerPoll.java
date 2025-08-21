package space.jamestang.simpletimer.client.handler;

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

    public void poll() {
        // Polling logic for task handlers
        // This method can be used to check for new tasks or updates
    }
}
