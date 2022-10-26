/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.clientinfo.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.clientinfo.ClientInfoErrorResponseType;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.model.clientinfo.ClientInfoParamsValidator;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.GluuAttribute;
import io.jans.orm.model.base.LocalizedString;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Set;

/**
 * Provides interface for Client Info REST web services
 *
 * @author Javier Rojas Blum
 * @version April 25, 2022
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
        log.debug("Attempting to request Client Info, Access token = {}, Is Secure = {}", accessToken, securityContext.isSecure());

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.CLIENTINFO);
        Response.ResponseBuilder builder = Response.ok();

        if (!ClientInfoParamsValidator.validateParams(accessToken)) {
            builder = Response.status(400);
            builder.entity(errorResponseFactory.errorAsJson(ClientInfoErrorResponseType.INVALID_REQUEST, "Failed to validate access token."));
        } else {
            AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);

            if (authorizationGrant == null) {
                log.trace("Failed to find authorization grant for access token.");
                return Response.status(400).entity(errorResponseFactory.getErrorAsJson(ClientInfoErrorResponseType.INVALID_TOKEN, "", "Unable to find grant object associated with access token.")).build();
            }

            final AbstractToken token = authorizationGrant.getAccessToken(accessToken);
            if (token == null || !token.isValid()) {
                log.trace("Invalid access token.");
                return Response.status(400).entity(errorResponseFactory.getErrorAsJson(ClientInfoErrorResponseType.INVALID_TOKEN, "", "Invalid access token.")).build();
            }

            builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
            builder.header(Constants.PRAGMA, Constants.NO_CACHE);
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

                if (scope.getClaims() != null) {
                    for (String claimDn : scope.getClaims()) {
                        GluuAttribute attribute = attributeService.getAttributeByDn(claimDn);

                        String ldapName = attribute.getName();
                        Object attributeValue = clientService.getAttribute(client, ldapName);

                        String claimName = attribute.getClaimName();

                        if (attributeValue instanceof LocalizedString) {
                            LocalizedString localizedString = (LocalizedString) attributeValue;
                            localizedString.addToJSON(jsonObj, claimName);
                        } else {
                            jsonObj.put(claimName, attributeValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return jsonObj.toString();
    }
}