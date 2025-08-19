package space.jamestang.simpletimer.client.network;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;

public class TCPHelper {
    private static final int LENGTH_FIELD_SIZE = 4;
    private static final int PUBLIC_PARAMETER_SIZE = 24;

    public static void sendMessage(@NotNull DataOutputStream out, @NotNull Message msg){
        int msgLength = PUBLIC_PARAMETER_SIZE + msg.topicLength() + msg.payload().length;
        ByteBuffer buffer = ByteBuffer.allocateDirect(LENGTH_FIELD_SIZE + msgLength);
        buffer.putInt(msgLength);
        buffer.putInt(msg.magic());
        buffer.putInt(msg.version());
        buffer.putInt(msg.type());
        buffer.putInt(msg.topicLength());
        buffer.put(msg.topic().getBytes());
        buffer.putLong(msg.delay());
        buffer.put(msg.payload());

        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        try {
            out.write(bytes);
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message: " + e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    public static @NotNull Message readMessage(@NotNull DataInputStream in) throws Exception {
        int length = in.readInt();
        if (length < PUBLIC_PARAMETER_SIZE) {
            throw new IllegalArgumentException("Invalid message length: " + length);
        }

        int magic = in.readInt();
        int version = in.readInt();
        int type = in.readInt();
        int topicLength = in.readInt();

        byte[] topicBytes = new byte[topicLength];
        in.readFully(topicBytes);
        String topic = new String(topicBytes);

        long delay = in.readLong();

        byte[] payload = new byte[length - PUBLIC_PARAMETER_SIZE - topicLength];
        in.readFully(payload);

        return new Message(magic, version, type, topicLength, topic, delay, payload);
    }
}
