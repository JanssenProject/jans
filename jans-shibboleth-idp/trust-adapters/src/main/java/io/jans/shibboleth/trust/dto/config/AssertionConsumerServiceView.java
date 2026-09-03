package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.config.metadata.manual.SamlBinding;

import java.util.Objects;

/**
 * Read view of a manually-described entity's assertion consumer service.
 */
public class AssertionConsumerServiceView {

    @JsonProperty("location")
    private final String location;

    @JsonProperty("binding")
    private final SamlBinding binding;

    @JsonProperty("index")
    private final int index;

    @JsonProperty("is_default")
    private final boolean isDefault;

    public AssertionConsumerServiceView(String location, SamlBinding binding, int index, boolean isDefault) {

        this.location = location;
        this.binding = binding;
        this.index = index;
        this.isDefault = isDefault;
    }

    public String getLocation() {

        return location;
    }

    public SamlBinding getBinding() {

        return binding;
    }

    public int getIndex() {

        return index;
    }

    public boolean getIsDefault() {

        return isDefault;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssertionConsumerServiceView that = (AssertionConsumerServiceView) o;
        return index == that.index
            && isDefault == that.isDefault
            && Objects.equals(location, that.location)
            && binding == that.binding;
    }

    @Override
    public int hashCode() {

        return Objects.hash(location, binding, index, isDefault);
    }

    @Override
    public String toString() {

        return "AssertionConsumerServiceView{location='" + location + "', binding=" + binding
            + ", index=" + index + ", isDefault=" + isDefault + '}';
    }
}
