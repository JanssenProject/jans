package org.xdi.oxd.server.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.client.uma.CreateGatService;
import org.xdi.oxauth.client.uma.CreateRptService;
import org.xdi.oxauth.client.uma.RptStatusService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.uma.GatRequest;
import org.xdi.oxauth.model.uma.RPTResponse;
import org.xdi.oxauth.model.uma.RptIntrospectionResponse;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxauth.model.uma.UmaScopeType;
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

    private final SiteConfigurationService siteService;
    private final ValidationService validationService;
    private final DiscoveryService discoveryService;
    private final HttpService httpService;
    private final Configuration configuration;

    @Inject
    public UmaTokenService(SiteConfigurationService siteService,
                           ValidationService validationService,
                           DiscoveryService discoveryService,
                           HttpService httpService,
                           Configuration configuration
    ) {
        this.siteService = siteService;
        this.validationService = validationService;
        this.discoveryService = discoveryService;
        this.httpService = httpService;
        this.configuration = configuration;
    }

    public String getRpt(String oxdId, boolean forceNew) {
        SiteConfiguration site = siteService.getSite(oxdId);
        UmaConfiguration discovery = discoveryService.getUmaDiscoveryByOxdId(oxdId);

        if (!forceNew && !Strings.isNullOrEmpty(site.getRpt()) && site.getRptExpiresAt() != null) {
            if (!isExpired(site.getRptExpiresAt())) {
                LOG.debug("RPT from site configuration, RPT: " + site.getRpt() + ", site: " + site);
                return site.getRpt();
            }
        }

        final CreateRptService rptService = UmaClientFactory.instance().createRequesterPermissionTokenService(discovery, httpService.getClientExecutor());
        final String aat = getAat(oxdId).getToken();
        final RPTResponse rptResponse = rptService.createRPT("Bearer " + aat, site.opHostWithoutProtocol());
        if (rptResponse != null && StringUtils.isNotBlank(rptResponse.getRpt())) {
            RptStatusService rptStatusService = UmaClientFactory.instance().createRptStatusService(discovery, httpService.getClientExecutor());
            RptIntrospectionResponse status = rptStatusService.requestRptStatus("Bearer " + getPat(oxdId).getToken(), rptResponse.getRpt(), "");
            LOG.debug("RPT " + rptResponse.getRpt() + ", status: " + status);
            if (status.getActive()) {
                LOG.debug("RPT is successfully obtained from AS. RPT: {}", rptResponse.getRpt());

                site.setRpt(rptResponse.getRpt());
                site.setRptCreatedAt(status.getIssuedAt());
                site.setRptExpiresAt(status.getExpiresAt());
                siteService.updateSilently(site);

                return rptResponse.getRpt();
            }
        }

        LOG.error("Failed to get RPT for site: " + site);
        throw new ErrorResponseException(ErrorResponseCode.FAILED_TO_GET_RPT);
    }

    public static boolean isExpired(Date expiredAt) {
        return expiredAt.before(new Date());
    }

    public String getGat(String oxdId, List<String> scopes) {
        SiteConfiguration site = siteService.getSite(oxdId);
        UmaConfiguration discovery = discoveryService.getUmaDiscoveryByOxdId(oxdId);

        if (!Strings.isNullOrEmpty(site.getGat()) && site.getGatExpiresAt() != null) {
            if (!isExpired(site.getGatExpiresAt())) {
                LOG.debug("GAT from site configuration, GAT: " + site.getGat() + ", site: " + site);
                return site.getGat();
            }
        }

        final CreateGatService gatService = UmaClientFactory.instance().createGatService(discovery, httpService.getClientExecutor());
        final String aat = getAat(oxdId).getToken();

        final RPTResponse response = gatService.createGAT("Bearer " + aat, site.opHostWithoutProtocol(), new GatRequest(scopes));
        if (response != null && StringUtils.isNotBlank(response.getRpt())) {
            RptStatusService rptStatusService = UmaClientFactory.instance().createRptStatusService(discovery, httpService.getClientExecutor());
            RptIntrospectionResponse status = rptStatusService.requestRptStatus("Bearer " + getPat(oxdId).getToken(), response.getRpt(), "");
            LOG.debug("RPT " + response.getRpt() + ", status: " + status);
            if (status.getActive()) {
                LOG.debug("RPT is successfully obtained from AS. RPT: {}", response.getRpt());

                site.setGat(response.getRpt());
                site.setGatCreatedAt(status.getIssuedAt());
                site.setGatExpiresAt(status.getExpiresAt());
                siteService.updateSilently(site);

                return response.getRpt();
            }
        }

        LOG.error("Failed to get GAT for site: " + site);
        throw new ErrorResponseException(ErrorResponseCode.FAILED_TO_GET_GAT);
    }

    public Pat getPat(String oxdId) {
        validationService.notBlankOxdId(oxdId);

        SiteConfiguration site = siteService.getSite(oxdId);

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
        SiteConfiguration site = siteService.getSite(oxdId);
        UmaToken token = obtainToken(oxdId, UmaScopeType.PROTECTION, site);

        site.setPat(token.getToken());
        site.setPatCreatedAt(new Date());
        site.setPatExpiresIn(token.getExpiresIn());
        site.setPatRefreshToken(token.getRefreshToken());

        siteService.updateSilently(site);

        return (Pat) token;
    }

    public Aat getAat(String oxdId) {
        validationService.notBlankOxdId(oxdId);

        SiteConfiguration site = siteService.getSite(oxdId);

        if (site.getAat() != null && site.getAatCreatedAt() != null && site.getAatExpiresIn() > 0) {
            Calendar expiredAt = Calendar.getInstance();
            expiredAt.setTime(site.getAatCreatedAt());
            expiredAt.add(Calendar.SECOND, site.getAatExpiresIn());

            if (!isExpired(expiredAt.getTime())) {
                LOG.debug("AAT from site configuration, site: " + site);
                return new Aat(site.getAat(), "", site.getAatExpiresIn());
            }
        }

        return obtainAat(oxdId);
    }

    public Aat obtainAat(String oxdId) {
        SiteConfiguration site = siteService.getSite(oxdId);
        UmaToken token = obtainToken(oxdId, UmaScopeType.AUTHORIZATION, site);

        site.setAat(token.getToken());
        site.setAatCreatedAt(new Date());
        site.setAatExpiresIn(token.getExpiresIn());
        site.setAatRefreshToken(token.getRefreshToken());

        siteService.updateSilently(site);

        return (Aat) token;
    }

    private UmaToken obtainToken(String oxdId, UmaScopeType scopeType, SiteConfiguration site) {

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
        } else if (scopeType == UmaScopeType.AUTHORIZATION) {
            return configuration.getUseClientAuthenticationForAat() != null && configuration.getUseClientAuthenticationForAat();
        } else {
            throw new RuntimeException("Unknown UMA scope type: " + scopeType);
        }
    }

    private UmaToken obtainTokenWithClientCredentials(OpenIdConfigurationResponse discovery, SiteConfiguration site, UmaScopeType scopeType) {
        final TokenClient tokenClient = new TokenClient(discovery.getTokenEndpoint());
        tokenClient.setExecutor(httpService.getClientExecutor());
        final TokenResponse response = tokenClient.execClientCredentialsGrant(scopesAsString(scopeType), site.getClientId(), site.getClientSecret());
        if (response != null) {
            if (Util.allNotBlank(response.getAccessToken())) {
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

    private UmaToken obtainTokenWithUserCredentials(OpenIdConfigurationResponse discovery, SiteConfiguration site, UmaScopeType scopeType) {

        // 1. Request authorization and receive the authorization code.
        final List<ResponseType> responseTypes = Lists.newArrayList();
        responseTypes.add(ResponseType.CODE);
        responseTypes.add(ResponseType.ID_TOKEN);


        final AuthorizationRequest request = new AuthorizationRequest(responseTypes, site.getClientId(), scopes(scopeType), site.getAuthorizationRedirectUri(), null);
        request.setState("af0ifjsldkj");
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
            }
        } else {
            LOG.debug("Authorization code is blank.");
        }
        throw new RuntimeException("Failed to obtain Token, scopeType: " + scopeType + ", site: " + site);
    }
}
