package io.jans.as.common.model.ssa;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Z
 */
public class SsaAttributes {

    @JsonProperty("oneTimeUse")
    private Boolean oneTimeUse;

    @JsonProperty("rotateSsa")
    private Boolean rotateSsa;

    @JsonProperty("clientDn")
    private String clientDn;

    @JsonProperty("customAttributes")
    private Map<String, String> customAttributes;

    @JsonProperty("softwareId")
    private String softwareId;

    @JsonProperty("lifetime")
    private Integer lifetime;

    @JsonProperty("softwareRoles")
    private List<String> softwareRoles;

    @JsonProperty("grantTypes")
    private List<String> grantTypes;

    // it should be `scope` because this is how it's named in spec and in RegisterRequest
    @JsonProperty("scope")
    private List<String> scopes;

    public List<String> getScopes() {
        return scopes;
    }

    public SsaAttributes setScopes(List<String> scopes) {
        this.scopes = scopes;
        return this;
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

    public String getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(String softwareId) {
        this.softwareId = softwareId;
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

    public String getClientDn() {
        return clientDn;
    }

    public void setClientDn(String clientDn) {
        this.clientDn = clientDn;
    }

    public Integer getLifetime() {
        return lifetime;
    }

    public void setLifetime(Integer lifetime) {
        this.lifetime = lifetime;
    }

    public Map<String, String> getCustomAttributes() {
        if (customAttributes == null) {
            customAttributes = new HashMap<>();
        }
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, String> customAttributes) {
        this.customAttributes = customAttributes;
    }

    @Override
    public String toString() {
        return "SsaAttributes{" +
                "oneTimeUse=" + oneTimeUse +
                ", rotateSsa=" + rotateSsa +
                ", clientDn='" + clientDn + '\'' +
                ", lifetime=" + lifetime +
                ", scopes=" + scopes +
                ", customAttributes=" + customAttributes +
                '}';
    }
}
