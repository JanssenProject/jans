package org.xdi.oxd.licenser.server.conf;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class Configuration implements Serializable {

    @JsonProperty(value = "thread-number-paid-license")
    private String threadNumberPaidLicense;
    @JsonProperty(value = "thread-number-premium-license")
    private String threadNumberPremiumLicense;

    public Configuration() {
    }

    public String getThreadNumberPaidLicense() {
        return threadNumberPaidLicense;
    }

    public void setThreadNumberPaidLicense(String threadNumberPaidLicense) {
        this.threadNumberPaidLicense = threadNumberPaidLicense;
    }

    public String getThreadNumberPremiumLicense() {
        return threadNumberPremiumLicense;
    }

    public void setThreadNumberPremiumLicense(String threadNumberPremiumLicense) {
        this.threadNumberPremiumLicense = threadNumberPremiumLicense;
    }
}

