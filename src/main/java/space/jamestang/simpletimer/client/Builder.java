package space.jamestang.simpletimer.client;

public class Builder {
    final String host;
    final int port;
    int maxReconnectAttempts = 10;
    long initialReconnectDelay = 1000; // 1秒
    long maxReconnectDelay = 60000; // 60秒
    long heartbeatInterval = 20; // 20秒
    boolean autoReconnect = true;

    Builder(String host, int port) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        this.host = host;
        this.port = port;
    }

    public Builder maxReconnectAttempts(int maxReconnectAttempts) {
        if (maxReconnectAttempts < 0) {
            throw new IllegalArgumentException("Max reconnect attempts cannot be negative");
        }
        this.maxReconnectAttempts = maxReconnectAttempts;
        return this;
    }

    public Builder initialReconnectDelay(long initialReconnectDelay) {
        if (initialReconnectDelay < 0) {
            throw new IllegalArgumentException("Initial reconnect delay cannot be negative");
        }
        this.initialReconnectDelay = initialReconnectDelay;
        return this;
    }

    public Builder maxReconnectDelay(long maxReconnectDelay) {
        if (maxReconnectDelay < 0) {
            throw new IllegalArgumentException("Max reconnect delay cannot be negative");
        }
        this.maxReconnectDelay = maxReconnectDelay;
        return this;
    }

    public Builder heartbeatInterval(long heartbeatInterval) {
        if (heartbeatInterval <= 0) {
            throw new IllegalArgumentException("Heartbeat interval must be positive");
        }
        this.heartbeatInterval = heartbeatInterval;
        return this;
    }

    public Builder autoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
        return this;
    }

    public STClientConfig build() {
        if (initialReconnectDelay > maxReconnectDelay) {
            throw new IllegalArgumentException("Initial reconnect delay cannot be greater than max reconnect delay");
        }
        return new STClientConfig(this);
    }
}