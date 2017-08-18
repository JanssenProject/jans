/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma;

import org.xdi.oxauth.model.uma.UmaMetadata;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

/**
 * The endpoint at which the requester can obtain UMA metadata.
 *
 * @author Yuriy Zabrovarnyy
 */
public interface UmaMetadataService {

	@GET
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	UmaMetadata getMetadata();

}