package space.jamestang.simpletimer.client.network;

public class MessageType {
    public static final int PING = 0x01;
    public static final int PONG = 0x01;
    public static final int SCHEDULE_TASK = 0x02;
    public static final int TASK_RECEIVED = 0x02;
    public static final int TASK_TRIGGERED = 0x03;
}
