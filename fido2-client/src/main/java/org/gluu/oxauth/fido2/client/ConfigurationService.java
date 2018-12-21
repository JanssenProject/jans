/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.oxauth.fido2.client;

import javax.ws.rs.POST;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The endpoint allows to obtain Fido2 metadata configuration
 * 
 * @author Yuriy Movchan
 * @version 12/21/2018
 *
 */
public interface ConfigurationService {

    @POST
    @Produces({ "application/json" })
	public JsonNode getMetadataConfiguration();

}