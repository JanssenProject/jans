package io.jans.shibboleth.trust.dto.mapper.activation;

import io.jans.shibboleth.trust.activation.model.WorkItemActivation;
import io.jans.shibboleth.trust.dto.activation.WorkItemView;

/**
 * Translates activation coordination aggregates to/from their DTOs. Mappers own translation only —
 * they carry no coordination logic.
 */
public final class WorkItemMapper {

    private WorkItemMapper() {
    }

    /**
     * Projects a {@link WorkItemActivation} onto its read view. The lease expiry is exposed as an ISO-8601
     * string when the item holds a live lease (ASSIGNED), and null otherwise.
     */
    public static WorkItemView toView(WorkItemActivation activation) {

        String leaseExpiresAt = activation.isAssigned()
            ? activation.leaseExpiresAt().toString()
            : null;

        return new WorkItemView(
            activation.id().value(),
            activation.type(),
            activation.trustRelationshipId().value(),
            activation.state(),
            leaseExpiresAt);
    }
}
