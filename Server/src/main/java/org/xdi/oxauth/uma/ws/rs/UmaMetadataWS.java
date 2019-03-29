/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.UmaMetadata;
import org.xdi.oxauth.util.ServerUtil;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * The endpoint at which the requester can obtain UMA2 metadata.
 */
@Path("/uma2-configuration")
@Api(value = "/.well-known/uma2-configuration", description = "The authorization server endpoint that provides configuration data in a JSON [RFC4627] document that resides in at /.well-known/uma2-configuration directory at its hostmeta [hostmeta] location. The configuration data documents conformance options and endpoints supported by the authorization server. ")
public class UmaMetadataWS {

    public static final String UMA_SCOPES_SUFFIX = "/uma/scopes";
    public static final String UMA_CLAIMS_GATHERING_PATH = "/uma/gather_claims";

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AppConfiguration appConfiguration;

    @GET
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(
            value = "Provides configuration data as json document. It contains options and endpoints supported by the authorization server.",
            response = UmaMetadata.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Failed to build Uma configuration json object.")
    })
    public Response getConfiguration() {
        try {
            final String baseEndpointUri = appConfiguration.getBaseEndpoint();

            final UmaMetadata c = new UmaMetadata();
            c.setIssuer(appConfiguration.getIssuer());
            c.setGrantTypesSupported(new String[]{
                    GrantType.AUTHORIZATION_CODE.getValue(),
                    GrantType.IMPLICIT.getValue(),
                    GrantType.CLIENT_CREDENTIALS.getValue(),
                    GrantType.OXAUTH_UMA_TICKET.getValue()
            });
            c.setResponseTypesSupported(new String[]{
                    ResponseType.CODE.getValue(), ResponseType.ID_TOKEN.getValue(), ResponseType.TOKEN.getValue()
            });
            c.setTokenEndpointAuthMethodsSupported(appConfiguration.getTokenEndpointAuthMethodsSupported().toArray(new String[appConfiguration.getTokenEndpointAuthMethodsSupported().size()]));
            c.setTokenEndpointAuthSigningAlgValuesSupported(appConfiguration.getTokenEndpointAuthSigningAlgValuesSupported().toArray(new String[appConfiguration.getTokenEndpointAuthSigningAlgValuesSupported().size()]));
            c.setUiLocalesSupported(appConfiguration.getUiLocalesSupported().toArray(new String[appConfiguration.getUiLocalesSupported().size()]));
            c.setOpTosUri(appConfiguration.getOpTosUri());
            c.setOpPolicyUri(appConfiguration.getOpPolicyUri());
            c.setJwksUri(appConfiguration.getJwksUri());
            c.setServiceDocumentation(appConfiguration.getServiceDocumentation());

            c.setUmaProfilesSupported(new String[0]);
            c.setRegistrationEndpoint(appConfiguration.getRegistrationEndpoint());
            c.setTokenEndpoint(appConfiguration.getTokenEndpoint());
            c.setAuthorizationEndpoint(appConfiguration.getAuthorizationEndpoint());
            c.setIntrospectionEndpoint(baseEndpointUri + "/rpt/status");
            c.setResourceRegistrationEndpoint(baseEndpointUri + "/host/rsrc/resource_set");
            c.setPermissionEndpoint(baseEndpointUri + "/host/rsrc_pr");
            c.setScopeEndpoint(baseEndpointUri + UMA_SCOPES_SUFFIX);
            c.setClaimsInteractionEndpoint(baseEndpointUri + UMA_CLAIMS_GATHERING_PATH);

            // convert manually to avoid possible conflicts between resteasy providers, e.g. jettison, jackson
            final String entity = ServerUtil.asPrettyJson(c);
            log.trace("Uma metadata: {}", entity);

            return Response.ok(entity).build();
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

}
