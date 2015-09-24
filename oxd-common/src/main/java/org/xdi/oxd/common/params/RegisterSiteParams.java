package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/09/2015
 */

public class RegisterSiteParams implements IParams {

    @JsonProperty(value = "redirect_url")
    private String redirectUrl;
    @JsonProperty(value = "app_type")
    private String appType;


    public RegisterSiteParams() {
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegisterSiteParams that = (RegisterSiteParams) o;

        if (appType != null ? !appType.equals(that.appType) : that.appType != null) return false;
        if (redirectUrl != null ? !redirectUrl.equals(that.redirectUrl) : that.redirectUrl != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = redirectUrl != null ? redirectUrl.hashCode() : 0;
        result = 31 * result + (appType != null ? appType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RegisterSiteParams");
        sb.append("{appType='").append(appType).append('\'');
        sb.append(", redirectUrl='").append(redirectUrl).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

