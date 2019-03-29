/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.authorize;

/**
 * @author Javier Rojas Blum
 * @version August 9, 2017
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

    /**
     * String that represents the End-User's login state at the OP.
     */
    String SESSION_ID = "session_id";

    String ACR_VALUES = "acr_values";
}