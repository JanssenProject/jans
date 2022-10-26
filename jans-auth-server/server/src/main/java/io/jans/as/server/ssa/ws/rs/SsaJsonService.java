/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs;

import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.model.json.JsonApplier;
import io.jans.as.model.util.Util;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import org.json.JSONException;
import org.json.JSONObject;

import static io.jans.as.model.ssa.SsaRequestParam.*;

@Stateless
@Named
public class SsaJsonService {

    public String jsonObjectToString(JSONObject jsonObject) throws JSONException {
        return jsonObject.toString(4).replace("\\/", "/");
    }

    public JSONObject getJSONObject(Ssa ssa) throws JSONException {
        JSONObject responseJsonObject = new JSONObject();
        JsonApplier.getInstance().apply(ssa, responseJsonObject);

        Util.addToJSONObjectIfNotNull(responseJsonObject, ORG_ID.toString(), ssa.getOrgId());
        Util.addToJSONObjectIfNotNull(responseJsonObject, EXPIRATION.toString(), ssa.getExpirationDate());
        Util.addToJSONObjectIfNotNull(responseJsonObject, DESCRIPTION.toString(), ssa.getDescription());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SOFTWARE_ID.toString(), ssa.getAttributes().getSoftwareId());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SOFTWARE_ROLES.toString(), ssa.getAttributes().getSoftwareRoles());
        Util.addToJSONObjectIfNotNull(responseJsonObject, GRANT_TYPES.toString(), ssa.getAttributes().getGrantTypes());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ONE_TIME_USE.toString(), ssa.getAttributes().getOneTimeUse());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ROTATE_SSA.toString(), ssa.getAttributes().getRotateSsa());
        return responseJsonObject;
    }

    public JSONObject getJSONObject(String jwt) throws JSONException {
        JSONObject responseJsonObject = new JSONObject();
        Util.addToJSONObjectIfNotNull(responseJsonObject, "ssa", jwt);
        return responseJsonObject;
    }
}
