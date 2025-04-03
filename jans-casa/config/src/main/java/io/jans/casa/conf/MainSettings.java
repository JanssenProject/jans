package io.jans.casa.conf;

import com.fasterxml.jackson.annotation.*;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MainSettings {

    @JsonProperty("enable_pass_reset")
    private boolean enablePassReset;

    @JsonProperty("use_password_policy")
    private boolean usePasswordPolicy;

    @JsonProperty("use_branding")
    private boolean useExternalBranding;

    @JsonProperty("log_level")
    private String logLevel;

    @JsonProperty("acr_plugin_mapping")
    private Map<String, String> acrPluginMap;
    
    @JsonProperty("acr_priority")
    private List<String> acrPriority;

    @JsonProperty("extra_css")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String extraCssSnippet;

    @JsonProperty("oidc_config")
    private OIDCSettings oidcSettings;

    @JsonProperty("allowed_cors_domains")
    private List<String> corsDomains;

    @JsonProperty("basic_2fa_settings")
    private Basic2FASettings basic2FASettings = new Basic2FASettings();
    
    @JsonProperty("plugins_settings")
    private Map<String, Map<String, Object>> pluginSettings = new HashMap<>();

    public boolean isEnablePassReset() {
        return enablePassReset;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public boolean isUseExternalBranding() {
        return useExternalBranding;
    }

    public boolean isUsePasswordPolicy() {
        return usePasswordPolicy;
    }

    public Map<String, String> getAcrPluginMap() {
        return acrPluginMap;
    }

    public List<String> getAcrPriority() {
        return acrPriority;
    }

    public String getExtraCssSnippet() {
        return extraCssSnippet;
    }

    public OIDCSettings getOidcSettings() {
        return oidcSettings;
    }

    public List<String> getCorsDomains() {
        return corsDomains;
    }
    
    public Basic2FASettings getBasic2FASettings() {
    	return basic2FASettings;
    }

    public Map<String, Map<String, Object>> getPluginSettings() {
        return pluginSettings;
    }

    public void setEnablePassReset(boolean enablePassReset) {
        this.enablePassReset = enablePassReset;
    }

    public void setUsePasswordPolicy(boolean usePasswordPolicy) {
        this.usePasswordPolicy = usePasswordPolicy;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public void setUseExternalBranding(boolean useExternalBranding) {
        this.useExternalBranding = useExternalBranding;
    }

    public void setAcrPluginMap(Map<String, String> acrPluginMap) {
        this.acrPluginMap = acrPluginMap;
    }

    public void setAcrPriority(List<String> acrPriority) {
        this.acrPriority = acrPriority;
    }

    public void setExtraCssSnippet(String extraCssSnippet) {
        this.extraCssSnippet = extraCssSnippet;
    }

    public void setOidcSettings(OIDCSettings oidcSettings) {
        this.oidcSettings = oidcSettings;
    }

    public void setCorsDomains(List<String> corsDomains) {
        this.corsDomains = corsDomains;
    }

    public void setBasic2FASettings(Basic2FASettings basic2FASettings) {
    	this.basic2FASettings = basic2FASettings;
    }
    
    public void setPluginSettings(Map<String, Map<String, Object>> pluginSettings) {
        this.pluginSettings = pluginSettings;
    }

}
