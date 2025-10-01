package io.jans.casa.plugins.strongauthn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TrustedDevice {

    private Browser browser;
    private OperatingSystem os;
    private long addedOn;
    private List<TrustedOrigin> origins;

    public Browser getBrowser() {
        return browser;
    }

    public OperatingSystem getOs() {
        return os;
    }

    public long getAddedOn() {
        return addedOn;
    }

    public List<TrustedOrigin> getOrigins() {
        return origins;
    }

    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    public void setOs(OperatingSystem os) {
        this.os = os;
    }

    public void setAddedOn(long addedOn) {
        this.addedOn = addedOn;
    }

    public void setOrigins(List<TrustedOrigin> origins) {
        this.origins = origins;
    }

    public void sortOriginsDescending() {

        if (origins != null) {
            origins.sort((o1, o2) -> {

                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null) {
                    return 1;
                }
                if (o2 == null) {
                    return -1;
                }

                Long origin1 = Long.valueOf(o1.getTimestamp());
                Long origin2 = Long.valueOf(o2.getTimestamp());

                return origin2.compareTo(origin1);
            });
        }

    }

}
