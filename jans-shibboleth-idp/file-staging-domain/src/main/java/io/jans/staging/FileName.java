package io.jans.staging;

import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

import java.util.Objects;

/**
 * The name a staged file is stored under (with extension, e.g. {@code a1b2c3.xml}). Produced by the
 * {@code FileStorageLayout} from the token and content type, it is stable across the file's life so
 * the staging and durable paths share one name. The domain treats it as a validated, non-blank
 * string; the layout port is responsible for producing a clean, separator-free name.
 */
public final class FileName {

    private final String value;

    private FileName(String value) {

        this.value = value;
    }

    public static Result<FileName> of(String value) {

        if (value == null || value.isBlank()) {

            return Result.failure(RequiredValueMissing.of(FileName.class));
        }
        return Result.success(new FileName(value.trim()));
    }

    public String getValue() {

        return value;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }
        if (!(o instanceof FileName)) {

            return false;
        }
        return value.equals(((FileName) o).value);
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
