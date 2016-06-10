package org.xdi.oxd.server.op;

import com.google.inject.Injector;
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

public class GetAuthorizationUrlOperation extends BaseOperation<GetAuthorizationUrlParams> {

//    private static final Logger LOG = LoggerFactory.getLogger(GetAuthorizationUrlOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected GetAuthorizationUrlOperation(Command command, final Injector injector) {
        super(command, injector, GetAuthorizationUrlParams.class);
    }

    @Override
    public CommandResponse execute(GetAuthorizationUrlParams params) throws Exception {
        final SiteConfiguration site = getSite();

        String authorizationEndpoint = getDiscoveryService().getConnectDiscoveryResponse(site.getOpHost()).getIssuer() + "/oxauth/authorize";

        authorizationEndpoint += "?response_type=" + Utils.joinAndUrlEncode(site.getResponseTypes());
        authorizationEndpoint += "&client_id=" + site.getClientId();
        authorizationEndpoint += "&client_secret=" + site.getClientSecret();
        authorizationEndpoint += "&redirect_uri=" + site.getAuthorizationRedirectUri();
        authorizationEndpoint += "&scope=" + Utils.joinAndUrlEncode(site.getScope());
        authorizationEndpoint += "&state=" + state();
        authorizationEndpoint += "&nonce=" + nonce();
        authorizationEndpoint += "&acr_values=" + Utils.joinAndUrlEncode(acrValues(site, params));
        authorizationEndpoint += "&prompt=login";


        return okResponse(new GetAuthorizationUrlResponse(authorizationEndpoint));
    }

    private List<String> acrValues(SiteConfiguration site, GetAuthorizationUrlParams params) {
        return params.getAcrValues() != null && !params.getAcrValues().isEmpty() ? params.getAcrValues() : site.getAcrValues();
    }

    private String nonce() {
        return "n-0S6_WzA2Mj"; // fixme todo
    }

    private String state() {
        return "af0ifjsldkj"; // fixme todo
    }


}
