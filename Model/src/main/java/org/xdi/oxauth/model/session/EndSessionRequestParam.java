package org.xdi.oxauth.model.session;

/**
 * @author Javier Rojas Blum Date: 30.10.2013
 */
public interface EndSessionRequestParam {

    /**
     * Previously issued ID Token passed to the logout endpoint as a hint about the End-User's current authenticated
     * session with the Client.
     */
    public static final String ID_TOKEN_HINT = "id_token_hint";

    /**
     * URL to which the RP is requesting that the End-User's User-Agent be redirected after a logout has been performed.
     */
    public static final String POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";
}