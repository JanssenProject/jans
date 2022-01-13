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
public interface AuthorizeResponseParam {

    String CODE = "code";
    String ACCESS_TOKEN = "access_token";
    String TOKEN_TYPE = "token_type";
    String EXPIRES_IN = "expires_in";
    String SCOPE = "scope";
    String ID_TOKEN = "id_token";
    String STATE = "state";
    String SESSION_STATE = "session_state";

    // JARM
    String RESPONSE = "response";
    String ISS = "iss";
    String AUD = "aud";
    String EXP = "exp";

    /**
     * String that represents the End-User's login state at the OP.
     */
    String SESSION_ID = "session_id";

    String SID = "sid";

    String ACR_VALUES = "acr_values";
}