package io.jans.shibboleth.model.rules.operations;

import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.core.TrustStatus;
import io.jans.shibboleth.model.error.DomainObjectConsistencyFailed;
import io.jans.shibboleth.model.error.OperationForbiddenFromStatus;
import io.jans.shibboleth.model.error.TrustError;
import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.TrustResult;

import java.util.EnumSet;
import java.util.Set;

public class StatusRestrictions {

    private StatusRestrictions() { }

    public static class IncorporateDiscoveredEntityIdsRestriction {

        public static TrustResult<Void> check(BuildContext context) {

            if(context.isAggregateNature() && context.incorporateDiscoveredEntityIdsCalled() && !context.isActivatingStatus()) {

                TrustError error = OperationForbiddenFromStatus.of("incorporateDiscoveredEntityIds",context.getStatus());
                return TrustResult.failure(error);
            }
            return TrustResult.success(null);
        }
    }

    public static class UpdateMetadataSourceRestriction {

        public static TrustResult<Void> check(BuildContext context) {

            if(context.getMetadataSource() != null && context.isActivatingStatus() && context.updateMetadataSourceCalled()) {

                TrustError error  = OperationForbiddenFromStatus.of("updateMetadataSource",context.getStatus());
                return TrustResult.failure(error);
            }

            return TrustResult.success(null);
        }
    }

    public static class UpdateProfileConfigurationRestriction {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.updateProfileConfigurationCalled() && context.isActivatingStatus()) {

                TrustError error = OperationForbiddenFromStatus.of("updateXXXProfileConfiguration",context.getStatus());
                return TrustResult.failure(error);
            }
            
            return TrustResult.success(null);
        }
    }

    public static class UpdateReleasedAttributesRestriction {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.updateReleasedAttributesCalled() && context.isActivatingStatus()) {

                TrustError error = OperationForbiddenFromStatus.of("updateReleasedAttributes",context.getStatus());
                return TrustResult.failure(error);
            }

            return TrustResult.success(null);
        }
    }

    public static class FinalizeActivationRestriction {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.finalizeActivationCalled() && !context.isActivatingStatus()) {

                TrustError error = OperationForbiddenFromStatus.of("finalizeActivation",context.getStatus());
                return TrustResult.failure(error);
            }

            return TrustResult.success(null);
        }
    }

    public static class CancelActivationRestriction {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.cancelActivationCalled() && !context.isActivatingStatus()) {

                TrustError error = OperationForbiddenFromStatus.of("cancelActivation",context.getStatus());
                return TrustResult.failure(error);
            }

            return TrustResult.success(null);
        }
    }

    public static class ActivateRestriction {

        private static final Set<TrustStatus> ALLOWED_STATUSES = EnumSet.of(TrustStatus.READY, TrustStatus.INACTIVE);

        public static TrustResult<Void> check(BuildContext context) {

            if (context.activateCalled() && !ALLOWED_STATUSES.contains(context.getStatus())) {

                TrustError error = OperationForbiddenFromStatus.of("activate",context.getStatus());
                return TrustResult.failure(error);
            }

            return TrustResult.success(null);
        }
    }

    public static class DeactivateRestriction {

        private static final Set<TrustStatus> ALLOWED_STATUSES = EnumSet.of(TrustStatus.ACTIVE);

        public static TrustResult<Void> check(BuildContext context) {

            if (context.deactivateCalled() && !ALLOWED_STATUSES.contains(context.getStatus())) {

                TrustError error = OperationForbiddenFromStatus.of("deactivate",context.getStatus());
                return TrustResult.failure(error);
            }

            return TrustResult.success(null);
        }
    }

}
