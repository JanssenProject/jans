/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.uma;

import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaConstants;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;

/**
 * The endpoint at which the host requests the status of an RPT presented to it by a requester.
 * The endpoint is RPT introspection profile implementation defined here:
 * http://docs.kantarainitiative.org/uma/draft-uma-core.html#uma-bearer-token-profile
 */
public interface UmaRptIntrospectionService {

    @POST
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    RptIntrospectionResponse requestRptStatus(@HeaderParam("Authorization") String authorization,
                                              @FormParam("token") String rptAsString,
                                              @FormParam("token_type_hint") String tokenTypeHint);

}
