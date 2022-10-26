package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AgamaConfiguration {

    /**
     * List of attributes required to create the Agama Flow
     */
    private List<String> mandatoryAttributes;

    /**
     * List of attributes that are optional
     */
    private List<String> optionalAttributes;

    public List<String> getMandatoryAttributes() {
        return mandatoryAttributes;
    }

    public void setMandatoryAttributes(List<String> mandatoryAttributes) {
        this.mandatoryAttributes = mandatoryAttributes;
    }

    public List<String> getOptionalAttributes() {
        return optionalAttributes;
    }

    public void setOptionalAttributes(List<String> optionalAttributes) {
        this.optionalAttributes = optionalAttributes;
    }

    @Override
    public String toString() {
        return "AgamaConfiguration [" + " mandatoryAttributes=" + mandatoryAttributes + ", optionalAttributes="
                + optionalAttributes + "]";
    }

}
