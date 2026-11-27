package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.config.profile.common.InterceptorFlows;
import io.jans.shibboleth.trust.config.profile.support.AuthenticationConfigurationSupport;

import java.time.Duration;

@SuppressWarnings("unchecked")
interface AuthenticationBuilderSteps<B extends AuthenticationBuilderSteps<B>> {

    AuthenticationConfigurationSupport.Builder authenticationBuilder();

    default B postAuthenticationFlows(InterceptorFlows flows) {

        authenticationBuilder().postAuthenticationFlows(flows);
        return (B) this;
    }

    default B authenticationResultReusePolicy(AuthenticationResultReusePolicy policy) {

        authenticationBuilder().authenticationResultReusePolicy(policy);
        return (B) this;
    }

    default B maximumAuthenticationAge(Duration age) {

        authenticationBuilder().maximumAuthenticationAge(age);
        return (B) this;
    }
}
