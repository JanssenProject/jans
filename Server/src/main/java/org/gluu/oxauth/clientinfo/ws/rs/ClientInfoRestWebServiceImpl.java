/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.clientinfo.ws.rs;

import org.gluu.model.GluuAttribute;
import org.gluu.oxauth.model.clientinfo.ClientInfoErrorResponseType;
import org.gluu.oxauth.model.clientinfo.ClientInfoParamsValidator;
import org.gluu.oxauth.model.common.AbstractToken;
import org.gluu.oxauth.model.common.AuthorizationGrant;
import org.gluu.oxauth.model.common.AuthorizationGrantList;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.AttributeService;
import org.gluu.oxauth.service.ClientService;
import org.gluu.oxauth.service.ScopeService;
import org.gluu.oxauth.service.token.TokenService;
import org.gluu.oxauth.util.ServerUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Set;

/**
 * Provides interface for Client Info REST web services
 *
 * @author Javier Rojas Blum
 * @version 0.9 March 27, 2015
 */
@Path("/")
public class ClientInfoRestWebServiceImpl implements ClientInfoRestWebService {

	@Inject
	private Logger log;

	@Inject
	private ErrorResponseFactory errorResponseFactory;

	@Inject
	private AuthorizationGrantList authorizationGrantList;

	@Inject
	private ScopeService scopeService;

	@Inject
	private ClientService clientService;

	@Inject
    private AttributeService attributeService;
    
    @Inject
    private TokenService tokenService;

    @Override
    public Response requestClientInfoGet(String accessToken, String authorization, SecurityContext securityContext) {
        return requestClientInfo(accessToken, authorization, securityContext);
    }

    @Override
    public Response requestClientInfoPost(String accessToken, String authorization, SecurityContext securityContext) {
        return requestClientInfo(accessToken, authorization, securityContext);
    }

    public Response requestClientInfo(String accessToken, String authorization, SecurityContext securityContext) {
        if (tokenService.isBearerAuthToken(authorization)) {
            accessToken = tokenService.getBearerToken(authorization);
        }
        log.debug("Attempting to request Client Info, Access token = {}, Is Secure = {}",
                new Object[] { accessToken, securityContext.isSecure() });
        Response.ResponseBuilder builder = Response.ok();

        if (!ClientInfoParamsValidator.validateParams(accessToken)) {
            builder = Response.status(400);
            builder.entity(errorResponseFactory.errorAsJson(ClientInfoErrorResponseType.INVALID_REQUEST, "Failed to validate access token."));
        } else {
            AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);

            if (authorizationGrant == null) {
                log.trace("Failed to find authorization grant for access token.");
                return Response.status(400).entity(errorResponseFactory.getErrorAsJson(ClientInfoErrorResponseType.INVALID_TOKEN,"","Unable to find grant object associated with access token.")).build();
            }

            final AbstractToken token = authorizationGrant.getAccessToken(accessToken);
            if (token == null || !token.isValid()) {
                log.trace("Invalid access token.");
                return Response.status(400).entity(errorResponseFactory.getErrorAsJson(ClientInfoErrorResponseType.INVALID_TOKEN,"","Invalid access token.")).build();
            }

            builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
            builder.header("Pragma", "no-cache");
            builder.entity(getJSonResponse(authorizationGrant.getClient(), authorizationGrant.getScopes()));
        }

        return builder.build();
    }

    /**
     * Builds a JSon String with the response parameters.
     */
    public String getJSonResponse(Client client, Set<String> scopes) {
        JSONObject jsonObj = new JSONObject();

        try {
            for (String scopeName : scopes) {
                Scope scope = scopeService.getScopeById(scopeName);

                if (scope.getOxAuthClaims() != null) {
                    for (String claimDn : scope.getOxAuthClaims()) {
                        GluuAttribute attribute = attributeService.getAttributeByDn(claimDn);

                        String attributeName = attribute.getName();
                        Object attributeValue = clientService.getAttribute(client, attribute.getName());

                        jsonObj.put(attributeName, attributeValue);
                    }
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return jsonObj.toString();
    }
}