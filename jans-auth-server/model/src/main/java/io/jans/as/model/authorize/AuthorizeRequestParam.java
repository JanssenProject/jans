/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.authorize;

/**
 * @author Javier Rojas Blum
 * @version October 7, 2019
 */
public final class AuthorizeRequestParam {

    private AuthorizeRequestParam() { throw new IllegalStateException("Utility class");}

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
    public static final String CODE_CHALLENGE = "code_challenge";
    public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    public static final String CUSTOM_RESPONSE_HEADERS = "custom_response_headers";
    public static final String AUTH_REQ_ID = "auth_req_id";
    public static final String SID = "sid";
    public static final String NBF = "nbf";

    /**
     * String that represents the End-User's login state at the OP.
     */
    public static final String SESSION_ID = "session_id";

    public static final String REQUEST_SESSION_ID = "request_session_id";
}