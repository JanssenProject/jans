package io.jans.shibboleth.trust.config.profile.support;

import java.util.Objects;

import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

public record CommonConfigurationSupport(
    ProfileStatus status,
    InterceptorFlows inboundFlows,
    InterceptorFlows outboundFlows) {

    public CommonConfigurationSupport {

        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(inboundFlows, "inboundFlows");
        Objects.requireNonNull(outboundFlows, "outboundFlows");
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
            
            status = base != null ? base.status() : null;
            inboundFlows = base != null ? base.inboundFlows() : null;
            outboundFlows = base != null ? base.outboundFlows() : null;
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

                return Result.failure(RequiredValueMissing.forField(CommonConfigurationSupport.class, "status"));
            }

            if (inboundFlows == null) {

                return Result.failure(RequiredValueMissing.forField(CommonConfigurationSupport.class, "inboundFlows"));
            }

            if (outboundFlows == null) {

                return Result.failure(RequiredValueMissing.forField(CommonConfigurationSupport.class, "outboundFlows"));
            }

            return Result.success(new CommonConfigurationSupport(status, inboundFlows, outboundFlows));
        }
    }
}