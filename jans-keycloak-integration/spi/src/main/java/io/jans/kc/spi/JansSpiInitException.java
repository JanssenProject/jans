package io.jans.kc.spi;

public class JansSpiInitException extends RuntimeException {
    
    public JansSpiInitException(final String msg) {
        super(msg);
    }

    public JansSpiInitException(final String msg, Throwable cause) {
        super(msg,cause);
    }
}
