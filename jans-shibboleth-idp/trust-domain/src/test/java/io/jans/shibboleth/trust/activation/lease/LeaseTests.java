package io.jans.shibboleth.trust.activation.lease;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The lease as a create-only satellite aggregate (AP2): it binds a work item, a fencing generation, a
 * holder and a time window. There is no {@code NONE} sentinel — absence is the lack of a lease, not a
 * value. It carries no id of its own; the adapter derives a deterministic inum from (workItemId, generation).
 */
@DisplayName("Lease — create-only satellite aggregate")
public class LeaseTests {

    private static final WorkItemId WORK_ITEM = WorkItemId.of(UUID.randomUUID()).getValue();
    private static final LeaseGeneration GENERATION = LeaseGeneration.first();
    private static final WorkerId WORKER = WorkerId.of(Origin.of("instance@host")).getValue();
    private static final Instant GRANTED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EXPIRES_AT = GRANTED_AT.plusSeconds(30);

    private static Lease granted() {

        return Lease.granted(WORK_ITEM, GENERATION, WORKER, GRANTED_AT, EXPIRES_AT).getValue();
    }

    @Test
    @DisplayName("GIVEN all fields WHEN a lease is granted THEN it exposes the work item, generation, holder and window")
    public void exposesAllFields() {

        Lease lease = granted();

        assertThat(lease.workItemId()).isEqualTo(WORK_ITEM);
        assertThat(lease.generation()).isEqualTo(GENERATION);
        assertThat(lease.isHeldBy(WORKER)).isTrue();
        assertThat(lease.grantedAt()).isEqualTo(GRANTED_AT);
        assertThat(lease.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("GIVEN a null argument WHEN a lease is granted THEN it fails and no lease is produced")
    public void failsWhenGrantedWithNullArgument() {

        assertThat(Lease.granted(null, GENERATION, WORKER, GRANTED_AT, EXPIRES_AT).isFailure()).isTrue();
        assertThat(Lease.granted(WORK_ITEM, null, WORKER, GRANTED_AT, EXPIRES_AT).isFailure()).isTrue();
        assertThat(Lease.granted(WORK_ITEM, GENERATION, null, GRANTED_AT, EXPIRES_AT).isFailure()).isTrue();
        assertThat(Lease.granted(WORK_ITEM, GENERATION, WORKER, null, EXPIRES_AT).isFailure()).isTrue();

        Result<Lease> result = Lease.granted(WORK_ITEM, GENERATION, WORKER, GRANTED_AT, null);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a lease WHEN renewed with a later expiry THEN a new lease carries it and the original is unchanged")
    public void renewExtendsExpiryImmutably() {

        Lease original = granted();
        Instant laterExpiry = EXPIRES_AT.plusSeconds(30);

        Lease renewed = original.renew(laterExpiry).getValue();

        assertThat(renewed).isNotSameAs(original);
        assertThat(renewed.expiresAt()).isEqualTo(laterExpiry);
        assertThat(renewed.workItemId()).isEqualTo(WORK_ITEM);
        assertThat(renewed.generation()).isEqualTo(GENERATION);
        assertThat(renewed.isHeldBy(WORKER)).isTrue();
        assertThat(renewed.grantedAt()).isEqualTo(GRANTED_AT);
        assertThat(original.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("GIVEN a lease WHEN renewed with a null expiry THEN it fails and no lease is produced")
    public void failsWhenRenewedWithNullExpiry() {

        Result<Lease> result = granted().renew(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a lease WHEN checked at, before and after its expiry THEN the boundary is inclusive (expired only strictly after)")
    public void expiryBoundaryIsInclusive() {

        Lease lease = granted();

        assertThat(lease.isExpired(EXPIRES_AT.minusSeconds(1))).isFalse();
        assertThat(lease.isExpired(EXPIRES_AT)).isFalse();
        assertThat(lease.isExpired(EXPIRES_AT.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("GIVEN a lease WHEN liveness is checked THEN it is the complement of expiry")
    public void liveIsComplementOfExpired() {

        Lease lease = granted();

        assertThat(lease.isLive(EXPIRES_AT)).isTrue();
        assertThat(lease.isLive(EXPIRES_AT.plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("GIVEN a lease WHEN a different worker is checked THEN it is not held by that worker")
    public void notHeldByOtherWorker() {

        WorkerId other = WorkerId.of(Origin.of("other@host")).getValue();

        assertThat(granted().isHeldBy(other)).isFalse();
    }

    @Test
    @DisplayName("GIVEN two leases WHEN compared THEN equality is by value and a differing generation makes them unequal")
    public void valueEquality() {

        Lease a = granted();
        Lease b = granted();
        Lease differentGeneration =
            Lease.granted(WORK_ITEM, GENERATION.next(), WORKER, GRANTED_AT, EXPIRES_AT).getValue();

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(differentGeneration);
    }
}
