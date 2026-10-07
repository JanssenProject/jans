package io.jans.shibboleth.trust.activation.coordination;

import java.time.Instant;

@FunctionalInterface
public interface TimeSource {

    Instant now();
}
