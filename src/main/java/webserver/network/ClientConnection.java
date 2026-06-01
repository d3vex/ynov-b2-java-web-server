package webserver.network;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import webserver.http.HttpParser;
import webserver.http.HttpRequest;
import webserver.http.HttpResponse;
import webserver.http.HttpResponseBuilder;

public final class ClientConnection {
    
    private SocketChannel channel;
    private int LastActivityTime;
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private ByteBuffer writeBuffer;

    private HttpParser httpParser = new HttpParser();
    private HttpRequest currentRequest = null;
    private HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    private HttpResponse pendingResponse = null;
    

    public ClientConnection(SocketChannel channel) {
        this.channel = channel;     
    }

    public SocketChannel getChannel() {
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
        this.LastActivityTime = (int) (System.currentTimeMillis() / 1000);
    }

    public int getLastActivityTime() {
        return LastActivityTime;
    }

}
