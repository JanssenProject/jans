package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.GetAuthorizationUrlParams;
import org.xdi.oxd.common.response.GetAuthorizationUrlResponse;
import org.xdi.oxd.server.service.SiteConfiguration;
import org.xdi.oxd.server.service.SiteConfigurationService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetAuthorizationUrlOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(GetAuthorizationUrlOperation.class);

    /**
     * Base constructor
     *
     * @param p_command command
     */
    protected GetAuthorizationUrlOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {
        try {
            final GetAuthorizationUrlParams params = asParams(GetAuthorizationUrlParams.class);
            final SiteConfigurationService siteService = getInjector().getInstance(SiteConfigurationService.class);
            final SiteConfiguration site = siteService.getSite(params.getOxdId());

            String authorizationEndpoint = getDiscoveryService().getConnectDiscoveryResponse().getAuthorizationEndpoint();

            authorizationEndpoint += "?response_type=" + site.getResponseTypes(); // todo
            authorizationEndpoint += "&client_id=" + site.getClientId();
            authorizationEndpoint += "&client_secret=" + site.getClientSecret();


            return okResponse(new GetAuthorizationUrlResponse(authorizationEndpoint));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }
}
