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
    private ConversionAttributes conversionAttributes;

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

    public ConversionAttributes getConversionAttributes() {
        return conversionAttributes;
    }

    public void setConversionAttributes(ConversionAttributes conversionAttributes) {
        this.conversionAttributes = conversionAttributes;
    }

    @Override
    public String toString() {
        return "DataFormatConversionConf [enabled=" + enabled + ", ignoreHttpMethod=" + ignoreHttpMethod
                + ", conversionAttributes=" + conversionAttributes + "]";
    }

}
