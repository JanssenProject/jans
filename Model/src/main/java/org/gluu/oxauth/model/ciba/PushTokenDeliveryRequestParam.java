/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.ciba;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public interface PushTokenDeliveryRequestParam {

    String AUTHORIZATION_REQUEST_ID = "auth_req_id";
    String ACCESS_TOKEN = "access_token";
    String TOKEN_TYPE = "token_type";
    String REFRESH_TOKEN = "refresh_token";
    String EXPIRES_IN = "expires_in";
    String ID_TOKEN = "id_token";
}