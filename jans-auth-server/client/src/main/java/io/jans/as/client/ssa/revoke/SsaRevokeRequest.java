/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.revoke;

import io.jans.as.client.BaseRequest;
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.ssa.SsaRequestParam;
import io.jans.as.model.util.QueryBuilder;
import jakarta.ws.rs.core.MediaType;

public class SsaRevokeRequest extends BaseRequest {

    private String accessToken;

    private String jti;

    private String orgId;

    public SsaRevokeRequest() {
        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);
        setAuthorizationMethod(AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    @Override
    public String getQueryString() {
        QueryBuilder builder = QueryBuilder.instance();
        builder.append(SsaRequestParam.JTI.getName(), jti);
        builder.append(SsaRequestParam.ORG_ID.getName(), orgId);
        return builder.toString();
    }
}
