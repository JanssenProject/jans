package io.jans.shibboleth.trust.config.profile.capabilities;

import io.jans.shibboleth.trust.config.profile.common.*;

public interface CommonConfigurationCapable {

    public ProfileType getType();
    public ProfileStatus getStatus();
    public InterceptorFlows getInboundFlows();
    public InterceptorFlows getOutboundFlows();
}