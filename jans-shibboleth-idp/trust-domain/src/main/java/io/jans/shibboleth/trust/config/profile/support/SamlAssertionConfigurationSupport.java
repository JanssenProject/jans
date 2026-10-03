package io.jans.shibboleth.trust.config.profile.support;

import java.time.Duration;
import java.util.Objects;

import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

public record SamlAssertionConfigurationSupport(
    AssertionSigningPolicy assertionSigningPolicy,
    AssertionTimeCondition assertionTimeCondition,
    Duration assertionLifetime) {

    public SamlAssertionConfigurationSupport {

        Objects.requireNonNull(assertionSigningPolicy, "assertionSigningPolicy");
        Objects.requireNonNull(assertionTimeCondition, "assertionTimeCondition");
        Objects.requireNonNull(assertionLifetime, "assertionLifetime");
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(SamlAssertionConfigurationSupport base) {

        return new Builder(base);
    }

    public static class Builder {

        private AssertionSigningPolicy assertionSigningPolicy;
        private AssertionTimeCondition assertionTimeCondition;
        private Duration assertionLifetime;

        public Builder(SamlAssertionConfigurationSupport base) {

            this.assertionSigningPolicy = base != null ? base.assertionSigningPolicy() : null;
            this.assertionTimeCondition = base != null ? base.assertionTimeCondition() : null;
            this.assertionLifetime  = base != null ? base.assertionLifetime() : null; 
        }

        public Builder assertionSigningPolicy(AssertionSigningPolicy policy) {

            this.assertionSigningPolicy = policy;
            return this;
        }

        public Builder assertionTimeCondition(AssertionTimeCondition timecondition) {

            this.assertionTimeCondition = timecondition;
            return this;
        }

        public Builder assertionLifetime(Duration lifetime) {

            this.assertionLifetime = lifetime;
            return this;
        }

        public Result<SamlAssertionConfigurationSupport> build() {

            if (assertionSigningPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(SamlAssertionConfigurationSupport.class, "assertionSigningPolicy"));
            }

            if (assertionTimeCondition == null) {
                
                return Result.failure(
                    RequiredValueMissing.forField(SamlAssertionConfigurationSupport.class, "assertionTimeCondition"));
            }

            if (assertionLifetime == null) {

                return Result.failure(
                    RequiredValueMissing.forField(SamlAssertionConfigurationSupport.class, "assertionLifetime"));
            }

            return Result.success(new SamlAssertionConfigurationSupport(assertionSigningPolicy, assertionTimeCondition, assertionLifetime));
        }
    }
}