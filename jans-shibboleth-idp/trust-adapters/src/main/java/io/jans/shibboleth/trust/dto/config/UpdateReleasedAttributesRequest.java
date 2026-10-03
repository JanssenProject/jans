package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Request body for setting the attributes a trust relationship releases
 * ({@code PUT /v1/trust/config/trust-relationships/{id}/released-attributes}).
 *
 * <p>Full replacement: {@code attributes} is the complete set the trust relationship should release
 * afterwards. An empty array clears all of them. A dumb data holder — unknown properties are rejected.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateReleasedAttributesRequest {

    @JsonProperty("attributes")
    private List<ReleasedAttributeDto> attributes;

    public UpdateReleasedAttributesRequest() {
    }

    public UpdateReleasedAttributesRequest(List<ReleasedAttributeDto> attributes) {

        this.attributes = attributes;
    }

    public List<ReleasedAttributeDto> getAttributes() {

        return attributes;
    }

    public void setAttributes(List<ReleasedAttributeDto> attributes) {

        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return Objects.equals(attributes, ((UpdateReleasedAttributesRequest) o).attributes);
    }

    @Override
    public int hashCode() {

        return Objects.hash(attributes);
    }

    @Override
    public String toString() {

        return "UpdateReleasedAttributesRequest{attributes=" + attributes + '}';
    }
}
