package io.jans.staging;

import io.jans.kernel.Result;
import io.jans.staging.error.InvalidDestination;

import java.util.Objects;

/**
 * The directory into which a claimed file is moved — a unix-style, absolute path in the document
 * store's directory-like namespace (e.g. {@code /opt/shibboleth-idp/metadata/}). Validation is
 * namespace hygiene, not a security boundary (the store enforces no ACLs): a blank, relative, or
 * traversal-bearing path is rejected. {@link #resolve(FileName)} derives the durable {@link Handle}
 * by placing the given file under the directory.
 */
public final class Destination {

    private final String path;

    private Destination(String path) {

        this.path = path;
    }

    public static Result<Destination> of(String raw) {

        if (raw == null || raw.isBlank()) {

            return Result.failure(InvalidDestination.forValue(String.valueOf(raw)));
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("/") || trimmed.contains("..")) {

            return Result.failure(InvalidDestination.forValue(trimmed));
        }
        return Result.success(new Destination(trimmed));
    }

    public Handle resolve(FileName fileName) {

        String directory = path.endsWith("/") ? path : path + "/";
        return Handle.of(directory + fileName.getValue());
    }

    public String getValue() {

        return path;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }
        if (!(o instanceof Destination)) {

            return false;
        }
        return path.equals(((Destination) o).path);
    }

    @Override
    public int hashCode() {

        return Objects.hash(path);
    }

    @Override
    public String toString() {

        return path;
    }
}
