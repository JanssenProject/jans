package org.xdi.oxauth.model.userinfo;

/**
 * Validates the parameters received for the user info web service.
 *
 * @author Javier Rojas Blum Date: 12.30.2011
 */
public class UserInfoParamsValidator {

    /**
     * Validates the parameters for an user info request.
     *
     * @param accessToken
     * @return Returns <code>true</code> when all the parameters are valid.
     */
    public static boolean validateParams(String accessToken) {
        return accessToken != null && !accessToken.isEmpty();
    }
}
