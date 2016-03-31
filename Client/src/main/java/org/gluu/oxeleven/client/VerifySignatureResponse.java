package org.gluu.oxeleven.client;

import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientResponse;

import static org.gluu.oxeleven.model.VerifySignatureResponseParam.VERIFIED;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
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
