package io.jans.shibboleth.trust.activation.lease;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The monotonic fencing counter that identifies a lease generation. Mirrors {@code Version}: the first
 * generation is 1, {@code next()} increments, and generations compare so a resurrected holder can be fenced.
 */
@DisplayName("LeaseGeneration — monotonic fencing counter")
public class LeaseGenerationTests {

    @Test
    @DisplayName("GIVEN the first generation WHEN its value is read THEN it is 1")
    public void firstIsOne() {

        assertThat(LeaseGeneration.first().getValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN a generation WHEN advanced THEN the next generation is one greater and the original is unchanged")
    public void nextIncrements() {

        LeaseGeneration first = LeaseGeneration.first();
        LeaseGeneration second = first.next();

        assertThat(second.getValue()).isEqualTo(2);
        assertThat(first.getValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN an integer WHEN a generation is rehydrated from it THEN it exposes that value")
    public void ofExposesValue() {

        assertThat(LeaseGeneration.of(7).getValue()).isEqualTo(7);
    }

    @Test
    @DisplayName("GIVEN two generations WHEN compared THEN a later generation is after an earlier one, but not after an equal or greater one")
    public void isAfterComparesGenerations() {

        LeaseGeneration third = LeaseGeneration.of(3);

        assertThat(third.isAfter(LeaseGeneration.of(2))).isTrue();
        assertThat(third.isAfter(LeaseGeneration.of(3))).isFalse();
        assertThat(third.isAfter(LeaseGeneration.of(4))).isFalse();
    }

    @Test
    @DisplayName("GIVEN generations WHEN compared for equality THEN equal values are equal and different values are not")
    public void valueEquality() {

        assertThat(LeaseGeneration.of(2)).isEqualTo(LeaseGeneration.of(2));
        assertThat(LeaseGeneration.of(2)).hasSameHashCodeAs(LeaseGeneration.of(2));
        assertThat(LeaseGeneration.of(2)).isNotEqualTo(LeaseGeneration.of(3));
    }
}
