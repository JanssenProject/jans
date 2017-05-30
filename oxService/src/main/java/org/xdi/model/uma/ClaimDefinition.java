package org.xdi.model.uma;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

/**
 * @author yuriyz on 05/30/2017.
 */
public class ClaimDefinition {

    @JsonProperty(value = "claim_token_format")
    @XmlElement(name = "claim_token_format")
    private List<String> claimTokenFormat;

    @JsonProperty(value = "claim_type")
    @XmlElement(name = "claim_type")
    private List<String> claimType;

    @JsonProperty(value = "friendly_name")
    @XmlElement(name = "friendly_name")
    private String friendlyName;

    @JsonProperty(value = "issuer")
    @XmlElement(name = "issuer")
    private List<String> issuer;

    @JsonProperty(value = "name")
    @XmlElement(name = "name")
    private String name;

    public List<String> getClaimTokenFormat() {
        return claimTokenFormat;
    }

    public void setClaimTokenFormat(List<String> claimTokenFormat) {
        this.claimTokenFormat = claimTokenFormat;
    }

    public List<String> getClaimType() {
        return claimType;
    }

    public void setClaimType(List<String> claimType) {
        this.claimType = claimType;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public List<String> getIssuer() {
        return issuer;
    }

    public void setIssuer(List<String> issuer) {
        this.issuer = issuer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
