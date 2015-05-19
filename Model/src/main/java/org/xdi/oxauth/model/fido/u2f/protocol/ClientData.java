/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.xdi.oxauth.model.fido.u2f.protocol;

import java.io.IOException;
import java.io.Serializable;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.xdi.oxauth.model.exception.BadInputException;
import org.xdi.oxauth.model.util.Base64Util;

/**
 * FIDO U2F client data
 *
 * @author Yuriy Movchan Date: 05/13/2015
 */
public class ClientData implements Serializable {

	private static final long serialVersionUID = -1483378146391551962L;

	private static final String TYPE_PARAM = "typ";
	private static final String CHALLENGE_PARAM = "challenge";
	private static final String ORIGIN_PARAM = "origin";

	private final String type;
	private final String challenge;
	private final String origin;
	private final String rawClientData;
	private final JsonNode data;

	public ClientData(String clientData) throws BadInputException {
		this.rawClientData = new String(Base64Util.base64urldecode(clientData));
		try {
			this.data = new ObjectMapper().readTree(rawClientData);
			this.type = getString(TYPE_PARAM);
			this.challenge = getString(CHALLENGE_PARAM);
			this.origin = getString(ORIGIN_PARAM);
		} catch (IOException ex) {
			throw new BadInputException("Malformed ClientData", ex);
		}
	}

	public String getType() {
		return type;
	}

	public String getChallenge() {
		return challenge;
	}

	public String getOrigin() {
		return origin;
	}

	public String getString(String key) {
		return data.get(key).asText();
	}

	@Override
	public String toString() {
		return rawClientData;
	}
}
