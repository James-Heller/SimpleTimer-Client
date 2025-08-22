package space.jamestang.simpletimer.client.network;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * This record class is only for the timer client to send messages to the server.
 * the payload is only meant to be a byte array, which can be any data.
 */
public record Message(
        int magic,
        int version,
        int type,
        int topicLength,
        String topic,
        long delay,
        byte[] payload
) {

    @Contract("!null -> new")
    public static @NotNull Message createPING(String topic){
        return createMessage(topic, 0, "PING".getBytes(), 1);
    }

    /**
     * Creates a message to schedule a task.
     * The delay must be at least 1000 milliseconds.
     * @param topic the topic to schedule the task on
     * @param delay the delay in milliseconds
     * @param payload The Timer does not concern about the payload exactly, So it can be any byte array.
     * @return a new Message instance representing the scheduled task
     */
    @Contract("!null,_,!null -> new")
    public static @NotNull Message createSchedule(String topic, long delay, byte[] payload) {
        return createMessage(topic, delay, payload, 2);
    }


    private static @NotNull Message createMessage(String topic, long delay, byte[] payload, int type) {
        if (delay < 1000 && type != 1) {
            throw new IllegalArgumentException("Delay must be at least 1000 milliseconds");
        }
        return new Message(0x7355608, 1, type, topic.length(), topic, delay, payload);
    }

    @Override
    public @NotNull String toString() {
        return "Message{" +
                "magic=" + magic +
                ", version=" + version +
                ", type=" + type +
                ", topicLength=" + topicLength +
                ", topic='" + topic + '\'' +
                ", delay=" + delay +
                ", payload=" + new String(payload) +
                '}';
    }
}
