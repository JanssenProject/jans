package org.xdi.oxauth.service.uma.authorization;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/04/2015
 */

public class RequiredClaim implements Serializable {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "friendly_name")
    private String friendlyName;

    @JsonProperty(value = "claim_type")
    private String claimType;

    @JsonProperty(value = "claim_token_format")
    private String[] claimTokenFormat;

    @JsonProperty(value = "issuer")
    private String[] issuer;

    public RequiredClaim() {
    }

    public String[] getClaimTokenFormat() {
        return claimTokenFormat;
    }

    public void setClaimTokenFormat(String[] claimTokenFormat) {
        this.claimTokenFormat = claimTokenFormat;
    }

    public String getClaimType() {
        return claimType;
    }

    public void setClaimType(String claimType) {
        this.claimType = claimType;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String[] getIssuer() {
        return issuer;
    }

    public void setIssuer(String[] issuer) {
        this.issuer = issuer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
