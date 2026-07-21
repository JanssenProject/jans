package io.jans.shibboleth.trust.dto.mapper.activation;

import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.dto.activation.WorkItemView;

/**
 * Translates activation coordination aggregates to/from their DTOs. Mappers own translation only —
 * they carry no coordination logic.
 */
public final class WorkItemMapper {

    private WorkItemMapper() {
    }

    /**
     * Projects a {@link WorkItem} onto its read view. The lease expiry is exposed as an ISO-8601
     * string when the item holds a lease, and null otherwise.
     */
    public static WorkItemView toView(WorkItem workItem) {

        String leaseExpiresAt = workItem.lease().isPresent()
            ? workItem.lease().expiresAt().toString()
            : null;

        return new WorkItemView(
            workItem.id().value(),
            workItem.type(),
            workItem.trustRelationshipId().value(),
            workItem.state(),
            leaseExpiresAt);
    }
}
