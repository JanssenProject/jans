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
public interface TokenRequestParam {

    String GRANT_TYPE = "grant_type";
    String CODE = "code";
    String CODE_VERIFIER = "code_verifier";
    String REDIRECT_URI = "redirect_uri";
    String USERNAME = "username";
    String PASSWORD = "password";
    String SCOPE = "scope";
    String ASSERTION = "assertion";
    String REFRESH_TOKEN = "refresh_token";
    String AUTH_REQ_ID = "auth_req_id";
    String DEVICE_CODE = "device_code";

    // Demonstrating Proof-of-Possession
    String DPOP = "DPoP";
}
