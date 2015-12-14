package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.GetAuthorizationUrlParams;
import org.xdi.oxd.common.response.GetAuthorizationUrlResponse;
import org.xdi.oxd.server.Utils;
import org.xdi.oxd.server.service.SiteConfiguration;

import java.util.List;

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
            final SiteConfiguration site = getSite(params.getOxdId());

            String authorizationEndpoint = getDiscoveryService().getConnectDiscoveryResponse().getIssuer() + "/oxauth/authorize";

            authorizationEndpoint += "?response_type=" + Utils.joinAndUrlEncode(site.getResponseTypes());
            authorizationEndpoint += "&client_id=" + site.getClientId();
            authorizationEndpoint += "&client_secret=" + site.getClientSecret();
            authorizationEndpoint += "&redirect_uri=" + site.getAuthorizationRedirectUri();
            authorizationEndpoint += "&scope=" + Utils.joinAndUrlEncode(site.getScope());
            authorizationEndpoint += "&state=" + state();
            authorizationEndpoint += "&nonce=" + nonce();
            authorizationEndpoint += "&acr_values=" + Utils.joinAndUrlEncode(acrValues(site));


            return okResponse(new GetAuthorizationUrlResponse(authorizationEndpoint));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }

    private List<String> acrValues(SiteConfiguration site) {
        final GetAuthorizationUrlParams params = asParams(GetAuthorizationUrlParams.class);
        return params.getAcrValues() != null && !params.getAcrValues().isEmpty() ? params.getAcrValues() : site.getAcrValues();
    }

    private String nonce() {
        return "n-0S6_WzA2Mj"; // fixme
    }

    private String state() {
        return "af0ifjsldkj"; // fixme
    }


}
