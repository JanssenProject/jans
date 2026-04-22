package io.jans.shibboleth.model.config.profiles.capabilities;

import io.jans.shibboleth.model.config.profiles.common.*;

public interface CommonConfigurationCapable {

    public ProfileType getType();
    public ProfileStatus getStatus();
    public InterceptorFlows getInboundFlows();
    public InterceptorFlows getOutboundFlows();
}