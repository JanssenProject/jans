package io.jans.shibboleth.model.rules.consistency;

import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.core.TrustNature;
import io.jans.shibboleth.model.error.DomainObjectConsistencyFailed;
import io.jans.shibboleth.model.error.OperationRestrictedToNature;
import io.jans.shibboleth.model.error.TrustError;
import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.TrustResult;

public class NatureConstrainedOperations {
    
    private NatureConstrainedOperations () { }

    public static class IncorporateDiscoveredEntityIdsRestrictedToAggregate {

        public static TrustResult<Void> check(BuildContext context) {

            if(context.incorporateDiscoveredEntityIdsCalled() && context.isIndividualNature()) {

                TrustError cause = OperationRestrictedToNature.of(
                    "incorporateDiscoveredEntityIds",
                    TrustNature.AGGREGATE,
                    context.getNature()
                );

                return TrustResult.failure(DomainObjectConsistencyFailed.forClassWithCause(TrustRelationship.class,cause));
            }
            return TrustResult.success(null);
        }
    }
}
