/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * @author Yuriy Movchan
 * @version Septempber 15, 2017
 */
public interface MetadataRestService {

	@GET
	@Produces({ "application/json" })
	Response getConfiguration();

}