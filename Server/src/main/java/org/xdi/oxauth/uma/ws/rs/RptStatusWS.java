/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.GatIntrospectionResponse;
import org.xdi.oxauth.model.uma.RptIntrospectionResponse;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.uma.AbstractRPTManager;
import org.xdi.oxauth.service.uma.RPTManager;
import org.xdi.oxauth.service.uma.ScopeService;
import org.xdi.oxauth.service.uma.UmaValidationService;
import org.xdi.oxauth.util.ServerUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
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
@Api(value = "/rpt/status", description = "The endpoint at which the host requests the status of an RPT presented to it by a requester." +
        " The endpoint is RPT introspection profile implementation defined by UMA specification")

@Name("rptStatusRestWebService")
public class RptStatusWS {

    @Logger
    private Log log;
    @In
    private ErrorResponseFactory errorResponseFactory;
    @In
    private RPTManager rptManager;
    @In
    private UmaValidationService umaValidationService;
    @In
    private ScopeService umaScopeService;

    @POST
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(value = "The resource server MUST determine a received RPT's status, including both whether it is active and, if so, its associated authorization data, before giving or refusing access to the client. An RPT is associated with a set of authorization data that governs whether the client is authorized for access. The token's nature and format are dictated by its profile; the profile might allow it to be self-contained, such that the resource server is able to determine its status locally, or might require or allow the resource server to make a run-time introspection request of the authorization server that issued the token.",
            produces = UmaConstants.JSON_MEDIA_TYPE,
            notes = "The endpoint MAY allow other parameters to provide further context to\n" +
                    "   the query.  For instance, an authorization service may need to know\n" +
                    "   the IP address of the client accessing the protected resource in\n" +
                    "   order to determine the appropriateness of the token being presented.\n" +
                    "\n" +
                    "   To prevent unauthorized token scanning attacks, the endpoint MUST\n" +
                    "   also require some form of authorization to access this endpoint, such\n" +
                    "   as client authentication as described in OAuth 2.0 [RFC6749] or a\n" +
                    "   separate OAuth 2.0 access token such as the bearer token described in\n" +
                    "   OAuth 2.0 Bearer Token Usage [RFC6750].  The methods of managing and\n" +
                    "   validating these authentication credentials are out of scope of this\n" +
                    "   specification.\n"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response requestRptStatus(@HeaderParam("Authorization") String authorization,
                                     @FormParam("token")
                                     @ApiParam(value = "The string value of the token.  For access tokens,\n" +
                                             "      this is the \"access_token\" value returned from the token endpoint\n" +
                                             "      defined in OAuth 2.0 [RFC6749] section 5.1.  For refresh tokens,\n" +
                                             "      this is the \"refresh_token\" value returned from the token endpoint\n" +
                                             "      as defined in OAuth 2.0 [RFC6749] section 5.1.  Other token types\n" +
                                             "      are outside the scope of this specification.", required = true)
                                     String rptAsString,
                                     @FormParam("token_type_hint")
                                     @ApiParam(value = "A hint about the type of the token\n" +
                                             "      submitted for introspection.  The protected resource re MAY pass\n" +
                                             "      this parameter in order to help the authorization server to\n" +
                                             "      optimize the token lookup.  If the server is unable to locate the\n" +
                                             "      token using the given hint, it MUST extend its search across all\n" +
                                             "      of its supported token types.  An authorization server MAY ignore\n" +
                                             "      this parameter, particularly if it is able to detect the token\n" +
                                             "      type automatically.  Values for this field are defined in OAuth\n" +
                                             "      Token Revocation [RFC7009].", required = false)
                                     String tokenTypeHint) {
        try {
            umaValidationService.assertHasProtectionScope(authorization);

            final UmaRPT rpt = rptManager.getRPTByCode(rptAsString);

            if (rpt != null && AbstractRPTManager.isGat(rpt.getCode())) {
                return gatResponse(rpt);
            }
            if (!isValid(rpt)) {
                return Response.status(Response.Status.OK).
                        entity(new RptIntrospectionResponse(false)).
                        cacheControl(ServerUtil.cacheControl(true)).
                        build();
            }

            final List<UmaPermission> permissions = buildStatusResponsePermissions(rpt);

            // active status
            final RptIntrospectionResponse statusResponse = new RptIntrospectionResponse();
            statusResponse.setActive(true);
            statusResponse.setExpiresAt(rpt.getExpirationDate());
            statusResponse.setIssuedAt(rpt.getCreationDate());
            statusResponse.setPermissions(permissions);

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

    private Response gatResponse(UmaRPT rpt) throws IOException {
        if (!isValid(rpt)) {
            return Response.status(Response.Status.OK).
                    entity(new GatIntrospectionResponse(false)).
                    cacheControl(ServerUtil.cacheControl(true)).
                    build();
        }

        final GatIntrospectionResponse statusResponse = new GatIntrospectionResponse();
        statusResponse.setActive(true);
        statusResponse.setExpiresAt(rpt.getExpirationDate());
        statusResponse.setIssuedAt(rpt.getCreationDate());
        statusResponse.setScopes(rpt.getPermissions());

        // convert manually to avoid possible conflict between resteasy providers, e.g. jettison, jackson
        final String entity = ServerUtil.asJson(statusResponse);

        return Response.status(Response.Status.OK).
                entity(entity).
                cacheControl(ServerUtil.cacheControl(true)).
                build();

    }

    private boolean isValid(UmaRPT p_rpt) {
        if (p_rpt != null) {
            p_rpt.checkExpired();
            return p_rpt.isValid();
        }
        return false;
    }

    private boolean isValid(ResourceSetPermission resourceSetPermission) {
        if (resourceSetPermission != null) {
            resourceSetPermission.checkExpired();
            return resourceSetPermission.isValid();
        }
        return false;
    }

    private List<UmaPermission> buildStatusResponsePermissions(UmaRPT p_rpt) {
        final List<UmaPermission> result = new ArrayList<UmaPermission>();
        if (p_rpt != null) {
            final List<ResourceSetPermission> rptPermissions = rptManager.getRptPermissions(p_rpt);
            if (rptPermissions != null && !rptPermissions.isEmpty()) {
                for (ResourceSetPermission permission : rptPermissions) {
                    if (isValid(permission)) {
                        final UmaPermission toAdd = ServerUtil.convert(permission, umaScopeService);
                        if (toAdd != null) {
                            result.add(toAdd);
                        }
                    } else {
                        log.debug("Ignore permission, skip it in response because permission is not valid. Permission dn: {0}, rpt dn: {1}",
                                permission.getDn(), p_rpt.getDn());
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
