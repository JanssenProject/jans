/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.as.server.discovery.ws.rs;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.gluu.GluuErrorResponseType;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.DiscoveryService;
import io.jans.as.server.service.LocalResponseCache;
import io.jans.as.server.service.external.ExternalDiscoveryService;
import io.jans.as.server.util.ServerUtil;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * OAuth 2.0 Authorization Server Metadata endpoint.
 * Per RFC 8414: GET /.well-known/oauth-authorization-server
 *
 * @author Yuriy Z
 */
@Path("/oauth-authorization-server")
public class OAuthAuthorizationServerMetadataWS {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private DiscoveryService discoveryService;

    @Inject
    private ExternalDiscoveryService externalDiscoveryService;

    @Inject
    private LocalResponseCache localResponseCache;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getMetadata(@Context HttpServletRequest httpRequest, @Context HttpServletResponse httpResponse) {
        try {
            final JSONObject cachedResponse = localResponseCache.getDiscoveryResponse();
            if (cachedResponse != null) {
                log.trace("Cached discovery response returned.");
                return buildResponse(cachedResponse);
            }

            JSONObject jsonObj = discoveryService.process();
            JSONObject clone = new JSONObject(jsonObj.toString());

            final ExecutionContext context = new ExecutionContext(httpRequest, httpResponse);
            if (!externalDiscoveryService.modifyDiscovery(jsonObj, context)) {
                jsonObj = clone; // revert to original state if object was modified in script
            }

            return buildResponse(jsonObj);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, GluuErrorResponseType.SERVER_ERROR, "Internal error.");
        }
    }

    private Response buildResponse(JSONObject jsonObj) throws JsonProcessingException {
        final String entity = ServerUtil.toPrettyJson(jsonObj).replace("\\/", "/");
        return Response.ok(entity).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
