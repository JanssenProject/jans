package io.jans.shibboleth.trust.config.metadata.manual;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

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

            return Result.failure(RequiredValueMissing.forField("validUntil"));
        }

        return Result.success(new ValidityPeriod(validUntil));
    }

    public static ValidityPeriod secondsFromNow(long seconds) {

        return new ValidityPeriod(Instant.now().plus(seconds,ChronoUnit.SECONDS));
    }

    public static ValidityPeriod daysFromNow(long days) {

        return new ValidityPeriod(Instant.now().plus(days,ChronoUnit.DAYS));
    }

    public static ValidityPeriod monthsFromNow(long months) {

        return new ValidityPeriod(Instant.now().plus(months,ChronoUnit.MONTHS));
    }

    public static ValidityPeriod yearsFromNow(long years) {

        return new ValidityPeriod(Instant.now().plus(years,ChronoUnit.YEARS));
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
