/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.ciba;

/**
 * @author Javier Rojas Blum
 * @version May 9, 2019
 */
public interface PushErrorRequestParam {

    String AUTHORIZATION_REQUEST_ID = "auth_req_id";
    String ERROR = "error";
    String ERROR_DESCRIPTION = "error_description";
    String ERROR_URI = "error_uri";
}
