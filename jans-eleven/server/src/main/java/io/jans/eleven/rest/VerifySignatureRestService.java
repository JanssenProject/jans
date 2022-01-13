/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.jans.eleven.model.VerifySignatureRequestParam;

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
