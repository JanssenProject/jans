package org.gluu.oxd.server.op;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.authorize.AuthorizeRequestParam;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.ExpiredObjectType;
import org.gluu.oxd.common.params.GetAuthorizationUrlParams;
import org.gluu.oxd.common.response.GetAuthorizationUrlResponse;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.Utils;
import org.gluu.oxd.server.service.Rp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
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
        final Rp rp = getRp();

        String authorizationEndpoint = getDiscoveryService().getConnectDiscoveryResponse(rp).getAuthorizationEndpoint();

        List<String> scope = Lists.newArrayList();
        if (params.getScope() != null && !params.getScope().isEmpty()) {
            scope.addAll(params.getScope());
        } else if (rp.getScope() != null) {
            scope.addAll(rp.getScope());
        }

        if (StringUtils.isNotBlank(params.getRedirectUri()) && !Utils.isValidUrl(params.getRedirectUri())) {
            throw new HttpException(ErrorResponseCode.INVALID_REDIRECT_URI);
        }

        if (StringUtils.isNotBlank(params.getRedirectUri()) && !rp.getRedirectUris().contains(params.getRedirectUri())) {
            throw new HttpException(ErrorResponseCode.REDIRECT_URI_IS_NOT_REGISTERED);
        }

        List<String> responseTypes = Lists.newArrayList();
        if (params.getResponseTypes() != null && !params.getResponseTypes().isEmpty()
                && rp.getResponseTypes().containsAll(params.getResponseTypes())) {
            responseTypes.addAll(params.getResponseTypes());
        } else {
            responseTypes.addAll(rp.getResponseTypes());
        }

        String state = StringUtils.isNotBlank(params.getState()) ? getStateService().putState(getStateService().encodeExpiredObject(params.getState(), ExpiredObjectType.STATE)) : getStateService().generateState();
        String nonce = StringUtils.isNotBlank(params.getNonce()) ? getStateService().putNonce(getStateService().encodeExpiredObject(params.getNonce(), ExpiredObjectType.NONCE)) : getStateService().generateNonce();
        String clientId = getConfigurationService().getConfiguration().getEncodeClientIdInAuthorizationUrl() ? Utils.encode(rp.getClientId()) : rp.getClientId();
        String redirectUri = StringUtils.isNotBlank(params.getRedirectUri()) ? params.getRedirectUri() : rp.getRedirectUri();

        authorizationEndpoint += "?response_type=" + Utils.joinAndUrlEncode(responseTypes);
        authorizationEndpoint += "&client_id=" + clientId;
        authorizationEndpoint += "&redirect_uri=" + redirectUri;
        authorizationEndpoint += "&scope=" + Utils.joinAndUrlEncode(scope);
        authorizationEndpoint += "&state=" + state;
        authorizationEndpoint += "&nonce=" + nonce;

        String acrValues = Utils.joinAndUrlEncode(acrValues(rp, params)).trim();
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

        if (params.getParams() != null && !params.getParams().isEmpty()) {
            authorizationEndpoint += "&" + Utils.mapAsStringWithEncodedValues(params.getParams());
        }

        return new GetAuthorizationUrlResponse(authorizationEndpoint);
    }

    private List<String> acrValues(Rp rp, GetAuthorizationUrlParams params) {
        List<String> acrList = params.getAcrValues() != null && !params.getAcrValues().isEmpty() ? params.getAcrValues() : rp.getAcrValues();
        if (acrList != null) {
            return acrList;
        } else {
            LOG.error("acr value is null for site: " + rp);
            return new ArrayList<>();
        }
    }
}
