/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.token;

/**
 * Validates the parameters received for the validate token web service.
 *
 * @author Javier Rojas Blum Date: 10.27.2011
 */
public class ValidateTokenParamsValidator {

	/**
	 * Validates the parameters for a validate token request.
	 *
	 * @param accessToken
	 *            The access token issued by the authorization server.
	 * @return Returns <code>true</code> when all the parameters are valid.
	 */
	public static boolean validateParams(String accessToken) {
		return accessToken != null && !accessToken.isEmpty();
	}
}