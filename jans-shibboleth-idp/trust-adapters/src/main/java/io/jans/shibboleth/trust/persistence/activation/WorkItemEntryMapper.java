package io.jans.shibboleth.trust.persistence.activation;

import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.shared.Result;

import java.util.Date;
import java.util.UUID;

/**
 * Translates between the {@link WorkItem} aggregate and its {@link WorkItemEntry}. Only the terminal flag is
 * stored ({@code jansWorkItemStatus}); a non-terminal item stores a null status and is reconstructed as
 * {@code PENDING} (its {@code ASSIGNED}/{@code PENDING} state is derived from lease presence, not stored).
 */
public final class WorkItemEntryMapper {

    private WorkItemEntryMapper() {
    }

    public static WorkItemEntry toEntry(WorkItem workItem) {

        WorkItemEntry entry = new WorkItemEntry();
        entry.setInum(workItem.id().value().toString());
        entry.setType(workItem.type().name());
        entry.setTrustRelationshipId(workItem.trustRelationshipId().value().toString());
        entry.setStatus(workItem.isTerminal() ? workItem.state().name() : null);
        entry.setCreatedAt(Date.from(workItem.createdAt()));
        entry.setLastTransitionAt(Date.from(workItem.lastTransitionAt()));

        return entry;
    }

    public static Result<WorkItem> toDomain(WorkItemEntry entry) {

        Result<WorkItemId> id = WorkItemId.of(UUID.fromString(entry.getInum()));

        if (id.isFailure()) {

            return Result.failure(id.getError());
        }

        Result<TrustRelationshipRef> trustRelationshipId =
            TrustRelationshipRef.of(UUID.fromString(entry.getTrustRelationshipId()));

        if (trustRelationshipId.isFailure()) {

            return Result.failure(trustRelationshipId.getError());
        }

        WorkItemState state = entry.getStatus() == null
            ? WorkItemState.PENDING
            : WorkItemState.valueOf(entry.getStatus());

        return WorkItem.rehydrate(
            id.getValue(),
            WorkItemType.valueOf(entry.getType()),
            trustRelationshipId.getValue(),
            state,
            entry.getCreatedAt().toInstant(),
            entry.getLastTransitionAt().toInstant());
    }
}
