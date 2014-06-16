package org.xdi.oxauth.model.authorize;

/**
 * @author Javier Rojas Blum Date: 09.16.2013
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
    public static final String CLAIMS = "claims";
    public static final String REGISTRATION = "registration";
    public static final String REQUEST = "request";
    public static final String REQUEST_URI = "request_uri";
}