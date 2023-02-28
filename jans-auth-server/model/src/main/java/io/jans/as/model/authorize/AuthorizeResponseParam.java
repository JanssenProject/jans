/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.authorize;

/**
 * @author Javier Rojas Blum
 * @version July 28, 2021
 */
public final class AuthorizeResponseParam {

    private AuthorizeResponseParam() { throw new IllegalStateException("Utility class");}

    public static final String CODE = "code";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String TOKEN_TYPE = "token_type";
    public static final String EXPIRES_IN = "expires_in";
    public static final String SCOPE = "scope";
    public static final String ID_TOKEN = "id_token";
    public static final String STATE = "state";
    public static final String SESSION_STATE = "session_state";

    // JARM
    public static final String RESPONSE = "response";
    public static final String ISS = "iss";
    public static final String AUD = "aud";
    public static final String EXP = "exp";
    public static final String DEVICE_SECRET = "device_secret";

    /**
     * String that represents the End-User's login state at the OP.
     */
    public static final String SESSION_ID = "session_id";

    public static final String SID = "sid";

    public static final String ACR_VALUES = "acr_values";
}