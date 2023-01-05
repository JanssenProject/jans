/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import static io.jans.eleven.model.DeleteKeyResponseParam.DELETED;

import jakarta.ws.rs.core.Response;
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
