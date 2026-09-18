package io.jans.shibboleth.trust.config.error;

import io.jans.shibboleth.trust.shared.Version;

public class InvalidVersion extends TrustError {

    private final Version version;
    private final Version minimum;

    private InvalidVersion(Version version, Version minimum) {

        super(String.format("Version <%s> is below the minimum allowed version <%s>", version, minimum));
        this.version = version;
        this.minimum = minimum;
    }

    public Version getVersion() {

        return version;
    }

    public Version getMinimum() {

        return minimum;
    }

    public static InvalidVersion belowMinimum(Version version, Version minimum) {

        return new InvalidVersion(version, minimum);
    }
}
