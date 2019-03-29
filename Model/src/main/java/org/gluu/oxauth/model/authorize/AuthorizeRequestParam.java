/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.authorize;

/**
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
public interface AuthorizeRequestParam {

    String ACCESS_TOKEN = "access_token";
    String RESPONSE_TYPE = "response_type";
    String CLIENT_ID = "client_id";
    String SCOPE = "scope";
    String REDIRECT_URI = "redirect_uri";
    String STATE = "state";
    String RESPONSE_MODE = "response_mode";
    String NONCE = "nonce";
    String DISPLAY = "display";
    String PROMPT = "prompt";
    String MAX_AGE = "max_age";
    String UI_LOCALES = "ui_locales";
    String CLAIMS_LOCALES = "claims_locales";
    String ID_TOKEN_HINT = "id_token_hint";
    String LOGIN_HINT = "login_hint";
    String ACR_VALUES = "acr_values";
    String AMR_VALUES = "amr_values";
    String CLAIMS = "claims";
    String REGISTRATION = "registration";
    String REQUEST = "request";
    String REQUEST_URI = "request_uri";
    String ORIGIN_HEADERS = "origin_headers";
    String CODE_CHALLENGE = "code_challenge";
    String CODE_CHALLENGE_METHOD = "code_challenge_method";
    String CUSTOM_RESPONSE_HEADERS = "custom_response_headers";

    /**
     * String that represents the End-User's login state at the OP.
     */
    String SESSION_ID = "session_id";

    String REQUEST_SESSION_ID = "request_session_id";
}