/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientResponse;

import static org.gluu.oxeleven.model.GenerateKeyResponseParam.*;

/**
 * @author Javier Rojas Blum
 * @version April 26, 2016
 */
public class GenerateKeyResponse extends BaseResponse {

    private String keyType;
    private String keyId;
    private String keyUse;
    private String algorithm;
    private String curve;

    public GenerateKeyResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            keyType = jsonObject.optString(KEY_TYPE);
            keyId = jsonObject.optString(KEY_ID);
            keyUse = jsonObject.optString(KEY_USE);
            algorithm = jsonObject.optString(ALGORITHM);
            curve = jsonObject.optString(CURVE);
        }
    }

    public String getKeyType() {
        return keyType;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getKeyUse() {
        return keyUse;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getCurve() {
        return curve;
    }
}
