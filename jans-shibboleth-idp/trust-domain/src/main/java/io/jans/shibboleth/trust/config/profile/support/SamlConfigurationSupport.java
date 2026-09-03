package io.jans.shibboleth.trust.config.profile.support;

import java.util.Objects;

import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

public record SamlConfigurationSupport(
    MessageSigningPolicy messageSigningPolicy) {

    public SamlConfigurationSupport {

        Objects.requireNonNull(messageSigningPolicy, "messageSigningPolicy");
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

            messageSigningPolicy = base != null ? base.messageSigningPolicy() : null;
        }

        public Builder messageSigningPolicy(MessageSigningPolicy policy) {

            messageSigningPolicy = policy;
            return this;
        }

        public Result<SamlConfigurationSupport> build() {

            if (messageSigningPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(SamlConfigurationSupport.class, "messageSigningPolicy"));
            }

            return Result.success(new SamlConfigurationSupport(messageSigningPolicy));
        }
    }
}