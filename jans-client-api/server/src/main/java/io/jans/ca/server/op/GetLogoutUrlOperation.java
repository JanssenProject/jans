package io.jans.ca.server.op;

import com.google.common.base.Strings;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.ExpiredObjectType;
import io.jans.ca.common.params.GetLogoutUrlParams;
import io.jans.ca.common.response.GetLogoutUriResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.persistence.service.MainPersistenceService;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.ServiceProvider;
import io.jans.ca.server.service.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/11/2015
 */

public class GetLogoutUrlOperation extends BaseOperation<GetLogoutUrlParams> {

    private static final String GOOGLE_OP_HOST = "https://accounts.google.com";

    private static final Logger LOG = LoggerFactory.getLogger(GetLogoutUrlOperation.class);

    private DiscoveryService discoveryService;
    private MainPersistenceService configurationService;
    private StateService stateService;

    public GetLogoutUrlOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, GetLogoutUrlParams.class);
        this.discoveryService = serviceProvider.getDiscoveryService();
        this.stateService = serviceProvider.getStateService();
        this.configurationService = serviceProvider.getJansConfigurationService();
    }

    @Override
    public IOpResponse execute(GetLogoutUrlParams params) throws Exception {
        final Rp rp = getRp();

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
            if (rp.getOpHost().startsWith(GOOGLE_OP_HOST) && configurationService.find().getSupportGoogleLogout()) {
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

    private static String separator(String uri) {
        return uri.contains("?") ? "&" : "?";
    }
}
