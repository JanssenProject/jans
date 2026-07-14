package io.jans.shibboleth.trust.config.profile.capabilities;

import java.util.List;
import java.time.Duration;

import io.jans.shibboleth.trust.config.profile.common.*;


public interface AuthenticationConfigurationCapable {

    public InterceptorFlows getPostAuthenticationFlows();
    public Duration getMaxAuthenticationAge();
    public AuthenticationResultReusePolicy getAuthenticationResultReusePolicy();
}