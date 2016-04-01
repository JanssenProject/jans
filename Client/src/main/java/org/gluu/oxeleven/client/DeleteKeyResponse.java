package org.gluu.oxeleven.client;

import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientResponse;

import static org.gluu.oxeleven.model.DeleteKeyResponseParam.DELETED;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
 */
public class DeleteKeyResponse extends BaseResponse {

    private boolean deleted;

    public DeleteKeyResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            deleted = jsonObject.optBoolean(DELETED);
        }
    }

    public boolean isDeleted() {
        return deleted;
    }
}
