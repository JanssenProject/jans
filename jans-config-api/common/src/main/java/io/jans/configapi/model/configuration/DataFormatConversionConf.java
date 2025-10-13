package io.jans.configapi.model.configuration;

import java.io.Serializable;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataFormatConversionConf implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Schema(description = "Flag to enable and disable data conversion.")
    private boolean enabled;

    @Schema(description = "HTTP methods for which data conversion is to be disabled.")
    private Collection<String> ignoreHttpMethod;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Collection<String> getIgnoreHttpMethod() {
        return ignoreHttpMethod;
    }

    public void setIgnoreHttpMethod(Collection<String> ignoreHttpMethod) {
        this.ignoreHttpMethod = ignoreHttpMethod;
    }

    @Override
    public String toString() {
        return "DataFormatConversionConf [enabled=" + enabled + ", ignoreHttpMethod=" + ignoreHttpMethod + "]";
    }

}
