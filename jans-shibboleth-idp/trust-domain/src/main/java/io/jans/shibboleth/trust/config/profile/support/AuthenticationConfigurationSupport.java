package io.jans.shibboleth.trust.config.profile.support;

import java.time.Duration;
import java.util.Objects;

import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

public record AuthenticationConfigurationSupport(
    InterceptorFlows postAuthenticationFlows,
    AuthenticationResultReusePolicy authenticationResultReusePolicy,
    Duration maximumAuthenticationAge) {

    public AuthenticationConfigurationSupport {

        Objects.requireNonNull(postAuthenticationFlows, "postAuthenticationFlows");
        Objects.requireNonNull(authenticationResultReusePolicy, "authenticationResultReusePolicy");
        Objects.requireNonNull(maximumAuthenticationAge, "maximumAuthenticationAge");
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(AuthenticationConfigurationSupport base) {

        return new Builder(base);
    }

    public static class Builder {

        private InterceptorFlows postAuthenticationFlows;
        private AuthenticationResultReusePolicy authenticationResultReusePolicy;
        private Duration maximumAuthenticationAge;

        public Builder(AuthenticationConfigurationSupport base) {

            postAuthenticationFlows = base != null ? base.postAuthenticationFlows() : null;
            authenticationResultReusePolicy = base != null ? base.authenticationResultReusePolicy() : null;
            maximumAuthenticationAge = base != null ? base.maximumAuthenticationAge() : null;
        }

        public Builder postAuthenticationFlows(InterceptorFlows postAuthenticationFlows) {

            this.postAuthenticationFlows = postAuthenticationFlows;
            return this;
        }

        public Builder authenticationResultReusePolicy(AuthenticationResultReusePolicy authenticationResultReusePolicy) {

            this.authenticationResultReusePolicy = authenticationResultReusePolicy;
            return this;
        }

        public Builder maximumAuthenticationAge(Duration maximumAuthenticationAge) {

            this.maximumAuthenticationAge = maximumAuthenticationAge;
            return this;
        }

        public Result<AuthenticationConfigurationSupport> build() {

            if (postAuthenticationFlows == null) {

                return Result.failure(
                    RequiredValueMissing.forField(AuthenticationConfigurationSupport.class, "postAuthenticationFlows"));
            }

            if (authenticationResultReusePolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(
                        AuthenticationConfigurationSupport.class, "authenticationResultReusePolicy"));
            }

            if (maximumAuthenticationAge == null) {

                return Result.failure(
                    RequiredValueMissing.forField(
                        AuthenticationConfigurationSupport.class, "maximumAuthenticationAge"));
            }

            AuthenticationConfigurationSupport ret = new AuthenticationConfigurationSupport(postAuthenticationFlows, 
                authenticationResultReusePolicy, maximumAuthenticationAge);
            
            return Result.success(ret);
        }
    }
    
    
}