package org.gluu.oxd.server.op;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.server.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.model.authorize.AuthorizeRequestParam;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.params.GetAuthorizationUrlParams;
import org.gluu.oxd.common.response.GetAuthorizationUrlResponse;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.server.Utils;
import org.gluu.oxd.server.service.Rp;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetAuthorizationUrlOperation extends BaseOperation<GetAuthorizationUrlParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetAuthorizationUrlOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected GetAuthorizationUrlOperation(Command command, final Injector injector) {
        super(command, injector, GetAuthorizationUrlParams.class);
    }

    @Override
    public IOpResponse execute(GetAuthorizationUrlParams params) throws Exception {
        final Rp site = getRp();

        String authorizationEndpoint = getDiscoveryService().getConnectDiscoveryResponse(site).getAuthorizationEndpoint();

        List<String> scope = Lists.newArrayList();
        if (params.getScope() != null && !params.getScope().isEmpty()) {
            scope.addAll(params.getScope());
        } else if (site.getScope() != null) {
            scope.addAll(site.getScope());
        }

        if (StringUtils.isNotBlank(params.getAuthorizationRedirectUri()) && !site.getRedirectUris().contains(params.getAuthorizationRedirectUri())) {
            throw new HttpException(ErrorResponseCode.REDIRECT_URI_IS_NOT_REGISTERED);
        }

        String redirectUri = StringUtils.isNotBlank(params.getAuthorizationRedirectUri()) ? params.getAuthorizationRedirectUri() : site.getAuthorizationRedirectUri();

        authorizationEndpoint += "?response_type=" + Utils.joinAndUrlEncode(site.getResponseTypes());
        authorizationEndpoint += "&client_id=" + site.getClientId();
        authorizationEndpoint += "&redirect_uri=" + redirectUri;
        authorizationEndpoint += "&scope=" + Utils.joinAndUrlEncode(scope);
        authorizationEndpoint += "&state=" + getStateService().generateState();
        authorizationEndpoint += "&nonce=" + getStateService().generateNonce();

        String acrValues = Utils.joinAndUrlEncode(acrValues(site, params)).trim();
        if (!Strings.isNullOrEmpty(acrValues)) {
            authorizationEndpoint += "&acr_values=" + acrValues;
        }

        if (!Strings.isNullOrEmpty(params.getPrompt())) {
            authorizationEndpoint += "&prompt=" + params.getPrompt();
        }
        if (!Strings.isNullOrEmpty(params.getHostedDomain())) {
            authorizationEndpoint += "&hd=" + params.getHostedDomain();
        }

        if (params.getCustomParameters() != null && !params.getCustomParameters().isEmpty()) {
            authorizationEndpoint += "&" + AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS + "=" + Utils.encode(Util.mapAsString(params.getCustomParameters()));
        }

        return new GetAuthorizationUrlResponse(authorizationEndpoint);
    }

    private List<String> acrValues(Rp site, GetAuthorizationUrlParams params) {
        List<String> acrList = params.getAcrValues() != null && !params.getAcrValues().isEmpty() ? params.getAcrValues() : site.getAcrValues();
        if (acrList != null) {
            return acrList;
        } else {
            LOG.error("acr value is null for site: " + site);
            return new ArrayList<>();
        }
    }
}
