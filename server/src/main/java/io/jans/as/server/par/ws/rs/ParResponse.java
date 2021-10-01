package io.jans.as.server.par.ws.rs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

/**
 * @author Yuriy Zabrovarnyy
 */
@JsonPropertyOrder({"request_uri", "expires_in"})
// ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@IgnoreMediaTypes("application/*+json")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParResponse {

    @JsonProperty(value = "request_uri")
    private String requestUri;
    @JsonProperty(value = "expires_in")
    private Integer expiresIn;

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public String toString() {
        return "ParResponse{" +
                "requestUri='" + requestUri + '\'' +
                ", expiresIn=" + expiresIn +
                '}';
    }
}
