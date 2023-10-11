package io.jans.casa.plugins.strongauthn.conf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TrustedDevicesSettings {

    @JsonProperty("location_exp_days")
    private Integer locationExpirationDays;

    @JsonProperty("device_exp_days")
    private Integer deviceExpirationDays;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Integer getLocationExpirationDays() {
        return locationExpirationDays;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Integer getDeviceExpirationDays() {
        return deviceExpirationDays;
    }

    public void setLocationExpirationDays(Integer locationExpirationDays) {
        this.locationExpirationDays = locationExpirationDays;
    }

    public void setDeviceExpirationDays(Integer deviceExpirationDays) {
        this.deviceExpirationDays = deviceExpirationDays;
    }

}
