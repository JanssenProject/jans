/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.get;

import io.jans.as.client.BaseResponseWithErrors;
import io.jans.as.model.ssa.SsaErrorResponseType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static io.jans.as.client.util.ClientUtil.stringOrNull;
import static io.jans.as.model.ssa.SsaRequestParam.JTI;
import static io.jans.as.model.ssa.SsaRequestParam.SSA;

public class SsaGetResponse extends BaseResponseWithErrors<SsaErrorResponseType> {

    private static final Logger LOG = Logger.getLogger(SsaGetResponse.class);

    private List<SsaGetJson> ssaList = new ArrayList<>();

    public SsaGetResponse() {
    }

    public SsaGetResponse(Response clientResponse) {
        super(clientResponse);
    }

    public List<SsaGetJson> getSsaList() {
        return ssaList;
    }

    public void setSsaList(List<SsaGetJson> ssaList) {
        this.ssaList = ssaList;
    }

    @Override
    public SsaErrorResponseType fromString(String p_str) {
        return SsaErrorResponseType.fromString(p_str);
    }

    public void injectDataFromJson() {
        injectDataFromJson(entity);
    }

    @Override
    public void injectDataFromJson(String json) {
        if (StringUtils.isNotBlank(entity)) {
            try {
                JSONArray jsonArray = new JSONArray(entity);
                if (!jsonArray.isEmpty()) {
                    for (Object item : jsonArray) {
                        if (item instanceof JSONObject) {
                            JSONObject ssaWrapper = (JSONObject) item;
                            SsaGetJson ssaGetJson = new SsaGetJson();
                            ssaGetJson.setJsonObject(ssaWrapper);
                            if (ssaWrapper.has(SSA.getName())) {
                                JSONObject ssaJson = ssaWrapper.getJSONObject(SSA.getName());
                                ssaGetJson.setJti(stringOrNull(ssaJson, JTI.getName()));
                            }
                            ssaList.add(ssaGetJson);
                        }
                    }
                }
            } catch (JSONException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}