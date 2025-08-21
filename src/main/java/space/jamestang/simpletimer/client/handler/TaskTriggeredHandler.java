package space.jamestang.simpletimer.client.handler;

import space.jamestang.simpletimer.client.network.Message;

public interface TaskTriggeredHandler {

    void handle(Message msg);
}
