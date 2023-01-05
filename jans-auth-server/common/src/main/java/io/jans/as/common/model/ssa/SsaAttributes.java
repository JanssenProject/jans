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

    @JsonProperty("softwareRoles")
    private List<String> softwareRoles;

    @JsonProperty("grantTypes")
    private List<String> grantTypes;

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
                ", customAttributes=" + customAttributes +
                '}';
    }
}
