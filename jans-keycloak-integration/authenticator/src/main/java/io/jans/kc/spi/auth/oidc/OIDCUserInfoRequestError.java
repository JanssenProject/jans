package io.jans.kc.spi.auth.oidc;

public class OIDCUserInfoRequestError extends Exception {
    
    public OIDCUserInfoRequestError(String msg) {
        super(msg);
    }

    
    public OIDCUserInfoRequestError(String msg, Throwable cause) {
        super(msg,cause);
    }
}
