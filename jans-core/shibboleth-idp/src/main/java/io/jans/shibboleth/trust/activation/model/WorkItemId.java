package io.jans.shibboleth.trust.activation.model;

import java.util.Objects;
import java.util.UUID;

import io.jans.shibboleth.trust.activation.error.RequiredValueMissing;
import io.jans.shibboleth.trust.activation.util.ActivationResult;

public final class WorkItemId {

    private final UUID value;

    private WorkItemId(UUID value) {

        this.value = value;
    }

    public static WorkItemId generate() {

        return new WorkItemId(UUID.randomUUID());
    }

    public static ActivationResult<WorkItemId> of(UUID value) {

        if (value == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("value"));
        }

        return ActivationResult.success(new WorkItemId(value));
    }

    public UUID value() {

        return value;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkItemId that = (WorkItemId) o;

        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {

        return Objects.hash(value);
    }

    @Override
    public String toString() {

        return value == null ? "[unassigned work-item id]" : value.toString();
    }
}
