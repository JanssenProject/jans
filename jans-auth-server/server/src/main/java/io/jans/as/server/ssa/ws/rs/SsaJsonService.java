/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs;

import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.json.JsonApplier;
import io.jans.as.model.util.DateUtil;
import io.jans.as.model.util.Util;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static io.jans.as.model.ssa.SsaRequestParam.*;

@Stateless
@Named
public class SsaJsonService {

    @Inject
    private AppConfiguration appConfiguration;

    public String jsonObjectToString(JSONObject jsonObject) throws JSONException {
        return jsonObject.toString(4).replace("\\/", "/");
    }

    public String jsonArrayToString(JSONArray jsonArray) throws JSONException {
        return jsonArray.toString(4).replace("\\/", "/");
    }

    public JSONArray getJSONArray(List<Ssa> ssaList) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        if (ssaList == null) {
            return jsonArray;
        }
        for (Ssa ssa : ssaList) {
            JSONObject responseJsonObject = new JSONObject();
            JsonApplier.getInstance().apply(ssaList, responseJsonObject);

            Util.addToJSONObjectIfNotNull(responseJsonObject, CREATED_AT.getName(), DateUtil.dateToUnixEpoch(ssa.getCreationDate()));
            Util.addToJSONObjectIfNotNull(responseJsonObject, EXPIRATION.getName(), DateUtil.dateToUnixEpoch(ssa.getExpirationDate()));
            Util.addToJSONObjectIfNotNull(responseJsonObject, ISSUER.getName(), ssa.getCreatorId());

            JSONObject jsonSsa = new JSONObject();
            JsonApplier.getInstance().apply(ssa, jsonSsa);
            Util.addToJSONObjectIfNotNull(jsonSsa, ORG_ID.getName(), Long.parseLong(ssa.getOrgId()));
            Util.addToJSONObjectIfNotNull(jsonSsa, SOFTWARE_ID.getName(), ssa.getAttributes().getSoftwareId());
            Util.addToJSONObjectIfNotNull(jsonSsa, SOFTWARE_ROLES.getName(), ssa.getAttributes().getSoftwareRoles());
            Util.addToJSONObjectIfNotNull(jsonSsa, GRANT_TYPES.getName(), ssa.getAttributes().getGrantTypes());
            Util.addToJSONObjectIfNotNull(jsonSsa, ISS.getName(), appConfiguration.getIssuer());
            Util.addToJSONObjectIfNotNull(jsonSsa, IAT.getName(), DateUtil.dateToUnixEpoch(ssa.getCreationDate()));
            Util.addToJSONObjectIfNotNull(jsonSsa, EXP.getName(), DateUtil.dateToUnixEpoch(ssa.getExpirationDate()));
            Util.addToJSONObjectIfNotNull(jsonSsa, JTI.getName(), ssa.getId());

            Util.addToJSONObjectIfNotNull(responseJsonObject, SSA.getName(), jsonSsa);
            jsonArray.put(responseJsonObject);
        }
        return jsonArray;
    }

    public JSONObject getJSONObject(String jwt) throws JSONException {
        JSONObject responseJsonObject = new JSONObject();
        Util.addToJSONObjectIfNotNull(responseJsonObject, SSA.getName(), jwt);
        return responseJsonObject;
    }
}
