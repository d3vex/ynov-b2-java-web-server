package webserver.network;

import java.nio.channels.SelectionKey;
import java.util.Iterator;

import webserver.config.ConfigDefaults;

public class EventLoop {

    private boolean running = false;
    private final SelectorManager selectorManager = SelectorManager.getInstance();
    private final ConnectionAcceptor acceptor = new ConnectionAcceptor(selectorManager);
    private final SocketWriter socketWriter = new SocketWriter();
    private final TimeoutManager timeoutManager = new TimeoutManager(ConfigDefaults.TIMEOUT_MS);
    private int errorCount = 0;

    public void start() {
        running = true;
        while (running) {
            try {
                if (selectorManager.select() == 0) {
                    timeoutManager.checkTimeouts(selectorManager.getClientChannels());
                    continue;
                }

                Iterator<SelectionKey> iter = selectorManager.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        acceptor.accept(key);
                    } else if (key.isReadable()) {
                        ConnectionHandler.handleRead(key);
                    } else if (key.isWritable()) {
                        socketWriter.write(key);
                    }
                }

                timeoutManager.checkTimeouts(selectorManager.getClientChannels());
                errorCount = 0;

            } catch (Exception e) {
                System.err.println("Error in event loop: " + e.getMessage());
                e.printStackTrace();
                errorCount++;
                if (errorCount >= 5) {
                    System.err.println("Too many errors in event loop, stopping...");
                    stop();
                }
            }
        }
    }

    public void stop() {
        running = false;
    }
}
