/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba.ping;

import io.jans.as.client.BaseResponse;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.ClientResponse;

/**
 * @author Javier Rojas Blum
 * @version December 21, 2019
 */
public class PingCallbackResponse extends BaseResponse {

    private static final Logger LOG = Logger.getLogger(PingCallbackResponse.class);

    public PingCallbackResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        String entity = clientResponse.getEntity(String.class);
        setEntity(entity);
        setHeaders(clientResponse.getMetadata());
    }
}
