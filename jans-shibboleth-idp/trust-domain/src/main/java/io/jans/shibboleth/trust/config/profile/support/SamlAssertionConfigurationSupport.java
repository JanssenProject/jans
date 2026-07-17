package io.jans.shibboleth.trust.config.profile.support;

import java.time.Duration;
import java.util.Objects;

import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

public class SamlAssertionConfigurationSupport {

    private final AssertionSigningPolicy assertionSigningPolicy;
    private final AssertionTimeCondition assertionTimeCondition;
    private final Duration assertionLifetime;

    private SamlAssertionConfigurationSupport(AssertionSigningPolicy assertionSigningPolicy, AssertionTimeCondition assertionTimeCondition,Duration assertionLifetime) {

        this.assertionSigningPolicy = assertionSigningPolicy;
        this.assertionTimeCondition = assertionTimeCondition;
        this.assertionLifetime = assertionLifetime;
    }

    public static Result<SamlAssertionConfigurationSupport> of(AssertionSigningPolicy assertionSigningPolicy, 
        AssertionTimeCondition assertionTimeCondition, Duration assertionLifetime) {

        return builder()
            .assertionSigningPolicy(assertionSigningPolicy)
            .assertionTimeCondition(assertionTimeCondition)
            .assertionLifetime(assertionLifetime)
            .build();
    }

    public AssertionSigningPolicy getAssertionSigningPolicy() {

        return assertionSigningPolicy;
    }

    public AssertionTimeCondition getAssertionTimeCondition() {

        return assertionTimeCondition;
    }

    public Duration getAssertionLifetime() {

        return assertionLifetime;
    }

    

    @Override
    public boolean equals(Object o) {

        if ( this == o ) return true;

        if ( o == null || getClass() != o.getClass() ) return false; 

        SamlAssertionConfigurationSupport other = (SamlAssertionConfigurationSupport) o; 

        return assertionSigningPolicy == other.assertionSigningPolicy 
            && assertionTimeCondition == other.assertionTimeCondition
            && Objects.equals(assertionLifetime,other.assertionLifetime);
    }

    @Override
    public int hashCode() {

        return Objects.hash(assertionSigningPolicy,assertionTimeCondition,assertionLifetime);
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

            this.assertionSigningPolicy = base != null ? base.assertionSigningPolicy : null;
            this.assertionTimeCondition = base != null ? base.assertionTimeCondition : null;
            this.assertionLifetime  = base != null ? base.assertionLifetime : null; 
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

                return Result.failure(RequiredValueMissing.forField("assertionSigningPolicy"));
            }

            if (assertionTimeCondition == null) {
                
                return Result.failure(RequiredValueMissing.forField("assertionTimeCondition"));
            }

            if (assertionLifetime == null) {

                return Result.failure(RequiredValueMissing.forField("assertionLifetime"));
            }

            return Result.success(new SamlAssertionConfigurationSupport(assertionSigningPolicy, assertionTimeCondition, assertionLifetime));
        }
    }
}