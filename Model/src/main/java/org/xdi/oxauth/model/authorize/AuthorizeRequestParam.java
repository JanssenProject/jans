/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.authorize;

/**
 * @author Javier Rojas Blum
 * @version December 15, 2015
 */
public interface AuthorizeRequestParam {

    public static final String ACCESS_TOKEN = "access_token";
    public static final String RESPONSE_TYPE = "response_type";
    public static final String CLIENT_ID = "client_id";
    public static final String SCOPE = "scope";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String STATE = "state";
    public static final String RESPONSE_MODE = "response_mode";
    public static final String NONCE = "nonce";
    public static final String DISPLAY = "display";
    public static final String PROMPT = "prompt";
    public static final String MAX_AGE = "max_age";
    public static final String UI_LOCALES = "ui_locales";
    public static final String CLAIMS_LOCALES = "claims_locales";
    public static final String ID_TOKEN_HINT = "id_token_hint";
    public static final String LOGIN_HINT = "login_hint";
    public static final String ACR_VALUES = "acr_values";
    public static final String AMR_VALUES = "amr_values";
    public static final String CLAIMS = "claims";
    public static final String REGISTRATION = "registration";
    public static final String REQUEST = "request";
    public static final String REQUEST_URI = "request_uri";
    public static final String ORIGIN_HEADERS = "origin_headers";

    /**
     * String that represents the End-User's login state at the OP.
     */
    public static final String SESSION_STATE = "session_state";

    public static final String REQUEST_SESSION_STATE = "request_session_state";
}