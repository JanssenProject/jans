package org.gluu.oxeleven.client;

import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientResponse;

/**
 * @author Javier Rojas Blum
 * @version March 29, 2016
 */
public class SignResponse extends BaseResponse {

    private String signature;

    public SignResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            signature = jsonObject.optString("signature");
        }
    }

    public String getSignature() {
        return signature;
    }
}
