package io.jans.shibboleth.trust.config.rules.invariants;

import io.jans.shibboleth.trust.config.Version;
import io.jans.shibboleth.trust.config.error.InvalidVersion;
import io.jans.shibboleth.trust.config.util.BuildContext;
import io.jans.shibboleth.trust.shared.Result;

public class ValidityInvariants {

    private ValidityInvariants() { }

    public static final class VersionAtLeastInitial {

        public static Result<Void> check(BuildContext context) {

            Version version = context.getVersion();

            if (version == null) {

                // presence is enforced by PresenceInvariants.VersionRequired; skip here
                return Result.success(null);
            }

            if (version.compareTo(Version.initial()) < 0) {

                return Result.failure(InvalidVersion.belowMinimum(version, Version.initial()));
            }

            return Result.success(null);
        }
    }
}
