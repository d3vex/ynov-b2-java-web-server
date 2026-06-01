package webserver.network;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import webserver.config.ServerConfig;
import webserver.http.HttpParser;
import webserver.http.HttpRequest;
import webserver.http.HttpResponse;
import webserver.http.HttpResponseBuilder;

public final class ClientConnection {
    
    private SocketChannel channel;
    private long LastActivityTime;
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private ByteBuffer writeBuffer;

    private long timeoutMs = 30000;
    private ServerConfig serverConfig;

    private HttpParser httpParser = new HttpParser();
    private HttpRequest currentRequest = null;
    private HttpResponse pendingResponse = null;
    

    public ClientConnection(SocketChannel channel) {
        this.channel = channel;
        this.LastActivityTime = System.currentTimeMillis();
    }

    public SocketChannel getSocketChannel() {
        return channel;
    }

    public void close() {
        try {
            channel.close();
        } catch (Exception e) {
            // Log error or ignore
        }
    }

    public boolean isOpen() {
        return channel.isOpen();
    }
    public boolean isConnected() {
        return channel.isConnected();
    }

    public String getRemoteAddress() {
        try {
            return channel.getRemoteAddress().toString();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }
    public void CreateWriteBuffer(int capacity) {
        this.writeBuffer = ByteBuffer.allocate(capacity);
    }
    
    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    public int RemainingWriteBuffer() {
        return writeBuffer.remaining();
    }

    public void UpdateLastActivityTime() {
        this.LastActivityTime = System.currentTimeMillis();
    }

    public long getLastActivityTime() {
        return LastActivityTime;
    }

    public HttpRequest getCurrentRequest() {
        return currentRequest;
    }

    public boolean parse() {
        httpParser.consume(readBuffer);
        currentRequest = httpParser.build();
        return currentRequest != null;
    }

    public void setPendingResponse(HttpResponse response) {
        this.pendingResponse = response;
    }
    public void clearRequestState() {
        this.pendingResponse = null;
        this.currentRequest = null;
        this.httpParser.reset();
    }
    public void clearResponseState() {
        this.pendingResponse = null;
        this.writeBuffer = null;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

}
