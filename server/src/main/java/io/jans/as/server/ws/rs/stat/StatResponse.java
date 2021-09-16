package io.jans.as.server.ws.rs.stat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.util.HashMap;
import java.util.Map;

@IgnoreMediaTypes("application/*+json")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatResponse {

    @JsonProperty(value = "response") // month to stat item
    private Map<String, StatResponseItem> response = new HashMap<>();

    public Map<String, StatResponseItem> getResponse() {
        if (response == null) response = new HashMap<>();
        return response;
    }

    public void setResponse(Map<String, StatResponseItem> response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "StatResponse{" +
                "response=" + response +
                '}';
    }
}