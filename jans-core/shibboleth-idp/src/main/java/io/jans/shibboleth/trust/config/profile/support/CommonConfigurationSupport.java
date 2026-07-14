package io.jans.shibboleth.trust.config.profile.support;

import java.util.Objects;

import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.shared.Result;

public class CommonConfigurationSupport {

    private final ProfileStatus status;
    private final InterceptorFlows inboundFlows;
    private final InterceptorFlows outboundFlows;

    private CommonConfigurationSupport(ProfileStatus status,InterceptorFlows inboundFlows, InterceptorFlows outboundFlows) {

        this.status = status;
        this.inboundFlows = inboundFlows;
        this.outboundFlows = outboundFlows;
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

    @Override
    public boolean equals(Object o) {

        if ( this == o ) return true;

        if ( o == null || getClass() != o.getClass() ) return false;

        CommonConfigurationSupport other = (CommonConfigurationSupport) o;

        return status == other.status 
            && Objects.equals(inboundFlows,other.inboundFlows)
            &&  Objects.equals(outboundFlows,other.outboundFlows);
    }
    
    @Override
    public int hashCode() {

        return Objects.hash(status,inboundFlows,outboundFlows);
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(CommonConfigurationSupport base) {

        return new Builder(base);
    }
    
    public static class Builder {

        private ProfileStatus status;
        private InterceptorFlows inboundFlows;
        private InterceptorFlows outboundFlows;

        public Builder(CommonConfigurationSupport base) {
            
            status = base != null ? base.status : null;
            inboundFlows = base != null ? base.inboundFlows : null;
            outboundFlows = base != null ? base.outboundFlows : null;
        }

        public Builder status(ProfileStatus status) {

            this.status = status;
            return this;
        }

        public Builder inboundFlows(InterceptorFlows inboundFlows) {

            this.inboundFlows = inboundFlows;
            return this;
        }

        public Builder outboundFlows(InterceptorFlows outboundFlows) {

            this.outboundFlows = outboundFlows;
            return this;
        }

        public Result<CommonConfigurationSupport> build() {

            if (status == null) {

                return Result.failure(CannotBeNullOrBlank.forField("status"));
            }

            if (inboundFlows == null) {

                return Result.failure(CannotBeNullOrBlank.forField("inboundFlows"));
            }

            if (outboundFlows == null) {

                return Result.failure(CannotBeNullOrBlank.forField("outboundFlows"));
            }

            return Result.success(new CommonConfigurationSupport(status, inboundFlows, outboundFlows));
        }
    }
}