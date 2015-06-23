package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.AuthorizationCodeFlowParams;
import org.xdi.oxd.common.response.AuthorizationCodeFlowResponse;
import org.xdi.oxd.server.DiscoveryService;
import org.xdi.oxd.server.HttpService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/06/2015
 */

public class AuthorizationCodeFlowOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationCodeFlowOperation.class);

    protected AuthorizationCodeFlowOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {
        try {
            final AuthorizationCodeFlowParams params = asParams(AuthorizationCodeFlowParams.class);

            final OpenIdConfigurationResponse discovery = DiscoveryService.getInstance().getDiscoveryResponse(params.getDiscoveryUrl());
            if (discovery != null) {
                return okResponse(requestToken(discovery, params));
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }

    private AuthorizationCodeFlowResponse requestToken(OpenIdConfigurationResponse discovery, AuthorizationCodeFlowParams params) {
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
        authorizeClient.setExecutor(HttpService.getInstance().getClientExecutor());
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
            tokenClient1.setExecutor(HttpService.getInstance().getClientExecutor());
            tokenClient1.setRequest(tokenRequest);
            final TokenResponse response2 = tokenClient1.exec();
            ClientUtils.showClient(tokenClient1);

            if (response2.getStatus() == 200 || response2.getStatus() == 302) { // success or redirect
                if (Util.allNotBlank(response2.getAccessToken(), response2.getRefreshToken())) {
                    final AuthorizationCodeFlowResponse opResponse = new AuthorizationCodeFlowResponse();
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