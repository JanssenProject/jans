/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import static org.gluu.oxeleven.model.DeleteKeyResponseParam.DELETED;

import org.jboss.resteasy.client.ClientResponse;
import org.json.JSONObject;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
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
