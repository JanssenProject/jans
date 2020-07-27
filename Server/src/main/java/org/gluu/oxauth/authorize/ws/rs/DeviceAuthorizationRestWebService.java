/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.authorize.ws.rs;

import org.gluu.oxauth.model.authorize.DeviceAuthorizationRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

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
     * @param scope The scope of the access request as defined by Section 3.3 of [RFC6749].
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