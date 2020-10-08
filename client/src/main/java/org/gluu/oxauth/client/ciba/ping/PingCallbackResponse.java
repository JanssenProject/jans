/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client.ciba.ping;

import org.apache.log4j.Logger;
import org.gluu.oxauth.client.BaseResponse;
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
