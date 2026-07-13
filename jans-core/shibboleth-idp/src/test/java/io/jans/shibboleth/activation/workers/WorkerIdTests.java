package io.jans.shibboleth.activation.workers;

import io.jans.shibboleth.shared.Origin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Group 1 — Identities: WorkerId")
public class WorkerIdTests {

    @Test
    @DisplayName("GIVEN an Origin of the form instance-at-host WHEN a WorkerId is formed from it THEN the WorkerId carries that origin as its identity")
    public void shouldCarryOriginIdentity_whenBuiltFromOrigin() {

        Origin origin = Origin.of("instance@host");

        WorkerId workerId = WorkerId.of(origin);

        assertThat(workerId.origin()).isEqualTo(origin);
    }

    @Test
    @DisplayName("GIVEN two WorkerIds built from the same Origin WHEN they are compared THEN they are equal and share the same hashCode")
    public void shouldBeEqual_whenSameOrigin() {

        WorkerId one = WorkerId.of(Origin.of("instance@host"));
        WorkerId another = WorkerId.of(Origin.of("instance@host"));

        assertThat(one).isEqualTo(another);
        assertThat(one.hashCode()).isEqualTo(another.hashCode());
    }

    @Test
    @DisplayName("GIVEN two WorkerIds built from different Origins WHEN they are compared THEN they are not equal")
    public void shouldDistinguishWorkers_whenDifferentOrigin() {

        WorkerId one = WorkerId.of(Origin.of("instance-a@host"));
        WorkerId another = WorkerId.of(Origin.of("instance-b@host"));

        assertThat(one).isNotEqualTo(another);
    }
}
