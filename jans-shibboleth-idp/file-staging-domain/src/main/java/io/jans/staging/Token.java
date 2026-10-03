package io.jans.staging;

import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

import java.util.Objects;

/**
 * Opaque identity of a staged file, minted by an infrastructure token generator and handed to the
 * uploader. The domain treats it only as a validated, non-blank string; it is also the seed for the
 * deterministic claim filename ({@link Destination#resolve(Token)}).
 */
public final class Token {

    private final String value;

    private Token(String value) {

        this.value = value;
    }

    public static Result<Token> of(String value) {

        if (value == null || value.isBlank()) {

            return Result.failure(RequiredValueMissing.of(Token.class));
        }
        return Result.success(new Token(value.trim()));
    }

    public String getValue() {

        return value;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }
        if (!(o instanceof Token)) {

            return false;
        }
        return value.equals(((Token) o).value);
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
