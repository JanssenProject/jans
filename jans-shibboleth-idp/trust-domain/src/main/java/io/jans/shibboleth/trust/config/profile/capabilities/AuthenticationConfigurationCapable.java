package io.jans.shibboleth.trust.config.profile.capabilities;

import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.AuthenticationConfigurationSupport;

import java.time.Duration;

public interface AuthenticationConfigurationCapable {

    AuthenticationConfigurationSupport authenticationConfigurationSupport();

    default InterceptorFlows getPostAuthenticationFlows() {

        return authenticationConfigurationSupport().postAuthenticationFlows();
    }

    default Duration getMaxAuthenticationAge() {

        return authenticationConfigurationSupport().maximumAuthenticationAge();
    }

    /**
     * Also declared by {@link Saml2SsoConfigurationCapable}. A profile implementing both must say
     * which support answers it — see {@code Saml2SsoProfileConfiguration}.
     */
    default AuthenticationResultReusePolicy getAuthenticationResultReusePolicy() {

        return authenticationConfigurationSupport().authenticationResultReusePolicy();
    }
}
