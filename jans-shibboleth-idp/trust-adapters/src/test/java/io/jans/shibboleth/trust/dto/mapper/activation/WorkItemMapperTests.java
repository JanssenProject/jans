package io.jans.shibboleth.trust.dto.mapper.activation;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.dto.activation.WorkItemView;
import io.jans.shibboleth.trust.shared.Origin;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WorkItemMapperTests {

    private static final UUID TR_REF = UUID.fromString("7f3a9c2e-4b1d-4c8a-9e2f-1a2b3c4d5e6f");
    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Test
    void shouldProjectPendingWorkItemWithoutLease() {

        WorkItem pending = pending();

        WorkItemView view = WorkItemMapper.toView(pending);

        assertThat(view.getId()).isEqualTo(pending.id().value());
        assertThat(view.getType()).isEqualTo(WorkItemType.PROCESS_INDIVIDUAL_METADATA);
        assertThat(view.getTrustRelationshipRef()).isEqualTo(TR_REF);
        assertThat(view.getState()).isEqualTo(WorkItemState.PENDING);
        assertThat(view.getLeaseExpiresAt()).isNull();
    }

    @Test
    void shouldProjectAssignedWorkItemWithLeaseExpiry() {

        Instant expiresAt = NOW.plusSeconds(300);
        WorkItem assigned = pending().claim(worker(), NOW, expiresAt).getValue();

        WorkItemView view = WorkItemMapper.toView(assigned);

        assertThat(view.getState()).isEqualTo(WorkItemState.ASSIGNED);
        assertThat(view.getLeaseExpiresAt()).isEqualTo(expiresAt.toString());
    }

    private static WorkItem pending() {

        return WorkItem.create(
                WorkItemType.PROCESS_INDIVIDUAL_METADATA,
                TrustRelationshipRef.of(TR_REF).getValue(),
                NOW)
            .getValue();
    }

    private static WorkerId worker() {

        return WorkerId.of(Origin.of("worker-1@host")).getValue();
    }
}
