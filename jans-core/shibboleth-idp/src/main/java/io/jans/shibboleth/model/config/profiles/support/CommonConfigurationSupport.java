package io.jans.shibboleth.model.config.profiles.support;

import io.jans.shibboleth.model.config.profiles.common.*;

public class CommonConfigurationSupport {

    private static final ProfileStatus DEFAULT_PROFILE_STATUS = ProfileStatus.INACTIVE;

    private final ProfileStatus status;
    private final InterceptorFlows inboundFlows;
    private final InterceptorFlows outboundFlows;

    private CommonConfigurationSupport(ProfileStatus status,InterceptorFlows inboundFlows, InterceptorFlows outboundFlows) {

        this.status = status != null ? status : DEFAULT_PROFILE_STATUS;
        this.inboundFlows = inboundFlows != null ? inboundFlows : InterceptorFlows.empty();
        this.outboundFlows = outboundFlows != null ? outboundFlows : InterceptorFlows.empty(); 
    }

    public static CommonConfigurationSupport of(ProfileStatus status,InterceptorFlows inboundFlows, InterceptorFlows outboundFlows) {
        return new CommonConfigurationSupport(status,inboundFlows,outboundFlows);
    }
    
    public static CommonConfigurationSupport of() {

        return new CommonConfigurationSupport(null,null,null);
    }
    
    public ProfileStatus getStatus() {

        return status;
    }

    public InterceptorFlows getInboundFlows() {

        return inboundFlows;
    }

    public InterceptorFlows getOutboundFlows() {

        return outboundFlows;
    }
    
}