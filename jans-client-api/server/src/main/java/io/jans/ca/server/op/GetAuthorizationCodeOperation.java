package io.jans.ca.server.op;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.GetAuthorizationCodeParams;
import io.jans.ca.common.response.GetAuthorizationCodeResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.HttpService;
import io.jans.ca.server.service.ServiceProvider;
import io.jans.ca.server.service.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2015
 */

public class GetAuthorizationCodeOperation extends BaseOperation<GetAuthorizationCodeParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetAuthorizationCodeOperation.class);

    DiscoveryService discoveryService;
    HttpService httpService;
    OpClientFactoryImpl opClientFactory;
    StateService stateService;

    public GetAuthorizationCodeOperation(Command pCommand, ServiceProvider serviceProvider) {
        super(pCommand, serviceProvider, GetAuthorizationCodeParams.class);
        this.discoveryService = serviceProvider.getDiscoveryService();
        this.stateService = serviceProvider.getStateService();
        this.opClientFactory = serviceProvider.getOpClientFactory();
        this.httpService = serviceProvider.getHttpService();
    }

    @Override
    public IOpResponse execute(GetAuthorizationCodeParams params) {
        final Rp rp = getRp();

        String nonce = Strings.isNullOrEmpty(params.getNonce()) ? UUID.randomUUID().toString() : params.getNonce();
        String state = Strings.isNullOrEmpty(params.getState()) ? UUID.randomUUID().toString() : params.getState();

        final AuthorizationRequest request = new AuthorizationRequest(responseTypes(rp.getResponseTypes()),
                rp.getClientId(), rp.getScope(), rp.getRedirectUri(), nonce);
        request.setState(state);
        request.setAuthUsername(params.getUsername());
        request.setAuthPassword(params.getPassword());
        request.getPrompts().add(Prompt.NONE);
        request.setAcrValues(acrValues(params, rp));

        stateService.putNonce(nonce);
        stateService.putState(state);
        String authorizationEndPoint = discoveryService.getConnectDiscoveryResponse(rp).getAuthorizationEndpoint();
        LOG.info("Authorization Code Operation - rpId:{} authorizationEndPoint: {}", rp.getRpId(), authorizationEndPoint);
        final AuthorizeClient authorizeClient = opClientFactory.createAuthorizeClient(authorizationEndPoint);
        authorizeClient.setRequest(request);
        final AuthorizationResponse response = authorizeClient.exec();

        if (response != null && response.getCode() != null) {
            if (!stateService.isExpiredObjectPresent(params.getState())) {
                stateService.putState(params.getState());
            }
            return new GetAuthorizationCodeResponse(response.getCode());
        } else {
            LOG.error("Failed to get Authorization Code - rpId:{} authorizationEndPoint: {} - Check keystorePath, keystorePassword, signatureAlgorithms, jansConfWebKeys, and credentials", rp.getRpId(), authorizationEndPoint);
            throw new HttpException(ErrorResponseCode.ERROR_AUTHORIZATION_CODE);
        }
    }

    private List<String> acrValues(GetAuthorizationCodeParams params, Rp rp) {
        List<String> acrs = Lists.newArrayList();
        if (params.getAcrValues() != null && !params.getAcrValues().isEmpty()) {
            acrs.addAll(params.getAcrValues());
        }
        if (acrs.isEmpty() && rp.getAcrValues() != null && !rp.getAcrValues().isEmpty()) {
            acrs.addAll(rp.getAcrValues());
        }
        return acrs;
    }

    private List<ResponseType> responseTypes(List<String> responseTypes) {
        List<ResponseType> result = Lists.newArrayList();
        for (String type : responseTypes) {
            result.add(ResponseType.fromString(type));
        }
        return result;
    }
}
