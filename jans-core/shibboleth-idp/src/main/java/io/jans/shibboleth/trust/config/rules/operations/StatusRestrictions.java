package io.jans.shibboleth.trust.config.rules.operations;

import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.config.error.OperationForbiddenFromStatus;
import io.jans.shibboleth.trust.config.error.TrustError;
import io.jans.shibboleth.trust.config.util.BuildContext;
import io.jans.shibboleth.trust.shared.Result;

import java.util.EnumSet;
import java.util.Set;

public class StatusRestrictions {

    private StatusRestrictions() { }

    public static class IncorporateDiscoveredEntityIdsRestriction {

        public static Result<Void> check(BuildContext context) {

            if(context.isAggregateNature() && context.incorporateDiscoveredEntityIdsCalled() && !context.isActivatingStatus()) {

                TrustError error = OperationForbiddenFromStatus.of("incorporateDiscoveredEntityIds",context.getStatus());
                return Result.failure(error);
            }
            return Result.success(null);
        }
    }

    public static class UpdateMetadataSourceRestriction {

        public static Result<Void> check(BuildContext context) {

            if(context.getMetadataSource() != null && context.isActivatingStatus() && context.updateMetadataSourceCalled()) {

                TrustError error  = OperationForbiddenFromStatus.of("updateMetadataSource",context.getStatus());
                return Result.failure(error);
            }

            return Result.success(null);
        }
    }

    public static class UpdateProfileConfigurationRestriction {

        public static Result<Void> check(BuildContext context) {

            if (context.updateProfileConfigurationCalled() && context.isActivatingStatus()) {

                TrustError error = OperationForbiddenFromStatus.of("updateXXXProfileConfiguration",context.getStatus());
                return Result.failure(error);
            }
            
            return Result.success(null);
        }
    }

    public static class UpdateReleasedAttributesRestriction {

        public static Result<Void> check(BuildContext context) {

            if (context.updateReleasedAttributesCalled() && context.isActivatingStatus()) {

                TrustError error = OperationForbiddenFromStatus.of("updateReleasedAttributes",context.getStatus());
                return Result.failure(error);
            }

            return Result.success(null);
        }
    }

    public static class FinalizeActivationRestriction {

        public static Result<Void> check(BuildContext context) {

            if (context.finalizeActivationCalled() && !context.isActivatingStatus()) {

                TrustError error = OperationForbiddenFromStatus.of("finalizeActivation",context.getStatus());
                return Result.failure(error);
            }

            return Result.success(null);
        }
    }

    public static class CancelActivationRestriction {

        public static Result<Void> check(BuildContext context) {

            if (context.cancelActivationCalled() && !context.isActivatingStatus()) {

                TrustError error = OperationForbiddenFromStatus.of("cancelActivation",context.getStatus());
                return Result.failure(error);
            }

            return Result.success(null);
        }
    }

    public static class ActivateRestriction {

        private static final Set<TrustStatus> ALLOWED_STATUSES = EnumSet.of(TrustStatus.READY, TrustStatus.INACTIVE);

        public static Result<Void> check(BuildContext context) {

            if (context.activateCalled() && !ALLOWED_STATUSES.contains(context.getStatus())) {

                TrustError error = OperationForbiddenFromStatus.of("activate",context.getStatus());
                return Result.failure(error);
            }

            return Result.success(null);
        }
    }

    public static class DeactivateRestriction {

        private static final Set<TrustStatus> ALLOWED_STATUSES = EnumSet.of(TrustStatus.ACTIVE);

        public static Result<Void> check(BuildContext context) {

            if (context.deactivateCalled() && !ALLOWED_STATUSES.contains(context.getStatus())) {

                TrustError error = OperationForbiddenFromStatus.of("deactivate",context.getStatus());
                return Result.failure(error);
            }

            return Result.success(null);
        }
    }

}
