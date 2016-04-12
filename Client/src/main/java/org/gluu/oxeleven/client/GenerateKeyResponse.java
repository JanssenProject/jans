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
 * @version April 12, 2016
 */
public class GenerateKeyResponse extends BaseResponse {

    private String alias;
    private String algorithm;
    private String curve;

    public GenerateKeyResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            alias = jsonObject.optString(ALIAS);
            algorithm = jsonObject.optString(ALGORITHM);
            curve = jsonObject.optString(CURVE);
        }
    }

    public String getAlias() {
        return alias;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getCurve() {
        return curve;
    }
}
