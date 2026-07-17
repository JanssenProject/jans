package io.jans.shibboleth.trust.activation.model;

import io.jans.shibboleth.trust.activation.error.LeaseStillValid;
import io.jans.shibboleth.trust.activation.error.NotLeaseHolder;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.activation.error.WorkItemTransitionNotAllowed;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Origin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.trust.activation.model.WorkItemType.PROCESS_AGGREGATE_METADATA;

@DisplayName("Group 5 — WorkItem State Machine")
public class WorkItemStateMachineTests {

    private static final WorkerId WORKER = WorkerId.of(Origin.of("worker@host")).getValue();
    private static final WorkerId OTHER_WORKER = WorkerId.of(Origin.of("other@host")).getValue();
    private static final TrustRelationshipRef TR_REF = TrustRelationshipRef.of(UUID.randomUUID()).getValue();
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EXPIRES = NOW.plusSeconds(30);

    private static WorkItem pending() {

        return WorkItem.create(PROCESS_AGGREGATE_METADATA, TR_REF, NOW).getValue();
    }

    private static WorkItem assigned() {

        return pending().claim(WORKER, NOW, EXPIRES).getValue();
    }

    private static WorkItem completed() {

        return assigned().complete(NOW).getValue();
    }

    private static WorkItem cancelled() {

        return pending().cancel(NOW).getValue();
    }

    static Stream<WorkItem> terminalWorkItems() {

        return Stream.of(completed(), cancelled());
    }

    static Stream<WorkItem> nonAssignedWorkItems() {

        return Stream.of(pending(), completed(), cancelled());
    }

    static Stream<WorkItem> allStateWorkItems() {

        return Stream.of(pending(), assigned(), completed(), cancelled());
    }

    @Test
    @DisplayName("GIVEN a PENDING WorkItem WHEN a Worker claims it with a granted lease THEN it becomes ASSIGNED holding that lease")
    public void shouldTransitionToAssigned_whenClaimedFromPending() {

        WorkItem assigned = pending().claim(WORKER, NOW, EXPIRES).getValue();

        assertThat(assigned.state()).isEqualTo(WorkItemState.ASSIGNED);
    }

    @Test
    @DisplayName("GIVEN a claimed WorkItem WHEN its lease is inspected THEN the lease is present and names the claiming worker")
    public void shouldHoldClaimingWorkersLease_whenAssigned() {

        WorkItem assigned = pending().claim(WORKER, NOW, EXPIRES).getValue();

        assertThat(assigned.lease().isPresent()).isTrue();
        assertThat(assigned.lease().isHeldBy(WORKER)).isTrue();
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN another claim is attempted THEN it fails and the item is unchanged")
    public void shouldFailClaim_whenAlreadyAssigned() {

        WorkItem item = assigned();

        Result<WorkItem> result = item.claim(WORKER, NOW, EXPIRES);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(WorkItemTransitionNotAllowed.class);
        assertThat(item.state()).isEqualTo(WorkItemState.ASSIGNED);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.activation.model.WorkItemStateMachineTests#terminalWorkItems")
    @DisplayName("GIVEN a terminal WorkItem WHEN a claim is attempted THEN it fails and the item is unchanged")
    public void shouldFailClaim_whenTerminal(WorkItem terminal) {

        WorkItemState before = terminal.state();

        Result<WorkItem> result = terminal.claim(WORKER, NOW, EXPIRES);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(WorkItemTransitionNotAllowed.class);
        assertThat(terminal.state()).isEqualTo(before);
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN its report is applied THEN it becomes COMPLETED which is terminal")
    public void shouldTransitionToCompleted_whenReportApplied() {

        WorkItem completed = assigned().complete(NOW).getValue();

        assertThat(completed.state()).isEqualTo(WorkItemState.COMPLETED);
        assertThat(completed.state().isTerminal()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.activation.model.WorkItemStateMachineTests#nonAssignedWorkItems")
    @DisplayName("GIVEN a WorkItem that is not ASSIGNED WHEN completion is attempted THEN it fails")
    public void shouldFailComplete_whenNotAssigned(WorkItem item) {

        Result<WorkItem> result = item.complete(NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(WorkItemTransitionNotAllowed.class);
    }

    @Test
    @DisplayName("GIVEN a PENDING WorkItem WHEN it is cancelled THEN it becomes CANCELLED which is terminal")
    public void shouldTransitionToCancelled_whenCancelledFromPending() {

        WorkItem cancelled = pending().cancel(NOW).getValue();

        assertThat(cancelled.state()).isEqualTo(WorkItemState.CANCELLED);
        assertThat(cancelled.state().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN it is cancelled THEN it becomes CANCELLED which is terminal")
    public void shouldTransitionToCancelled_whenCancelledFromAssigned() {

        WorkItem cancelled = assigned().cancel(NOW).getValue();

        assertThat(cancelled.state()).isEqualTo(WorkItemState.CANCELLED);
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN it is cancelled THEN its lease is cleared to Lease.NONE")
    public void shouldClearLease_whenCancelledFromAssigned() {

        WorkItem cancelled = assigned().cancel(NOW).getValue();

        assertThat(cancelled.lease().isNone()).isTrue();
    }

    @Test
    @DisplayName("GIVEN a WorkItem already in a terminal state WHEN the same terminal transition is applied again THEN it fails so the item reaches a terminal state at most once")
    public void shouldReachTerminalAtMostOnce() {

        assertThat(completed().complete(NOW).isFailure()).isTrue();
        assertThat(cancelled().cancel(NOW).isFailure()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.activation.model.WorkItemStateMachineTests#allStateWorkItems")
    @DisplayName("GIVEN a WorkItem in any state WHEN its lease is inspected THEN the lease is never Java null")
    public void shouldNeverExposeNullLease_inAnyState(WorkItem item) {

        assertThat(item.lease()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN a PENDING WorkItem WHEN it is claimed with a null instant THEN it fails and no transition occurs")
    public void shouldFailClaim_whenNowIsNull() {

        Result<WorkItem> result = pending().claim(WORKER, null, EXPIRES);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN completion is attempted with a null instant THEN it fails and no transition occurs")
    public void shouldFailComplete_whenNowIsNull() {

        Result<WorkItem> result = assigned().complete(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a PENDING WorkItem WHEN cancellation is attempted with a null instant THEN it fails and no transition occurs")
    public void shouldFailCancel_whenNowIsNull() {

        Result<WorkItem> result = pending().cancel(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN the lease holder heartbeats THEN the lease expiry is extended and it remains ASSIGNED")
    public void shouldRenewLeaseAndRemainAssigned_whenHolderHeartbeats() {

        Instant laterNow = NOW.plusSeconds(10);
        Instant newExpires = laterNow.plusSeconds(30);

        WorkItem beat = assigned().heartbeat(WORKER, laterNow, newExpires).getValue();

        assertThat(beat.state()).isEqualTo(WorkItemState.ASSIGNED);
        assertThat(beat.lease().expiresAt()).isEqualTo(newExpires);
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN a worker that is not the lease holder heartbeats THEN it is rejected and the item is unchanged")
    public void shouldRejectHeartbeat_whenNotLeaseHolder() {

        WorkItem item = assigned();

        Result<WorkItem> result = item.heartbeat(OTHER_WORKER, NOW, EXPIRES);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(NotLeaseHolder.class);
        assertThat(item.lease().isHeldBy(WORKER)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.activation.model.WorkItemStateMachineTests#nonAssignedWorkItems")
    @DisplayName("GIVEN a WorkItem that is not ASSIGNED WHEN a heartbeat is attempted THEN it fails")
    public void shouldRejectHeartbeat_whenNotAssigned(WorkItem item) {

        Result<WorkItem> result = item.heartbeat(WORKER, NOW, EXPIRES);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(WorkItemTransitionNotAllowed.class);
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem whose lease has expired WHEN it is reclaimed THEN it returns to PENDING with lease Lease.NONE")
    public void shouldReturnToPendingAndClearLease_whenReclaimed() {

        WorkItem reclaimed = assigned().reclaim(EXPIRES.plusSeconds(1)).getValue();

        assertThat(reclaimed.state()).isEqualTo(WorkItemState.PENDING);
        assertThat(reclaimed.lease().isNone()).isTrue();
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN it is reclaimed THEN its WorkItemId is unchanged so it remains the same episode")
    public void shouldPreserveIdentityAndEpisode_whenReclaimed() {

        WorkItem assigned = assigned();

        WorkItem reclaimed = assigned.reclaim(EXPIRES.plusSeconds(1)).getValue();

        assertThat(reclaimed.id()).isEqualTo(assigned.id());
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem whose lease is not expired WHEN reclaim is attempted THEN it is rejected and the item stays ASSIGNED")
    public void shouldRejectReclaim_whenLeaseStillValid() {

        WorkItem item = assigned();

        Result<WorkItem> result = item.reclaim(NOW.plusSeconds(10));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(LeaseStillValid.class);
        assertThat(item.state()).isEqualTo(WorkItemState.ASSIGNED);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.activation.model.WorkItemStateMachineTests#terminalWorkItems")
    @DisplayName("GIVEN a terminal WorkItem WHEN any transition is attempted THEN it fails and the item is unchanged")
    public void shouldRejectAnyTransition_whenTerminal(WorkItem terminal) {

        assertThat(terminal.claim(WORKER, NOW, EXPIRES).isFailure()).isTrue();
        assertThat(terminal.heartbeat(WORKER, NOW, EXPIRES).isFailure()).isTrue();
        assertThat(terminal.complete(NOW).isFailure()).isTrue();
        assertThat(terminal.reclaim(NOW).isFailure()).isTrue();
        assertThat(terminal.cancel(NOW).isFailure()).isTrue();
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN a heartbeat is attempted with a null instant THEN it fails and no transition occurs")
    public void shouldFailHeartbeat_whenNowIsNull() {

        Result<WorkItem> result = assigned().heartbeat(WORKER, null, EXPIRES);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN reclaim is attempted with a null instant THEN it fails and no transition occurs")
    public void shouldFailReclaim_whenNowIsNull() {

        Result<WorkItem> result = assigned().reclaim(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }
}
