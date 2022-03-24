/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.rest;

import static io.jans.eleven.model.GenerateKeyRequestParam.EXPIRATION_TIME;
import static io.jans.eleven.model.GenerateKeyRequestParam.SIGNATURE_ALGORITHM;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author Javier Rojas Blum
 * @version April 27, 2016
 */
public interface GenerateKeyRestService {

    @POST
    @Path("/generateKey")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.APPLICATION_JSON})
    Response generateKey(
            @FormParam(SIGNATURE_ALGORITHM) String signatureAlgorithm,
            @FormParam(EXPIRATION_TIME) Long expirationTime);
}
