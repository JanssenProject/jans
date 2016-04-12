/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.gluu.oxeleven.model.SignRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
 */
@Path("/oxeleven")
public interface SignRestService {

    @POST
    @Path("/sign")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.APPLICATION_JSON})
    Response sign(
            @FormParam(SIGNING_INPUT) String signingInput,
            @FormParam(ALIAS) String alias,
            @FormParam(SIGNATURE_ALGORITHM) String signatureAlgorithm);
}
