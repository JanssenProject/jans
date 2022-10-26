/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import io.jans.as.model.authorize.DeviceAuthorizationRequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * <p>
 * Provides interface to process OAuth2 Device Flow.
 * </p>
 */
public interface DeviceAuthorizationRestWebService {


    /**
     * Device Authorization Request [RFC8628 3.1].
     * Generates user_code, device_code and data needed to follow the device authorization flow
     * in other rest services.
     *
     * @param clientId REQUIRED The client identifier as described in Section 2.2 of [RFC6749].
     * @param scope    The scope of the access request as defined by Section 3.3 of [RFC6749].
     */
    @POST
    @Path("/device_authorization")
    @Produces({MediaType.APPLICATION_JSON})
    Response deviceAuthorization(
            @FormParam(DeviceAuthorizationRequestParam.CLIENT_ID) String clientId,
            @FormParam(DeviceAuthorizationRequestParam.SCOPE) String scope,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse,
            @Context SecurityContext securityContext);

}