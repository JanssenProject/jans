/*
  All rights reserved -- Copyright 2015 Gluu Inc.
*/
package io.jans.ca.server.op;

import io.jans.as.client.JwkClient;
import io.jans.as.client.JwkResponse;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.GetJwksParams;
import io.jans.ca.common.response.GetJwksResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.service.DiscoveryService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang.StringUtils;

@RequestScoped
@Named
public class GetJwksOperation extends BaseOperation<GetJwksParams> {

    @Inject
    DiscoveryService discoveryService;

    @Override
    public IOpResponse execute(GetJwksParams params, HttpServletRequest httpServletRequest) {

        if (StringUtils.isEmpty(params.getOpHost()) && StringUtils.isEmpty(params.getOpConfigurationEndpoint())) {
            throw new HttpException(ErrorResponseCode.INVALID_OP_HOST_AND_CONFIGURATION_ENDPOINT);
        }

        try {

            final OpenIdConfigurationResponse openIdConfigurationResponse = discoveryService.getConnectDiscoveryResponse(params.getOpConfigurationEndpoint(), params.getOpHost(), params.getOpDiscoveryPath());

            final String jwksUri = openIdConfigurationResponse.getJwksUri();

            final JwkClient jwkClient = new JwkClient(jwksUri);
            jwkClient.setExecutor(discoveryService.getHttpService().getClientEngine());

            final JwkResponse serverResponse = jwkClient.exec();

            final GetJwksResponse response = new GetJwksResponse();

            response.setKeys(serverResponse.getJwks().getKeys());

            return new POJOResponse(response);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public Class<GetJwksParams> getParameterClass() {
        return GetJwksParams.class;
    }

    @Override
    public boolean isAuthorizationRequired() {
        return false;
    }

    @Override
    public String getReturnType() {
        return MediaType.APPLICATION_JSON;
    }

}
