/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.as.client.util.ClientUtil;
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.json.JsonApplier;
import jakarta.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static io.jans.as.client.util.ClientUtil.*;
import static io.jans.as.model.ssa.SsaRequestParam.*;

public class SsaRequest extends BaseRequest {

    private static final Logger log = Logger.getLogger(SsaRequest.class);

    @JsonProperty(value = "org_id")
    private Long orgId;

    private Long expiration;

    private String description;

    @JsonProperty(value = "software_id")
    private String softwareId;

    @JsonProperty(value = "software_roles")
    private List<String> softwareRoles;

    @JsonProperty(value = "grant_types")
    private List<String> grantTypes;

    @JsonProperty(value = "one_time_use")
    private Boolean oneTimeUse;

    @JsonProperty(value = "rotate_ssa")
    private Boolean rotateSsa;

    private String accessToken;

    public SsaRequest() {
        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);
        setAuthorizationMethod(AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD);
        this.softwareRoles = new ArrayList<>();
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(String softwareId) {
        this.softwareId = softwareId;
    }

    public List<String> getSoftwareRoles() {
        return softwareRoles;
    }

    public void setSoftwareRoles(List<String> softwareRoles) {
        this.softwareRoles = softwareRoles;
    }

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public Boolean getOneTimeUse() {
        return oneTimeUse;
    }

    public void setOneTimeUse(Boolean oneTimeUse) {
        this.oneTimeUse = oneTimeUse;
    }

    public Boolean getRotateSsa() {
        return rotateSsa;
    }

    public void setRotateSsa(Boolean rotateSsa) {
        this.rotateSsa = rotateSsa;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public static SsaRequest fromJson(String json) throws JSONException {
        return fromJson(new JSONObject(json));
    }

    public static SsaRequest fromJson(JSONObject requestObject) throws JSONException {
        final SsaRequest result = new SsaRequest();
        JsonApplier.getInstance().apply(requestObject, result);
        result.setOrgId(requestObject.getLong(ORG_ID.toString()));
        result.setExpiration(longOrNull(requestObject, EXPIRATION.toString()));
        result.setDescription(requestObject.optString(DESCRIPTION.toString()));
        result.setSoftwareId(requestObject.optString(SOFTWARE_ID.toString()));
        result.setSoftwareRoles(extractListByKey(requestObject, SOFTWARE_ROLES.toString()));
        result.setGrantTypes(extractListByKey(requestObject, GRANT_TYPES.toString()));
        result.setOneTimeUse(booleanOrNull(requestObject, ONE_TIME_USE.toString()));
        result.setRotateSsa(booleanOrNull(requestObject, ROTATE_SSA.toString()));
        return result;
    }

    @Override
    public String getQueryString() {
        try {
            return ClientUtil.toPrettyJson(getJSONParameters()).replace("\\/", "/");
        } catch (JSONException | JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public JSONObject getJSONParameters() throws JSONException {
        JSONObject parameters = new JSONObject();
        parameters.put(ORG_ID.getName(), orgId);
        parameters.put(EXPIRATION.getName(), expiration);
        parameters.put(DESCRIPTION.getName(), description);
        parameters.put(SOFTWARE_ID.getName(), softwareId);
        parameters.put(SOFTWARE_ROLES.getName(), softwareRoles);
        parameters.put(GRANT_TYPES.getName(), grantTypes);
        parameters.put(ONE_TIME_USE.getName(), oneTimeUse);
        parameters.put(ROTATE_SSA.getName(), rotateSsa);
        return parameters;
    }

    @Override
    public String toString() {
        return "SsaRequest{" +
                "orgId=" + orgId +
                ", expiration=" + expiration +
                ", description='" + description + '\'' +
                ", softwareId='" + softwareId + '\'' +
                ", softwareRoles=" + softwareRoles +
                ", grantTypes=" + grantTypes +
                ", oneTimeUse=" + oneTimeUse +
                ", rotateSsa=" + rotateSsa +
                ", accessToken='" + accessToken + '\'' +
                '}';
    }
}
