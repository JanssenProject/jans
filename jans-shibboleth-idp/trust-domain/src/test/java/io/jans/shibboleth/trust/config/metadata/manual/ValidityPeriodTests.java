package io.jans.shibboleth.trust.config.metadata.manual;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ValidityPeriod — absolute instant factory & accessor")
public class ValidityPeriodTests {

    @Test
    @DisplayName("GIVEN an instant WHEN until() is called THEN it succeeds and exposes that instant")
    public void shouldBuildFromInstant_andExposeIt() {

        Instant instant = Instant.parse("2027-01-01T00:00:00Z");

        Result<ValidityPeriod> result = ValidityPeriod.until(instant);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getValidUntil()).isEqualTo(instant);
    }

    @Test
    @DisplayName("GIVEN a null instant WHEN until() is called THEN it fails with RequiredValueMissing")
    public void shouldFail_whenInstantIsNull() {

        Result<ValidityPeriod> result = ValidityPeriod.until(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a relative factory WHEN the accessor is read THEN it returns a future instant")
    public void shouldExposeInstant_fromRelativeFactory() {

        assertThat(ValidityPeriod.daysFromNow(1).getValidUntil()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("GIVEN two ValidityPeriods for the same instant WHEN compared THEN they are equal")
    public void shouldBeEqual_whenSameInstant() {

        Instant instant = Instant.parse("2027-06-15T12:00:00Z");

        ValidityPeriod one = ValidityPeriod.until(instant).getValue();
        ValidityPeriod another = ValidityPeriod.until(instant).getValue();

        assertThat(one).isEqualTo(another);
        assertThat(one.hashCode()).isEqualTo(another.hashCode());
    }
}
