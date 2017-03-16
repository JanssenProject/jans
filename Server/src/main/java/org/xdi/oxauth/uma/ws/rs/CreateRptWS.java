/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.config.WebKeysConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.token.JsonWebResponse;
import org.xdi.oxauth.model.token.JwtSigner;
import org.xdi.oxauth.model.uma.GatRequest;
import org.xdi.oxauth.model.uma.RPTResponse;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.service.uma.RptManager;
import org.xdi.oxauth.service.uma.UmaValidationService;
import org.xdi.oxauth.service.uma.authorization.AuthorizationService;
import org.xdi.oxauth.util.ServerUtil;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * The endpoint at which the requester can obtain UMA metadata configuration.
 *
 * @author Yuriy Zabrovarnyy
 */
@Path("/requester")
@Api(value = "/requester/rpt", description = "The endpoint at which the requester asks the AM to issue an RPT")
public class CreateRptWS {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private RptManager rptManager;

    @Inject
    private UmaValidationService umaValidationService;

    @Inject
    private TokenService tokenService;

    @Inject
    private AuthorizationService umaAuthorizationService;

    @Inject
    private LdapEntryManager ldapEntryManager;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Path("rpt")
    @POST
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(value = "The endpoint at which the requester asks the AM to issue an RPT",
            produces = UmaConstants.JSON_MEDIA_TYPE,
            notes = "The endpoint at which the requester asks the AM to issue an RPT")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response getRpt(@HeaderParam("Authorization") String authorization,
                           @HeaderParam("Host") String amHost) {
        try {
            umaValidationService.assertHasAuthorizationScope(authorization);
            String validatedAmHost = umaValidationService.validateAmHost(amHost);

            UmaRPT rpt = rptManager.createRPT(authorization, validatedAmHost, false);

            String rptResponse = rpt.getCode();
            final Boolean umaRptAsJwt = appConfiguration.getUmaRptAsJwt();
            if (umaRptAsJwt != null && umaRptAsJwt) {
                rptResponse = createJwr(rpt, authorization, Lists.<String>newArrayList()).asString();
            }

            return Response.status(Response.Status.CREATED).
                    entity(ServerUtil.asJson(new RPTResponse(rptResponse))).
                    build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

    private JsonWebResponse createJwr(UmaRPT rpt, String authorization, List<String> gluuAccessTokenScopes) throws Exception {
        final AuthorizationGrant grant = tokenService.getAuthorizationGrant(authorization);

        JwtSigner jwtSigner = JwtSigner.newJwtSigner(appConfiguration, webKeysConfiguration, grant.getClient());
        Jwt jwt = jwtSigner.newJwt();

        jwt.getClaims().setExpirationTime(rpt.getExpirationDate());
        jwt.getClaims().setIssuedAt(rpt.getCreationDate());

        if (!gluuAccessTokenScopes.isEmpty()) {
            jwt.getClaims().setClaim("scopes", gluuAccessTokenScopes);
        }

        return jwtSigner.sign();
    }

    @Path("gat")
    @POST
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(value = "The endpoint at which the requester asks the AM to issue an GAT",
            produces = UmaConstants.JSON_MEDIA_TYPE,
            notes = "The endpoint at which the requester asks the AM to issue an GAT")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response getGat(@HeaderParam("Authorization") String authorization,
                           @HeaderParam("Host") String amHost,
                           GatRequest request,
                           @Context HttpServletRequest httpRequest) {
        try {
            umaValidationService.assertHasAuthorizationScope(authorization);
            String validatedAmHost = umaValidationService.validateAmHost(amHost);

            UmaRPT rpt = rptManager.createRPT(authorization, validatedAmHost, true);

            authorizeGat(request, rpt, authorization, httpRequest);

            String rptResponse = rpt.getCode();
            final Boolean umaRptAsJwt = appConfiguration.getUmaRptAsJwt();
            if (umaRptAsJwt != null && umaRptAsJwt) {
                rptResponse = createJwr(rpt, authorization, request.getScopes()).asString();
            }

            return Response.status(Response.Status.CREATED).
                    entity(ServerUtil.asJson(new RPTResponse(rptResponse))).
                    build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

    private void authorizeGat(GatRequest request, UmaRPT rpt, String authorization, HttpServletRequest httpRequest) {
        if (request.getScopes().isEmpty()) {
            return; // nothing to authorize
        }

        AuthorizationGrant grant = tokenService.getAuthorizationGrant(authorization);
        if (umaAuthorizationService.allowToAddPermissionForGat(grant, rpt, request.getScopes(), httpRequest, request.getClaims())) {
            final List<String> scopes = new ArrayList<String>();
            if (rpt.getPermissions() != null) {
                scopes.addAll(rpt.getPermissions());
            }
            scopes.addAll(request.getScopes());
            rpt.setPermissions(scopes);

            try {
                ldapEntryManager.merge(rpt);
                return;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        // throw not authorized exception
        throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.NOT_AUTHORIZED_PERMISSION)).build());

    }
}
