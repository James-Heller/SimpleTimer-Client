package space.jamestang.simpletimer.client;

/**
 * Configuration class for STClient
 */
public class STClientConfig {
    private final String host;
    private final int port;
    private final int maxReconnectAttempts;
    private final long initialReconnectDelay;
    private final long maxReconnectDelay;
    private final long heartbeatInterval;
    private final boolean autoReconnect;
    
    STClientConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.maxReconnectAttempts = builder.maxReconnectAttempts;
        this.initialReconnectDelay = builder.initialReconnectDelay;
        this.maxReconnectDelay = builder.maxReconnectDelay;
        this.heartbeatInterval = builder.heartbeatInterval;
        this.autoReconnect = builder.autoReconnect;
    }
    
    public String getHost() { return host; }
    public int getPort() { return port; }
    public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
    public long getInitialReconnectDelay() { return initialReconnectDelay; }
    public long getMaxReconnectDelay() { return maxReconnectDelay; }
    public long getHeartbeatInterval() { return heartbeatInterval; }
    public boolean isAutoReconnect() { return autoReconnect; }
    
    public static Builder builder(String host, int port) {
        return new Builder(host, port);
    }

}
