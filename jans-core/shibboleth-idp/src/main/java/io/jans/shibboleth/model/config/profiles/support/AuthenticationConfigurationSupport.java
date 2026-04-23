package io.jans.shibboleth.model.config.profiles.support;

import java.time.Duration;

import io.jans.shibboleth.model.config.profiles.common.*;

public class AuthenticationConfigurationSupport {

    private static final Duration DEFAULT_MAX_AUTHENTICATION_AGE = Duration.ofMinutes(30);

    private final InterceptorFlows postAuthenticationFlows;
    private final AuthenticationResultReusePolicy authenticationResultReusePolicy;
    private final Duration maximumAuthenticationAge;

    private AuthenticationConfigurationSupport(InterceptorFlows postAuthenticationFlows, 
        AuthenticationResultReusePolicy authenticationResultReusePolicy, Duration maximumAuthenticationAge) {
        
        this.postAuthenticationFlows = postAuthenticationFlows != null ? postAuthenticationFlows : InterceptorFlows.empty();
        this.authenticationResultReusePolicy = authenticationResultReusePolicy != null ? authenticationResultReusePolicy : AuthenticationResultReusePolicy.ALLOW_REUSE;
        this.maximumAuthenticationAge = maximumAuthenticationAge != null ? maximumAuthenticationAge : DEFAULT_MAX_AUTHENTICATION_AGE;
    }

    public static AuthenticationConfigurationSupport of(InterceptorFlows postAuthenticationFlows, AuthenticationResultReusePolicy authenticationResultReusePolicy, Duration maximumAuthenticationAge) {
        
        return new AuthenticationConfigurationSupport(postAuthenticationFlows,authenticationResultReusePolicy,maximumAuthenticationAge);
    }

    public static AuthenticationConfigurationSupport of() {

        return new AuthenticationConfigurationSupport(null,null,null);
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
}