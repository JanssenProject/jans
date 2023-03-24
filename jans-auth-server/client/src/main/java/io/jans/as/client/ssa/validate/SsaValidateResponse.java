/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.validate;

import io.jans.as.client.BaseResponseWithErrors;
import io.jans.as.model.ssa.SsaErrorResponseType;
import jakarta.ws.rs.core.Response;

public class SsaValidateResponse extends BaseResponseWithErrors<SsaErrorResponseType> {

    public SsaValidateResponse(Response clientResponse) {
        super(clientResponse);
    }

    @Override
    public SsaErrorResponseType fromString(String p_str) {
        return SsaErrorResponseType.fromString(p_str);
    }

    @Override
    public void injectDataFromJson(String json) {
    }
}