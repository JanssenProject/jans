/*
  All rights reserved -- Copyright 2015 Gluu Inc.
*/
package io.jans.ca.server.op;

import io.jans.as.client.JwkClient;
import io.jans.as.client.JwkResponse;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.GetJwksParams;
import io.jans.ca.common.response.GetJwksResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.ServiceProvider;
import org.apache.commons.lang.StringUtils;

/**
 * Service class for fetching JSON Web Key set
 *
 * @author Shoeb
 * @version 12/01/2018
 */

public class GetJwksOperation extends BaseOperation<GetJwksParams> {

    private DiscoveryService discoveryService;

    public GetJwksOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, GetJwksParams.class);
        this.discoveryService = serviceProvider.getDiscoveryService();
    }

    @Override
    public IOpResponse execute(GetJwksParams params) {

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
}
