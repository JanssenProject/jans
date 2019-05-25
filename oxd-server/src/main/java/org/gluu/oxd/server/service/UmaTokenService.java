package org.gluu.oxd.server.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.client.*;
import org.gluu.oxauth.client.uma.UmaClientFactory;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.uma.UmaMetadata;
import org.gluu.oxauth.model.uma.UmaScopeType;
import org.gluu.oxauth.model.uma.UmaTokenResponse;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.introspection.CorrectRptIntrospectionResponse;
import org.gluu.oxd.common.params.RpGetRptParams;
import org.gluu.oxd.common.response.RpGetRptResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.gluu.oxd.server.ServerLauncher;
import org.gluu.oxd.server.Utils;
import org.gluu.oxd.server.model.Pat;
import org.gluu.oxd.server.model.UmaToken;
import org.gluu.oxd.server.model.UmaTokenFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */

public class UmaTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(UmaTokenService.class);

    private final RpService rpService;
    private final ValidationService validationService;
    private final DiscoveryService discoveryService;
    private final HttpService httpService;
    private final OxdServerConfiguration configuration;
    private final StateService stateService;

    @Inject
    public UmaTokenService(RpService rpService,
                           ValidationService validationService,
                           DiscoveryService discoveryService,
                           HttpService httpService,
                           OxdServerConfiguration configuration,
                           StateService stateService
    ) {
        this.rpService = rpService;
        this.validationService = validationService;
        this.discoveryService = discoveryService;
        this.httpService = httpService;
        this.configuration = configuration;
        this.stateService = stateService;
    }

    public RpGetRptResponse getRpt(RpGetRptParams params) throws UnsupportedEncodingException {
        Rp rp = rpService.getRp(params.getOxdId());
        UmaMetadata discovery = discoveryService.getUmaDiscoveryByOxdId(params.getOxdId());

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

        final org.gluu.oxauth.client.uma.UmaTokenService tokenService = UmaClientFactory.instance().createTokenService(discovery, httpService.getClientExecutor());
        final UmaTokenResponse tokenResponse = tokenService.requestRpt(
                "Basic " + Utils.encodeCredentials(rp.getClientId(), rp.getClientSecret()),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                params.getTicket(),
                params.getClaimToken(),
                params.getClaimTokenFormat(),
                params.getPct(),
                params.getRpt(),
                params.getScope() != null ? Utils.joinAndUrlEncode(params.getScope()) : null
        );

        if (tokenResponse != null && StringUtils.isNotBlank(tokenResponse.getAccessToken())) {
            final IntrospectionService introspectionService = ServerLauncher.getInjector().getInstance(IntrospectionService.class);
            CorrectRptIntrospectionResponse status = introspectionService.introspectRpt(params.getOxdId(), tokenResponse.getAccessToken());

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
        }

        LOG.error("Failed to get RPT for rp: " + rp);
        throw new HttpException(ErrorResponseCode.FAILED_TO_GET_RPT);
    }

    public Pat getPat(String oxdId) {
        validationService.notBlankOxdId(oxdId);

        Rp site = rpService.getRp(oxdId);

        if (site.getPat() != null && site.getPatCreatedAt() != null && site.getPatExpiresIn() > 0) {
            Calendar expiredAt = Calendar.getInstance();
            expiredAt.setTime(site.getPatCreatedAt());
            expiredAt.add(Calendar.SECOND, site.getPatExpiresIn());

            if (!CoreUtils.isExpired(expiredAt.getTime())) {
                LOG.debug("PAT from site configuration, PAT: " + site.getPat());
                return new Pat(site.getPat(), "", site.getPatExpiresIn());
            }
        }

        return obtainPat(oxdId);
    }

    public Pat obtainPat(String oxdId) {
        Rp site = rpService.getRp(oxdId);
        UmaToken token = obtainToken(oxdId, UmaScopeType.PROTECTION, site);

        site.setPat(token.getToken());
        site.setPatCreatedAt(new Date());
        site.setPatExpiresIn(token.getExpiresIn());
        site.setPatRefreshToken(token.getRefreshToken());

        rpService.updateSilently(site);

        return (Pat) token;
    }

    private UmaToken obtainToken(String oxdId, UmaScopeType scopeType, Rp site) {

        OpenIdConfigurationResponse discovery = discoveryService.getConnectDiscoveryResponseByOxdId(oxdId);

        final UmaToken token;
        if (useClientAuthentication(scopeType)) {
            token = obtainTokenWithClientCredentials(discovery, site, scopeType);
            LOG.trace("Obtained token with client authentication: " + token);
        } else {
            token = obtainTokenWithUserCredentials(discovery, site, scopeType);
            LOG.trace("Obtained token with user credentials: " + token);
        }

        return token;
    }

    public boolean useClientAuthentication(UmaScopeType scopeType) {
        if (scopeType == UmaScopeType.PROTECTION) {
            return configuration.getUseClientAuthenticationForPat() != null && configuration.getUseClientAuthenticationForPat();
        } else {
            throw new RuntimeException("Unknown UMA scope type: " + scopeType);
        }
    }

    private UmaToken obtainTokenWithClientCredentials(OpenIdConfigurationResponse discovery, Rp site, UmaScopeType scopeType) {
        final TokenClient tokenClient = new TokenClient(discovery.getTokenEndpoint());
        tokenClient.setExecutor(httpService.getClientExecutor());
        final TokenResponse response = tokenClient.execClientCredentialsGrant(scopesAsString(scopeType), site.getClientId(), site.getClientSecret());
        if (response != null) {
            if (Util.allNotBlank(response.getAccessToken())) {
                if (!response.getScope().contains(scopeType.getValue())) {
                    LOG.error("oxd requested scope " + scopeType + " but AS returned access_token without that scope, token scopes :" + response.getScope());
                    LOG.error("Please check AS(oxauth) configuration and make sure UMA scope (uma_protection) is enabled.");
                    throw new RuntimeException("oxd requested scope " + scopeType + " but AS returned access_token without that scope, token scopes :" + response.getScope());
                }

                final UmaToken opResponse = UmaTokenFactory.newToken(scopeType);
                opResponse.setToken(response.getAccessToken());
                opResponse.setRefreshToken(response.getRefreshToken());
                opResponse.setExpiresIn(response.getExpiresIn());
                return opResponse;
            } else {
                LOG.error("Token is blank in response, site: " + site);
            }
        } else {
            LOG.error("No response from TokenClient");
        }
        throw new RuntimeException("Failed to obtain PAT.");
    }

    private List<String> scopes(UmaScopeType scopeType) {
        final List<String> scopes = new ArrayList<String>();
        scopes.add(scopeType.getValue());
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

    private UmaToken obtainTokenWithUserCredentials(OpenIdConfigurationResponse discovery, Rp site, UmaScopeType scopeType) {

        // 1. Request authorization and receive the authorization code.
        final List<ResponseType> responseTypes = Lists.newArrayList();
        responseTypes.add(ResponseType.CODE);
        responseTypes.add(ResponseType.ID_TOKEN);

        final String state = stateService.generateState();

        final AuthorizationRequest request = new AuthorizationRequest(responseTypes, site.getClientId(), scopes(scopeType), site.getAuthorizationRedirectUri(), null);
        request.setState(state);
        request.setAuthUsername(site.getUserId());
        request.setAuthPassword(site.getUserSecret());
        request.getPrompts().add(Prompt.NONE);

        final AuthorizeClient authorizeClient = new AuthorizeClient(discovery.getAuthorizationEndpoint());
        authorizeClient.setExecutor(httpService.getClientExecutor());
        authorizeClient.setRequest(request);
        final AuthorizationResponse response1 = authorizeClient.exec();

        ClientUtils.showClient(authorizeClient);

        final String scope = response1.getScope();
        final String authorizationCode = response1.getCode();
        if (!state.equals(response1.getState())) {
            throw new HttpException(ErrorResponseCode.INVALID_STATE);
        }

        if (Util.allNotBlank(authorizationCode)) {

            // 2. Request access token using the authorization code.
            final TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(site.getAuthorizationRedirectUri());
            tokenRequest.setAuthUsername(site.getClientId());
            tokenRequest.setAuthPassword(site.getClientSecret());
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
            tokenRequest.setScope(scope);

            final TokenClient tokenClient1 = new TokenClient(discovery.getTokenEndpoint());
            tokenClient1.setRequest(tokenRequest);
            tokenClient1.setExecutor(httpService.getClientExecutor());
            final TokenResponse response2 = tokenClient1.exec();
            ClientUtils.showClient(authorizeClient);

            if (response2.getStatus() == 200 && Util.allNotBlank(response2.getAccessToken())) {
                final UmaToken token = UmaTokenFactory.newToken(scopeType);
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
        throw new RuntimeException("Failed to obtain Token, scopeType: " + scopeType + ", site: " + site);
    }
}
