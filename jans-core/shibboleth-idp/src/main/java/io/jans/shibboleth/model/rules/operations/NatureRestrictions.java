package io.jans.shibboleth.model.rules.operations;

import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.core.TrustNature;
import io.jans.shibboleth.model.error.DomainObjectConsistencyFailed;
import io.jans.shibboleth.model.error.OperationRestrictedToNature;
import io.jans.shibboleth.model.error.TrustError;
import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.TrustResult;

public class NatureRestrictions {

    private NatureRestrictions () { }

    public static class IncorporateDiscoveredEntityIdsRestriction {

        private static final String OPERATION = "incorporateDiscoveredEntityIds()";

        public static TrustResult<Void> check(BuildContext context) {

            if(context.incorporateDiscoveredEntityIdsCalled() && context.isIndividualNature()) {

                TrustError error = OperationRestrictedToNature.of(OPERATION,TrustNature.AGGREGATE,context.getNature());
                return TrustResult.failure(error);
            }
            
            return TrustResult.success(null);
        }
    }
}
