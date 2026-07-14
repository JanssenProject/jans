package io.jans.shibboleth.trust.activation.model;

import io.jans.shibboleth.trust.activation.error.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.trust.activation.model.WorkItemType.PROCESS_AGGREGATE_METADATA;

@DisplayName("Group 4 — WorkItem Creation & Invariants")
public class WorkItemCreationTests {

    private static final TrustRelationshipRef TR_REF = TrustRelationshipRef.of(UUID.randomUUID()).getValue();
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("GIVEN a WorkItemType and an opaque trustRelationshipId WHEN a WorkItem is created THEN it is PENDING with a fresh WorkItemId and that type and that TR reference")
    public void shouldCreatePendingWorkItem_whenCreatedForTr() {

        Result<WorkItem> result = WorkItem.create(PROCESS_AGGREGATE_METADATA, TR_REF, NOW);

        assertThat(result.isSuccess()).isTrue();

        WorkItem item = result.getValue();
        assertThat(item.state()).isEqualTo(WorkItemState.PENDING);
        assertThat(item.type()).isEqualTo(PROCESS_AGGREGATE_METADATA);
        assertThat(item.trustRelationshipId()).isEqualTo(TR_REF);
        assertThat(item.id()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN a freshly created WorkItem WHEN its lease is inspected THEN the lease is Lease.NONE")
    public void shouldHaveNoLease_whenPending() {

        WorkItem item = WorkItem.create(PROCESS_AGGREGATE_METADATA, TR_REF, NOW).getValue();

        assertThat(item.lease().isNone()).isTrue();
    }

    @Test
    @DisplayName("GIVEN a time source WHEN a WorkItem is created THEN its createdAt and lastTransitionAt are stamped from that time")
    public void shouldStampCreatedAt_whenCreated() {

        WorkItem item = WorkItem.create(PROCESS_AGGREGATE_METADATA, TR_REF, NOW).getValue();

        assertThat(item.createdAt()).isEqualTo(NOW);
        assertThat(item.lastTransitionAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("GIVEN a WorkItem WHEN its TR reference is inspected THEN it references exactly one trustRelationshipId and never null")
    public void shouldReferenceExactlyOneTr_whenCreated() {

        WorkItem item = WorkItem.create(PROCESS_AGGREGATE_METADATA, TR_REF, NOW).getValue();

        assertThat(item.trustRelationshipId()).isNotNull();
        assertThat(item.trustRelationshipId()).isEqualTo(TR_REF);
    }

    @Test
    @DisplayName("GIVEN a null WorkItemType WHEN a WorkItem is created THEN it fails and no WorkItem is produced")
    public void shouldFailCreation_whenTypeIsNull() {

        Result<WorkItem> result = WorkItem.create(null, TR_REF, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a null trustRelationshipId WHEN a WorkItem is created THEN it fails and no WorkItem is produced")
    public void shouldFailCreation_whenTrReferenceIsNull() {

        Result<WorkItem> result = WorkItem.create(PROCESS_AGGREGATE_METADATA, null, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a null creation instant WHEN a WorkItem is created THEN it fails and no WorkItem is produced")
    public void shouldFailCreation_whenNowIsNull() {

        Result<WorkItem> result = WorkItem.create(PROCESS_AGGREGATE_METADATA, TR_REF, null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a WorkItem WHEN its TR reference type is inspected THEN it is an opaque value and not the trust Id type")
    public void shouldHoldTrReferenceAsOpaqueValue_notTrustId() throws NoSuchFieldException {

        Field trustRelationshipId = WorkItem.class.getDeclaredField("trustRelationshipId");
        assertThat(trustRelationshipId.getType()).isEqualTo(TrustRelationshipRef.class);

        for (Field field : WorkItem.class.getDeclaredFields()) {
            assertThat(field.getType().getName()).isNotEqualTo("io.jans.shibboleth.trust.config.Id");
        }
    }
}
