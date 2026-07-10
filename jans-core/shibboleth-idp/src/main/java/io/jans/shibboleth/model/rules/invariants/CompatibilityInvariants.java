package io.jans.shibboleth.model.rules.invariants;

import io.jans.shibboleth.model.error.IncompatibleMetadataSourceForNature;
import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.TrustPredicates;
import io.jans.shibboleth.model.util.TrustResult;

public class CompatibilityInvariants {

    private CompatibilityInvariants() { }

    public static final class MetadataSourceCompatibility {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getMetadataSource() == null || context.getNature() == null) {

                //skip rule application
                return TrustResult.success(null);
            }

            if (!TrustPredicates.supportsMetadataSource(context, context.getMetadataSource())) {

                return TrustResult.failure(IncompatibleMetadataSourceForNature.of(context.getMetadataSource(),context.getNature()));
            }

            return TrustResult.success(null);
        }
    }
}
