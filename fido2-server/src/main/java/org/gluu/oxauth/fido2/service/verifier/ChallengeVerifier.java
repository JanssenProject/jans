/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package org.gluu.oxauth.fido2.service.verifier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.service.Base64Service;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class ChallengeVerifier {

	@Inject
	private Base64Service base64Service;

	public String getChallenge(JsonNode clientDataJSONNode) {
		try {
			String clientDataChallenge = base64Service
					.urlEncodeToStringWithoutPadding(base64Service.urlDecode(clientDataJSONNode.get("challenge").asText()));

			return clientDataChallenge;
		} catch (Exception ex) {
			throw new Fido2RPRuntimeException("Can't get challenge from clientData");
		}
	}

}
