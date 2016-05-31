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
import org.xdi.oxd.common.params.RsProtectParams;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.service.SiteConfiguration;
import org.xdi.oxd.server.uma.Pat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public class RsProtectOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RsProtectOperation.class);

    protected RsProtectOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() throws Exception {
        final RsProtectParams params = asParams(RsProtectParams.class);

        SiteConfiguration site = getSite(params.getOxdId());
        UmaConfiguration umaDiscovery = getDiscoveryService().getUmaDiscoveryByOxdId(params.getOxdId());

        final Pat pat = obtainPat();


//        final UmaConfiguration umaDiscovery = getDiscoveryService().getUmaDiscovery(params.getUmaDiscoveryUrl());
//                     final ResourceSetRegistrationService registrationService = UmaClientFactory.instance().createResourceSetRegistrationService(umaDiscovery, getHttpService().getClientExecutor());
//
//                     final ResourceSet resourceSet = new ResourceSet();
//                     resourceSet.setName(params.getName());
//                     resourceSet.setScopes(params.getScopes());
//
//                     ResourceSetResponse addResponse = registrationService.addResourceSet("Bearer " + params.getPatToken(), resourceSet);
//                     if (addResponse != null) {
//                         final RegisterResourceOpResponse opResponse = new RegisterResourceOpResponse();
//                         opResponse.setId(addResponse.getId());
//                         return okResponse(opResponse);
//                     } else {
//                         LOG.error("No response on addResourceSet call from OP.");
//                     }
        return null;
    }

    private Pat obtainPat() throws IOException {
        final RsProtectParams params = asParams(RsProtectParams.class);

        OpenIdConfigurationResponse discovery = getDiscoveryService().getConnectDiscoveryResponseByOxdId(params.getOxdId());
        SiteConfiguration site = getSite(params.getOxdId());

        if (site.getPat() != null && site.getPatCreatedAt() != null && site.getPatExpiresIn() > 0) {
            Calendar c = Calendar.getInstance();
            c.setTime(site.getPatCreatedAt());
            c.add(Calendar.SECOND, site.getPatExpiresIn());

            boolean isPatExpired = c.getTime().after(new Date());
            if (isPatExpired) {
                LOG.debug("PAT from site configuration, PAT: " + site.getPat());
                return new Pat(site.getPat(), "", site.getPatExpiresIn());
            }
        }

        final Pat pat;
        if (useClientAuthentication()) {
            pat = obtainPatWithClientCredentials(discovery, site);
            LOG.trace("Obtained PAT with client authentication.");
        } else {
            pat = obtainPatWithUserCredentials(discovery, site);
            LOG.trace("Obtained PAT with user credentials.");
        }

        site.setPat(pat.getPat());
        site.setPatCreatedAt(new Date());
        site.setPatExpiresIn(pat.getExpiresIn());
        site.setPatRefreshToken(pat.getRefreshToken());

        getSiteService().update(site);
        return pat;
    }

    public boolean useClientAuthentication() {
        final Configuration c = getConfiguration();
        return c != null && c.getUseClientAuthenticationForPat() != null && c.getUseClientAuthenticationForPat();
    }

    private Pat obtainPatWithClientCredentials(OpenIdConfigurationResponse discovery, SiteConfiguration site) {
        final TokenClient tokenClient = new TokenClient(discovery.getTokenEndpoint());
        tokenClient.setExecutor(getHttpService().getClientExecutor());
        final TokenResponse response = tokenClient.execClientCredentialsGrant(scopesAsString(), site.getClientId(), site.getClientSecret());
        if (response != null) {
            final String patToken = response.getAccessToken();
            if (Util.allNotBlank(patToken)) {
                final Pat opResponse = new Pat();
                opResponse.setPat(patToken);
                opResponse.setRefreshToken(response.getRefreshToken());
                opResponse.setExpiresIn(response.getExpiresIn());
                return opResponse;
            } else {
                LOG.error("PAT token is blank in response");
            }
        } else {
            LOG.error("No response from TokenClient");
        }
        throw new RuntimeException("Failed to obtain PAT.");
    }

    private List<String> scopes() {
        final List<String> scopes = new ArrayList<String>();
        scopes.add("openid");
        scopes.add(UmaScopeType.PROTECTION.getValue());
        return scopes;
    }

    private String scopesAsString() {
        String scopesAsString = "";
        for (String scope : scopes()) {
            scopesAsString += scope + " ";
        }
        return scopesAsString;
    }

    private Pat obtainPatWithUserCredentials(OpenIdConfigurationResponse discovery, SiteConfiguration site) {
        // 1. Request authorization and receive the authorization code.
        final List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        responseTypes.add(ResponseType.ID_TOKEN);


        final AuthorizationRequest request = new AuthorizationRequest(responseTypes, site.getClientId(), scopes(), site.getAuthorizationRedirectUri(), null);
        request.setState("af0ifjsldkj");
        request.setAuthUsername(site.getUserId());
        request.setAuthPassword(site.getUserSecret());
        request.getPrompts().add(Prompt.NONE);

        final AuthorizeClient authorizeClient = new AuthorizeClient(discovery.getAuthorizationEndpoint());
        authorizeClient.setExecutor(getHttpService().getClientExecutor());
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
            tokenClient1.setExecutor(getHttpService().getClientExecutor());
            final TokenResponse response2 = tokenClient1.exec();
            ClientUtils.showClient(authorizeClient);

            if (response2.getStatus() == 200) {
                final String patToken = response2.getAccessToken();
                final String patRefreshToken = response2.getRefreshToken();
                if (Util.allNotBlank(patToken, patRefreshToken)) {
                    return new Pat(patToken, patRefreshToken, response2.getExpiresIn());
                }
            }
        } else {
            LOG.debug("Authorization code is blank.");
        }
        throw new RuntimeException("Failed to obtain PAT.");
    }
}
