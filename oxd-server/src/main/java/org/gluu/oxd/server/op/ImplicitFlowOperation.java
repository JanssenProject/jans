/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.params.ImplicitFlowParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.ImplicitFlowResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/06/2015
 */

public class ImplicitFlowOperation extends BaseOperation<ImplicitFlowParams> {

    private static final Logger LOG = LoggerFactory.getLogger(ImplicitFlowOperation.class);

    protected ImplicitFlowOperation(Command p_command, final Injector injector) {
        super(p_command, injector, ImplicitFlowParams.class);
    }

    @Override
    public IOpResponse execute(ImplicitFlowParams params) {
        final OpenIdConfigurationResponse discovery = getDiscoveryService().getConnectDiscoveryResponseByOxdId(params.getOxdId());
        if (discovery != null) {
            return requestToken(discovery, params);
        }
        return null;
    }

    private ImplicitFlowResponse requestToken(OpenIdConfigurationResponse discovery, ImplicitFlowParams params) {
        // 1. Request authorization and receive the authorization code.
        final List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        responseTypes.add(ResponseType.ID_TOKEN);
        final List<String> scopes = new ArrayList<String>();
        scopes.add(params.getScope());

        String nonce = params.getNonce();
        final AuthorizationRequest request = new AuthorizationRequest(responseTypes, params.getClientId(), scopes, params.getRedirectUrl(), nonce);
        request.setState("af0ifjsldkj");
        request.setAuthUsername(params.getUserId());
        request.setAuthPassword(params.getUserSecret());
        request.getPrompts().add(Prompt.NONE);
        request.setNonce(UUID.randomUUID().toString());

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
            ClientUtils.showClient(tokenClient1);

            if (response2.getStatus() == 200 || response2.getStatus() == 302) { // success or redirect
                if (Util.allNotBlank(response2.getAccessToken(), response2.getRefreshToken())) {
                    final ImplicitFlowResponse opResponse = new ImplicitFlowResponse();
                    opResponse.setAccessToken(response2.getAccessToken());
                    opResponse.setIdToken(response2.getIdToken());
                    opResponse.setRefreshToken(response2.getRefreshToken());
                    opResponse.setAuthorizationCode(authorizationCode);
                    opResponse.setScope(scope);
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