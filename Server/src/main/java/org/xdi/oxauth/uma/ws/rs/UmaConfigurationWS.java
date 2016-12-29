/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.TokenType;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.RptProfiles;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.util.ServerUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * The endpoint at which the requester can obtain UMA metadata configuration.
 *
 * @author Yuriy Movchan Date: 10/25/2012
 * @author Yuriy Zabrovarnyy Date: 03/12/2015
 */
@Name("umaMetaDataConfigurationRestWebService")
@Path("/oxauth/uma-configuration")
@Api(value = "/.well-known/uma-configuration", description = "The authorization server endpoint that provides configuration data in a JSON [RFC4627] document that resides in at /.well-known/uma-configuration directory at its hostmeta [hostmeta] location. The configuration data documents conformance options and endpoints supported by the authorization server. ")
public class UmaConfigurationWS {

    public static final String UMA_SCOPES_SUFFIX = "/uma/scopes";

    @Logger
    private Log log;
    @In
    private ErrorResponseFactory errorResponseFactory;

    @In
    private AppConfiguration appConfiguration;

    @GET
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(
            value = "Provides configuration data as json document. It contains options and endpoints supported by the authorization server.",
            response = UmaConfiguration.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Failed to build Uma configuration json object.")
    })
    public Response getConfiguration() {
        try {
            final String baseEndpointUri = appConfiguration.getBaseEndpoint();

            final UmaConfiguration c = new UmaConfiguration();
            c.setVersion("1.0");
            c.setIssuer(appConfiguration.getIssuer());
            c.setPatProfilesSupported(new String[]{TokenType.BEARER.getName()});
            c.setAatProfilesSupported(new String[]{TokenType.BEARER.getName()});
            c.setRptProfilesSupported(new String[]{RptProfiles.BEARER.getIdentifyingUri()});
            c.setPatGrantTypesSupported(new String[]{
                    GrantType.AUTHORIZATION_CODE.getValue(),
                    GrantType.IMPLICIT.getValue(),
                    GrantType.CLIENT_CREDENTIALS.getValue()
            });
            c.setAatGrantTypesSupported(new String[]{
                    GrantType.AUTHORIZATION_CODE.getValue(),
                    GrantType.IMPLICIT.getValue(),
                    GrantType.CLIENT_CREDENTIALS.getValue()
            });
            c.setClaimTokenProfilesSupported(new String[]{"openid"});
            c.setUmaProfilesSupported(new String[0]);
            c.setDynamicClientEndpoint(baseEndpointUri + "/oxauth/register");
            c.setTokenEndpoint(baseEndpointUri + "/oxauth/token");
            c.setAuthorizationEndpoint(baseEndpointUri + "/requester/perm");
            c.setRequestingPartyClaimsEndpoint("");
            c.setIntrospectionEndpoint(baseEndpointUri + "/rpt/status");
            c.setResourceSetRegistrationEndpoint(baseEndpointUri + "/host/rsrc/resource_set");
            c.setPermissionRegistrationEndpoint(baseEndpointUri + "/host/rsrc_pr");
            c.setRptEndpoint(baseEndpointUri + "/requester/rpt");
            c.setGatEndpoint(baseEndpointUri + "/requester/gat");
            c.setScopeEndpoint(baseEndpointUri + UMA_SCOPES_SUFFIX);
            c.setRptAsJwt(appConfiguration.getUmaRptAsJwt());

            // convert manually to avoid possible conflicts between resteasy providers, e.g. jettison, jackson
            final String entity = ServerUtil.asPrettyJson(c);
            log.trace("Uma configuration: {0}", entity);

            return Response.ok(entity).build();
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

}
