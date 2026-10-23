package io.jans.shibboleth.trust.activation.lease;

import java.util.Objects;

/**
 * The monotonic counter that identifies a lease generation and serves as its fencing token. The first
 * generation is 1; a takeover creates the {@link #next()} generation. Because generations only ever
 * advance, comparing them lets a resurrected holder be fenced out: a holder whose generation is not the
 * latest has lost the lease.
 */
public final class LeaseGeneration implements Comparable<LeaseGeneration> {

    private final int value;

    private LeaseGeneration(int value) {

        this.value = value;
    }

    public static LeaseGeneration first() {

        return new LeaseGeneration(1);
    }

    public static LeaseGeneration of(int value) {

        return new LeaseGeneration(value);
    }

    public LeaseGeneration next() {

        return new LeaseGeneration(value + 1);
    }

    public boolean isAfter(LeaseGeneration other) {

        return this.value > other.value;
    }

    public int getValue() {

        return value;
    }

    @Override
    public int compareTo(LeaseGeneration other) {

        return Integer.compare(this.value, other.value);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaseGeneration that = (LeaseGeneration) o;

        return value == that.value;
    }

    @Override
    public int hashCode() {

        return Integer.hashCode(value);
    }

    @Override
    public String toString() {

        return "LeaseGeneration{" + value + "}";
    }
}
