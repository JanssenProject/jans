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
public interface AuthorizeResponseParam {

    public static final String CODE = "code";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String TOKEN_TYPE = "token_type";
    public static final String EXPIRES_IN = "expires_in";
    public static final String SCOPE = "scope";
    public static final String ID_TOKEN = "id_token";
    public static final String STATE = "state";

    /**
     * String that represents the End-User's login state at the OP.
     */
    public static final String SESSION_STATE = "session_state";

    public static final String ACR_VALUES = "acr_values";
}