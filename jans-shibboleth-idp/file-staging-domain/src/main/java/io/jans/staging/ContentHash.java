package io.jans.staging;

import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

import java.util.Objects;

/**
 * Integrity digest of a staged file's bytes (a hex SHA-256), computed by infrastructure at upload and
 * carried so a consumer can verify a file after claiming it. The domain holds it as a validated,
 * non-blank string and does not recompute it.
 */
public final class ContentHash {

    private final String value;

    private ContentHash(String value) {

        this.value = value;
    }

    public static Result<ContentHash> of(String value) {

        if (value == null || value.isBlank()) {

            return Result.failure(RequiredValueMissing.of(ContentHash.class));
        }
        return Result.success(new ContentHash(value.trim()));
    }

    public String getValue() {

        return value;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }
        if (!(o instanceof ContentHash)) {

            return false;
        }
        return value.equals(((ContentHash) o).value);
    }

    @Override
    public int hashCode() {

        return Objects.hash(value);
    }

    @Override
    public String toString() {

        return value;
    }
}
