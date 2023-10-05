package io.jans.casa.plugins.strongauthn.conf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.casa.conf.Basic2FASettings;

import java.util.List;

public class Configuration {

	@JsonProperty("basic_2fa_settings")
    private Basic2FASettings basic2FASettings;

    @JsonProperty("policy_2fa")
    private List<EnforcementPolicy> enforcement2FA;

    @JsonProperty("trusted_dev_settings")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private TrustedDevicesSettings trustedDevicesSettings;

    public Basic2FASettings getBasic2FASettings() {
    	return basic2FASettings;
    }

    public TrustedDevicesSettings getTrustedDevicesSettings() {
        return trustedDevicesSettings;
    }

    public List<EnforcementPolicy> getEnforcement2FA() {
        return enforcement2FA;
    }

    public void setBasic2FASettings(Basic2FASettings basic2FASettings) {
    	this.basic2FASettings = basic2FASettings;
    }

    public void setEnforcement2FA(List<EnforcementPolicy> enforcement2FA) {
        this.enforcement2FA = enforcement2FA;
    }

    public void setTrustedDevicesSettings(TrustedDevicesSettings trustedDevicesSettings) {
        this.trustedDevicesSettings = trustedDevicesSettings;
    }

}
