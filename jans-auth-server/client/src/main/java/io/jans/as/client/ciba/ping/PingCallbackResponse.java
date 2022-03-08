/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba.ping;

import io.jans.as.client.BaseResponse;

import jakarta.ws.rs.core.Response;

/**
 * @author Javier Rojas Blum
 * @version December 21, 2019
 */
public class PingCallbackResponse extends BaseResponse {

    public PingCallbackResponse(Response clientResponse) {
        super(clientResponse);

        setHeaders(clientResponse.getMetadata());
    }
}
