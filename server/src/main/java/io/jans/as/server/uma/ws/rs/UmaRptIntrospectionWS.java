/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import io.jans.as.model.common.ComponentType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaErrorResponseType;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.external.ExternalUmaRptClaimsService;
import io.jans.as.server.service.external.context.ExternalUmaRptClaimsContext;
import io.jans.as.server.uma.authorization.UmaPCT;
import io.jans.as.server.uma.authorization.UmaRPT;
import io.jans.as.server.uma.service.UmaPctService;
import io.jans.as.server.uma.service.UmaRptService;
import io.jans.as.server.uma.service.UmaScopeService;
import io.jans.as.server.uma.service.UmaValidationService;
import io.jans.as.server.util.ServerUtil;
import io.jans.util.StringHelper;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * The endpoint at which the host requests the status of an RPT presented to it by a requester.
 * The endpoint is RPT introspection profile implementation defined by
 * http://docs.kantarainitiative.org/uma/draft-uma-core.html#uma-bearer-token-profile
 *
 * @author Yuriy Zabrovarnyy
 */
@Path("/rpt/status")
public class UmaRptIntrospectionWS {

    @Inject
    private Logger log;
    @Inject
    private ErrorResponseFactory errorResponseFactory;
    @Inject
    private UmaRptService rptService;
    @Inject
    private UmaValidationService umaValidationService;
    @Inject
    private UmaScopeService umaScopeService;
    @Inject
    private UmaPctService pctService;
    @Inject
    private ExternalUmaRptClaimsService externalUmaRptClaimsService;
    @Inject
    private ClientService clientService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectGet(@HeaderParam("Authorization") String authorization,
                                  @QueryParam("token") String token,
                                  @QueryParam("token_type_hint") String tokenTypeHint,
                                  @Context HttpServletRequest httpRequest,
                                  @Context HttpServletResponse httpResponse) {
        return introspect(authorization, token, tokenTypeHint, httpRequest, httpResponse);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectPost(@HeaderParam("Authorization") String authorization,
                                   @FormParam("token") String token,
                                   @FormParam("token_type_hint") String tokenTypeHint,
                                   @Context HttpServletRequest httpRequest,
                                   @Context HttpServletResponse httpResponse) {
        return introspect(authorization, token, tokenTypeHint, httpRequest, httpResponse);
    }

    private Response introspect(String authorization, String token, String tokenTypeHint, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            errorResponseFactory.validateComponentEnabled(ComponentType.UMA);
            umaValidationService.assertHasProtectionScope(authorization);

            final UmaRPT rpt = rptService.getRPTByCode(token);

            if (!isValid(rpt)) {
                return Response.status(Response.Status.OK).
                        entity(new RptIntrospectionResponse(false)).
                        cacheControl(ServerUtil.cacheControl(true)).
                        build();
            }

            final List<io.jans.as.model.uma.UmaPermission> permissions = buildStatusResponsePermissions(rpt);

            // active status
            final RptIntrospectionResponse statusResponse = new RptIntrospectionResponse();
            statusResponse.setActive(true);
            statusResponse.setExpiresAt(ServerUtil.dateToSeconds(rpt.getExpirationDate()));
            statusResponse.setIssuedAt(ServerUtil.dateToSeconds(rpt.getCreationDate()));
            statusResponse.setPermissions(permissions);
            statusResponse.setClientId(rpt.getClientId());
            statusResponse.setAud(rpt.getClientId());
            statusResponse.setSub(rpt.getUserId());

            final List<UmaPermission> rptPermissions = rptService.getRptPermissions(rpt);
            if (!rptPermissions.isEmpty()) {
                UmaPermission permission = rptPermissions.iterator().next();
                String pctCode = permission.getAttributes().get(UmaPermission.PCT);
                if (StringHelper.isNotEmpty(pctCode)) {
                    UmaPCT pct = pctService.getByCode(pctCode);
                    if (pct != null) {
                        statusResponse.setPctClaims(pct.getClaims().toMap());
                    } else {
                        log.error("Failed to find PCT with code: " + pctCode + " which is taken from permission object: " + permission.getDn());
                    }
                } else {
                    log.trace("PCT code is blank for RPT: " + rpt.getCode());
                }
            }

            JSONObject rptAsJson = new JSONObject(ServerUtil.asJson(statusResponse));

            ExternalUmaRptClaimsContext context = new ExternalUmaRptClaimsContext(clientService.getClient(rpt.getClientId()), httpRequest, httpResponse);
            if (externalUmaRptClaimsService.externalModify(rptAsJson, context)) {
                log.trace("Successfully run external RPT Claims script associated with {}", rpt.getClientId());
            } else {
                rptAsJson = new JSONObject(ServerUtil.asJson(statusResponse));
                log.trace("Canceled changes made by external RPT Claims script since method returned `false`.");
            }

            return Response.status(Response.Status.OK)
                    .entity(rptAsJson.toString())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .cacheControl(ServerUtil.cacheControl(true))
                    .build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, UmaErrorResponseType.SERVER_ERROR, "Internal error.");
        }
    }

    private boolean isValid(UmaRPT p_rpt) {
        if (p_rpt != null) {
            p_rpt.checkExpired();
            return p_rpt.isValid();
        }
        return false;
    }

    private boolean isValid(UmaPermission permission) {
        if (permission != null) {
            permission.checkExpired();
            return permission.isValid();
        }
        return false;
    }

    private List<io.jans.as.model.uma.UmaPermission> buildStatusResponsePermissions(UmaRPT rpt) {
        final List<io.jans.as.model.uma.UmaPermission> result = new ArrayList<io.jans.as.model.uma.UmaPermission>();
        if (rpt != null) {
            final List<UmaPermission> rptPermissions = rptService.getRptPermissions(rpt);
            if (rptPermissions != null && !rptPermissions.isEmpty()) {
                for (UmaPermission permission : rptPermissions) {
                    if (isValid(permission)) {
                        final io.jans.as.model.uma.UmaPermission toAdd = ServerUtil.convert(permission, umaScopeService);
                        if (toAdd != null) {
                            result.add(toAdd);
                        }
                    } else {
                        log.debug("Ignore permission, skip it in response because permission is not valid. Permission dn: {}, rpt dn: {}",
                                permission.getDn(), rpt.getDn());
                    }
                }
            }
        }
        return result;
    }

    @GET
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response requestRptStatusGet(@HeaderParam("Authorization") String authorization,
                                        @FormParam("token") String rpt,
                                        @FormParam("token_type_hint") String tokenTypeHint) {
        throw new WebApplicationException(Response.status(405).type(MediaType.APPLICATION_JSON_TYPE).entity("Introspection of RPT is not allowed by GET HTTP method.").build());
    }
}
