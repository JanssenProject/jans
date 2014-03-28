package org.xdi.oxauth.model.discovery;

/**
 * @author Javier Rojas Date: 01.28.2013
 */
public class OpenIdConnectDiscoveryParamsValidator {

    public static boolean validateParams(String resource, String rel) {
        return resource != null && !resource.isEmpty();
    }
}