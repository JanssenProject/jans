package io.jans.as.model.ssa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class SsaConfiguration {

    private String ssaEndpoint;

    private List<String> ssaCustomAttributes = new ArrayList<>();

    private String ssaSigningAlg = "RS256";

    private Integer ssaExpirationInDays = 30;

    private Map<String, List<String>> ssaMapSoftwareRolesToScopes;

    public Map<String, List<String>> getSsaMapSoftwareRolesToScopes() {
        return ssaMapSoftwareRolesToScopes;
    }

    public SsaConfiguration setSsaMapSoftwareRolesToScopes(Map<String, List<String>> ssaMapSoftwareRolesToScopes) {
        this.ssaMapSoftwareRolesToScopes = ssaMapSoftwareRolesToScopes;
        return this;
    }

    public String getSsaEndpoint() {
        return ssaEndpoint;
    }

    public void setSsaEndpoint(String ssaEndpoint) {
        this.ssaEndpoint = ssaEndpoint;
    }

    public List<String> getSsaCustomAttributes() {
        return ssaCustomAttributes;
    }

    public void setSsaCustomAttributes(List<String> ssaCustomAttributes) {
        this.ssaCustomAttributes = ssaCustomAttributes;
    }

    public String getSsaSigningAlg() {
        return ssaSigningAlg;
    }

    public void setSsaSigningAlg(String ssaSigningAlg) {
        this.ssaSigningAlg = ssaSigningAlg;
    }

    public Integer getSsaExpirationInDays() {
        return ssaExpirationInDays;
    }

    public void setSsaExpirationInDays(Integer ssaExpirationInDays) {
        this.ssaExpirationInDays = ssaExpirationInDays;
    }
}
