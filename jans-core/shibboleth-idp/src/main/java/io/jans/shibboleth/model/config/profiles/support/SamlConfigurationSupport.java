package io.jans.shibboleth.model.config.profiles.support;

import java.util.Objects;

import io.jans.shibboleth.model.config.profiles.common.MessageSigningPolicy;
import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.util.TrustResult;

public class SamlConfigurationSupport {

    private final MessageSigningPolicy messageSigningPolicy;

    private SamlConfigurationSupport(MessageSigningPolicy messageSigningPolicy) {

        this.messageSigningPolicy = messageSigningPolicy;
    }

    public static TrustResult<SamlConfigurationSupport> of(MessageSigningPolicy messageSigningPolicy) {

        return builder().messageSigningPolicy(messageSigningPolicy).build();
    }

    public MessageSigningPolicy getMessageSigningPolicy() {

        return messageSigningPolicy;
    }

    @Override
    public boolean equals(Object o) {

        if ( this == o ) return true;

        if ( o == null || getClass() != o.getClass() ) return false; 

        SamlConfigurationSupport other = (SamlConfigurationSupport) o;

        return messageSigningPolicy == other.messageSigningPolicy; 
    }

    @Override
    public int hashCode() {

        return Objects.hash(messageSigningPolicy);
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(SamlConfigurationSupport base) {

        return new Builder(base);
    }

    public static class Builder {

        private MessageSigningPolicy messageSigningPolicy;

        Builder(SamlConfigurationSupport base) {

            messageSigningPolicy = base != null ? base.messageSigningPolicy : null;
        }

        public Builder messageSigningPolicy(MessageSigningPolicy policy) {

            messageSigningPolicy = policy;
            return this;
        }

        public TrustResult<SamlConfigurationSupport> build() {

            if (messageSigningPolicy == null) {

                return TrustResult.failure(new CannotBeNullOrBlank("messageSigningPolicy"));
            }

            return TrustResult.success(new SamlConfigurationSupport(messageSigningPolicy));
        }
    }
}