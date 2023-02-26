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

/**
 * Provides json utilities for SSA
 */
@Stateless
@Named
public class SsaJsonService {

    @Inject
    private AppConfiguration appConfiguration;

    /**
     * Convert to json string from jsonObject.
     *
     * @param jsonObject Json object to convert
     * @return Json string
     * @throws JSONException If an error is found when converting.
     */
    public String jsonObjectToString(JSONObject jsonObject) throws JSONException {
        return jsonObject.toString(4).replace("\\/", "/");
    }

    /**
     * Convert to json string from jsonArray.
     *
     * @param jsonArray Json array to convert
     * @return Json string
     * @throws JSONException If an error is found when converting.
     */
    public String jsonArrayToString(JSONArray jsonArray) throws JSONException {
        return jsonArray.toString(4).replace("\\/", "/");
    }

    /**
     * Convert to JSONArray from ssaList with structure SSA.
     *
     * <p>
     * Method generates the SSA structure to add them to a json array.
     * </p>
     *
     * @param ssaList List of SSA
     * @return Json array
     * @throws JSONException If an error is found when converting.
     */
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
            Util.addToJSONObjectIfNotNull(responseJsonObject, STATUS.getName(), ssa.getState());

            JSONObject jsonSsa = new JSONObject();
            JsonApplier.getInstance().apply(ssa, jsonSsa);
            Util.addToJSONObjectIfNotNull(jsonSsa, ORG_ID.getName(), ssa.getOrgId());
            Util.addToJSONObjectIfNotNull(jsonSsa, SOFTWARE_ID.getName(), ssa.getAttributes().getSoftwareId());
            Util.addToJSONObjectIfNotNull(jsonSsa, SOFTWARE_ROLES.getName(), ssa.getAttributes().getSoftwareRoles());
            Util.addToJSONObjectIfNotNull(jsonSsa, GRANT_TYPES.getName(), ssa.getAttributes().getGrantTypes());
            Util.addToJSONObjectIfNotNull(jsonSsa, ISS.getName(), appConfiguration.getIssuer());
            Util.addToJSONObjectIfNotNull(jsonSsa, IAT.getName(), DateUtil.dateToUnixEpoch(ssa.getCreationDate()));
            Util.addToJSONObjectIfNotNull(jsonSsa, EXP.getName(), DateUtil.dateToUnixEpoch(ssa.getExpirationDate()));
            Util.addToJSONObjectIfNotNull(jsonSsa, JTI.getName(), ssa.getId());
            Util.addToJSONObjectIfNotNull(jsonSsa, DESCRIPTION.getName(), ssa.getDescription());
            Util.addToJSONObjectIfNotNull(jsonSsa, ONE_TIME_USE.getName(), ssa.getAttributes().getOneTimeUse());
            Util.addToJSONObjectIfNotNull(jsonSsa, ROTATE_SSA.getName(), ssa.getAttributes().getRotateSsa());
            if (!ssa.getAttributes().getCustomAttributes().isEmpty()) {
                ssa.getAttributes().getCustomAttributes().forEach((key, value) -> Util.addToJSONObjectIfNotNull(jsonSsa, key, value));
            }

            Util.addToJSONObjectIfNotNull(responseJsonObject, SSA.getName(), jsonSsa);
            jsonArray.put(responseJsonObject);
        }
        return jsonArray;
    }

    /**
     * Convert to JSON using jwt.
     *
     * @param jwt json web token of SSA
     * @return Json object.
     * @throws JSONException If an error is found when converting.
     */
    public JSONObject getJSONObject(String jwt) throws JSONException {
        JSONObject responseJsonObject = new JSONObject();
        Util.addToJSONObjectIfNotNull(responseJsonObject, SSA.getName(), jwt);
        return responseJsonObject;
    }
}
