package io.jans.shibboleth.trust.config.metadata.manual;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

public class ValidityPeriod {

    private final Instant validUntil;

    private ValidityPeriod(Instant validUntil) {

        this.validUntil = validUntil;
    }

    public Instant getValidUntil() {

        return validUntil;
    }

    public static Result<ValidityPeriod> until(Instant validUntil) {

        if (validUntil == null) {

            return Result.failure(RequiredValueMissing.of(ValidityPeriod.class));
        }

        return Result.success(new ValidityPeriod(validUntil));
    }

    public static ValidityPeriod secondsFromNow(long seconds) {

        Instant expiresIn = Instant.now()
            .atOffset(ZoneOffset.UTC)
            .plusSeconds(seconds)
            .toInstant();
        
        return new ValidityPeriod(expiresIn);
    }

    public static ValidityPeriod daysFromNow(long days) {

        Instant expiresIn = Instant.now()
            .atOffset(ZoneOffset.UTC)
            .plusDays(days)
            .toInstant();
        
        return new ValidityPeriod(expiresIn);
    }

    public static ValidityPeriod monthsFromNow(long months) {

        Instant expiresIn = Instant.now()
            .atOffset(ZoneOffset.UTC)
            .plusMonths(months)
            .toInstant();
        
        return new ValidityPeriod(expiresIn);
    }

    public static ValidityPeriod yearsFromNow(long years) {

        Instant expiresIn = Instant.now()
            .atOffset(ZoneOffset.UTC)
            .plusYears(years)
            .toInstant();
        
        return new ValidityPeriod(expiresIn);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ValidityPeriod other = (ValidityPeriod) o;

        return Objects.equals(validUntil, other.validUntil);
    }

    @Override
    public int hashCode() {

        return Objects.hash(validUntil);
    }
}
