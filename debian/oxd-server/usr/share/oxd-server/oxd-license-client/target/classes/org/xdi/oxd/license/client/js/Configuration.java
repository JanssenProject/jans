package org.xdi.oxd.license.client.js;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class Configuration implements Serializable {

    @JsonProperty(value = "base-dn")
    private String baseDn;
    @JsonProperty(value = "thread-number-paid-license")
    private Integer threadNumberPaidLicense;
    @JsonProperty(value = "thread-number-premium-license")
    private Integer threadNumberPremiumLicense;
    @JsonProperty(value = "authorize-request")
    private String authorizeRequest;
    @JsonProperty(value = "logout-url")
    private String logoutUrl;
    @JsonProperty(value = "client-id")
    private String clientId;
    @JsonProperty(value = "license-possible-features")
    private List<String> licensePossibleFeatures;
    @JsonProperty(value = "ejbca-ws-url")
    private String ejbCaWsUrl;

    public String getEjbCaWsUrl() {
        return ejbCaWsUrl;
    }

    public void setEjbCaWsUrl(String ejbCaWsUrl) {
        this.ejbCaWsUrl = ejbCaWsUrl;
    }

    public List<String> getLicensePossibleFeatures() {
        return licensePossibleFeatures;
    }

    public void setLicensePossibleFeatures(List<String> licensePossibleFeatures) {
        this.licensePossibleFeatures = licensePossibleFeatures;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public String getAuthorizeRequest() {
        return authorizeRequest;
    }

    public void setAuthorizeRequest(String authorizeRequest) {
        this.authorizeRequest = authorizeRequest;
    }

    public Configuration() {
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public Integer getThreadNumberPaidLicense() {
        return threadNumberPaidLicense;
    }

    public void setThreadNumberPaidLicense(Integer threadNumberPaidLicense) {
        this.threadNumberPaidLicense = threadNumberPaidLicense;
    }

    public Integer getThreadNumberPremiumLicense() {
        return threadNumberPremiumLicense;
    }

    public void setThreadNumberPremiumLicense(Integer threadNumberPremiumLicense) {
        this.threadNumberPremiumLicense = threadNumberPremiumLicense;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Configuration");
        sb.append("{baseDn='").append(baseDn).append('\'');
        sb.append(", threadNumberPaidLicense=").append(threadNumberPaidLicense);
        sb.append(", threadNumberPremiumLicense=").append(threadNumberPremiumLicense);
        sb.append('}');
        return sb.toString();
    }
}

