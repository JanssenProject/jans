/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.model.attestation.AttestationOptions;
import io.jans.fido2.model.attestation.AttestationResult;
import io.jans.fido2.model.attestation.PublicKeyCredentialCreationOptions;
import io.jans.fido2.model.common.AttestationOrAssertionResponse;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.operation.AttestationService;
import io.jans.fido2.service.sg.converter.AttestationSuperGluuController;
import io.jans.fido2.service.util.CommonUtilService;
import io.jans.fido2.service.verifier.CommonVerifiers;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import org.slf4j.Logger;

import javax.validation.constraints.NotNull;
import java.io.IOException;

/**
 * serves request for /attestation endpoint exposed by FIDO2 sever
 *
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
@Path("/attestation")
public class AttestationController {

    @Inject
    private Logger log;

    @Inject
    private AttestationService attestationService;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private AttestationSuperGluuController attestationSuperGluuController;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Path("/options")
    public Response register(@NotNull AttestationOptions attestationOptions) {
        return processRequest(() -> {
            if (appConfiguration.getFido2Configuration() == null) {
                throw errorResponseFactory.forbiddenException();
            }
            commonVerifiers.verifyNotUseGluuParameters(CommonUtilService.toJsonNode(attestationOptions));
            PublicKeyCredentialCreationOptions result = attestationService.options(attestationOptions);
            return Response.ok().entity(result).build();
        });
    }

    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Path("/result")
    public Response verify(@NotNull AttestationResult attestationResult) {
        return processRequest(() -> {
            if (appConfiguration.getFido2Configuration() == null) {
                throw errorResponseFactory.forbiddenException();
            }
            commonVerifiers.verifyNotUseGluuParameters(CommonUtilService.toJsonNode(attestationResult));
            AttestationOrAssertionResponse result = attestationService.verify(attestationResult);
            return Response.ok().entity(result).build();
        });
    }

    @GET
    @Produces({"application/json"})
    @Path("/registration")
    public Response startRegistration(@QueryParam("username") String userName,
                                      @QueryParam("application") String appId,
                                      @QueryParam("session_id") String sessionId,
                                      @QueryParam("enrollment_code") String enrollmentCode) {
        return processRequest(() -> {
            if ((appConfiguration.getFido2Configuration() == null) && !appConfiguration.isSuperGluuEnabled()) {
                throw errorResponseFactory.forbiddenException();
            }
            log.debug("Start registration: username = {}, application = {}, session_id = {}, enrollment_code = {}", userName, appId, sessionId, enrollmentCode);
            JsonNode result = attestationSuperGluuController.startRegistration(userName, appId, sessionId, enrollmentCode);
            log.debug("Prepared U2F_V2 registration options request: {}", result.toString());
            return Response.ok().entity(result).build();
        });
    }

    @POST
    @Produces({"application/json"})
    @Path("/registration")
    public Response finishRegistration(@FormParam("username") String userName, @FormParam("tokenResponse") String registerResponseString) {
        return processRequest(() -> {
            if ((appConfiguration.getFido2Configuration() == null) && !appConfiguration.isSuperGluuEnabled()) {
                throw errorResponseFactory.forbiddenException();
            }
            log.debug("Finish registration: username = {}, tokenResponse = {}", userName, registerResponseString);
            JsonNode result = attestationSuperGluuController.finishRegistration(userName, registerResponseString);
            log.debug("Prepared U2F_V2 registration verify request: {}", result.toString());
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
