package io.jans.shibboleth.trust.activation.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.jans.shibboleth.trust.activation.error.LeaseAlreadyHeld;
import io.jans.shibboleth.trust.activation.error.LeaseNotPresent;
import io.jans.shibboleth.trust.activation.lease.Lease;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.repository.LeaseRepository;
import io.jans.shibboleth.trust.shared.Result;

/**
 * In-memory {@link LeaseRepository} for domain tests. Its {@code create} keys on {@code (workItemId,
 * generation)} — the same tuple the persistence adapter folds into a deterministic id — so a second create
 * for that tuple collides exactly as a duplicate primary key would, exercising the lock without a database.
 */
public final class FakeLeaseRepository implements LeaseRepository {

    private final Map<String, Lease> leasesByIdentity = new LinkedHashMap<>();

    @Override
    public Result<Lease> create(Lease lease) {

        String key = identity(lease.workItemId(), lease.generation().getValue());

        if (leasesByIdentity.containsKey(key)) {

            return Result.failure(LeaseAlreadyHeld.instance());
        }

        leasesByIdentity.put(key, lease);

        return Result.success(lease);
    }

    @Override
    public Result<List<Lease>> findByWorkItem(WorkItemId workItemId) {

        List<Lease> found = new ArrayList<>();

        for (Lease lease : leasesByIdentity.values()) {

            if (lease.workItemId().equals(workItemId)) {

                found.add(lease);
            }
        }

        return Result.success(found);
    }

    @Override
    public Result<Lease> renew(Lease lease) {

        String key = identity(lease.workItemId(), lease.generation().getValue());

        if (!leasesByIdentity.containsKey(key)) {

            return Result.failure(LeaseNotPresent.instance());
        }

        leasesByIdentity.put(key, lease);

        return Result.success(lease);
    }

    @Override
    public Result<Void> delete(Lease lease) {

        leasesByIdentity.remove(identity(lease.workItemId(), lease.generation().getValue()));

        return Result.success(null);
    }

    private static String identity(WorkItemId workItemId, int generation) {

        return workItemId.value() + ":" + generation;
    }
}
