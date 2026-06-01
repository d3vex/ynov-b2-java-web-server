package webserver.network;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;

public class TimeoutManager {

    private final long defaultTimeoutMs;
    private long lastCheck = System.currentTimeMillis();

    public TimeoutManager(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    public void checkTimeouts(Map<SocketChannel, SelectionKey> clientChannels) {
        long now = System.currentTimeMillis();
        if (now - lastCheck < 1000) {
            return;
        }
        lastCheck = now;

        Iterator<Map.Entry<SocketChannel, SelectionKey>> it = clientChannels.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<SocketChannel, SelectionKey> entry = it.next();
            ClientConnection connection = (ClientConnection) entry.getValue().attachment();
            if (connection == null) continue;

            long effectiveTimeout = connection.getTimeoutMs() > 0
                    ? connection.getTimeoutMs()
                    : defaultTimeoutMs;

            if ((now - connection.getLastActivityTime()) > effectiveTimeout) {
                System.out.println("Timeout connection: " + connection.getRemoteAddress());
                connection.close();
                entry.getValue().cancel();
                it.remove();
            }
        }
    }
}
