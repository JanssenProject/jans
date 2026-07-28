package io.jans.shibboleth.trust.activation.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.activation.error.WorkItemNotFound;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FakeWorkItemRepository — CRUD and claimable-candidate query")
public class FakeWorkItemRepositoryTests {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    private final FakeWorkItemRepository repository = new FakeWorkItemRepository();

    private static WorkItem item(WorkItemType type, WorkItemState state, Instant createdAt) {

        return WorkItem.rehydrate(WorkItemId.of(UUID.randomUUID()).getValue(), type,
            TrustRelationshipRef.of(UUID.randomUUID()).getValue(), state, createdAt, createdAt).getValue();
    }

    @Test
    @DisplayName("GIVEN a saved work item WHEN found by id THEN the same item is returned")
    public void savesAndFinds() {

        WorkItem saved = item(WorkItemType.PROCESS_AGGREGATE_METADATA, WorkItemState.PENDING, BASE);
        repository.save(saved);

        assertThat(repository.findById(saved.id()).getValue()).isSameAs(saved);
    }

    @Test
    @DisplayName("GIVEN no such id WHEN found THEN it fails with WorkItemNotFound")
    public void findByIdMissing() {

        Result<WorkItem> found = repository.findById(WorkItemId.of(UUID.randomUUID()).getValue());

        assertThat(found.isFailure()).isTrue();
        assertThat(found.getError()).isInstanceOf(WorkItemNotFound.class);
    }

    @Test
    @DisplayName("GIVEN a saved work item WHEN deleted THEN it can no longer be found")
    public void deletes() {

        WorkItem saved = item(WorkItemType.PROCESS_AGGREGATE_METADATA, WorkItemState.PENDING, BASE);
        repository.save(saved);

        repository.delete(saved.id());

        assertThat(repository.findById(saved.id()).isFailure()).isTrue();
    }

    @Test
    @DisplayName("GIVEN items of mixed type and state WHEN claimable candidates are queried THEN only non-terminal items of that type return, oldest first")
    public void claimableCandidatesFilterAndOrder() {

        WorkItem newest = item(WorkItemType.PROCESS_AGGREGATE_METADATA, WorkItemState.PENDING, BASE.plusSeconds(30));
        WorkItem oldest = item(WorkItemType.PROCESS_AGGREGATE_METADATA, WorkItemState.PENDING, BASE);
        WorkItem terminal = item(WorkItemType.PROCESS_AGGREGATE_METADATA, WorkItemState.COMPLETED, BASE.plusSeconds(10));
        WorkItem otherType = item(WorkItemType.PROCESS_INDIVIDUAL_METADATA, WorkItemState.PENDING, BASE.plusSeconds(5));

        repository.save(newest);
        repository.save(oldest);
        repository.save(terminal);
        repository.save(otherType);

        List<WorkItem> candidates =
            repository.findClaimableCandidates(WorkItemType.PROCESS_AGGREGATE_METADATA).getValue();

        assertThat(candidates).containsExactly(oldest, newest);
    }
}
