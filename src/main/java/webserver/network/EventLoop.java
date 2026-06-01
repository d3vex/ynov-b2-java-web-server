package webserver.network;

import java.nio.channels.SelectionKey;

public class EventLoop {
 
    private boolean running = false;
    private SelectorManager selectorManager = SelectorManager.getInstance();
    private int errorCount = 0;

    public void start() {
        running = true;
        while (running) {
            try {
                selectorManager.select();
                
                for (SelectionKey key : selectorManager.selectedKeys()) {
                    if (key.isAcceptable()) {
                        System.out.println("Accepting new connection...");
                    } else if (key.isReadable()) {
                        System.out.println("Reading data from client..." + key.attachment());
                    } else if (key.isWritable()) {
                        System.out.println("Writing data to client..." + key.attachment());
                    }
                }

            } catch (Exception e) {
                System.err.println("Error in event loop: " + e.getMessage());
                System.err.println("Stack trace:");
                e.printStackTrace();
                errorCount++;
                if(errorCount >= 5) {
                    System.err.println("Too many errors in event loop, stopping...");
                    stop();
                }
                System.err.println("Continuing event loop...");
            }
        }
    }

    public void stop() {
        running = false;
    }
    
}
