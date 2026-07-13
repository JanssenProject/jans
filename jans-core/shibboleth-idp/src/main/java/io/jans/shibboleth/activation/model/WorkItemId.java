package io.jans.shibboleth.activation.model;

import java.util.Objects;
import java.util.UUID;

public final class WorkItemId {

    private final UUID value;

    private WorkItemId(UUID value) {

        this.value = value;
    }

    public static WorkItemId generate() {

        return new WorkItemId(UUID.randomUUID());
    }

    public static WorkItemId of(UUID value) {

        return new WorkItemId(value);
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
