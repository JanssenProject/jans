package io.jans.kc.spi.custom;


public class JansThinBridgeOperationException extends RuntimeException {
    
    public JansThinBridgeOperationException(final String msg) {
        super(msg);
    }

    public JansThinBridgeOperationException(final String msg, Throwable cause) {
        super(msg,cause);
    }
}
