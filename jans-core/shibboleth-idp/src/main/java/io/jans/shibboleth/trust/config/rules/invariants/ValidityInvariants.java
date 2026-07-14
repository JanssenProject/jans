package io.jans.shibboleth.trust.config.rules.invariants;

import io.jans.shibboleth.trust.config.Version;
import io.jans.shibboleth.trust.config.error.InvalidVersion;
import io.jans.shibboleth.trust.config.util.BuildContext;
import io.jans.shibboleth.trust.config.util.TrustResult;

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
