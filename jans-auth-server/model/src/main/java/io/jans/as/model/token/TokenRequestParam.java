/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.token;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public final class TokenRequestParam {

    private TokenRequestParam() {
        throw new IllegalStateException("Utility class");
    }

    public static final String GRANT_TYPE = "grant_type";
    public static final String CODE = "code";
    public static final String CODE_VERIFIER = "code_verifier";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String SCOPE = "scope";
    public static final String ASSERTION = "assertion";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String AUTH_REQ_ID = "auth_req_id";
    public static final String DEVICE_CODE = "device_code";
    public static final String AUDIENCE = "audience";
    public static final String SUBJECT_TOKEN = "subject_token";
    public static final String SUBJECT_TOKEN_TYPE = "subject_token_type";
    public static final String ACTOR_TOKEN = "actor_token";
    public static final String ACTOR_TOKEN_TYPE = "actor_token_type";
    public static final String REQUESTED_TOKEN_TYPE = "requested_token_type";


    // Demonstrating Proof-of-Possession
    public static final String DPOP = "DPoP";
}
