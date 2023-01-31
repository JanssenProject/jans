/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import java.io.IOException;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.exception.Fido2RpRuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.operation.AssertionService;
import io.jans.fido2.service.sg.converter.AssertionSuperGluuController;
import io.jans.fido2.service.verifier.CommonVerifiers;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

/**
 * serves request for /assertion endpoint exposed by FIDO2 sever 
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
    private AssertionSuperGluuController assertionSuperGluuController;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CommonVerifiers commonVerifiers;

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/options")
    public Response authenticate(String content) {
        if (appConfiguration.getFido2Configuration() == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        JsonNode params;
        try {
        	params = dataMapperService.readTree(content);
        } catch (IOException ex) {
            throw new Fido2RpRuntimeException("Failed to parse options assertion request", ex);
        }

        commonVerifiers.verifyNotUseGluuParameters(params);
        JsonNode result = assertionService.options(params);

        ResponseBuilder builder = Response.ok().entity(result.toString());
        return builder.build();
    }

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/result")
    public Response verify(String content) {
        if (appConfiguration.getFido2Configuration() == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        JsonNode params;
        try {
        	params = dataMapperService.readTree(content);
        } catch (IOException ex) {
            throw new Fido2RpRuntimeException("Failed to parse finish assertion request", ex);
        }

        commonVerifiers.verifyNotUseGluuParameters(params);
        JsonNode result = assertionService.verify(params);

        ResponseBuilder builder = Response.ok().entity(result.toString());
        return builder.build();
    }

    @GET
    @Produces({ "application/json" })
    @Path("/authentication")
    public Response startAuthentication(@QueryParam("username") String userName, @QueryParam("keyhandle") String keyHandle, @QueryParam("application") String appId, @QueryParam("session_id") String sessionId) {
        if ((appConfiguration.getFido2Configuration() == null) && !appConfiguration.isSuperGluuEnabled()) {
            return Response.status(Status.FORBIDDEN).build();
        }
        log.debug("Start authentication: username = {}, keyhandle = {}, application = {}, session_id = {}", userName, keyHandle, appId, sessionId);

        log.debug("Start authentication: username = {}, keyhandle = {}, application = {}, session_id = {}", userName, keyHandle, appId, sessionId);

        log.debug("Start authentication: username = {}, keyhandle = {}, application = {}, session_id = {}", userName, keyHandle, appId, sessionId);

        JsonNode result = assertionSuperGluuController.startAuthentication(userName, keyHandle, appId, sessionId);

        log.debug("Prepared U2F_V2 authentication options request: {}", result.toString());

        ResponseBuilder builder = Response.ok().entity(result.toString());
        return builder.build();
    }

    @POST
    @Produces({ "application/json" })
    @Path("/authentication")
    public Response finishAuthentication(@FormParam("username") String userName, @FormParam("tokenResponse") String authenticateResponseString) {
        if ((appConfiguration.getFido2Configuration() == null) && !appConfiguration.isSuperGluuEnabled()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        log.debug("Finish authentication: username = {}, tokenResponse = {}", userName, authenticateResponseString);

        JsonNode result = assertionSuperGluuController.finishAuthentication(userName, authenticateResponseString);

        log.debug("Prepared U2F_V2 authentication verify request: {}", result.toString());

        ResponseBuilder builder = Response.ok().entity(result.toString());
        return builder.build();
    }

}
