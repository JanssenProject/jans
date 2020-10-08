/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.uma.ws.rs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.gluu.oxauth.model.common.AuthorizationGrant;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.uma.PermissionTicket;
import org.gluu.oxauth.model.uma.UmaConstants;
import org.gluu.oxauth.model.uma.UmaErrorResponseType;
import org.gluu.oxauth.model.uma.UmaPermissionList;
import org.gluu.oxauth.service.token.TokenService;
import org.gluu.oxauth.uma.service.UmaPermissionService;
import org.gluu.oxauth.uma.service.UmaValidationService;
import org.gluu.oxauth.util.ServerUtil;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
            org.gluu.oxauth.model.uma.UmaPermission permission = mapper.readValue(requestAsString, org.gluu.oxauth.model.uma.UmaPermission.class);
            return new UmaPermissionList().addPermission(permission);
        } catch (IOException e) {
            // ignore
        }

        try {
            UmaPermissionList permissions = mapper.readValue(requestAsString, org.gluu.oxauth.model.uma.UmaPermissionList.class);
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
