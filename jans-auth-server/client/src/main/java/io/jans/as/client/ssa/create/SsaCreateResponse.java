/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.create;

import io.jans.as.client.BaseResponseWithErrors;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.ssa.SsaErrorResponseType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import static io.jans.as.model.ssa.SsaRequestParam.JTI;
import static io.jans.as.model.ssa.SsaRequestParam.SSA;

public class SsaCreateResponse extends BaseResponseWithErrors<SsaErrorResponseType> {

    private static final Logger LOG = Logger.getLogger(SsaCreateResponse.class);

    private String ssa;

    private String jti;

    public SsaCreateResponse() {
    }

    public SsaCreateResponse(Response clientResponse) {
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
                    if (StringUtils.isNotBlank(ssa)) {
                        JwtClaims jwtClaims = Objects.requireNonNull(Jwt.parseSilently(ssa)).getClaims();
                        if (jwtClaims.hasClaim(JTI.getName())) {
                            jti = jwtClaims.getClaimAsString(JTI.getName());
                        }
                    }
                }
            } catch (JSONException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public String getSsa() {
        return ssa;
    }

    public void setSsa(String ssa) {
        this.ssa = ssa;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }
}