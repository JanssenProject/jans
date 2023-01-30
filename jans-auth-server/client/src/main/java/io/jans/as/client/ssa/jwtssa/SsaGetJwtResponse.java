/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.jwtssa;

import io.jans.as.client.BaseResponseWithErrors;
import io.jans.as.model.ssa.SsaErrorResponseType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import static io.jans.as.model.ssa.SsaRequestParam.SSA;

public class SsaGetJwtResponse extends BaseResponseWithErrors<SsaErrorResponseType> {

    private static final Logger log = Logger.getLogger(SsaGetJwtResponse.class);

    private String ssa;

    public SsaGetJwtResponse(Response clientResponse) {
        super(clientResponse);
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
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has(SSA.getName())) {
                    ssa = jsonObj.getString(SSA.getName());
                }
            } catch (JSONException e) {
                log.error("Error on inject data from json: " + e.getMessage(), e);
            }
        }
    }

    public String getSsa() {
        return ssa;
    }

    public void setSsa(String ssa) {
        this.ssa = ssa;
    }
}