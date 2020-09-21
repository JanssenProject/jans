package org.gluu.oxd.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.gluu.oxd.common.ExpiredObjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.client.OpenIdConfigurationResponse;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.GetLogoutUrlParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.GetLogoutUriResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.service.ConfigurationService;
import org.gluu.oxd.server.service.Rp;

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
    public IOpResponse execute(GetLogoutUrlParams params) throws Exception {
        final Rp rp = getRp();

        OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponse(rp);
        String endSessionEndpoint = discoveryResponse.getEndSessionEndpoint();

        String postLogoutRedirectUrl = params.getPostLogoutRedirectUri();
        if (Strings.isNullOrEmpty(postLogoutRedirectUrl)) {
            postLogoutRedirectUrl = rp.getPostLogoutRedirectUri();
        }
        if (Strings.isNullOrEmpty(postLogoutRedirectUrl)) {
            postLogoutRedirectUrl = "";
        }

        if (Strings.isNullOrEmpty(endSessionEndpoint)) {
            if (rp.getOpHost().startsWith(GOOGLE_OP_HOST) && getInstance(ConfigurationService.class).get().getSupportGoogleLogout()) {
                String logoutUrl = "https://www.google.com/accounts/Logout?continue=https://appengine.google.com/_ah/logout?continue=" + postLogoutRedirectUrl;
                return new GetLogoutUriResponse(logoutUrl);
            }

            LOG.error("Failed to get end_session_endpoint at: " + getDiscoveryService().getConnectDiscoveryUrl(rp));
            throw new HttpException(ErrorResponseCode.FAILED_TO_GET_END_SESSION_ENDPOINT);
        }

        String uri = endSessionEndpoint;
        if (!Strings.isNullOrEmpty(postLogoutRedirectUrl)) {
            uri += separator(uri) + "post_logout_redirect_uri=" + URLEncoder.encode(postLogoutRedirectUrl, "UTF-8");
        }
        if (!Strings.isNullOrEmpty(params.getState())) {
            uri += separator(uri) + "state=" + getStateService().encodeExpiredObject(params.getState(), ExpiredObjectType.STATE);
        }
        if (!Strings.isNullOrEmpty(params.getSessionState())) {
            uri += separator(uri) + "session_state=" + params.getSessionState();
        }
        if (!Strings.isNullOrEmpty(params.getIdTokenHint())) {
            uri += separator(uri) + "id_token_hint=" + params.getIdTokenHint();
        }

        return new GetLogoutUriResponse(uri);
    }

    private static String separator(String uri) {
        return uri.contains("?") ? "&" : "?";
    }
}
