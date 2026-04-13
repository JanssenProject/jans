package io.jans.shibboleth.model.profiles;

import io.jans.shibboleth.model.profiles.config.*;

public abstract class BaseProfileConfiguration {

    protected ProfileStatus status;
    protected InterceptorFlows flows;

    protected BaseProfileConfiguration() {

        status = ProfileStatus.INACTIVE;
        flows  = InterceptorFlows.empty();
    }

    public boolean isActive() {

        return status == ProfileStatus.ACTIVE;
    }

    public abstract ProfileType getType();
}