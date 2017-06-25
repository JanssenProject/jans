/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.oxeleven.model.VerifySignatureRequestParam;

/**
 * @author Javier Rojas Blum
 * @version April 18, 2016
 */
public interface VerifySignatureRestService {

    @POST
    @Path("/verifySignature")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    Response verifySignature(VerifySignatureRequestParam verifySignatureRequestParam);
}
