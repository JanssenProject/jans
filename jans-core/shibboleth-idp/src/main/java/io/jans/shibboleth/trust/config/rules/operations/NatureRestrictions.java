package io.jans.shibboleth.trust.config.rules.operations;

import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.error.DomainObjectConsistencyFailed;
import io.jans.shibboleth.trust.config.error.OperationRestrictedToNature;
import io.jans.shibboleth.trust.config.error.TrustError;
import io.jans.shibboleth.trust.config.util.BuildContext;
import io.jans.shibboleth.trust.config.util.TrustResult;

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
