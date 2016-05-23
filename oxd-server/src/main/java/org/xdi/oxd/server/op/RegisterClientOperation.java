/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.OpenIdConfigurationResponse;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.RegisterClientParams;
import org.xdi.oxd.common.response.RegisterClientOpResponse;
import org.xdi.oxd.server.Convertor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class RegisterClientOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterClientOperation.class);

    public RegisterClientOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {

        final RegisterClientParams params = asParams(RegisterClientParams.class);
        final OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponse(params.getDiscoveryUrl());

        final String applicationTypeString = StringUtils.isNotBlank(params.getApplicationType()) ?
                params.getApplicationType() : getConfiguration().getRegisterClientAppType();

        final String responseTypeString = StringUtils.isNotBlank(params.getResponseTypes()) ?
                params.getResponseTypes() : getConfiguration().getRegisterClientResponesType();

        final ApplicationType applicationType = ApplicationType.fromString(applicationTypeString);
        final List<ResponseType> responseTypes = ResponseType.fromString(responseTypeString, " ");
        final RegisterRequest request = new RegisterRequest(applicationType, params.getClientName(), params.getRedirectUrl());
        request.setResponseTypes(responseTypes);
        request.setJwksUri(params.getJwksUri());
        request.setPostLogoutRedirectUris(org.xdi.oxauth.model.util.StringUtils.spaceSeparatedToList(params.getLogoutRedirectUrl()));

        if (StringUtils.isNotBlank(params.getContacts())) {
            request.setContacts(org.xdi.oxauth.model.util.StringUtils.spaceSeparatedToList(params.getContacts()));
        }

        if (StringUtils.isNotBlank(params.getGrantTypes())) {
            request.setGrantTypes(grantTypes(params.getGrantTypes()));
        }

        if (StringUtils.isNotBlank(params.getTokenEndpointAuthMethod())) {
            final AuthenticationMethod authenticationMethod = AuthenticationMethod.fromString(params.getTokenEndpointAuthMethod());
            if (authenticationMethod != null) {
                request.setTokenEndpointAuthMethod(authenticationMethod);
            }
        }

        if (params.getRequestUris() != null && !params.getRequestUris().isEmpty()) {
            request.setRequestUris(params.getRequestUris());
        }

        final RegisterClient registerClient = new RegisterClient(discoveryResponse.getRegistrationEndpoint());
        registerClient.setRequest(request);
        registerClient.setExecutor(getHttpService().getClientExecutor());
        final RegisterResponse response = registerClient.exec();
        if (response != null) {
            LOG.trace("RegisterResponse: {}, client_id: {}", response, response.getClientId());
            final RegisterClientOpResponse r = Convertor.asRegisterClientOpResponse(response);
            return okResponse(r);
        } else {
            LOG.error("There is no response for registerClient.");
        }
        return null;
    }

    private List<GrantType> grantTypes(String grantTypesString) {
        List<GrantType> grantTypes = new ArrayList<GrantType>();
        try {
            final List<String> strings = org.xdi.oxauth.model.util.StringUtils.spaceSeparatedToList(grantTypesString);
            for (String str : strings) {
                grantTypes.add(GrantType.fromString(str));
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return grantTypes;
    }
}
