package org.xdi.oxd.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.OpenIdConfigurationResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.GetLogoutUrlParams;
import org.xdi.oxd.common.response.LogoutResponse;
import org.xdi.oxd.server.service.ConfigurationService;
import org.xdi.oxd.server.service.SiteConfiguration;

import java.net.URLEncoder;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/11/2015
 */

public class GetLogoutUrlOperation extends BaseOperation<GetLogoutUrlParams> {

    private static final String GOOGLE_OP_HOST = "https://accounts.google.com";

    private static final Logger LOG = LoggerFactory.getLogger(GetLogoutUrlOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected GetLogoutUrlOperation(Command command, final Injector injector) {
        super(command, injector, GetLogoutUrlParams.class);
    }

    @Override
    public CommandResponse execute(GetLogoutUrlParams params) throws Exception {
        final SiteConfiguration site = getSite();

        OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponse(site.getOpHost());
        String endSessionEndpoint = discoveryResponse.getEndSessionEndpoint();

        String postLogoutRedirectUrl = params.getPostLogoutRedirectUri();
        if (Strings.isNullOrEmpty(postLogoutRedirectUrl)) {
            postLogoutRedirectUrl = site.getPostLogoutRedirectUri();
        }
        if (Strings.isNullOrEmpty(postLogoutRedirectUrl)) {
            postLogoutRedirectUrl = "";
        }

        if (Strings.isNullOrEmpty(endSessionEndpoint)) {
            if (site.getOpHost().startsWith(GOOGLE_OP_HOST) && getInstance(ConfigurationService.class).get().getSupportGoogleLogout()) {
                String logoutUrl = "https://www.google.com/accounts/Logout?continue=https://appengine.google.com/_ah/logout?continue=" + postLogoutRedirectUrl;
                return okResponse(new LogoutResponse(logoutUrl));
            }

            LOG.error("Failed to get end_session_endpoint at: " + getDiscoveryService().getConnectDiscoveryUrl(site.getOpHost()));
            throw new ErrorResponseException(ErrorResponseCode.FAILED_TO_GET_END_SESSION_ENDPOINT);
        }

        String uri = endSessionEndpoint +
                "?id_token_hint=" + getIdToken(params, site);
        if (!Strings.isNullOrEmpty(params.getPostLogoutRedirectUri())) {
            uri += "&post_logout_redirect_uri=" + URLEncoder.encode(postLogoutRedirectUrl, "UTF-8");
        }
        if (!Strings.isNullOrEmpty(params.getState())) {
            uri += "&state=" + params.getState();
        }
        if (!Strings.isNullOrEmpty(params.getSessionState())) {
            uri += "&session_state=" + params.getSessionState();
        }

        return okResponse(new LogoutResponse(uri));
    }

    private String getIdToken(GetLogoutUrlParams params, SiteConfiguration site) {
        if (!Strings.isNullOrEmpty(params.getIdTokenHint())) {
            return params.getIdTokenHint();
        }
        if (!Strings.isNullOrEmpty(site.getIdToken())) {
            return site.getIdToken();
        }
        throw new RuntimeException("id_token is not present in command parameter and also is not present in site conf.");
    }

}
