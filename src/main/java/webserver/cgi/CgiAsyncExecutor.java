package webserver.cgi;

import webserver.config.ServerConfig;
import webserver.handlers.ErrorHandler;
import webserver.handlers.RequestDispatcher;
import webserver.http.HttpRequest;
import webserver.http.HttpResponse;
import webserver.http.HttpStatus;
import webserver.network.ClientConnection;
import webserver.network.SelectorManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CgiAsyncExecutor {

    private static final int CGI_POOL_SIZE = 4;
    private static CgiAsyncExecutor instance;
    private final ExecutorService pool;

    private CgiAsyncExecutor() {
        this.pool = Executors.newFixedThreadPool(CGI_POOL_SIZE, r -> {
            Thread t = new Thread(r, "cgi-worker");
            t.setDaemon(true);
            return t;
        });
    }

    public static synchronized CgiAsyncExecutor getInstance() {
        if (instance == null) {
            instance = new CgiAsyncExecutor();
        }
        return instance;
    }

    public void execute(ClientConnection connection, HttpRequest request, ServerConfig config) {
        pool.submit(() -> {
            try {
                RequestDispatcher dispatcher = new RequestDispatcher();
                HttpResponse response = dispatcher.dispatch(request, config);
                complete(connection, response);
            } catch (Exception e) {
                System.err.println("Async CGI error: " + e.getMessage());
                ErrorHandler errorHandler = new ErrorHandler();
                HttpResponse errorResponse = errorHandler.handleError(
                        HttpStatus.INTERNAL_SERVER_ERROR, config,
                        request.getPath(), request.getMethod());
                complete(connection, errorResponse);
            }
        });
    }

    private void complete(ClientConnection connection, HttpResponse response) {
        connection.setPendingResponse(response);
        connection.setCgiResponsePending(true);
        SelectorManager.getInstance().addPendingCgi(connection);
        SelectorManager.getInstance().wakeup();
    }

    public void shutdown() {
        pool.shutdownNow();
    }
}
