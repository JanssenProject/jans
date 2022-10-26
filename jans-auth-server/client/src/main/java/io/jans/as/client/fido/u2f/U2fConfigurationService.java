/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.fido.u2f;

import io.jans.as.model.fido.u2f.U2fConfiguration;
import io.jans.as.model.uma.UmaConstants;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;

/**
 * The endpoint at which the requester can obtain FIDO U2F metadata configuration
 *
 * @author Yuriy Movchan Date: 05/27/2015
 */
public interface U2fConfigurationService {

    @GET
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    U2fConfiguration getMetadataConfiguration();

}