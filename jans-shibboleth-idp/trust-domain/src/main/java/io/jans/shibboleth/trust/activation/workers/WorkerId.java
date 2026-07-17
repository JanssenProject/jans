package io.jans.shibboleth.trust.activation.workers;

import java.util.Objects;

import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.shared.Origin;

public final class WorkerId {

    private final Origin origin;

    private WorkerId(Origin origin) {

        this.origin = origin;
    }

    public static Result<WorkerId> of(Origin origin) {

        if (origin == null) {

            return Result.failure(RequiredValueMissing.forField("origin"));
        }

        return Result.success(new WorkerId(origin));
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
