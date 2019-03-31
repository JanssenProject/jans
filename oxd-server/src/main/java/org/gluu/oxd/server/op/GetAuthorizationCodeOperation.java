package org.gluu.oxd.server.op;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.client.AuthorizationRequest;
import org.gluu.oxauth.client.AuthorizationResponse;
import org.gluu.oxauth.client.AuthorizeClient;
import org.gluu.oxauth.client.ClientUtils;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.params.GetAuthorizationCodeParams;
import org.gluu.oxd.common.response.GetAuthorizationCodeResponse;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.server.service.Rp;

import java.util.List;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2015
 */

public class GetAuthorizationCodeOperation extends BaseOperation<GetAuthorizationCodeParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetAuthorizationCodeOperation.class);

    /**
     * Base constructor
     *
     * @param p_command command
     */
    protected GetAuthorizationCodeOperation(Command p_command, final Injector injector) {
        super(p_command, injector, GetAuthorizationCodeParams.class);
    }

    @Override
    public IOpResponse execute(GetAuthorizationCodeParams params) {
        final Rp site = getRp();

        String nonce = Strings.isNullOrEmpty(params.getNonce()) ? UUID.randomUUID().toString() : params.getNonce();
        String state = Strings.isNullOrEmpty(params.getState()) ? UUID.randomUUID().toString() : params.getState();

        final AuthorizationRequest request = new AuthorizationRequest(responseTypes(site.getResponseTypes()),
                site.getClientId(), site.getScope(), site.getAuthorizationRedirectUri(), nonce);
        request.setState(state);
        request.setAuthUsername(params.getUsername());
        request.setAuthPassword(params.getPassword());
        request.getPrompts().add(Prompt.NONE);
        request.setAcrValues(acrValues(params, site));

        getStateService().putNonce(nonce);
        getStateService().putState(state);

        final AuthorizeClient authorizeClient = new AuthorizeClient(getDiscoveryService().getConnectDiscoveryResponse(site).getAuthorizationEndpoint());
        authorizeClient.setRequest(request);
        authorizeClient.setExecutor(getHttpService().getClientExecutor());
        final AuthorizationResponse response = authorizeClient.exec();

        ClientUtils.showClient(authorizeClient);
        if (response != null) {
            getStateService().putState(params.getState());
            return new GetAuthorizationCodeResponse(response.getCode());
        } else {
            LOG.error("Failed to get response from oxauth client.");
        }

        return null;
    }

    private List<String> acrValues(GetAuthorizationCodeParams params, Rp site) {
        List<String> acrs = Lists.newArrayList();
        if (params.getAcrValues() != null && !params.getAcrValues().isEmpty()) {
            acrs.addAll(params.getAcrValues());
        }
        if (acrs.isEmpty() && site.getAcrValues() != null && !site.getAcrValues().isEmpty()) {
            acrs.addAll(site.getAcrValues());
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
