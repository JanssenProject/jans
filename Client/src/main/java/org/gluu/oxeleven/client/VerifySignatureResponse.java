/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import org.json.JSONObject;
import org.jboss.resteasy.client.ClientResponse;

import static org.gluu.oxeleven.model.VerifySignatureResponseParam.VERIFIED;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
 */
public class VerifySignatureResponse extends BaseResponse {

    private boolean verified;

    public VerifySignatureResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            verified = jsonObject.optBoolean(VERIFIED);
        }
    }

    public boolean isVerified() {
        return verified;
    }
}
