package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AgamaConfiguration implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Schema(description = "List of allowed domains to download Agama Project.")
    private List<String> allowedDomain;
    
    @Schema(description = "List of attributes required to create the Agama Flow.")
    private List<String> mandatoryAttributes;

    @Schema(description = "List of attributes that are optional.")
    private List<String> optionalAttributes;
       
    public List<String> getAllowedDomain() {
        return allowedDomain;
    }

    public void setAllowedDomain(List<String> allowedDomain) {
        this.allowedDomain = allowedDomain;
    }

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
        return "AgamaConfiguration [" + " allowedDomain=" + allowedDomain + " mandatoryAttributes=" + mandatoryAttributes + ", optionalAttributes="
                + optionalAttributes + "]";
    }

}
