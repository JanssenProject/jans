package io.jans.kc.spi.custom;

public class JansThinBridgeInitException extends RuntimeException {
    
    public JansThinBridgeInitException(final String msg) {
        super(msg);
    }

    public JansThinBridgeInitException(final String msg, Throwable cause) {
        super(msg,cause);
    }
}
