package org.gluu.oxeleven.client;

import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientResponse;

import static org.gluu.oxeleven.model.SignResponseParam.SIGNATURE;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
 */
public class SignResponse extends BaseResponse {

    private String signature;

    public SignResponse(ClientResponse<String> clientResponse) {
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
