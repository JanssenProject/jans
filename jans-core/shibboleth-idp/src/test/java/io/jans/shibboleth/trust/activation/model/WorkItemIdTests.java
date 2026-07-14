package io.jans.shibboleth.trust.activation.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import io.jans.shibboleth.trust.activation.error.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

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

        WorkItemId one = WorkItemId.of(value).getValue();
        WorkItemId another = WorkItemId.of(value).getValue();

        assertThat(one).isEqualTo(another);
        assertThat(one.hashCode()).isEqualTo(another.hashCode());
    }

    @Test
    @DisplayName("GIVEN two WorkItemIds built from different values WHEN they are compared THEN they are not equal")
    public void shouldNotBeEqual_whenDifferentValues() {

        WorkItemId one = WorkItemId.of(UUID.randomUUID()).getValue();
        WorkItemId another = WorkItemId.of(UUID.randomUUID()).getValue();

        assertThat(one).isNotEqualTo(another);
    }

    @Test
    @DisplayName("GIVEN a null underlying value WHEN a WorkItemId is built THEN it fails and no WorkItemId is produced")
    public void shouldFail_whenBuiltFromNullValue() {

        Result<WorkItemId> result = WorkItemId.of(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }
}
