package io.jans.kc.protocol.mapper.config;

public class PersistenceConfigurationException extends RuntimeException {


    public PersistenceConfigurationException(String msg) {
        super(msg);
    }
    
    public PersistenceConfigurationException(String msg, Throwable cause) {
        super(msg,cause);
    }
}
