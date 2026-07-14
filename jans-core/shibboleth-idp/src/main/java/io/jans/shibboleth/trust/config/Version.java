package io.jans.shibboleth.trust.config;

public class Version implements Comparable<Version> {

    private final int value;

    private Version(int value) {

        this.value = value;
    }

    public static final Version initial() {

        return new Version(1);
    }

    public static final Version of(int value) {

        return new Version(value);
    }

    public Version next() {

        return new Version(value+1);
    }

    public boolean isInitial() {

        return value == 1;
    }

    public boolean isGreaterThan(Version other) {

        return this.value > other.value;
    }

    public int getValue() {

        return value;
    }

    @Override
    public int compareTo(Version o) {

        return Integer.compare(this.value,o.value);
    }

    @Override 
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return value == version.value;
    }

    @Override
    public int hashCode() {

        return Integer.hashCode(value);
    }

    @Override
    public String toString() {

        return "Version{" + value + "}";
    }
}
