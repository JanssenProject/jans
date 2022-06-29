/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.op;

import com.google.api.client.util.Lists;
import io.jans.as.client.*;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.util.Util;
import io.jans.ca.common.CommandType;
import io.jans.ca.common.params.ImplicitFlowParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.ImplicitFlowResponse;
import io.jans.ca.server.service.DiscoveryService;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/06/2015
 */

public class ImplicitFlowOperation extends TemplateOperation<ImplicitFlowParams> {

    private static final Logger LOG = LoggerFactory.getLogger(ImplicitFlowOperation.class);
    @Inject
    DiscoveryService discoveryService;

    @Override
    public IOpResponse execute(ImplicitFlowParams params, HttpServletRequest httpServletRequest) {
        final OpenIdConfigurationResponse discovery = discoveryService.getConnectDiscoveryResponseByRpId(params.getRpId());
        if (discovery != null) {
            return requestToken(discovery, params);
        }
        return null;
    }

    private ImplicitFlowResponse requestToken(OpenIdConfigurationResponse discovery, ImplicitFlowParams params) {
        // 1. Request authorization and receive the authorization code.
        final List<ResponseType> implicitResponseTypes = new ArrayList<ResponseType>();
        implicitResponseTypes.add(ResponseType.CODE);
        implicitResponseTypes.add(ResponseType.ID_TOKEN);
        final List<String> scopes = Lists.newArrayList();
        scopes.add(params.getScope());

        String nonce = params.getNonce();
        final AuthorizationRequest implicitRequest = new AuthorizationRequest(implicitResponseTypes, params.getClientId(), scopes, params.getRedirectUrl(), nonce);
        implicitRequest.setState("af0ifjsldkj");
        implicitRequest.setAuthUsername(params.getUserId());
        implicitRequest.setAuthPassword(params.getUserSecret());
        implicitRequest.getPrompts().add(Prompt.NONE);
        implicitRequest.setNonce(UUID.randomUUID().toString());

        final AuthorizeClient authorizeClient = new AuthorizeClient(discovery.getAuthorizationEndpoint());
        authorizeClient.setRequest(implicitRequest);
        authorizeClient.setExecutor(getHttpService().getClientEngine());
        final AuthorizationResponse response1 = authorizeClient.exec();

        final String scope = response1.getScope();
        final String authorizationCode = response1.getCode();

        if (Util.allNotBlank(authorizationCode)) {

            // 2. Request access token using the authorization code.
            final TokenRequest implicitTokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            implicitTokenRequest.setCode(authorizationCode);
            implicitTokenRequest.setRedirectUri(params.getRedirectUrl());
            implicitTokenRequest.setAuthUsername(params.getClientId());
            implicitTokenRequest.setAuthPassword(params.getClientSecret());
            implicitTokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
            implicitTokenRequest.setScope(scope);

            final TokenClient tokenClient1 = new TokenClient(discovery.getTokenEndpoint());
            tokenClient1.setExecutor(getHttpService().getClientEngine());
            tokenClient1.setRequest(implicitTokenRequest);
            final TokenResponse response2 = tokenClient1.exec();

            if (response2.getStatus() == 200 || response2.getStatus() == 302) { // success or redirect
                if (Util.allNotBlank(response2.getAccessToken(), response2.getRefreshToken())) {
                    final ImplicitFlowResponse implicitFlowResponse = new ImplicitFlowResponse();
                    implicitFlowResponse.setAccessToken(response2.getAccessToken());
                    implicitFlowResponse.setIdToken(response2.getIdToken());
                    implicitFlowResponse.setRefreshToken(response2.getRefreshToken());
                    implicitFlowResponse.setAuthorizationCode(authorizationCode);
                    implicitFlowResponse.setScope(scope);
                    implicitFlowResponse.setExpiresIn(response2.getExpiresIn());
                    return implicitFlowResponse;
                }
            }
        } else {
            LOG.debug("Authorization code is blank.");
        }
        return null;
    }

    @Override
    public Class<ImplicitFlowParams> getParameterClass() {
        return ImplicitFlowParams.class;
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.IMPLICIT_FLOW;
    }
}