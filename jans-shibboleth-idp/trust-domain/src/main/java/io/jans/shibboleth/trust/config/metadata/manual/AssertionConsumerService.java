package io.jans.shibboleth.trust.config.metadata.manual;

import java.net.URI;
import java.util.Objects;

import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

public class AssertionConsumerService {
    
    private final URI location;
    private final SamlBinding binding;
    private final int index;
    private final boolean isDefault;

    private AssertionConsumerService(URI location, SamlBinding binding, int index, boolean isDefault) {

        this.location = location;
        this.binding = binding;
        this.index = index;
        this.isDefault = isDefault;
    }

    public static final Result<AssertionConsumerService> of (URI location, SamlBinding binding, int index, boolean isDefault) {

        if (location == null) {

            return Result.failure(RequiredValueMissing.forField("location"));
        }

        if (binding == null ) {

            return Result.failure(RequiredValueMissing.forField("binding"));
        }

        return Result.success(new AssertionConsumerService(location, binding,index,isDefault));
    }

    public static final Result<AssertionConsumerService> of(URI location, SamlBinding binding) {

        return of(location,binding,1,true);
    }

    public URI getLocation() {

        return location;
    }

    public SamlBinding getBinding() {

        return binding;
    }

    public int getIndex() {

        return index;
    }

    public boolean isDefault() {

        return isDefault;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (o == null || getClass() != o.getClass() ) return false;

        AssertionConsumerService that = (AssertionConsumerService) o;

        return Objects.equals(location,that.location)
            && Objects.equals(binding,that.binding)
            && Objects.equals(index,that.index)
            && Objects.equals(isDefault,that.isDefault);
    }

    @Override
    public int hashCode() {

        return Objects.hash(location,binding,index,isDefault);
    }
}
