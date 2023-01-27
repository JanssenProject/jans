/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import io.jans.as.model.fido.u2f.message.RawAuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.AuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.ClientData;
import io.jans.fido2.exception.Fido2RpRuntimeException;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.AuthenticatorDataParser;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.operation.AssertionService;
import io.jans.fido2.service.sg.RawAuthenticationService;
import io.jans.fido2.service.sg.converter.AssertionSuperGluuController;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.sg.SuperGluuMode;
import io.jans.util.StringHelper;
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

        JsonNode result = assertionSuperGluuController.startAuthentication(userName, keyHandle, appId, sessionId);

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

        JsonNode result = assertionSuperGluuController.finishAuthentication(userName, authenticateResponseString);

        ResponseBuilder builder = Response.ok().entity(result.toString());
        return builder.build();
    }

}
