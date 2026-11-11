package io.jans.as.server.rate;

import java.io.IOException;

public class RequestBodyTooLargeException extends IOException {

    public RequestBodyTooLargeException(String message) {
        super(message);
    }
}
