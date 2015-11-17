package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.ClientUtils;
import org.xdi.oxauth.client.TokenClient;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.client.TokenResponse;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.GetTokensByCodeParams;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.server.service.SiteConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetTokensByCodeOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(GetTokensByCodeOperation.class);

    /**
     * Base constructor
     *
     * @param p_command command
     */
    protected GetTokensByCodeOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {
        try {
            final GetTokensByCodeParams params = asParams(GetTokensByCodeParams.class);
            final SiteConfiguration site = getSite(params.getOxdId());

            final TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(params.getCode());
            tokenRequest.setRedirectUri(site.getAuthorizationRedirectUri());
            tokenRequest.setAuthUsername(site.getClientId());
            tokenRequest.setAuthPassword(site.getClientSecret());
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
            tokenRequest.setScope(asCommaSeparatedString(site.getScope()));

            final TokenClient tokenClient = new TokenClient(getDiscoveryService().getConnectDiscoveryResponse().getTokenEndpoint());
            tokenClient.setExecutor(getHttpService().getClientExecutor());
            tokenClient.setRequest(tokenRequest);
            final TokenResponse response = tokenClient.exec();
            ClientUtils.showClient(tokenClient);

            if (response.getStatus() == 200 || response.getStatus() == 302) { // success or redirect
                if (Util.allNotBlank(response.getAccessToken(), response.getRefreshToken())) {
                    final GetTokensByCodeResponse opResponse = new GetTokensByCodeResponse();
                    opResponse.setAccessToken(response.getAccessToken());
                    opResponse.setIdToken(response.getIdToken());
                    opResponse.setRefreshToken(response.getRefreshToken());
                    opResponse.setExpiresIn(response.getExpiresIn());

                    final Jwt jwt = Jwt.parse(response.getIdToken());
                    if (CheckIdTokenOperation.isValid(jwt, getDiscoveryService().getConnectDiscoveryResponse())) {
                        final Map<String, List<String>> claims = jwt.getClaims() != null ? jwt.getClaims().toMap() : new HashMap<String, List<String>>();
                        opResponse.setIdTokenClaims(claims);

                        // persist tokens
                        site.setIdToken(response.getIdToken());
                        site.setAccessToken(response.getAccessToken());
                        getSiteService().update(site);

                        return okResponse(opResponse);
                    } else {
                        LOG.error("ID Token is not valid, token: " + response.getIdToken());
                    }
                }
            } else {
                LOG.error("Failed to get tokens because response code is: " + response.getScope());
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }

    private static String asCommaSeparatedString(List<String> scope) {
        String result = "";
        for (String s : scope) {
            result = result + " " + s;
        }
        return result.trim();
    }
}
