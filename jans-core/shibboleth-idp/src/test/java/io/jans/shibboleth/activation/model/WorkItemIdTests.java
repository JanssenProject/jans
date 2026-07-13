package io.jans.shibboleth.activation.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Group 1 — Identities: WorkItemId")
public class WorkItemIdTests {

    @Test
    @DisplayName("GIVEN the WorkItemId factory WHEN generate() is called repeatedly THEN each call yields a distinct identity")
    public void shouldGenerateDistinctIds_whenGeneratedRepeatedly() {

        WorkItemId first = WorkItemId.generate();
        WorkItemId second = WorkItemId.generate();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("GIVEN two WorkItemIds built from the same underlying value WHEN they are compared THEN they are equal and share the same hashCode")
    public void shouldBeEqual_whenSameUnderlyingValue() {

        UUID value = UUID.randomUUID();

        WorkItemId one = WorkItemId.of(value);
        WorkItemId another = WorkItemId.of(value);

        assertThat(one).isEqualTo(another);
        assertThat(one.hashCode()).isEqualTo(another.hashCode());
    }

    @Test
    @DisplayName("GIVEN two WorkItemIds built from different values WHEN they are compared THEN they are not equal")
    public void shouldNotBeEqual_whenDifferentValues() {

        WorkItemId one = WorkItemId.of(UUID.randomUUID());
        WorkItemId another = WorkItemId.of(UUID.randomUUID());

        assertThat(one).isNotEqualTo(another);
    }
}
