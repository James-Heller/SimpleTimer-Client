package space.jamestang.simpletimer.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.jamestang.simpletimer.client.network.Message;
import space.jamestang.simpletimer.client.network.TCPHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class STClient {

    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final String topic;


    private final Logger logger = LoggerFactory.getLogger(STClient.class);


    public STClient(String host, int port, String topic) throws IOException {
        socket = new Socket(host, port);
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());
        this.topic = topic;

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::doHeartBeat, 0, 20000, TimeUnit.MILLISECONDS);

        registerShutdownHook();
    }


    private void doHeartBeat(){
        var pingMessage = Message.createPING(topic);
        TCPHelper.sendMessage(output, pingMessage);
        try {
            var pong = TCPHelper.readMessage(input);
            logger.debug(String.valueOf(pong));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.info("Shutting down STClient...");

                if (socket != null && !socket.isClosed()) {
                    output.close();
                    input.close();
                    socket.close();
                }
            } catch (IOException e) {
                logger.error("Error closing socket: {}", e.getMessage(), e);
            }
        }));
    }

    public static void main(String[] args) throws IOException {
        new STClient("localhost", 8080, "test-topic");
    }

}
