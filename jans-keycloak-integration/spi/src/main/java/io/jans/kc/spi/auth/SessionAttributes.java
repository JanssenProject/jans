package io.jans.kc.spi.auth;

public class SessionAttributes {
    
    public static final String JANS_OIDC_STATE = "jans.oidc.state";
    public static final String JANS_OIDC_NONCE = "jans.oidc.nonce";
    public static final String KC_ACTION_URI  = "kc.action-uri";
    public static final String JANS_OIDC_CODE = "jans.oidc.code";
    public static final String JANS_SESSION_STATE = "jans.session.state";

    private SessionAttributes() {
        //private constructor
    }
}
