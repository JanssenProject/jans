package io.jans.shibboleth.trust.config.rules.invariants;

import io.jans.shibboleth.trust.config.error.IncompatibleMetadataSourceForNature;
import io.jans.shibboleth.trust.config.util.BuildContext;
import io.jans.shibboleth.trust.config.util.TrustPredicates;
import io.jans.shibboleth.trust.shared.Result;

public class CompatibilityInvariants {

    private CompatibilityInvariants() { }

    public static final class MetadataSourceCompatibility {

        public static Result<Void> check(BuildContext context) {

            if (context.getMetadataSource() == null || context.getNature() == null) {

                //skip rule application
                return Result.success(null);
            }

            if (!TrustPredicates.supportsMetadataSource(context, context.getMetadataSource())) {

                return Result.failure(IncompatibleMetadataSourceForNature.of(context.getMetadataSource(),context.getNature()));
            }

            return Result.success(null);
        }
    }
}
