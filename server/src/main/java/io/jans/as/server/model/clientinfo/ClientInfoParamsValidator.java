/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.clientinfo;

/**
 * Validates the parameters received for the client info web service.
 *
 * @author Javier Rojas Blum Date: 07.19.2012
 */
public class ClientInfoParamsValidator {

    /**
     * Validates the parameters for a client info request.
     *
     * @param accessToken
     * @return Returns <code>true</code> when all the parameters are valid.
     */
    public static boolean validateParams(String accessToken) {
        return accessToken != null && !accessToken.isEmpty();
    }
}