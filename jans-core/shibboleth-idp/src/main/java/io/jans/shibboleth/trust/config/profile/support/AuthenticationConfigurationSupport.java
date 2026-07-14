package io.jans.shibboleth.trust.config.profile.support;

import java.time.Duration;
import java.util.Objects;

import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.config.util.TrustResult;

public class AuthenticationConfigurationSupport {

    private final InterceptorFlows postAuthenticationFlows;
    private final AuthenticationResultReusePolicy authenticationResultReusePolicy;
    private final Duration maximumAuthenticationAge;

    private AuthenticationConfigurationSupport(InterceptorFlows postAuthenticationFlows, 
        AuthenticationResultReusePolicy authenticationResultReusePolicy, Duration maximumAuthenticationAge) {
        
        this.postAuthenticationFlows = postAuthenticationFlows;
        this.authenticationResultReusePolicy = authenticationResultReusePolicy;
        this.maximumAuthenticationAge = maximumAuthenticationAge;
    }

    public static TrustResult<AuthenticationConfigurationSupport> of(InterceptorFlows postAuthenticationFlows,
        AuthenticationResultReusePolicy authenticationResultReusePolicy, Duration maximumAuthenticationAge ) {
        
        return builder()
            .postAuthenticationFlows(postAuthenticationFlows)
            .authenticationResultReusePolicy(authenticationResultReusePolicy)
            .maximumAuthenticationAge(maximumAuthenticationAge)
            .build();
    }
    
    public InterceptorFlows getPostAuthenticationFlows() {

        return postAuthenticationFlows;
    }

    public AuthenticationResultReusePolicy getAuthenticationResultReusePolicy() {

        return authenticationResultReusePolicy;
    }

    public Duration getMaximumAuthenticationAge() {

        return maximumAuthenticationAge;
    }

    public boolean equals (Object o) {

        if ( this == o ) return true;

        if ( o == null || getClass() != o.getClass() ) return false;

        AuthenticationConfigurationSupport other = (AuthenticationConfigurationSupport) o;
        return Objects.equals(postAuthenticationFlows,other.postAuthenticationFlows)
            && authenticationResultReusePolicy == other.authenticationResultReusePolicy
            && Objects.equals(maximumAuthenticationAge,other.maximumAuthenticationAge);
    }

    public int hashCode() {

        return Objects.hash(postAuthenticationFlows,authenticationResultReusePolicy,maximumAuthenticationAge);
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

            postAuthenticationFlows = base != null ? base.postAuthenticationFlows : null;
            authenticationResultReusePolicy = base != null ? base.authenticationResultReusePolicy : null;
            maximumAuthenticationAge = base != null ? base.maximumAuthenticationAge : null;
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

        public TrustResult<AuthenticationConfigurationSupport> build() {

            if (postAuthenticationFlows == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("postAuthenticationFlows"));
            }

            if (authenticationResultReusePolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("authenticationResultReusePolicy"));
            }

            if (maximumAuthenticationAge == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("maximumAuthenticationAge"));
            }

            AuthenticationConfigurationSupport ret = new AuthenticationConfigurationSupport(postAuthenticationFlows, 
                authenticationResultReusePolicy, maximumAuthenticationAge);
            
            return TrustResult.success(ret);
        }
    }
    
    
}