/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.gluu.oxeleven.model.GenerateKeyRequestParam.EXPIRATION_TIME;
import static org.gluu.oxeleven.model.GenerateKeyRequestParam.SIGNATURE_ALGORITHM;

/**
 * @author Javier Rojas Blum
 * @version April 27, 2016
 */
@Path("/oxeleven")
public interface GenerateKeyRestService {

    @POST
    @Path("/generateKey")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.APPLICATION_JSON})
    Response generateKey(
            @FormParam(SIGNATURE_ALGORITHM) String signatureAlgorithm,
            @FormParam(EXPIRATION_TIME) Long expirationTime);
}
