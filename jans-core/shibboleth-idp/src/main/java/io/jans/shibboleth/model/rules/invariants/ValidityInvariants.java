package io.jans.shibboleth.model.rules.invariants;

import io.jans.shibboleth.model.core.Version;
import io.jans.shibboleth.model.error.InvalidVersion;
import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.TrustResult;

public class ValidityInvariants {

    private ValidityInvariants() { }

    public static final class VersionAtLeastInitial {

        public static TrustResult<Void> check(BuildContext context) {

            Version version = context.getVersion();

            if (version == null) {

                // presence is enforced by PresenceInvariants.VersionRequired; skip here
                return TrustResult.success(null);
            }

            if (version.compareTo(Version.initial()) < 0) {

                return TrustResult.failure(InvalidVersion.belowMinimum(version, Version.initial()));
            }

            return TrustResult.success(null);
        }
    }
}
