/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.RptIntrospectionResponse;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.uma.authorization.UmaPCT;
import org.xdi.oxauth.uma.authorization.UmaRPT;
import org.xdi.oxauth.uma.service.UmaPctService;
import org.xdi.oxauth.uma.service.UmaRptService;
import org.xdi.oxauth.uma.service.UmaScopeService;
import org.xdi.oxauth.uma.service.UmaValidationService;
import org.xdi.oxauth.util.ServerUtil;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * The endpoint at which the host requests the status of an RPT presented to it by a requester.
 * The endpoint is RPT introspection profile implementation defined by
 * http://docs.kantarainitiative.org/uma/draft-uma-core.html#uma-bearer-token-profile
 *
 * @author Yuriy Zabrovarnyy
 */
@Path("/rpt/status")
@Api(value = "/rpt/status", description = "The endpoint at which the host requests the status of an RPT presented to it by a requester." +
        " The endpoint is RPT introspection profile implementation defined by UMA specification")
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectGet(@HeaderParam("Authorization") String authorization,
                                  @QueryParam("token") String token,
                                  @QueryParam("token_type_hint") String tokenTypeHint) {
        return introspect(authorization, token, tokenTypeHint);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectPost(@HeaderParam("Authorization") String authorization,
                                   @FormParam("token") String token,
                                   @FormParam("token_type_hint") String tokenTypeHint) {
        return introspect(authorization, token, tokenTypeHint);
    }

    private Response introspect(String authorization, String token, String tokenTypeHint) {
        try {
            umaValidationService.assertHasProtectionScope(authorization);

            final UmaRPT rpt = rptService.getRPTByCode(token);

            if (!isValid(rpt)) {
                return Response.status(Response.Status.OK).
                        entity(new RptIntrospectionResponse(false)).
                        cacheControl(ServerUtil.cacheControl(true)).
                        build();
            }

            final List<org.xdi.oxauth.model.uma.UmaPermission> permissions = buildStatusResponsePermissions(rpt);

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


            // convert manually to avoid possible conflict between resteasy providers, e.g. jettison, jackson
            final String entity = ServerUtil.asJson(statusResponse);

            return Response.status(Response.Status.OK).entity(entity).cacheControl(ServerUtil.cacheControl(true)).build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
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

    private List<org.xdi.oxauth.model.uma.UmaPermission> buildStatusResponsePermissions(UmaRPT rpt) {
        final List<org.xdi.oxauth.model.uma.UmaPermission> result = new ArrayList<org.xdi.oxauth.model.uma.UmaPermission>();
        if (rpt != null) {
            final List<UmaPermission> rptPermissions = rptService.getRptPermissions(rpt);
            if (rptPermissions != null && !rptPermissions.isEmpty()) {
                for (UmaPermission permission : rptPermissions) {
                    if (isValid(permission)) {
                        final org.xdi.oxauth.model.uma.UmaPermission toAdd = ServerUtil.convert(permission, umaScopeService);
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
    @ApiOperation(value = "Not allowed")
    @ApiResponses(value = {
            @ApiResponse(code = 405, message = "Introspection of RPT is not allowed by GET HTTP method.")
    })
    public Response requestRptStatusGet(@HeaderParam("Authorization") String authorization,
                                        @FormParam("token") String rpt,
                                        @FormParam("token_type_hint") String tokenTypeHint) {
        throw new WebApplicationException(Response.status(405).entity("Introspection of RPT is not allowed by GET HTTP method.").build());
    }
}
