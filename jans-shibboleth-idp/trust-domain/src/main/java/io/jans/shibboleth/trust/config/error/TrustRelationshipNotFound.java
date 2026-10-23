package io.jans.shibboleth.trust.config.error;

import java.util.UUID;

/**
 * No trust relationship exists for a given id. A lookup outcome surfaced by the persistence layer through
 * {@code Result}; the API maps it to {@code 404}.
 */
public class TrustRelationshipNotFound extends TrustError {

    private TrustRelationshipNotFound(String message) {

        super(message);
    }

    public static TrustRelationshipNotFound forId(UUID id) {

        return new TrustRelationshipNotFound("No trust relationship was found for id " + id);
    }
}
