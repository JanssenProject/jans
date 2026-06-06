package io.jans.shibboleth.model.metadata.manual;

import java.net.URI;
import java.util.Objects;

import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.util.TrustResult;

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

    public static final TrustResult<AssertionConsumerService> of (URI location, SamlBinding binding, int index, boolean isDefault) {

        if (location == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("location"));
        }

        if (binding == null ) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("binding"));
        }

        return TrustResult.success(new AssertionConsumerService(location, binding,index,isDefault));
    }

    public static final TrustResult<AssertionConsumerService> of(URI location, SamlBinding binding) {

        return of(location,binding,1,true);
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
