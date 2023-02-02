/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.as.client.BaseRequest;
import io.jans.as.client.util.ClientUtil;
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.json.JsonApplier;
import jakarta.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static io.jans.as.client.util.ClientUtil.extractListByKey;
import static io.jans.as.client.util.ClientUtil.longOrNull;
import static io.jans.as.model.ssa.SsaRequestParam.*;

public class SsaCreateRequest extends BaseRequest {

    private static final Logger log = Logger.getLogger(SsaCreateRequest.class);

    @JsonProperty(value = "org_id")
    private String orgId;

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

    public SsaCreateRequest() {
        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);
        setAuthorizationMethod(AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD);
        this.softwareRoles = new ArrayList<>();
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
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

    public static SsaCreateRequest fromJson(String json) throws JSONException {
        return fromJson(new JSONObject(json));
    }

    public static SsaCreateRequest fromJson(JSONObject requestObject) throws JSONException {
        final SsaCreateRequest result = new SsaCreateRequest();
        JsonApplier.getInstance().apply(requestObject, result);
        result.setOrgId(requestObject.getString(ORG_ID.getName()));
        result.setExpiration(longOrNull(requestObject, EXPIRATION.getName()));
        result.setDescription(requestObject.optString(DESCRIPTION.getName()));
        result.setSoftwareId(requestObject.optString(SOFTWARE_ID.getName()));
        result.setSoftwareRoles(extractListByKey(requestObject, SOFTWARE_ROLES.getName()));
        result.setGrantTypes(extractListByKey(requestObject, GRANT_TYPES.getName()));
        result.setOneTimeUse(requestObject.optBoolean(ONE_TIME_USE.getName(), true));
        result.setRotateSsa(requestObject.optBoolean(ROTATE_SSA.getName(), true));
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
                '}';
    }
}
