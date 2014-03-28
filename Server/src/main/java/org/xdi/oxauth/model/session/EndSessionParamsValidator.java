package org.xdi.oxauth.model.session;

/**
 * @author Javier Rojas Date: 12.15.2011
 */
public class EndSessionParamsValidator {

    public static boolean validateParams(String idTokenHint, String postLogoutRedirectUri) {
        return idTokenHint != null && !idTokenHint.isEmpty()
                && postLogoutRedirectUri != null && !postLogoutRedirectUri.isEmpty();
    }
}