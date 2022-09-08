/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaErrorResponseType;
import io.jans.as.model.uma.UmaPermission;
import io.jans.as.model.uma.UmaPermissionList;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.uma.service.UmaPermissionService;
import io.jans.as.server.uma.service.UmaValidationService;
import io.jans.as.server.util.ServerUtil;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

/**
 * The endpoint at which the host registers permissions that it anticipates a
 * requester will shortly be asking for from the AM. This AM's endpoint is part
 * of resource set registration API.
 * <p>
 * In response to receiving an access request accompanied by an RPT that is
 * invalid or has insufficient authorization data, the host SHOULD register a
 * permission with the AM that would be sufficient for the type of access
 * sought. The AM returns a permission ticket for the host to give to the
 * requester in its response.
 *
 * @author Yuriy Zabrovarnyy
 */
@Path("/host/rsrc_pr")
public class UmaPermissionRegistrationWS {

    @Inject
    private Logger log;

    @Inject
    private TokenService tokenService;

    @Inject
    private UmaPermissionService permissionService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private UmaValidationService umaValidationService;

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response registerPermission(@Context HttpServletRequest request,
                                       @HeaderParam("Authorization") String authorization,
                                       String requestAsString) {
        try {
            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.UMA);

            final AuthorizationGrant authorizationGrant = umaValidationService.assertHasProtectionScope(authorization);

            // UMA2 spec defined 2 possible requests, single permission or list of permission. So here we parse manually
            UmaPermissionList permissionList = parseRequest(requestAsString);
            umaValidationService.validatePermissions(permissionList, authorizationGrant.getClient());

            String ticket = permissionService.addPermission(permissionList, tokenService.getClientDn(authorization));

            return Response.status(Response.Status.CREATED).
                    type(MediaType.APPLICATION_JSON_TYPE).
                    entity(new PermissionTicket(ticket)).
                    build();
        } catch (Exception ex) {
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            log.error("Exception happened", ex);
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, UmaErrorResponseType.SERVER_ERROR, "Internal error.");
        }
    }

    /**
     * UMA2 spec (edit 4) defined to possible requests, single permission or list of permission. So here we parse manually
     *
     * @param requestAsString request as string
     * @return uma permission list
     */
    private UmaPermissionList parseRequest(String requestAsString) {
        final ObjectMapper mapper = ServerUtil.createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        try {
            UmaPermission permission = mapper.readValue(requestAsString, UmaPermission.class);
            return new UmaPermissionList().addPermission(permission);
        } catch (IOException e) {
            // ignore
        }

        try {
            UmaPermissionList permissions = mapper.readValue(requestAsString, UmaPermissionList.class);
            if (!permissions.isEmpty()) {
                return permissions;
            }
            log.error("Permission list is empty.");
        } catch (IOException e) {
            log.error("Failed to parse uma permission request" + requestAsString, e);
        }
        throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, UmaErrorResponseType.INVALID_PERMISSION_REQUEST, "Failed to parse uma permission request.");
    }
}
