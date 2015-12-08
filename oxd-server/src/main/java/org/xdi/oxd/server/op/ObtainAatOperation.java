/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxauth.model.uma.UmaScopeType;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.ObtainAatParams;
import org.xdi.oxd.common.response.ObtainAatOpResponse;
import org.xdi.oxd.server.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * AAT operation.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class ObtainAatOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(ObtainAatOperation.class);

    protected ObtainAatOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    public UmaScopeType getScope() {
        return UmaScopeType.AUTHORIZATION;
    }

    @Override
    public CommandResponse execute() {
        try {
            final ObtainAatParams params = asParams(ObtainAatParams.class);
            if (params != null) {
                final OpenIdConfigurationResponse discovery = getDiscoveryService().getConnectDiscoveryResponse(params.getDiscoveryUrl());
                final UmaConfiguration umaDiscovery = getDiscoveryService().getUmaDiscovery(params.getUmaDiscoveryUrl());
                if (discovery != null && umaDiscovery != null) {
                    final ObtainAatOpResponse r;
                    if (useClientAuthentication()) {
                        r = obtainAatWithClientCredentials(discovery, params);
                    } else {
                        r = obtainAatWithUserCredentials(discovery, params);
                    }
                    if (r != null) {
                        return okResponse(r);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }

    public boolean useClientAuthentication() {
        final Configuration c = getConfiguration();
        return c != null && c.getUseClientAuthenticationForPat() != null && c.getUseClientAuthenticationForPat();
    }

    private ObtainAatOpResponse obtainAatWithClientCredentials(OpenIdConfigurationResponse discovery, ObtainAatParams params) {
        final TokenClient tokenClient = new TokenClient(discovery.getTokenEndpoint());
        tokenClient.setExecutor(getHttpService().getClientExecutor());
        final TokenResponse response = tokenClient.execClientCredentialsGrant(getScope().getValue() + " openid", params.getClientId(), params.getClientSecret());
        if (response != null) {

            ClientUtils.showClient(tokenClient);

            final String patToken = response.getAccessToken();
            if (Util.allNotBlank(patToken)) {
                final ObtainAatOpResponse opResponse = new ObtainAatOpResponse();
                opResponse.setAatToken(patToken);
                opResponse.setAatRefreshToken(response.getRefreshToken());
                opResponse.setScope(getScope().getValue());
                opResponse.setExpiresIn(response.getExpiresIn());
                return opResponse;
            }
        }
        return null;
    }

    private ObtainAatOpResponse obtainAatWithUserCredentials(OpenIdConfigurationResponse discovery, ObtainAatParams params) {
        // 1. Request authorization and receive the authorization code.
        final List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        responseTypes.add(ResponseType.ID_TOKEN);
        final List<String> scopes = new ArrayList<String>();
        scopes.add(getScope().getValue());

        final AuthorizationRequest request = new AuthorizationRequest(responseTypes, params.getClientId(), scopes, params.getRedirectUrl(), null);
        request.setState("af0ifjsldkj");
        request.setAuthUsername(params.getUserId());
        request.setAuthPassword(params.getUserSecret());
        request.getPrompts().add(Prompt.NONE);

        final AuthorizeClient authorizeClient = new AuthorizeClient(discovery.getAuthorizationEndpoint());
        authorizeClient.setRequest(request);
        authorizeClient.setExecutor(getHttpService().getClientExecutor());
        final AuthorizationResponse response1 = authorizeClient.exec();

        ClientUtils.showClient(authorizeClient);

        final String scope = response1.getScope();
        final String authorizationCode = response1.getCode();

        if (Util.allNotBlank(authorizationCode)) {

            // 2. Request access token using the authorization code.
            final TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(params.getRedirectUrl());
            tokenRequest.setAuthUsername(params.getClientId());
            tokenRequest.setAuthPassword(params.getClientSecret());
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
            tokenRequest.setScope(scope);

            final TokenClient tokenClient1 = new TokenClient(discovery.getTokenEndpoint());
            tokenClient1.setExecutor(getHttpService().getClientExecutor());
            tokenClient1.setRequest(tokenRequest);
            final TokenResponse response2 = tokenClient1.exec();
            ClientUtils.showClient(authorizeClient);

            if (response2.getStatus() == 200) {
                final String patToken = response2.getAccessToken();
                final String patRefreshToken = response2.getRefreshToken();
                if (Util.allNotBlank(patToken, patRefreshToken)) {
                    final ObtainAatOpResponse opResponse = new ObtainAatOpResponse();
                    opResponse.setAatToken(patToken);
                    opResponse.setAatRefreshToken(patRefreshToken);
                    opResponse.setAuthorizationCode(authorizationCode);
                    opResponse.setScope(getScope().getValue());
                    opResponse.setExpiresIn(response2.getExpiresIn());
                    return opResponse;
                }
            }
        } else {
            LOG.debug("Authorization code is blank.");
        }
        return null;
    }
}
