package io.jans.configapi.model.configuration;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversionAttributes {

    /**
     * Date attributes for conversion
     */
    private Collection<String> dateAttributes;

    /**
     * boolean attributes for conversion
     */
    private Collection<String> booleanAttributes;

    public Collection<String> getDateAttributes() {
        return dateAttributes;
    }

    public void setDateAttributes(Collection<String> dateAttributes) {
        this.dateAttributes = dateAttributes;
    }

    public Collection<String> getBooleanAttributes() {
        return booleanAttributes;
    }

    public void setBooleanAttributes(Collection<String> booleanAttributes) {
        this.booleanAttributes = booleanAttributes;
    }

    @Override
    public String toString() {
        return "ConversionAttributes [dateAttributes=" + dateAttributes + ", booleanAttributes=" + booleanAttributes
                + "]";
    }

}
