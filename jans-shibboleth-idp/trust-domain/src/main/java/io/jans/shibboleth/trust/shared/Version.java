package io.jans.shibboleth.trust.shared;

/**
 * An aggregate's optimistic-concurrency version. Starts at 1 and only ever moves forward.
 */
public record Version(int value) implements Comparable<Version> {

    public static Version initial() {

        return new Version(1);
    }

    public static Version of(int value) {

        return new Version(value);
    }

    public Version next() {

        return new Version(value + 1);
    }

    public boolean isInitial() {

        return value == 1;
    }

    public boolean isGreaterThan(Version other) {

        return value > other.value;
    }

    @Override
    public int compareTo(Version other) {

        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {

        return "Version{" + value + "}";
    }
}
