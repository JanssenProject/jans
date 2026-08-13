package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.config.metadata.manual.SamlBinding;

import java.util.Objects;

/**
 * The endpoint where a service provider receives assertions, for a manually-described entity.
 * {@code index} and {@code is_default} are optional (default 1 and true).
 */
public class AssertionConsumerServiceRequest {

    @JsonProperty("location")
    private String location;

    @JsonProperty("binding")
    private SamlBinding binding;

    @JsonProperty("index")
    private Integer index;

    @JsonProperty("is_default")
    private Boolean isDefault;

    public AssertionConsumerServiceRequest() {
    }

    public String getLocation() {

        return location;
    }

    public void setLocation(String location) {

        this.location = location;
    }

    public SamlBinding getBinding() {

        return binding;
    }

    public void setBinding(SamlBinding binding) {

        this.binding = binding;
    }

    public Integer getIndex() {

        return index;
    }

    public void setIndex(Integer index) {

        this.index = index;
    }

    public Boolean getIsDefault() {

        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {

        this.isDefault = isDefault;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssertionConsumerServiceRequest that = (AssertionConsumerServiceRequest) o;
        return Objects.equals(location, that.location)
            && binding == that.binding
            && Objects.equals(index, that.index)
            && Objects.equals(isDefault, that.isDefault);
    }

    @Override
    public int hashCode() {

        return Objects.hash(location, binding, index, isDefault);
    }

    @Override
    public String toString() {

        return "AssertionConsumerServiceRequest{location='" + location + "', binding=" + binding
            + ", index=" + index + ", isDefault=" + isDefault + '}';
    }
}
