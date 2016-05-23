package org.xdi.oxd.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.GetLogoutUrlParams;
import org.xdi.oxd.common.response.LogoutResponse;
import org.xdi.oxd.server.service.SiteConfiguration;

import java.net.URLEncoder;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/11/2015
 */

public class GetLogoutUrlOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(GetLogoutUrlOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected GetLogoutUrlOperation(Command command, final Injector injector) {
        super(command, injector);
    }

    @Override
    public CommandResponse execute() {
        try {
            final GetLogoutUrlParams params = asParams(GetLogoutUrlParams.class);
            final SiteConfiguration site = getSite(params.getOxdId());

            String uri = getDiscoveryService().getConnectDiscoveryResponse().getEndSessionEndpoint() +
                    "?id_token_hint=" + getIdToken(params, site);
            if (!Strings.isNullOrEmpty(params.getPostLogoutRedirectUri())) {
                uri += "&post_logout_redirect_uri=" + URLEncoder.encode(params.getPostLogoutRedirectUri(), "UTF-8");
            }
            if (!Strings.isNullOrEmpty(params.getState())) {
                uri += "&state=" + params.getState();
            }
            if (!Strings.isNullOrEmpty(params.getSessionState())) {
                uri += "&session_state=" + params.getSessionState();
            }

            return okResponse(new LogoutResponse(uri));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
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
