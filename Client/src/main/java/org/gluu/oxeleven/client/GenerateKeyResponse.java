package org.gluu.oxeleven.client;

import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientResponse;

/**
 * @author Javier Rojas Blum
 * @version March 29, 2016
 */
public class GenerateKeyResponse extends BaseResponse {

    private String alias;

    public GenerateKeyResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            alias = jsonObject.optString("alias");
        }
    }

    public String getAlias() {
        return alias;
    }
}
