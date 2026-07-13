package io.jans.shibboleth.activation.model;

import java.time.Instant;
import java.util.Objects;

import io.jans.shibboleth.activation.error.LeaseNotPresent;
import io.jans.shibboleth.activation.error.RequiredValueMissing;
import io.jans.shibboleth.activation.util.ActivationResult;
import io.jans.shibboleth.activation.workers.WorkerId;

public final class Lease {

    public static final Lease NONE = new Lease(null, null, null);

    private final WorkerId workerId;
    private final Instant grantedAt;
    private final Instant expiresAt;

    private Lease(WorkerId workerId, Instant grantedAt, Instant expiresAt) {

        this.workerId = workerId;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
    }

    public static ActivationResult<Lease> granted(WorkerId workerId, Instant grantedAt, Instant expiresAt) {

        if (workerId == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("workerId"));
        }

        if (grantedAt == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("grantedAt"));
        }

        if (expiresAt == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("expiresAt"));
        }

        return ActivationResult.success(new Lease(workerId, grantedAt, expiresAt));
    }

    public ActivationResult<Lease> renew(Instant newExpiresAt) {

        if (isNone()) {

            return ActivationResult.failure(LeaseNotPresent.forRenewal());
        }

        if (newExpiresAt == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("expiresAt"));
        }

        return ActivationResult.success(new Lease(workerId, grantedAt, newExpiresAt));
    }

    public boolean isNone() {

        return this == NONE;
    }

    public boolean isPresent() {

        return !isNone();
    }

    public boolean isHeldBy(WorkerId candidate) {

        return isPresent() && Objects.equals(workerId, candidate);
    }

    public boolean isExpired(Instant now) {

        return now.isAfter(expiresAt);
    }

    public Instant grantedAt() {

        return grantedAt;
    }

    public Instant expiresAt() {

        return expiresAt;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lease lease = (Lease) o;

        return Objects.equals(workerId, lease.workerId)
            && Objects.equals(grantedAt, lease.grantedAt)
            && Objects.equals(expiresAt, lease.expiresAt);
    }

    @Override
    public int hashCode() {

        return Objects.hash(workerId, grantedAt, expiresAt);
    }
}
