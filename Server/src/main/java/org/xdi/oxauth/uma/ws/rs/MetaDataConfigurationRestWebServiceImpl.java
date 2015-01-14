/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.Configuration;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.MetadataConfiguration;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.util.ServerUtil;

/**
 * The endpoint at which the requester can obtain UMA metadata configuration.
 *
 * @author Yuriy Movchan Date: 10/25/2012
 */
@Name("umaMetaDataConfigurationRestWebService")
public class MetaDataConfigurationRestWebServiceImpl implements MetaDataConfigurationRestWebService {

    public static final String UMA_SCOPES_SUFFIX = "/uma/scopes";

    @Logger
    private Log log;
    @In
    private ErrorResponseFactory errorResponseFactory;

    public Response getMetadataConfiguration() {
        try {
            final Configuration configuration = ConfigurationFactory.getConfiguration();
            final String baseEndpointUri = configuration.getBaseEndpoint();

            final MetadataConfiguration c = new MetadataConfiguration();
            c.setVersion("1.0");
            c.setIssuer(configuration.getIssuer());
            c.setPatProfilesSupported(new String[]{"bearer"});
            c.setAatProfilesSupported(new String[]{"bearer"});
            c.setRptProfilesSupported(new String[]{"bearer"});
            c.setPatGrantTypesSupported(new String[]{"authorization_code"});
            c.setAatGrantTypesSupported(new String[]{"authorization_code"});
            c.setClaimProfilesSupported(new String[]{"openid"});
            c.setDynamicClientEndpoint(baseEndpointUri + "/oxauth/register");
            c.setTokenEndpoint(baseEndpointUri + "/oxauth/token");
            c.setUserEndpoint(baseEndpointUri + "/oxauth/authorize");
            c.setIntrospectionEndpoint(baseEndpointUri + "/host/status");
            c.setResourceSetRegistrationEndpoint(baseEndpointUri + "/host/rsrc/resource_set");
            c.setPermissionRegistrationEndpoint(baseEndpointUri + "/host/rsrc_pr");
            c.setRptEndpoint(baseEndpointUri + "/requester/rpt");
            c.setAuthorizationRequestEndpoint(baseEndpointUri + "/requester/perm");
            c.setScopeEndpoint(baseEndpointUri + UMA_SCOPES_SUFFIX);


            // convert manually to avoid possible conflicts between resteasy providers, e.g. jettison, jackson
            final String entity = ServerUtil.asJson(c);
            log.trace("Uma configuration: {0}", entity);

            return Response.ok(entity).build();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

}
