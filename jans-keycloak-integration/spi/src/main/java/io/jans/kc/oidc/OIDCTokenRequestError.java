package io.jans.kc.oidc;

public class OIDCTokenRequestError extends Exception {
    
    public OIDCTokenRequestError(String msg) {
        super(msg);
    }

    public OIDCTokenRequestError(String msg, Throwable cause) {
        super(msg,cause);
    }
}
