package io.jans.shibboleth.trust.config.profile.capabilities;

import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;

/**
 * The settings every profile carries. Implementors supply the support that holds them; the
 * individual readings are derived from it, so no profile restates the same delegation.
 */
public interface CommonConfigurationCapable {

    CommonConfigurationSupport commonConfigurationSupport();

    ProfileType getType();

    default ProfileStatus getStatus() {

        return commonConfigurationSupport().status();
    }

    default InterceptorFlows getInboundFlows() {

        return commonConfigurationSupport().inboundFlows();
    }

    default InterceptorFlows getOutboundFlows() {

        return commonConfigurationSupport().outboundFlows();
    }
}
