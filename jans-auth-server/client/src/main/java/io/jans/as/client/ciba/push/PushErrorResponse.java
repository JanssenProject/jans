/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba.push;

import io.jans.as.client.BaseResponse;
import org.apache.log4j.Logger;

import jakarta.ws.rs.core.Response;

/**
 * @author Javier Rojas Blum
 * @version May 9, 2020
 */
public class PushErrorResponse extends BaseResponse {

    private static final Logger LOG = Logger.getLogger(PushErrorResponse.class);

    public PushErrorResponse(Response clientResponse) {
        super(clientResponse);

        setHeaders(clientResponse.getMetadata());
    }
}
