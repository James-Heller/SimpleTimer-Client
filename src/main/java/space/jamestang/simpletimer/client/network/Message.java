package space.jamestang.simpletimer.client.network;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
        return new Message(0x7355608, 1, 1, topic.length(), topic, 0, "PING".getBytes());
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
        if (delay < 1000) {
            throw new IllegalArgumentException("Delay must be at least 1000 milliseconds");
        }
        return new Message(0x7355608, 1, 2, topic.length(), topic, delay, payload);
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
