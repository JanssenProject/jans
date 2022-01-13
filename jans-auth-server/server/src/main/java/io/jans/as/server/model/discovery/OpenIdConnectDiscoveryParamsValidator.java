/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.discovery;

/**
 * @author Javier Rojas Date: 01.28.2013
 */
public class OpenIdConnectDiscoveryParamsValidator {

    public static boolean validateParams(String resource, String rel) {
        return resource != null && !resource.isEmpty();
    }
}