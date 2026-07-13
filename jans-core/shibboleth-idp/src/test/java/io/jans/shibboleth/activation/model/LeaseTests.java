package io.jans.shibboleth.activation.model;

import io.jans.shibboleth.activation.error.LeaseNotPresent;
import io.jans.shibboleth.activation.error.RequiredValueMissing;
import io.jans.shibboleth.activation.util.ActivationResult;
import io.jans.shibboleth.activation.workers.WorkerId;
import io.jans.shibboleth.shared.Origin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Group 3 — Lease & the Lease.NONE null object")
public class LeaseTests {

    private static final WorkerId WORKER = WorkerId.of(Origin.of("instance@host")).getValue();
    private static final Instant GRANTED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EXPIRES_AT = GRANTED_AT.plusSeconds(30);

    @Test
    @DisplayName("GIVEN a workerId and grant and expiry instants WHEN a Lease is granted THEN it exposes that workerId and grantedAt and expiresAt")
    public void shouldExposeHolderAndWindow_whenLeaseGranted() {

        Lease lease = Lease.granted(WORKER, GRANTED_AT, EXPIRES_AT).getValue();

        assertThat(lease.isHeldBy(WORKER)).isTrue();
        assertThat(lease.grantedAt()).isEqualTo(GRANTED_AT);
        assertThat(lease.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("GIVEN a granted Lease WHEN it is renewed with a later expiry THEN a new Lease instance is produced and the original is unchanged")
    public void shouldReturnNewLeaseAndLeaveOriginalUnchanged_whenRenewed() {

        Lease original = Lease.granted(WORKER, GRANTED_AT, EXPIRES_AT).getValue();
        Instant laterExpiry = EXPIRES_AT.plusSeconds(30);

        Lease renewed = original.renew(laterExpiry).getValue();

        assertThat(renewed).isNotSameAs(original);
        assertThat(renewed.expiresAt()).isEqualTo(laterExpiry);
        assertThat(original.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("GIVEN a Lease and a time past its expiresAt WHEN expiry is checked THEN the Lease is expired")
    public void shouldBeExpired_whenNowAfterExpiresAt() {

        Lease lease = Lease.granted(WORKER, GRANTED_AT, EXPIRES_AT).getValue();

        assertThat(lease.isExpired(EXPIRES_AT.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("GIVEN a Lease and a time before its expiresAt WHEN expiry is checked THEN the Lease is not expired")
    public void shouldNotBeExpired_whenNowWithinWindow() {

        Lease lease = Lease.granted(WORKER, GRANTED_AT, EXPIRES_AT).getValue();

        assertThat(lease.isExpired(EXPIRES_AT.minusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("GIVEN a Lease and a time exactly at its expiresAt WHEN expiry is checked THEN the Lease is not expired because the boundary is inclusive")
    public void shouldNotBeExpired_whenNowEqualsExpiresAt() {

        Lease lease = Lease.granted(WORKER, GRANTED_AT, EXPIRES_AT).getValue();

        assertThat(lease.isExpired(EXPIRES_AT)).isFalse();
    }

    @Test
    @DisplayName("GIVEN the Lease.NONE sentinel WHEN it is queried THEN isNone() is true and isPresent() is false")
    public void shouldReportIsNone_forSentinel() {

        assertThat(Lease.NONE.isNone()).isTrue();
        assertThat(Lease.NONE.isPresent()).isFalse();
    }

    @Test
    @DisplayName("GIVEN a granted Lease WHEN it is queried THEN isPresent() is true and isNone() is false")
    public void shouldReportIsPresent_forRealLease() {

        Lease lease = Lease.granted(WORKER, GRANTED_AT, EXPIRES_AT).getValue();

        assertThat(lease.isPresent()).isTrue();
        assertThat(lease.isNone()).isFalse();
    }

    @Test
    @DisplayName("GIVEN the Lease.NONE sentinel WHEN its holder is requested THEN no worker is returned and the caller is expected to check isNone() first")
    public void shouldNotExposeHolder_whenNone() {

        assertThat(Lease.NONE.isHeldBy(WORKER)).isFalse();
    }

    @Test
    @DisplayName("GIVEN a null argument WHEN a Lease is granted THEN it fails and no Lease is produced")
    public void shouldFail_whenGrantedWithNullArgument() {

        ActivationResult<Lease> result = Lease.granted(null, GRANTED_AT, EXPIRES_AT);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a granted Lease WHEN it is renewed with a null expiry THEN it fails and no Lease is produced")
    public void shouldFail_whenRenewedWithNullExpiry() {

        Lease lease = Lease.granted(WORKER, GRANTED_AT, EXPIRES_AT).getValue();

        ActivationResult<Lease> result = lease.renew(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN the Lease.NONE sentinel WHEN it is renewed THEN it fails because an absent lease cannot be renewed")
    public void shouldFail_whenRenewingAbsentLease() {

        ActivationResult<Lease> result = Lease.NONE.renew(EXPIRES_AT);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(LeaseNotPresent.class);
    }
}
