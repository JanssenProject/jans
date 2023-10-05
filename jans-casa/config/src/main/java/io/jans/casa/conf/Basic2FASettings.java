package io.jans.casa.conf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Basic2FASettings {

    public static final int MIN_CREDS_2FA_DEFAULT = 2;

    private boolean autoEnable;

    private boolean allowSelfEnableDisable = true;

    private boolean allowSelectPreferred = true;
	
    @JsonProperty("min_creds")
    private Integer minCreds = MIN_CREDS_2FA_DEFAULT;

    public Integer getMinCreds() {
        return minCreds;
    }

    public boolean isAutoEnable() {
        return autoEnable;
    }

    public boolean isAllowSelfEnableDisable() {
        return allowSelfEnableDisable;
    }

    public boolean isAllowSelectPreferred() {
        return allowSelectPreferred;
    }
    
    public void setMinCreds(Integer minCreds) {
        this.minCreds = minCreds;
    }
    
    public void setAutoEnable(boolean autoEnable) {
        this.autoEnable = autoEnable;
    }
    
    public void setAllowSelfEnableDisable(boolean allowSelfEnableDisable) {
        this.allowSelfEnableDisable = allowSelfEnableDisable;
    }
    
    public void setAllowSelectPreferred(boolean allowSelectPreferred) {
        this.allowSelectPreferred = allowSelectPreferred;
    }
    
}