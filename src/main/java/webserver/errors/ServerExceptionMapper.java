package webserver.errors;

import java.io.FileNotFoundException;
import java.nio.channels.ClosedChannelException;

import webserver.http.HttpStatus;

public class ServerExceptionMapper {

    public HttpStatus toStatus(Exception e) {
        if (e instanceof FileNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (e instanceof SecurityException) {
            return HttpStatus.FORBIDDEN;
        }
        if (e instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (e instanceof ClosedChannelException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public HttpStatus toStatus(HttpStatus status) {
        return status;
    }
}
