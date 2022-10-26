/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * The endpoint allows to obtain Fido2 metadata configuration
 * 
 * @author Yuriy Movchan
 * @version 12/21/2018
 *
 */
public interface ConfigurationService {

    @GET
    @Produces({ "application/json" })
	public Response getMetadataConfiguration();

}