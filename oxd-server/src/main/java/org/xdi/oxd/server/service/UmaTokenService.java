package org.xdi.oxd.server.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.UmaRptIntrospectionService;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.uma.RptIntrospectionResponse;
import org.xdi.oxauth.model.uma.UmaMetadata;
import org.xdi.oxauth.model.uma.UmaScopeType;
import org.xdi.oxauth.model.uma.UmaTokenResponse;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.model.Aat;
import org.xdi.oxd.server.model.Pat;
import org.xdi.oxd.server.model.UmaToken;
import org.xdi.oxd.server.model.UmaTokenFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/06/2016
 */

public class UmaTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(UmaTokenService.class);

    private final RpService rpService;
    private final ValidationService validationService;
    private final DiscoveryService discoveryService;
    private final HttpService httpService;
    private final Configuration configuration;
    private final StateService stateService;

    @Inject
    public UmaTokenService(RpService rpService,
                           ValidationService validationService,
                           DiscoveryService discoveryService,
                           HttpService httpService,
                           Configuration configuration,
                           StateService stateService
    ) {
        this.rpService = rpService;
        this.validationService = validationService;
        this.discoveryService = discoveryService;
        this.httpService = httpService;
        this.configuration = configuration;
        this.stateService = stateService;
    }

    public String getRpt(String oxdId, boolean forceNew) {
        Rp site = rpService.getRp(oxdId);
        UmaMetadata discovery = discoveryService.getUmaDiscoveryByOxdId(oxdId);

        if (!forceNew && !Strings.isNullOrEmpty(site.getRpt()) && site.getRptExpiresAt() != null) {
            if (!isExpired(site.getRptExpiresAt())) {
                LOG.debug("RPT from site configuration, RPT: " + site.getRpt() + ", site: " + site);
                return site.getRpt();
            }
        }

        final org.xdi.oxauth.client.uma.UmaTokenService tokenService = UmaClientFactory.instance().createTokenService(discovery, httpService.getClientExecutor());
        final UmaTokenResponse tokenResponse = tokenService.requestRpt();

        if (tokenResponse != null && StringUtils.isNotBlank(tokenResponse.getAccessToken())) {
            UmaRptIntrospectionService introspectionService = UmaClientFactory.instance().createRptStatusService(discovery, httpService.getClientExecutor());
            RptIntrospectionResponse status = introspectionService.requestRptStatus("Bearer " + getPat(oxdId).getToken(), tokenResponse.getAccessToken(), "");
            LOG.debug("RPT " + tokenResponse.getAccessToken() + ", status: " + status);
            if (status.getActive()) {
                LOG.debug("RPT is successfully obtained from AS. RPT: {}", tokenResponse.getAccessToken());

                site.setRpt(tokenResponse.getAccessToken());
                site.setRptCreatedAt(status.getIssuedAt());
                site.setRptExpiresAt(status.getExpiresAt());
                rpService.updateSilently(site);

                return tokenResponse.getAccessToken();
            }
        }

        LOG.error("Failed to get RPT for site: " + site);
        throw new ErrorResponseException(ErrorResponseCode.FAILED_TO_GET_RPT);
    }

    public static boolean isExpired(Date expiredAt) {
        return expiredAt.before(new Date());
    }

    public Pat getPat(String oxdId) {
        validationService.notBlankOxdId(oxdId);

        Rp site = rpService.getRp(oxdId);

        if (site.getPat() != null && site.getPatCreatedAt() != null && site.getPatExpiresIn() > 0) {
            Calendar expiredAt = Calendar.getInstance();
            expiredAt.setTime(site.getPatCreatedAt());
            expiredAt.add(Calendar.SECOND, site.getPatExpiresIn());

            if (!isExpired(expiredAt.getTime())) {
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
                    LOG.error("Please check AS(oxauth) configuration and make sure UMA scopes (uma_protection and uma_authorization) are enabled.");
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
            throw new ErrorResponseException(ErrorResponseCode.INVALID_STATE);
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
