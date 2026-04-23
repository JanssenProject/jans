package io.jans.shibboleth.model.config.profiles.capabilities;

import java.util.List;
import java.time.Duration;

import io.jans.shibboleth.model.config.profiles.common.*;


public interface AuthenticationConfigurationCapable {

    public InterceptorFlows getPostAuthenticationFlows();
    public Duration getMaxAuthenticationAge();
    public AuthenticationResultReusePolicy getAuthenticationResultReusePolicy();
}