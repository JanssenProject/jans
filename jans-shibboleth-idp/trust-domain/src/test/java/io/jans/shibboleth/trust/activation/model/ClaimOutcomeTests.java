package io.jans.shibboleth.trust.activation.model;

import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static io.jans.shibboleth.trust.activation.model.WorkItemType.PROCESS_AGGREGATE_METADATA;

@DisplayName("ClaimOutcome — claimed-or-nothing value")
public class ClaimOutcomeTests {

    private WorkItem anItem() {

        return WorkItem.create(PROCESS_AGGREGATE_METADATA,
            TrustRelationshipRef.of(UUID.randomUUID()).getValue(), Instant.parse("2026-01-01T00:00:00Z")).getValue();
    }

    @Test
    @DisplayName("GIVEN a claimed item WHEN wrapped THEN it is claimed and exposes the item")
    public void shouldCarryClaimedItem() {

        WorkItem item = anItem();

        ClaimOutcome outcome = ClaimOutcome.of(item);

        assertThat(outcome.isClaimed()).isTrue();
        assertThat(outcome.isEmpty()).isFalse();
        assertThat(outcome.workItem()).isSameAs(item);
    }

    @Test
    @DisplayName("GIVEN none WHEN inspected THEN it is empty")
    public void shouldBeEmptyForNone() {

        ClaimOutcome outcome = ClaimOutcome.none();

        assertThat(outcome.isEmpty()).isTrue();
        assertThat(outcome.isClaimed()).isFalse();
    }

    @Test
    @DisplayName("GIVEN a null item WHEN wrapped THEN it collapses to none")
    public void shouldTreatNullAsNone() {

        assertThat(ClaimOutcome.of(null).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("GIVEN an empty outcome WHEN workItem() is read THEN it fails fast")
    public void shouldRejectReadingItemWhenEmpty() {

        assertThatThrownBy(() -> ClaimOutcome.none().workItem())
            .isInstanceOf(IllegalStateException.class);
    }
}
