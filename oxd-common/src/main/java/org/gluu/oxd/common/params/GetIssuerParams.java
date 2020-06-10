package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetIssuerParams implements IParams {

    @JsonProperty(value = "resource")
    private String resource;

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    @Override
    public String toString() {
        return "WebfingerParams{" +
                "resource='" + resource + '\'' +
                '}';
    }

}
