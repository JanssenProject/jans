package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.common.InterceptorFlows;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;

@SuppressWarnings("unchecked")
interface CommonBuilderSteps<B extends CommonBuilderSteps<B>> {

    CommonConfigurationSupport.Builder commonBuilder();

    default B status(ProfileStatus status) {

        commonBuilder().status(status);
        return (B) this;
    }

    default B inboundFlows(InterceptorFlows flows) {

        commonBuilder().inboundFlows(flows);
        return (B) this;
    }

    default B outboundFlows(InterceptorFlows flows) {

        commonBuilder().outboundFlows(flows);
        return (B) this;
    }
}
