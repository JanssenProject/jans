/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.client;

import io.jans.fido2.model.assertion.AssertionOptions;
import io.jans.fido2.model.assertion.AssertionOptionsGenerate;
import io.jans.fido2.model.assertion.AssertionResult;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * The endpoint allows to start and finish Fido2 assertion process
 *
 * @author Yuriy Movchan
 * @version 12/21/2018
 */
public interface AssertionService {

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/options")
    public Response authenticate(AssertionOptions assertionOptions);

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/options/generate")
    public Response generateAuthenticate(AssertionOptionsGenerate assertionOptionsGenerate);

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/result")
    public Response verify(AssertionResult assertionResult);

}