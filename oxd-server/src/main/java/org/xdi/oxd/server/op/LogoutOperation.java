package org.xdi.oxd.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.ClientUtils;
import org.xdi.oxauth.client.EndSessionClient;
import org.xdi.oxauth.client.EndSessionRequest;
import org.xdi.oxauth.client.EndSessionResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.LogoutParams;
import org.xdi.oxd.common.response.LogoutResponse;
import org.xdi.oxd.server.service.SiteConfiguration;

import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/11/2015
 */

public class LogoutOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(LogoutOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected LogoutOperation(Command command, final Injector injector) {
        super(command, injector);
    }

    @Override
    public CommandResponse execute() {
        try {
            final LogoutParams params = asParams(LogoutParams.class);
            final SiteConfiguration site = getSite(params.getOxdId());

            final EndSessionRequest request = new EndSessionRequest(
                    getIdToken(params, site), params.getPostLogoutRedirectUri(), UUID.randomUUID().toString());

            final EndSessionClient client = new EndSessionClient(getDiscoveryService().getConnectDiscoveryResponse().getEndSessionEndpoint());
            client.setExecutor(getHttpService().getClientExecutor());
            client.setRequest(request);
            final EndSessionResponse response = client.exec();

            ClientUtils.showClient(client);
            if (response != null && response.getErrorType() == null) {
                return okResponse(new LogoutResponse(response.getHtmlPage()));
            } else {
                LOG.error("Failed to get response from oxauth client.");
            }

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }

    private String getIdToken(LogoutParams params, SiteConfiguration site) {
        if (!Strings.isNullOrEmpty(params.getIdToken())) {
            return params.getIdToken();
        }
        if (!Strings.isNullOrEmpty(site.getIdToken())) {
            return site.getIdToken();
        }
        throw new RuntimeException("id_token is not present in command parameter and also is not present in site conf.");
    }

}
