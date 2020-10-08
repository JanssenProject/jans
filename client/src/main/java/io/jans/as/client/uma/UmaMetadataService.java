/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.uma;

import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaMetadata;

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