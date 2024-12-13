/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.model.assertion.*;
import io.jans.fido2.model.common.AttestationOrAssertionResponse;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.operation.AssertionService;
import jakarta.validation.constraints.NotNull;
import io.jans.fido2.service.util.CommonUtilService;
import io.jans.fido2.service.verifier.CommonVerifiers;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;



/**
 * serves request for /assertion endpoint exposed by FIDO2 sever
 *
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
@Path("/assertion")
public class AssertionController {

    @Inject
    private Logger log;

    @Inject
    private AssertionService assertionService;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Path("/options")
    public Response authenticate(@NotNull AssertionOptions assertionOptions) {
        return processRequest(() -> {
            if (appConfiguration.getFido2Configuration() == null) {
                throw errorResponseFactory.forbiddenException();
            }
            AssertionOptionsResponse result = assertionService.options(assertionOptions);
            return Response.ok().entity(result).build();
        });
    }

    //TODO: delete this when checking issue related to isAssertionOptionsGenerateEndpointEnabled
    /*@POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Path("/options/generate")
    public Response generateAuthenticate(@NotNull AssertionOptionsGenerate assertionOptionsGenerate) {
        return processRequest(() -> {
            if (appConfiguration.getFido2Configuration() == null || !appConfiguration.getFido2Configuration().isAssertionOptionsGenerateEndpointEnabled()) {
                throw errorResponseFactory.forbiddenException();
            }
            AsserOptGenerateResponse result = assertionService.generateOptions(assertionOptionsGenerate);
            return Response.ok().entity(result).build();
        });
    }*/

    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Path("/result")
    public Response verify(@NotNull AssertionResult assertionResult) {
        return processRequest(() -> {
            if (appConfiguration.getFido2Configuration() == null) {
                throw errorResponseFactory.forbiddenException();
            }
            AttestationOrAssertionResponse result = assertionService.verify(assertionResult);
            return Response.ok().entity(result).build();
        });
    }

   

   
    private Response processRequest(RequestProcessor processor) {
        try {
            return processor.process();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unknown Error: {}", e.getMessage(), e);
            throw errorResponseFactory.unknownError(e.getMessage());
        }
    }

    @FunctionalInterface
    private interface RequestProcessor {
        Response process() throws Exception;
    }
}
