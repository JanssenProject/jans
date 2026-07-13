package io.jans.shibboleth.activation.workers;

import java.util.Objects;

import io.jans.shibboleth.shared.Origin;

public final class WorkerId {

    private final Origin origin;

    private WorkerId(Origin origin) {

        this.origin = origin;
    }

    public static WorkerId of(Origin origin) {

        return new WorkerId(origin);
    }

    public Origin origin() {

        return origin;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkerId that = (WorkerId) o;

        return Objects.equals(origin, that.origin);
    }

    @Override
    public int hashCode() {

        return Objects.hash(origin);
    }

    @Override
    public String toString() {

        return origin == null ? "[no worker id]" : origin.toString();
    }
}
