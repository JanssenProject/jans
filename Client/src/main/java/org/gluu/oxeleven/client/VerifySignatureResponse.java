package org.gluu.oxeleven.client;

import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientResponse;

/**
 * @author Javier Rojas Blum
 * @version March 29, 2016
 */
public class VerifySignatureResponse extends BaseResponse {

    private boolean verified;

    public VerifySignatureResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            verified = jsonObject.optBoolean("verified");
        }
    }

    public boolean isVerified() {
        return verified;
    }
}
