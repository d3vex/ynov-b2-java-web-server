package webserver.bootstrap;

import webserver.network.EventLoop;
import webserver.network.SelectorManager;

public class ShutdownManager {

    private EventLoop eventLoop;
    private static ShutdownManager instance;

    private ShutdownManager() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }


    public static synchronized ShutdownManager getInstance() {
        if (instance == null) {
            instance = new ShutdownManager();
        }
        return instance;
    }

    public void register(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public void shutdown() {
        System.out.println("Shutting down all servers...");
        if (eventLoop != null) {
            eventLoop.stop();
        }
        SelectorManager.getInstance().closeChannels();
        System.out.println("Server stopped.");
    }
}
