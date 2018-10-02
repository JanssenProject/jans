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
import org.xdi.oxd.server.service.Rp;

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
        final Rp site = getRp();

        OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponse(site);
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

            LOG.error("Failed to get end_session_endpoint at: " + getDiscoveryService().getConnectDiscoveryUrl(site));
            throw new ErrorResponseException(ErrorResponseCode.FAILED_TO_GET_END_SESSION_ENDPOINT);
        }

        String uri = endSessionEndpoint;
        if (!Strings.isNullOrEmpty(postLogoutRedirectUrl)) {
            uri += separator(uri) + "post_logout_redirect_uri=" + URLEncoder.encode(postLogoutRedirectUrl, "UTF-8");
        }
        if (!Strings.isNullOrEmpty(params.getState())) {
            uri += separator(uri) + "state=" + params.getState();
        }
        if (!Strings.isNullOrEmpty(params.getSessionState())) {
            uri += separator(uri) + "session_state=" + params.getSessionState();
        }

        return okResponse(new LogoutResponse(uri));
    }

    private static String separator(String uri) {
        return uri.contains("?") ? "&" : "?";
    }
}
