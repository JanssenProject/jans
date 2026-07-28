package io.jans.shibboleth.trust.activation.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.activation.error.LeaseAlreadyHeld;
import io.jans.shibboleth.trust.activation.error.LeaseNotPresent;
import io.jans.shibboleth.trust.activation.lease.Lease;
import io.jans.shibboleth.trust.activation.lease.LeaseGeneration;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The fake lease repository must reproduce the store's one guarantee: an identity — here (workItemId,
 * generation) — can exist at most once, so a second create for the same tuple loses. These tests pin that
 * lock behaviour, plus find/renew/delete.
 */
@DisplayName("FakeLeaseRepository — identity-as-lock semantics")
public class FakeLeaseRepositoryTests {

    private static final WorkItemId WORK_ITEM = WorkItemId.of(UUID.randomUUID()).getValue();
    private static final WorkerId WORKER = WorkerId.of(Origin.of("a@host")).getValue();
    private static final WorkerId OTHER_WORKER = WorkerId.of(Origin.of("b@host")).getValue();
    private static final Instant GRANTED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EXPIRES_AT = GRANTED_AT.plusSeconds(30);

    private final FakeLeaseRepository repository = new FakeLeaseRepository();

    private static Lease lease(LeaseGeneration generation, WorkerId holder, Instant expiresAt) {

        return Lease.granted(WORK_ITEM, generation, holder, GRANTED_AT, expiresAt).getValue();
    }

    @Test
    @DisplayName("GIVEN no lease WHEN one is created THEN it succeeds")
    public void firstCreateWins() {

        assertThat(repository.create(lease(LeaseGeneration.first(), WORKER, EXPIRES_AT)).isSuccess()).isTrue();
    }

    @Test
    @DisplayName("GIVEN a lease for a (workItem, generation) WHEN a second worker creates the same generation THEN it loses with LeaseAlreadyHeld")
    public void secondCreateForSameGenerationLoses() {

        repository.create(lease(LeaseGeneration.first(), WORKER, EXPIRES_AT));

        Result<Lease> race = repository.create(lease(LeaseGeneration.first(), OTHER_WORKER, EXPIRES_AT));

        assertThat(race.isFailure()).isTrue();
        assertThat(race.getError()).isInstanceOf(LeaseAlreadyHeld.class);
    }

    @Test
    @DisplayName("GIVEN a lease at one generation WHEN the next generation is created THEN both coexist and are found for the work item")
    public void differentGenerationsCoexist() {

        repository.create(lease(LeaseGeneration.first(), WORKER, EXPIRES_AT));
        repository.create(lease(LeaseGeneration.first().next(), OTHER_WORKER, EXPIRES_AT));

        List<Lease> found = repository.findByWorkItem(WORK_ITEM).getValue();

        assertThat(found).hasSize(2);
        assertThat(found).extracting(l -> l.generation().getValue()).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    @DisplayName("GIVEN no leases for a work item WHEN queried THEN the result is empty (the item is unassigned)")
    public void findByWorkItemIsEmptyWhenUnassigned() {

        assertThat(repository.findByWorkItem(WORK_ITEM).getValue()).isEmpty();
    }

    @Test
    @DisplayName("GIVEN an existing lease WHEN renewed with a later expiry THEN the stored lease reflects the new window")
    public void renewUpdatesExistingLease() {

        repository.create(lease(LeaseGeneration.first(), WORKER, EXPIRES_AT));
        Instant later = EXPIRES_AT.plusSeconds(30);

        Result<Lease> renewed = repository.renew(lease(LeaseGeneration.first(), WORKER, later));

        assertThat(renewed.isSuccess()).isTrue();
        assertThat(repository.findByWorkItem(WORK_ITEM).getValue().get(0).expiresAt()).isEqualTo(later);
    }

    @Test
    @DisplayName("GIVEN no matching lease WHEN renewed THEN it fails with LeaseNotPresent (the holder has lost it)")
    public void renewFailsWhenAbsent() {

        Result<Lease> renewed = repository.renew(lease(LeaseGeneration.first(), WORKER, EXPIRES_AT));

        assertThat(renewed.isFailure()).isTrue();
        assertThat(renewed.getError()).isInstanceOf(LeaseNotPresent.class);
    }

    @Test
    @DisplayName("GIVEN a lease WHEN deleted THEN it is gone, and deleting an absent lease still succeeds (idempotent GC)")
    public void deleteIsIdempotent() {

        Lease lease = lease(LeaseGeneration.first(), WORKER, EXPIRES_AT);
        repository.create(lease);

        assertThat(repository.delete(lease).isSuccess()).isTrue();
        assertThat(repository.findByWorkItem(WORK_ITEM).getValue()).isEmpty();
        assertThat(repository.delete(lease).isSuccess()).isTrue();
    }
}
