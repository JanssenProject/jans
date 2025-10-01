/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.token;

/**
 * Validates the parameters received for the validate token web service.
 *
 * @author Javier Rojas Blum Date: 10.27.2011
 */
public class ValidateTokenParamsValidator {

    /**
     * Validates the parameters for a validate token request.
     *
     * @param accessToken The access token issued by the authorization server.
     * @return Returns <code>true</code> when all the parameters are valid.
     */
    public static boolean validateParams(String accessToken) {
        return accessToken != null && !accessToken.isEmpty();
    }
}