package io.jans.shibboleth.activation.coordination;

import java.time.Instant;

@FunctionalInterface
public interface TimeSource {

    Instant now();
}
