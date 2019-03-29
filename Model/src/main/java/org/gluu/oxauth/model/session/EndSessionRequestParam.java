/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.session;

/**
 * @author Javier Rojas Blum
 * @version August 9, 2017
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


    /**
     * Opaque value used by the RP to maintain state between the logout request and the callback to the endpoint
     * specified by the post_logout_redirect_uri parameter. If included in the logout request, the OP passes this
     * value back to the RP using the state query parameter when redirecting the User Agent back to the RP.
     */
    public static final String STATE = "state";

    /**
     * String that represents the End-User's login state at the OP.
     */
    public static final String SESSION_ID = "session_id";
}