package org.xdi.oxd.server.op;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.AuthorizationResponse;
import org.xdi.oxauth.client.AuthorizeClient;
import org.xdi.oxauth.client.ClientUtils;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.GetAuthorizationCodeParams;
import org.xdi.oxd.common.response.GetAuthorizationCodeResponse;
import org.xdi.oxd.server.service.SiteConfiguration;

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
    public CommandResponse execute(GetAuthorizationCodeParams params) {
        final SiteConfiguration site = getSite();

        final AuthorizationRequest request = new AuthorizationRequest(responseTypes(site.getResponseTypes()),
                site.getClientId(), site.getScope(), site.getAuthorizationRedirectUri(), UUID.randomUUID().toString());
        request.setState(params.getState());
        request.setAuthUsername(params.getUsername());
        request.setAuthPassword(params.getPassword());
        request.getPrompts().add(Prompt.NONE);
        request.setNonce(UUID.randomUUID().toString());
        request.setAcrValues(acrValues(params, site));

        final AuthorizeClient authorizeClient = new AuthorizeClient(getDiscoveryService().getConnectDiscoveryResponse(site.getOpHost()).getAuthorizationEndpoint());
        authorizeClient.setRequest(request);
        authorizeClient.setExecutor(getHttpService().getClientExecutor());
        final AuthorizationResponse response = authorizeClient.exec();

        ClientUtils.showClient(authorizeClient);
        if (response != null) {
            getStateService().putState(params.getState());
            return okResponse(new GetAuthorizationCodeResponse(response.getCode()));
        } else {
            LOG.error("Failed to get response from oxauth client.");
        }

        return null;
    }

    private List<String> acrValues(GetAuthorizationCodeParams params, SiteConfiguration site) {
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
