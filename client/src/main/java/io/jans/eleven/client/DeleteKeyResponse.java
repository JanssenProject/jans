/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import static io.jans.eleven.model.DeleteKeyResponseParam.DELETED;

import javax.ws.rs.core.Response;
import org.json.JSONObject;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
 */
public class DeleteKeyResponse extends BaseResponse {

    private boolean deleted;

    public DeleteKeyResponse(Response clientResponse) {
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
