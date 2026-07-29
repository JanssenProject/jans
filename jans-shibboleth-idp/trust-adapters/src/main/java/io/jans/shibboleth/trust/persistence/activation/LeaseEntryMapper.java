package io.jans.shibboleth.trust.persistence.activation;

import io.jans.shibboleth.trust.activation.lease.Lease;
import io.jans.shibboleth.trust.activation.lease.LeaseGeneration;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.Result;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Translates between the {@link Lease} aggregate and its {@link LeaseEntry}. The lease has no id of its own;
 * its storage identity is a <b>deterministic</b> name-based UUID of {@code (workItemId, generation)} — the
 * determinism is the lock, so two workers racing for the same generation compute the same inum and collide.
 * A random UUID here would silently break mutual exclusion.
 */
public final class LeaseEntryMapper {

    private LeaseEntryMapper() {
    }

    /**
     * The deterministic storage id for a lease at {@code (workItemId, generation)}. Same inputs → same id on
     * every node (a name-based UUID), so an at-most-once identity per generation falls out of the store's
     * primary-key uniqueness. Must never be randomised.
     */
    public static String inumFor(UUID workItemId, int generation) {

        String name = "lease|" + workItemId + "|" + generation;

        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static LeaseEntry toEntry(Lease lease) {

        LeaseEntry entry = new LeaseEntry();
        entry.setInum(inumFor(lease.workItemId().value(), lease.generation().getValue()));
        entry.setWorkItemRef(lease.workItemId().value().toString());
        entry.setGeneration(lease.generation().getValue());
        entry.setWorker(lease.holder().origin().getValue());
        entry.setGrantedAt(Date.from(lease.grantedAt()));
        entry.setExpiresAt(Date.from(lease.expiresAt()));

        return entry;
    }

    public static Result<Lease> toDomain(LeaseEntry entry) {

        Result<WorkItemId> workItemId = WorkItemId.of(UUID.fromString(entry.getWorkItemRef()));

        if (workItemId.isFailure()) {

            return Result.failure(workItemId.getError());
        }

        Result<WorkerId> holder = WorkerId.of(Origin.of(entry.getWorker()));

        if (holder.isFailure()) {

            return Result.failure(holder.getError());
        }
        return Lease.granted(
            workItemId.getValue(),
            LeaseGeneration.of(entry.getGeneration()),
            holder.getValue(),
            entry.getGrantedAt().toInstant(),
            entry.getExpiresAt().toInstant());
    }
}
