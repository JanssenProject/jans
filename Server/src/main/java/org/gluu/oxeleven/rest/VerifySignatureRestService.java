/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.rest;

import org.gluu.oxeleven.model.VerifySignatureRequestParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.gluu.oxeleven.model.VerifySignatureRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version April 18, 2016
 */
@Path("/oxeleven")
public interface VerifySignatureRestService {

    @POST
    @Path("/verifySignature")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    Response verifySignature(VerifySignatureRequestParam verifySignatureRequestParam);
}
