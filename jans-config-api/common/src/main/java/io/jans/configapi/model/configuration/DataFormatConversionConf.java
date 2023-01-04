package io.jans.configapi.model.configuration;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataFormatConversionConf {

    /**
     * Flag to enable and disable data conversion
     */
    private boolean enabled;

    /**
     * HTTP methods for which data conversion is to be disabled
     */
    private Collection<String> ignoreHttpMethod;
    
    /**
     * attributes for data conversion
     */
    private Collection<String> attributes;

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
    
    
    public Collection<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Collection<String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "DataFormatConversionConf [enabled=" + enabled 
                + ", ignoreHttpMethod=" + ignoreHttpMethod
                + ", attributes=" + attributes 
                + "]";
    }

}
