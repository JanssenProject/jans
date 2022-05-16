package io.jans.ca.server.op;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.util.Util;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.ExpiredObjectType;
import io.jans.ca.common.params.GetAuthorizationUrlParams;
import io.jans.ca.common.response.GetAuthorizationUrlResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.Utils;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.ServiceProvider;
import io.jans.ca.server.service.StateService;
import io.jans.ca.server.persistence.service.MainPersistenceService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetAuthorizationUrlOperation extends BaseOperation<GetAuthorizationUrlParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetAuthorizationUrlOperation.class);

    DiscoveryService discoveryService;
    StateService stateService;
    MainPersistenceService jansConfigurationService;

    /**
     * Base constructor
     *
     * @param command command
     */
    public GetAuthorizationUrlOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, GetAuthorizationUrlParams.class);
        this.discoveryService = serviceProvider.getDiscoveryService();
        this.stateService = serviceProvider.getStateService();
        this.jansConfigurationService = serviceProvider.getJansConfigurationService();
    }

    @Override
    public IOpResponse execute(GetAuthorizationUrlParams params) throws Exception {
        final Rp rp = getRp();

        String authorizationEndpoint = discoveryService.getConnectDiscoveryResponse(rp).getAuthorizationEndpoint();

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

        String state = StringUtils.isNotBlank(params.getState()) ? stateService.putState(stateService.encodeExpiredObject(params.getState(), ExpiredObjectType.STATE)) : stateService.generateState();
        String nonce = StringUtils.isNotBlank(params.getNonce()) ? stateService.putNonce(stateService.encodeExpiredObject(params.getNonce(), ExpiredObjectType.NONCE)) : stateService.generateNonce();
        String clientId = jansConfigurationService.find().getEncodeClientIdInAuthorizationUrl() ? Utils.encode(rp.getClientId()) : rp.getClientId();
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
