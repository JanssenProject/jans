package io.jans.ca.server.op;

import com.google.common.base.Strings;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.ca.common.CommandType;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.ExpiredObjectType;
import io.jans.ca.common.params.GetLogoutUrlParams;
import io.jans.ca.common.response.GetLogoutUriResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.StateService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;

@RequestScoped
@Named
public class GetLogoutUrlOperation extends BaseOperation<GetLogoutUrlParams> {

    private static final String GOOGLE_OP_HOST = "https://accounts.google.com";

    private static final Logger LOG = LoggerFactory.getLogger(GetLogoutUrlOperation.class);

    @Inject
    DiscoveryService discoveryService;
    @Inject
    StateService stateService;

    @Override
    public IOpResponse execute(GetLogoutUrlParams params, HttpServletRequest httpServletRequest) throws Exception {
        final Rp rp = getRp(params);

        OpenIdConfigurationResponse discoveryResponse = discoveryService.getConnectDiscoveryResponse(rp);
        String endSessionEndpoint = discoveryResponse.getEndSessionEndpoint();

        String postLogoutRedirectUrl = params.getPostLogoutRedirectUri();
        if (Strings.isNullOrEmpty(postLogoutRedirectUrl)) {
            postLogoutRedirectUrl = rp.getPostLogoutRedirectUri();
        }
        if (Strings.isNullOrEmpty(postLogoutRedirectUrl)) {
            postLogoutRedirectUrl = "";
        }

        if (Strings.isNullOrEmpty(endSessionEndpoint)) {
            if (rp.getOpHost().startsWith(GOOGLE_OP_HOST) && getJansConfigurationService().find().getSupportGoogleLogout()) {
                String logoutUrl = "https://www.google.com/accounts/Logout?continue=https://appengine.google.com/_ah/logout?continue=" + postLogoutRedirectUrl;
                return new GetLogoutUriResponse(logoutUrl);
            }

            LOG.error("Failed to get end_session_endpoint at: {}", discoveryService.getConnectDiscoveryUrl(rp));
            throw new HttpException(ErrorResponseCode.FAILED_TO_GET_END_SESSION_ENDPOINT);
        }

        String uri = endSessionEndpoint;
        if (!Strings.isNullOrEmpty(postLogoutRedirectUrl)) {
            uri += separator(uri) + "post_logout_redirect_uri=" + URLEncoder.encode(postLogoutRedirectUrl, "UTF-8");
        }
        if (!Strings.isNullOrEmpty(params.getState())) {
            uri += separator(uri) + "state=" + stateService.encodeExpiredObject(params.getState(), ExpiredObjectType.STATE);
        }
        if (!Strings.isNullOrEmpty(params.getSessionState())) {
            uri += separator(uri) + "session_state=" + params.getSessionState();
        }
        if (!Strings.isNullOrEmpty(params.getIdTokenHint())) {
            uri += separator(uri) + "id_token_hint=" + params.getIdTokenHint();
        }

        return new GetLogoutUriResponse(uri);
    }

    @Override
    public Class<GetLogoutUrlParams> getParameterClass() {
        return GetLogoutUrlParams.class;
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.GET_LOGOUT_URI;
    }

    private static String separator(String uri) {
        return uri.contains("?") ? "&" : "?";
    }
}
