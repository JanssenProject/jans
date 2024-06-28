package io.jans.kc.oidc;

public class OIDCUserInfoRequestError extends Exception {
    
    public OIDCUserInfoRequestError(String msg) {
        super(msg);
    }

    
    public OIDCUserInfoRequestError(String msg, Throwable cause) {
        super(msg,cause);
    }
}
