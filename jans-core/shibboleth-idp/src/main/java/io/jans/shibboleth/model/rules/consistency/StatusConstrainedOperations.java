package io.jans.shibboleth.model.rules.consistency;

import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.error.DomainObjectConsistencyFailed;
import io.jans.shibboleth.model.error.OperationRestrictedToStatus;
import io.jans.shibboleth.model.error.TrustError;
import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.TrustResult;

public class StatusConstrainedOperations {
    
    private StatusConstrainedOperations() { }

    public static class IncorporateEntityIdsRestrictedToActivating {

        public static TrustResult<Void> check(BuildContext context) {

            if(context.isAggregateNature() && context.incorporateDiscoveredEntityIdsCalled() && !context.isActivatingStatus()) {

                TrustError cause = OperationRestrictedToStatus.of("incorporateDiscoveredEntityIds",context.getStatus());
                return TrustResult.failure(DomainObjectConsistencyFailed.forClassWithCause(TrustRelationship.class, cause));
            }
            return TrustResult.success(null);
        }
    }
}
