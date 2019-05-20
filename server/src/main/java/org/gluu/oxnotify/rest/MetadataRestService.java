/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxnotify.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Yuriy Movchan
 * @version Septempber 15, 2017
 */
public interface MetadataRestService {

	@GET
	@Produces({ "application/json" })
	Response getConfiguration();

}