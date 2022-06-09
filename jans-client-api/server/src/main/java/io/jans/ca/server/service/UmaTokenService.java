package io.jans.ca.server.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.jans.as.client.*;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaScopeType;
import io.jans.as.model.uma.UmaTokenResponse;
import io.jans.as.model.util.Util;
import io.jans.ca.common.CoreUtils;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import io.jans.ca.common.params.RpGetRptParams;
import io.jans.ca.common.response.RpGetRptResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.Utils;
import io.jans.ca.server.configuration.ApiAppConfiguration;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.model.Pat;
import io.jans.ca.server.model.Token;
import io.jans.ca.server.model.TokenFactory;
import io.jans.ca.server.op.OpClientFactoryImpl;
import io.jans.ca.server.op.RpGetRptOperation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

/**
 * @author Yuriy Zabrovarnyy
 */

@ApplicationScoped
public class UmaTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(UmaTokenService.class);
    @Inject
    RpService rpService;
    @Inject
    RpSyncService rpSyncService;
    @Inject
    ValidationService validationService;
    @Inject
    DiscoveryService discoveryService;
    @Inject
    HttpService httpService;
    @Inject
    ApiAppConfiguration configuration;
    @Inject
    StateService stateService;
    @Inject
    private OpClientFactoryImpl opClientFactory;
    @Inject
    IntrospectionService introspectionService;


    public RpGetRptResponse getRpt(RpGetRptParams params) throws Exception {
        Rp rp = rpSyncService.getRp(params.getRpId());
        UmaMetadata discovery = discoveryService.getUmaDiscoveryByRpId(params.getRpId());

        if (!Strings.isNullOrEmpty(rp.getRpt()) && rp.getRptExpiresAt() != null) {
            if (!CoreUtils.isExpired(rp.getRptExpiresAt())) {
                LOG.debug("RPT from rp, RPT: " + rp.getRpt() + ", rp: " + rp);

                RpGetRptResponse result = new RpGetRptResponse();
                result.setRpt(rp.getRpt());
                result.setTokenType(rp.getRptTokenType());
                result.setPct(rp.getRptPct());
                result.setUpdated(rp.getRptUpgraded());
                return result;
            }
        }

        Builder client = opClientFactory.createClientRequest(discovery.getTokenEndpoint(), httpService.getClientEngine());
        client.header("Authorization", "Basic " + Utils.encodeCredentials(rp.getClientId(), rp.getClientSecret()));

        Form formRequest = new Form();
        formRequest.param("grant_type", GrantType.OXAUTH_UMA_TICKET.getValue());
        formRequest.param("ticket", params.getTicket());

        if (params.getClaimToken() != null) {
            formRequest.param("claim_token", params.getClaimToken());
        }

        if (params.getClaimTokenFormat() != null) {
            formRequest.param("claim_token_format", params.getClaimTokenFormat());
        }

        if (params.getPct() != null) {
            formRequest.param("pct", params.getPct());
        }

        if (params.getRpt() != null) {
            formRequest.param("rpt", params.getRpt());
        }

        if (params.getScope() != null) {
            formRequest.param("scope", Utils.joinAndUrlEncode(params.getScope()));
        }

        if (params.getParams() != null && !params.getParams().isEmpty()) {
            for (Map.Entry<String, String> p : params.getParams().entrySet()) {
                formRequest.param(p.getKey(), p.getValue());
            }
        }

        Response response = null;
        try {
            response = client.buildPost(Entity.form(formRequest)).invoke();
        } catch (Exception e) {
            LOG.error("Failed to receive RPT response for rp: " + rp, e);
            throw new HttpException(ErrorResponseCode.FAILED_TO_GET_RPT);
        }

        String entityResponse = null;
        try {
            entityResponse = response.readEntity(String.class);
        } catch (Exception e) {
            LOG.error("Failed to read RPT response for rp: " + rp, e);
            throw new HttpException(ErrorResponseCode.FAILED_TO_GET_RPT);
        } finally {
            response.close();
        }
        UmaTokenResponse tokenResponse = asTokenResponse(entityResponse);

        if (tokenResponse != null && StringUtils.isNotBlank(tokenResponse.getAccessToken())) {
            CorrectRptIntrospectionResponse status = introspectionService.introspectRpt(params.getRpId(), tokenResponse.getAccessToken());

            LOG.debug("RPT " + tokenResponse.getAccessToken() + ", status: " + status);
            if (status.getActive()) {
                LOG.debug("RPT is successfully obtained from AS. RPT: {}", tokenResponse.getAccessToken());

                rp.setRpt(tokenResponse.getAccessToken());
                rp.setRptTokenType(tokenResponse.getTokenType());
                rp.setRptPct(tokenResponse.getPct());
                rp.setRptUpgraded(tokenResponse.getUpgraded());
                rp.setRptCreatedAt(new Date(status.getIssuedAt() * 1000));
                rp.setRptExpiresAt(new Date(status.getExpiresAt() * 1000));
                rpService.updateSilently(rp);

                RpGetRptResponse result = new RpGetRptResponse();
                result.setRpt(rp.getRpt());
                result.setTokenType(rp.getRptTokenType());
                result.setPct(rp.getRptPct());
                result.setUpdated(rp.getRptUpgraded());
                return result;
            }
        } else {
            RpGetRptOperation.handleRptError(response.getStatus(), entityResponse);
        }

        LOG.error("Failed to get RPT for rp: " + rp);
        throw new HttpException(ErrorResponseCode.FAILED_TO_GET_RPT);
    }

    private static UmaTokenResponse asTokenResponse(String entity) {
        try {
            return Jackson2.createJsonMapper().readValue(entity, UmaTokenResponse.class);
        } catch (IOException e) {
            return null;
        }
    }

    public Pat getPat(String rpId) {
        validationService.notBlankRpId(rpId);

        Rp rp = rpSyncService.getRp(rpId);

        if (rp.getPat() != null && rp.getPatCreatedAt() != null && rp.getPatExpiresIn() != null && rp.getPatExpiresIn() > 0) {
            Calendar expiredAt = Calendar.getInstance();
            expiredAt.setTime(rp.getPatCreatedAt());
            expiredAt.add(Calendar.SECOND, rp.getPatExpiresIn());

            if (!CoreUtils.isExpired(expiredAt.getTime())) {
                LOG.debug("PAT from site configuration, PAT: " + rp.getPat());
                return new Pat(rp.getPat(), "", rp.getPatExpiresIn());
            }
        }

        return obtainPat(rpId);
    }

    public Pat obtainPat(String rpId) {
        Rp rp = rpSyncService.getRp(rpId);
        Token token = obtainToken(rpId, UmaScopeType.PROTECTION, rp);

        rp.setPat(token.getToken());
        rp.setPatCreatedAt(new Date());
        rp.setPatExpiresIn(token.getExpiresIn());
        rp.setPatRefreshToken(token.getRefreshToken());

        rpService.updateSilently(rp);

        return (Pat) token;
    }

    public Token getOAuthToken(String rpId) {
        validationService.notBlankRpId(rpId);

        Rp rp = rpSyncService.getRp(rpId);

        if (rp.getOauthToken() != null && rp.getOauthTokenCreatedAt() != null && rp.getOauthTokenExpiresIn() != null && rp.getOauthTokenExpiresIn() > 0) {
            Calendar expiredAt = Calendar.getInstance();
            expiredAt.setTime(rp.getOauthTokenCreatedAt());
            expiredAt.add(Calendar.SECOND, rp.getOauthTokenExpiresIn());

            if (!CoreUtils.isExpired(expiredAt.getTime())) {
                LOG.debug("OauthToken from site configuration, OauthToken: " + rp.getOauthToken());
                return new Token(rp.getOauthToken(), "", rp.getOauthTokenExpiresIn());
            }
        }

        return obtainOauthToken(rpId);
    }

    public Token obtainOauthToken(String rpId) {
        Rp rp = rpSyncService.getRp(rpId);
        Token token = obtainToken(rpId, null, rp);

        rp.setOauthToken(token.getToken());
        rp.setOauthTokenCreatedAt(new Date());
        rp.setOauthTokenExpiresIn(token.getExpiresIn());
        rp.setOauthTokenRefreshToken(token.getRefreshToken());

        rpService.updateSilently(rp);

        return token;
    }

    private Token obtainToken(String rpId, UmaScopeType scopeType, Rp rp) {

        OpenIdConfigurationResponse discovery = discoveryService.getConnectDiscoveryResponseByRpId(rpId);

        final Token token;
        if (useClientAuthentication(scopeType)) {
            token = obtainTokenWithClientCredentials(discovery, rp, scopeType);
            LOG.trace("Obtained token with client authentication: " + token);
        } else {
            token = obtainTokenWithUserCredentials(discovery, rp, scopeType);
            LOG.trace("Obtained token with user credentials: " + token);
        }

        return token;
    }

    public boolean useClientAuthentication(UmaScopeType scopeType) {
        if (scopeType == UmaScopeType.PROTECTION) {
            return configuration.getUseClientAuthenticationForPat() != null && configuration.getUseClientAuthenticationForPat();
        }
        return true;
    }

    private Token obtainTokenWithClientCredentials(OpenIdConfigurationResponse discovery, Rp rp, UmaScopeType scopeType) {
        final TokenClient tokenClient = opClientFactory.createTokenClientWithUmaProtectionScope(discovery.getTokenEndpoint());
        tokenClient.setExecutor(httpService.getClientEngine());
        final TokenResponse response = tokenClient.execClientCredentialsGrant(scopesAsString(scopeType), rp.getClientId(), rp.getClientSecret());
        if (response != null) {
            if (Util.allNotBlank(response.getAccessToken())) {
                if (scopeType != null && !response.getScope().contains(scopeType.getValue())) {
                    LOG.error("rp requested scope " + scopeType + " but AS returned access_token without that scope, token scopes :" + response.getScope());
                    LOG.error("Please check AS(oxauth) configuration and make sure UMA scope (uma_protection) is enabled.");
                    throw new RuntimeException("rp requested scope " + scopeType + " but AS returned access_token without that scope, token scopes :" + response.getScope());
                }

                final Token opResponse = TokenFactory.newToken(scopeType);
                opResponse.setToken(response.getAccessToken());
                opResponse.setRefreshToken(response.getRefreshToken());
                opResponse.setExpiresIn(response.getExpiresIn());
                return opResponse;
            } else {
                LOG.error("Token is blank in response, site: " + rp);
            }
        } else {
            LOG.error("No response from TokenClient");
        }
        throw new RuntimeException("Failed to obtain PAT.");
    }

    private List<String> scopes(UmaScopeType scopeType) {
        final List<String> scopes = new ArrayList<String>();
        if (scopeType != null) {
            scopes.add(scopeType.getValue());
        }
        scopes.add("openid");
        return scopes;
    }

    private String scopesAsString(UmaScopeType scopeType) {
        String scopesAsString = "";
        for (String scope : scopes(scopeType)) {
            scopesAsString += scope + " ";
        }
        return scopesAsString.trim();
    }

    private Token obtainTokenWithUserCredentials(OpenIdConfigurationResponse discovery, Rp rp, UmaScopeType scopeType) {

        // 1. Request authorization and receive the authorization code.
        final List<ResponseType> responseTypes = Lists.newArrayList();
        responseTypes.add(ResponseType.CODE);
        responseTypes.add(ResponseType.ID_TOKEN);

        final String state = stateService.generateState();

        final AuthorizationRequest request = new AuthorizationRequest(responseTypes, rp.getClientId(), scopes(scopeType), rp.getRedirectUri(), null);
        request.setState(state);
        request.setAuthUsername(rp.getUserId());
        request.setAuthPassword(rp.getUserSecret());
        request.getPrompts().add(Prompt.NONE);

        final AuthorizeClient authorizeClient = new AuthorizeClient(discovery.getAuthorizationEndpoint());
        authorizeClient.setExecutor(httpService.getClientEngine());
        authorizeClient.setRequest(request);
        final AuthorizationResponse response1 = authorizeClient.exec();

        final String scope = response1.getScope();
        final String authorizationCode = response1.getCode();
        if (!state.equals(response1.getState())) {
            throw new HttpException(ErrorResponseCode.INVALID_STATE);
        }

        if (Util.allNotBlank(authorizationCode)) {

            // 2. Request access token using the authorization code.
            final TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(rp.getRedirectUri());
            tokenRequest.setAuthUsername(rp.getClientId());
            tokenRequest.setAuthPassword(rp.getClientSecret());
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
            tokenRequest.setScope(scope);

            final TokenClient tokenClient1 = new TokenClient(discovery.getTokenEndpoint());
            tokenClient1.setRequest(tokenRequest);
            tokenClient1.setExecutor(httpService.getClientEngine());
            final TokenResponse response2 = tokenClient1.exec();

            if (response2.getStatus() == 200 && Util.allNotBlank(response2.getAccessToken())) {
                final Token token = TokenFactory.newToken(scopeType);
                token.setToken(response2.getAccessToken());
                token.setRefreshToken(response2.getRefreshToken());
                token.setExpiresIn(response2.getExpiresIn());
                return token;
            } else {
                LOG.error("Status: " + response2.getStatus() + ", Entity: " + response2.getEntity());
            }
        } else {
            LOG.debug("Authorization code is blank.");
        }
        throw new RuntimeException("Failed to obtain Token, scopeType: " + scopeType + ", site: " + rp);
    }

    public HttpService getHttpService() {
        return httpService;
    }

    public OpClientFactoryImpl getOpClientFactory() {
        return opClientFactory;
    }

    public IntrospectionService getIntrospectionService() {
        return introspectionService;
    }

    public RpService getRpService() {
        return rpService;
    }

    public DiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    public StateService getStateService() {
        return stateService;
    }
}