/*
  All rights reserved -- Copyright 2015 Gluu Inc.
*/
package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.client.JwkClient;
import org.gluu.oxauth.client.JwkResponse;
import org.gluu.oxauth.model.uma.UmaMetadata;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.GetJwksParams;
import org.gluu.oxd.common.response.GetJwksResponse;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.POJOResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.service.DiscoveryService;

/**
 * Service class for fetching JSON Web Key set
 *
 * @author Shoeb
 * @version 12/01/2018
 */

public class GetJwksOperation extends BaseOperation<GetJwksParams> {

    protected GetJwksOperation(Command command, Injector injector) {
        super(command, injector, GetJwksParams.class);
    }

    @Override
    public IOpResponse execute(GetJwksParams params) {

        if (StringUtils.isEmpty(params.getOpHost())) {
            throw new HttpException(ErrorResponseCode.INVALID_OP_HOST);
        }

        try {

            final DiscoveryService discoveryService = getDiscoveryService();

            final UmaMetadata umaMetadata = discoveryService.getUmaDiscovery(params.getOpHost(), params.getOpDiscoveryPath());

            final String jwksUri = umaMetadata.getJwksUri();

            final JwkClient jwkClient = new JwkClient(jwksUri);
            jwkClient.setExecutor(getHttpService().getClientExecutor());

            final JwkResponse serverResponse = jwkClient.exec();

            final GetJwksResponse response = new GetJwksResponse();

            response.setKeys(serverResponse.getJwks().getKeys());

            return new POJOResponse(response);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }
}
