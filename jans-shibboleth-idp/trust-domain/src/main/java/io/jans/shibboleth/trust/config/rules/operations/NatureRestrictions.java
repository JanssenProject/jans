package io.jans.shibboleth.trust.config.rules.operations;

import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.error.OperationRestrictedToNature;
import io.jans.shibboleth.trust.config.error.TrustError;
import io.jans.shibboleth.trust.config.util.BuildContext;
import io.jans.shibboleth.trust.shared.Result;

public class NatureRestrictions {

    private NatureRestrictions () { }

    public static class IncorporateDiscoveredEntityIdsRestriction {

        private static final String OPERATION = "incorporateDiscoveredEntityIds()";

        public static Result<Void> check(BuildContext context) {

            if(context.incorporateDiscoveredEntityIdsCalled() && context.isIndividualNature()) {

                TrustError error = OperationRestrictedToNature.of(OPERATION,TrustNature.AGGREGATE,context.getNature());
                return Result.failure(error);
            }
            
            return Result.success(null);
        }
    }
}
