package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiEndpointMgt {

    @Schema(description = "Name of api endpoint.")
    private String name;

    @Schema(description = "List of mandatory attribute.")
    private List<String> mandatoryAttributes;

    @Schema(description = "Attribute that should not be returned in response.")
    private List<String> exclusionAttributes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMandatoryAttributes() {
        return mandatoryAttributes;
    }

    public void setMandatoryAttributes(List<String> mandatoryAttributes) {
        this.mandatoryAttributes = mandatoryAttributes;
    }

    public List<String> getExclusionAttributes() {
        return exclusionAttributes;
    }

    public void setExclusionAttributes(List<String> exclusionAttributes) {
        this.exclusionAttributes = exclusionAttributes;
    }

    @Override
    public String toString() {
        return "ApiEndpointMgt [name=" + name + ", mandatoryAttributes=" + mandatoryAttributes
                + ", exclusionAttributes=" + exclusionAttributes + "]";
    }

}
