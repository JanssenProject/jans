/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.ciba;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public interface BackchannelAuthenticationRequestParam {

    /**
     * The scope of the access request.
     * CIBA authentication requests must contain the openid scope value.
     * Other scope values may be present.
     */
    String SCOPE = "scope";

    /**
     * A bearer token provided by the Client that will be used by the OpenID Provider to authenticate the callback
     * request to the Client. Required if the Client is registered to use Ping or Push modes.
     */
    String CLIENT_NOTIFICATION_TOKEN = "client_notification_token";

    /**
     * Requested Authentication Context Class Reference values.
     */
    String ACR_VALUES = "acr_values";

    /**
     * A token containing information identifying the end-user for whom authentication is being requested.
     */
    String LOGIN_HINT_TOKEN = "login_hint_token";

    /**
     * An ID Token previously issued to the Client by the OpenID Provider being passed back as a hint to identify
     * the end-user for whom authentication is being requested.
     */
    String ID_TOKEN_HINT = "id_token_hint";

    /**
     * A hint to the OpenID Provider regarding the end-user for whom authentication is being requested.
     */
    String LOGIN_HINT = "login_hint";

    /**
     * A human readable identifier or message intended to be displayed on both the consumption device and the
     * authentication device to interlock them together for the transaction by way of a visual cue for the end-user.
     */
    String BINDING_MESSAGE = "binding_message";

    /**
     * A secret code, such as password or pin, known only to the user but verifiable by the OP.
     * The code is used to authorize sending an authentication request to user's authentication device.
     */
    String USER_CODE = "user_code";

    /**
     * A positive integer allowing the client to request the expires_in value for the auth_req_id the server will return.
     */
    String REQUESTED_EXPIRY = "requested_expiry";
}