/*
  All rights reserved -- Copyright 2015 Gluu Inc.
*/
package org.xdi.oxd.server.op;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Injector;
import org.xdi.oxauth.client.JwkClient;
import org.xdi.oxauth.client.JwkResponse;
import org.xdi.oxauth.model.uma.UmaMetadata;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.params.GetJwksParams;
import org.xdi.oxd.common.response.GetJwksResponse;
import org.xdi.oxd.common.response.IOpResponse;
import org.xdi.oxd.server.service.DiscoveryService;

/**
 * Service class for fetching JSON Web Key set
 *
 * @author Shoeb
 * @version 11/10/2018
 */
public class GetJwksOperation extends BaseOperation<GetJwksParams> {

    protected GetJwksOperation(Command command, Injector injector) {
        super(command, injector, GetJwksParams.class);
    }

    @Override
    public IOpResponse execute(GetJwksParams params) {
        try {

            final DiscoveryService discoveryService = getDiscoveryService();

            final UmaMetadata umaMetadata = discoveryService.getUmaDiscovery(params.getOpHost(), params.getOpDiscoveryPath());

            final String jwksUri = umaMetadata.getJwksUri();

            final JwkClient jwkClient = new JwkClient(jwksUri);
            jwkClient.setExecutor(getHttpService().getClientExecutor());

            final JwkResponse serverResponse = jwkClient.exec();
            final String jwksJson = new ObjectMapper().writer(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                    .writeValueAsString(serverResponse.getJwks());

            final GetJwksResponse response = new GetJwksResponse();
            response.setJwks(jwksJson);

            return response;

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }
}
