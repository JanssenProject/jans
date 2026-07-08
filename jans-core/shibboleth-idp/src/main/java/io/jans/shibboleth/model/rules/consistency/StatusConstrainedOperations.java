package io.jans.shibboleth.model.rules.consistency;

import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.core.TrustStatus;
import io.jans.shibboleth.model.error.DomainObjectConsistencyFailed;
import io.jans.shibboleth.model.error.OperationForbiddenFromStatus;
import io.jans.shibboleth.model.error.TrustError;
import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.TrustResult;

public class StatusConstrainedOperations {
    
    private StatusConstrainedOperations() { }

    public static class IncorporateEntityIdsRestrictedToActivating {

        public static TrustResult<Void> check(BuildContext context) {

            if(context.isAggregateNature() && context.incorporateDiscoveredEntityIdsCalled() && !context.isActivatingStatus()) {

                TrustError cause = OperationForbiddenFromStatus.of("incorporateDiscoveredEntityIds",context.getStatus());
                return TrustResult.failure(DomainObjectConsistencyFailed.forClassWithCause(TrustRelationship.class, cause));
            }
            return TrustResult.success(null);
        }
    }

    public static class UpdateMetadataSourceDeniedFromActivating {

        public static TrustResult<Void> check(BuildContext context) {

            if(context.getMetadataSource() != null && context.isActivatingStatus() && context.updateMetadataSourceCalled()) {

                TrustError cause  = OperationForbiddenFromStatus.of("updateMetadataSource",context.getStatus());
                return TrustResult.failure(DomainObjectConsistencyFailed.forClassWithCause(TrustRelationship.class, cause));
            }

            return TrustResult.success(null);
        }
    }

    public static class UpdateProfileConfigurationDeniedFromActivating {

        public static TrustResult<Void> check(BuildContext context) {

            if (!context.isActivatingStatus() || !context.updateProfileConfigurationCalled()) {

                return TrustResult.success(null);
            }

            TrustError cause = OperationForbiddenFromStatus.of("updateXXXProfileConfiguration",context.getStatus());
            return TrustResult.failure(DomainObjectConsistencyFailed.forClassWithCause(TrustRelationship.class, cause));
        }
    }

    public static class FinalizeActivationRestrictedToActivating {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.finalizeActivationCalled() && context.getStatus() != TrustStatus.ACTIVATING) {

                TrustError cause = OperationForbiddenFromStatus.of("finalizeActivation",context.getStatus());
                return TrustResult.failure(DomainObjectConsistencyFailed.forClassWithCause(TrustRelationship.class, cause));
            }

            return TrustResult.success(null);
        }
    }

    public static class CancelActivationRestrictedToActivating {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.cancelActivationCalled() && context.getStatus() != TrustStatus.ACTIVATING) {

                TrustError cause = OperationForbiddenFromStatus.of("cancelActivation",context.getStatus());
                return TrustResult.failure(DomainObjectConsistencyFailed.forClassWithCause(TrustRelationship.class, cause));
            }
            
            return TrustResult.success(null);
        }
    }
}
