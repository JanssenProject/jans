package org.xdi.model.passport;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Map;

/**
 * Created by jgomer on 2019-02-21.
 */
public class ProviderDetails {

    private String id;
    private String displayName;
    private String type;
    private String mapping;
    private String passportStrategyId;

    @JsonProperty("logo_img")
    private String logoImg;

    private boolean requestForEmail;
    private boolean emailLinkingSafe;
    private String passportAuthnParams;

    private Map<String, String> options;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMapping() {
        return mapping;
    }

    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    public String getPassportStrategyId() {
        return passportStrategyId;
    }

    public void setPassportStrategyId(String passportStrategyId) {
        this.passportStrategyId = passportStrategyId;
    }

    public String getLogoImg() {
        return logoImg;
    }

    public void setLogoImg(String logoImg) {
        this.logoImg = logoImg;
    }

    public boolean isRequestForEmail() {
        return requestForEmail;
    }

    public void setRequestForEmail(boolean requestForEmail) {
        this.requestForEmail = requestForEmail;
    }

    public boolean isEmailLinkingSafe() {
        return emailLinkingSafe;
    }

    public void setEmailLinkingSafe(boolean emailLinkingSafe) {
        this.emailLinkingSafe = emailLinkingSafe;
    }

    public String getPassportAuthnParams() {
        return passportAuthnParams;
    }

    public void setPassportAuthnParams(String passportAuthnParams) {
        this.passportAuthnParams = passportAuthnParams;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

}
