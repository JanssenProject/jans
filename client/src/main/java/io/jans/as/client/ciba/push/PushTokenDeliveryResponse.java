/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba.push;

import io.jans.as.client.BaseResponse;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.ClientResponse;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class PushTokenDeliveryResponse extends BaseResponse {

    private static final Logger LOG = Logger.getLogger(PushTokenDeliveryResponse.class);

    public PushTokenDeliveryResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        String entity = clientResponse.getEntity(String.class);
        setEntity(entity);
        setHeaders(clientResponse.getMetadata());
    }
}