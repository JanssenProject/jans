/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import static io.jans.eleven.model.SignResponseParam.SIGNATURE;

import javax.ws.rs.core.Response;
import org.json.JSONObject;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
 */
public class SignResponse extends BaseResponse {

    private String signature;

    public SignResponse(Response clientResponse) {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            signature = jsonObject.optString(SIGNATURE);
        }
    }

    public String getSignature() {
        return signature;
    }
}
