package org.gluu.oxeleven.client;

import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientResponse;

/**
 * @author Javier Rojas Blum
 * @version March 29, 2016
 */
public class DeleteKeyResponse extends BaseResponse {

    private boolean deleted;


    public DeleteKeyResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            deleted = jsonObject.optBoolean("deleted");
        }
    }

    public boolean isDeleted() {
        return deleted;
    }
}
