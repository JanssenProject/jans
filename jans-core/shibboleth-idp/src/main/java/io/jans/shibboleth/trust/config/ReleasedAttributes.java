package io.jans.shibboleth.trust.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import io.jans.shibboleth.trust.config.ReleasedAttribute;
import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.config.error.DomainObjectCreationFailed;
import io.jans.shibboleth.trust.config.error.TrustError;
import io.jans.shibboleth.trust.shared.Result;

import java.util.Objects;
import java.util.Set;

public class ReleasedAttributes {
    
    private final Set<ReleasedAttribute> attributes;

    private ReleasedAttributes(Set<ReleasedAttribute> attributes) {

        this.attributes = Set.copyOf(attributes);
    }

    public Set<ReleasedAttribute> getAttributes() {

        return attributes;
    }

    public boolean hasAny() {

        return !attributes.isEmpty();
    }

    public boolean hasNone() {

        return attributes.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        
        if (this == o) return true;
    
        if (o == null || getClass() != o.getClass()) return false;
        
        ReleasedAttributes other = (ReleasedAttributes) o;
    
        return Objects.equals(attributes,other.attributes);
    }
    
    @Override
    public int hashCode() {
    
        return Objects.hash(attributes);
    }
    
    public static Builder from(ReleasedAttributes existing) {

        return new Builder(existing);
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static ReleasedAttributes empty() {

        return new ReleasedAttributes(Set.of());
    }

    public static class Builder {

        private final Set<ReleasedAttribute> attributes = new LinkedHashSet<>();
        private TrustError error;

        private Builder() { }

        private Builder (ReleasedAttributes existing) {

            if (existing != null) {

                this.attributes.addAll(existing.attributes);
            }
        }

        public Builder add(ReleasedAttribute attribute) {

            if (error != null) {

                return this; //already failed. Ignore further calls
            }

            if (attribute == null) {

                error = CannotBeNullOrBlank.forField("attribute");
                return this;
            }

            attributes.add(attribute);
            return this;
        }

        public Builder addAll(Collection<ReleasedAttribute> attributes) {

            if (error != null) {

                return this;
            }

            for(ReleasedAttribute attr : attributes) {

                if (attr == null) {
                    error = CannotBeNullOrBlank.forField("attribute");
                    return this;
                }
                this.attributes.add(attr);
            }

            return this;

        }

        public Result<ReleasedAttributes> build() {

            if (error != null) {

                return Result.failure(DomainObjectCreationFailed.forClassWithCause(ReleasedAttributes.class, error));
            }

            return Result.success(new ReleasedAttributes(attributes));
        }
    }
}
