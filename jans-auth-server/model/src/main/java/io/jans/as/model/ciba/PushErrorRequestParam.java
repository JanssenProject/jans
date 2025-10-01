/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.ciba;

/**
 * @author Javier Rojas Blum
 * @version May 9, 2019
 */
public class PushErrorRequestParam {

    private PushErrorRequestParam() {
    }

    public static final String AUTHORIZATION_REQUEST_ID = "auth_req_id";
    public static final String ERROR = "error";
    public static final String ERROR_DESCRIPTION = "error_description";
    public static final String ERROR_URI = "error_uri";
}
