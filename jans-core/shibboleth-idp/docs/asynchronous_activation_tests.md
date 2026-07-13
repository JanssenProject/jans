# TrustRelationship Asynchronous Activation — Test Plan

Companion to [`asynchronous_activation.md`](./asynchronous_activation.md). This plan drives an
**incremental, dependency-ordered, test-first** implementation of the **Activation Coordination** bounded
context (`io.jans.shibboleth.activation`): each group relies only on behaviour proven by an **earlier**
group, so the suite can be implemented and green-barred top-to-bottom without forward references.

Unlike the `TrustRelationship` plan, this context is **greenfield** — the classes are stubs today, so
**every coverage box starts unticked**. Tick a box (`- [x]`) once a test in the corresponding
`activation` test class exercises the case. This document is the guardrail the code is written against.

---

## Conventions

- **Each case = a method name + a `@DisplayName`.** The `####` heading holds the case ID and the generated
  method name; the single line beneath the checkbox is a `GIVEN … WHEN … THEN …` sentence written to be
  **pasted verbatim into the JUnit 5 `@DisplayName`** — plain text, ALL-CAPS keywords, no markdown.
- **Naming.** `should<Outcome>_when<Condition>` (camelCase, `should` prefix, `_when` separating the
  condition; the `_when…` clause is dropped when there is no meaningful condition).
- **Result-oriented, not exception-oriented.** Every mutating operation returns a result value, asserted
  with `isSuccess()` + a fluent assert, or `isFailure()` + the concrete error. No test asserts a thrown
  exception for a domain-rule violation. **The result/error type is activation-owned and must not be
  `TrustResult`** (§3 — the coordination context does not depend on trust infrastructure); the concrete
  type is an open choice (see *Open implementation choices*).
- **Structure.** `Group → Subgroup → Case`, IDs of the form `A<group>.<subgroup>.<case>` (`A` = Activation).
- **Time is injected.** Lease expiry and Worker liveness are time-relative. Every such case drives a
  **controllable `TimeSource`** (a fake clock) — no test reads the wall clock. This is what makes the
  expiry/liveness/fencing cases deterministic.
- **Immutability.** Value objects (`WorkItemId`, `Lease`) are immutable; a `Lease` renewal yields a **new**
  `Lease`. `WorkItem` transitions yield the item in its new state and never mutate a caller's prior
  reference (immutability preferred, matching the `TrustRelationship` aggregate).
- **No-null lease.** A `WorkItem`'s `lease` is **never** Java `null`; absence is the sentinel `Lease.NONE`
  (§4.1). Asserted with `isNone()` / `isPresent()`, never `== null`.
- **Fixtures & asserts.** New harness (see *Test harness & fixtures to build*): `WorkItemFixtures`,
  `LeaseFixtures`, `WorkerFixtures`, `WorkItemAssert`, an in-memory `WorkItemStore` with compare-and-set
  semantics, a fake `TimeSource`, and a `FinalizeActivationPort` test double standing in for the TR side.
- **Coverage checkbox.** Each case carries `- [ ] covered by test`; tick it once a real test covers it.

### Definitions (used verbatim across cases)

| Term | Meaning |
|------|---------|
| **Episode** | One `ACTIVATING` cycle of a TR. Exactly **one** `WorkItem` per episode; its `WorkItemId` is the fence token (§7b). |
| **Current WorkItem (for a TR)** | The single `WorkItem` the orchestrator's *"current WorkItem for this TR"* pointer names for that TR's live episode. |
| **Alive Worker** | `now - lastHeartbeatAt ≤ heartbeatTtl` (boundary inclusive). |
| **Expired lease** | `now > lease.expiresAt` (boundary at equality is **not** expired). |
| **Lease holder** | The `Worker` named by a `WorkItem`'s current (non-`NONE`) `Lease`. |
| **Stale report** | A report naming a `WorkItem` that is not the TR's current one, or from a Worker that is not the current lease holder. |
| **Terminal state** | `COMPLETED` or `CANCELLED`. `PENDING` / `ASSIGNED` are non-terminal. |

### Dependency order (why the groups are numbered this way)

```
G1  identities (WorkItemId, WorkerId) ...... nothing (foundation)
G2  WorkItem enums (Type, State) ........... nothing
G3  Lease + Lease.NONE null object ......... G1 (workerId), TimeSource
G4  WorkItem creation & invariants ......... G1, G2, G3
G5  WorkItem state machine ................. G4              (claim/heartbeat/report/reclaim/cancel)
G6  Worker entity & liveness ............... G1, TimeSource
G7  Orchestrator: demand -> WorkItem ....... G4, G5          (current-WorkItem pointer; the "queue")
G8  Orchestrator: claim / assignment ....... G5, G6, G7      (atomic claim; mutual exclusion)
G9  Orchestrator: heartbeat & reclaim ...... G8, TimeSource  (lease expiry -> reclaim to PENDING)
G10 Orchestrator: report & fencing ......... G8, G9, FinalizeActivationPort
G11 Orchestrator: cancellation ............. G7, G10
G12 Cross-cutting: delivery, lifecycle, boundary .. all of the above
```

A test that *exercises* a state/behaviour never appears before the group that first *produces* it.

---

## Group 1 — Identities

*Foundation. `WorkItemId` and `WorkerId` are activation-owned; neither reuses the trust `Id` (§3).*

### 1.1 `WorkItemId`

#### A1.1.1 · `shouldGenerateDistinctIds_whenGeneratedRepeatedly`

- [x] covered by test

GIVEN the WorkItemId factory WHEN generate() is called repeatedly THEN each call yields a distinct identity

#### A1.1.2 · `shouldBeEqual_whenSameUnderlyingValue`

- [x] covered by test

GIVEN two WorkItemIds built from the same underlying value WHEN they are compared THEN they are equal and share the same hashCode

#### A1.1.3 · `shouldNotBeEqual_whenDifferentValues`

- [x] covered by test

GIVEN two WorkItemIds built from different values WHEN they are compared THEN they are not equal

### 1.2 `WorkerId` (unified with `Origin`)

#### A1.2.1 · `shouldCarryOriginIdentity_whenBuiltFromOrigin`

- [x] covered by test

GIVEN an Origin of the form instance-at-host WHEN a WorkerId is formed from it THEN the WorkerId carries that origin as its identity

#### A1.2.2 · `shouldBeEqual_whenSameOrigin`

- [x] covered by test

GIVEN two WorkerIds built from the same Origin WHEN they are compared THEN they are equal and share the same hashCode

#### A1.2.3 · `shouldDistinguishWorkers_whenDifferentOrigin`

- [x] covered by test

GIVEN two WorkerIds built from different Origins WHEN they are compared THEN they are not equal

---

## Group 2 — WorkItem Enums

*Foundation. Locks the value sets the state machine and type selection depend on.*

### 2.1 `WorkItemType`

#### A2.1.1 · `shouldExposeAggregateAndIndividualTypes`

- [x] covered by test

GIVEN WorkItemType WHEN its values are inspected THEN both PROCESS_AGGREGATE_METADATA and PROCESS_INDIVIDUAL_METADATA are present

### 2.2 `WorkItemState`

#### A2.2.1 · `shouldExposeAllFourStates`

- [x] covered by test

GIVEN WorkItemState WHEN its values are inspected THEN PENDING and ASSIGNED and COMPLETED and CANCELLED are all present

#### A2.2.2 · `shouldClassifyTerminalStates`

- [x] covered by test

GIVEN the WorkItem states WHEN terminality is queried THEN COMPLETED and CANCELLED are terminal while PENDING and ASSIGNED are not

---

## Group 3 — Lease & the `Lease.NONE` Null Object

*Depends on G1 (workerId) and the TimeSource. Establishes lease semantics and the no-null modelling (§4.1).*

### 3.1 Real lease construction & accessors

#### A3.1.1 · `shouldExposeHolderAndWindow_whenLeaseGranted`

- [ ] covered by test

GIVEN a workerId and grant and expiry instants WHEN a Lease is granted THEN it exposes that workerId and grantedAt and expiresAt

#### A3.1.2 · `shouldReturnNewLeaseAndLeaveOriginalUnchanged_whenRenewed`

- [ ] covered by test

GIVEN a granted Lease WHEN it is renewed with a later expiry THEN a new Lease instance is produced and the original is unchanged

### 3.2 Expiry (time-relative)

#### A3.2.1 · `shouldBeExpired_whenNowAfterExpiresAt`

- [ ] covered by test

GIVEN a Lease and a TimeSource advanced past its expiresAt WHEN expiry is checked THEN the Lease is expired

#### A3.2.2 · `shouldNotBeExpired_whenNowWithinWindow`

- [ ] covered by test

GIVEN a Lease and a TimeSource before its expiresAt WHEN expiry is checked THEN the Lease is not expired

#### A3.2.3 · `shouldNotBeExpired_whenNowEqualsExpiresAt`

- [ ] covered by test

GIVEN a Lease and a TimeSource exactly at its expiresAt WHEN expiry is checked THEN the Lease is not expired because the boundary is inclusive

### 3.3 The `Lease.NONE` null object

#### A3.3.1 · `shouldReportIsNone_forSentinel`

- [ ] covered by test

GIVEN the Lease.NONE sentinel WHEN it is queried THEN isNone() is true and isPresent() is false

#### A3.3.2 · `shouldReportIsPresent_forRealLease`

- [ ] covered by test

GIVEN a granted Lease WHEN it is queried THEN isPresent() is true and isNone() is false

#### A3.3.3 · `shouldNotExposeHolder_whenNone`

- [ ] covered by test

GIVEN the Lease.NONE sentinel WHEN its holder is requested THEN no worker is returned and the caller is expected to check isNone() first

---

## Group 4 — WorkItem Creation & Invariants

*Depends on G1, G2, G3. A new WorkItem is the start of an episode.*

### 4.1 Creation

#### A4.1.1 · `shouldCreatePendingWorkItem_whenCreatedForTr`

- [ ] covered by test

GIVEN a WorkItemType and an opaque trustRelationshipId WHEN a WorkItem is created THEN it is PENDING with a fresh WorkItemId and that type and that TR reference

#### A4.1.2 · `shouldHaveNoLease_whenPending`

- [ ] covered by test

GIVEN a freshly created WorkItem WHEN its lease is inspected THEN the lease is Lease.NONE

#### A4.1.3 · `shouldStampCreatedAt_whenCreated`

- [ ] covered by test

GIVEN a TimeSource WHEN a WorkItem is created THEN its createdAt and lastTransitionAt are stamped from that TimeSource

#### A4.1.4 · `shouldReferenceExactlyOneTr_whenCreated`

- [ ] covered by test

GIVEN a WorkItem WHEN its TR reference is inspected THEN it references exactly one trustRelationshipId and never null

#### A4.1.5 · `shouldFailCreation_whenTypeIsNull`

- [ ] covered by test

GIVEN a null WorkItemType WHEN a WorkItem is created THEN it fails and no WorkItem is produced

#### A4.1.6 · `shouldFailCreation_whenTrReferenceIsNull`

- [ ] covered by test

GIVEN a null trustRelationshipId WHEN a WorkItem is created THEN it fails and no WorkItem is produced

### 4.2 Structural invariants

#### A4.2.1 · `shouldHoldTrReferenceAsOpaqueValue_notTrustId`

- [ ] covered by test

GIVEN a WorkItem WHEN its TR reference type is inspected THEN it is an opaque value and not the trust Id type
*Architecture / dependency guard for §3 — enforced by an import-scan or ArchUnit rule, not runtime behaviour.*

#### A4.2.2 · `shouldNeverExposeNullLease_inAnyState`

- [ ] covered by test

GIVEN a WorkItem in any state WHEN its lease is inspected THEN the lease is never Java null
*Parametrized over PENDING / ASSIGNED / COMPLETED / CANCELLED.*

---

## Group 5 — WorkItem State Machine

*Depends on G4. Covers every edge of the §5.1 diagram: claim, heartbeat self-loop, report, reclaim, cancel.*

### 5.1 `claim` (PENDING → ASSIGNED)

#### A5.1.1 · `shouldTransitionToAssigned_whenClaimedFromPending`

- [ ] covered by test

GIVEN a PENDING WorkItem WHEN a Worker claims it with a granted lease THEN it becomes ASSIGNED holding that lease

#### A5.1.2 · `shouldHoldClaimingWorkersLease_whenAssigned`

- [ ] covered by test

GIVEN a claimed WorkItem WHEN its lease is inspected THEN the lease is present and names the claiming worker

#### A5.1.3 · `shouldFailClaim_whenAlreadyAssigned`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem WHEN another claim is attempted THEN it fails and the item is unchanged

#### A5.1.4 · `shouldFailClaim_whenTerminal`

- [ ] covered by test

GIVEN a terminal WorkItem WHEN a claim is attempted THEN it fails and the item is unchanged
*Parametrized over COMPLETED / CANCELLED.*

### 5.2 `heartbeat` / renew (ASSIGNED self-loop)

#### A5.2.1 · `shouldRenewLeaseAndRemainAssigned_whenHolderHeartbeats`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem WHEN the lease holder heartbeats THEN the lease expiry is extended and it remains ASSIGNED

#### A5.2.2 · `shouldRejectHeartbeat_whenNotLeaseHolder`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem WHEN a worker that is not the lease holder heartbeats THEN it is rejected and the item is unchanged

#### A5.2.3 · `shouldRejectHeartbeat_whenNotAssigned`

- [ ] covered by test

GIVEN a WorkItem that is not ASSIGNED WHEN a heartbeat is attempted THEN it fails
*Parametrized over PENDING / COMPLETED / CANCELLED.*

### 5.3 `report` / complete (ASSIGNED → COMPLETED)

#### A5.3.1 · `shouldTransitionToCompleted_whenReportApplied`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem WHEN its report is applied THEN it becomes COMPLETED which is terminal

#### A5.3.2 · `shouldFailComplete_whenNotAssigned`

- [ ] covered by test

GIVEN a WorkItem that is not ASSIGNED WHEN completion is attempted THEN it fails
*Parametrized over PENDING / COMPLETED / CANCELLED.*

### 5.4 reclaim (ASSIGNED → PENDING on lease expiry)

#### A5.4.1 · `shouldReturnToPendingAndClearLease_whenReclaimed`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem whose lease has expired WHEN it is reclaimed THEN it returns to PENDING with lease Lease.NONE

#### A5.4.2 · `shouldPreserveIdentityAndEpisode_whenReclaimed`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem WHEN it is reclaimed THEN its WorkItemId is unchanged so it remains the same episode

#### A5.4.3 · `shouldRejectReclaim_whenLeaseStillValid`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem whose lease is not expired WHEN reclaim is attempted THEN it is rejected and the item stays ASSIGNED

### 5.5 cancel (PENDING → CANCELLED, ASSIGNED → CANCELLED)

#### A5.5.1 · `shouldTransitionToCancelled_whenCancelledFromPending`

- [ ] covered by test

GIVEN a PENDING WorkItem WHEN it is cancelled THEN it becomes CANCELLED which is terminal

#### A5.5.2 · `shouldTransitionToCancelled_whenCancelledFromAssigned`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem WHEN it is cancelled THEN it becomes CANCELLED which is terminal

#### A5.5.3 · `shouldClearLease_whenCancelledFromAssigned`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem WHEN it is cancelled THEN its lease is cleared to Lease.NONE

### 5.6 Terminal-once & invalid transitions

#### A5.6.1 · `shouldRejectAnyTransition_whenTerminal`

- [ ] covered by test

GIVEN a terminal WorkItem WHEN any transition is attempted THEN it fails and the item is unchanged
*Parametrized over the cross-product of COMPLETED / CANCELLED and claim / heartbeat / complete / reclaim / cancel.*

#### A5.6.2 · `shouldReachTerminalAtMostOnce`

- [ ] covered by test

GIVEN a WorkItem already in a terminal state WHEN the same terminal transition is applied again THEN it fails so the item reaches a terminal state at most once

---

## Group 6 — Worker Entity & Liveness

*Depends on G1 and the TimeSource. Liveness is derived, not stored (§4.2, §5.2).*

### 6.1 Registration & heartbeat

#### A6.1.1 · `shouldRegisterWithIdentityAndTimestamps`

- [ ] covered by test

GIVEN a WorkerId and a TimeSource WHEN a Worker registers THEN it carries that id and its registeredAt and lastHeartbeatAt are stamped from the TimeSource

#### A6.1.2 · `shouldAdvanceLastHeartbeat_whenHeartbeatRecorded`

- [ ] covered by test

GIVEN a registered Worker WHEN a heartbeat is recorded at a later instant THEN its lastHeartbeatAt advances to that instant

### 6.2 Liveness derivation

#### A6.2.1 · `shouldBeAlive_whenWithinHeartbeatTtl`

- [ ] covered by test

GIVEN a Worker whose last heartbeat is within the heartbeat TTL WHEN liveness is evaluated THEN it is alive

#### A6.2.2 · `shouldBeExpired_whenBeyondHeartbeatTtl`

- [ ] covered by test

GIVEN a Worker whose last heartbeat is older than the heartbeat TTL WHEN liveness is evaluated THEN it is expired

#### A6.2.3 · `shouldBeAlive_whenExactlyAtTtlBoundary`

- [ ] covered by test

GIVEN a Worker whose last heartbeat is exactly the heartbeat TTL ago WHEN liveness is evaluated THEN it is alive because the boundary is inclusive

#### A6.2.4 · `shouldUseConfiguredTtl_notHardcoded`

- [ ] covered by test

GIVEN two different configured heartbeat TTLs WHEN the same heartbeat age is evaluated against each THEN liveness follows the configured value rather than a hard-coded one
*Guards §10 "Lease TTL / heartbeat interval externally configured".*

---

## Group 7 — Orchestrator: Activation Demand → WorkItem

*Depends on G4, G5. Turns an `ActivationRequested` event into a PENDING WorkItem and tracks the current-WorkItem pointer (the "queue").*

### 7.1 `ActivationRequested` → WorkItem

#### A7.1.1 · `shouldCreatePendingWorkItem_whenActivationRequested`

- [ ] covered by test

GIVEN an ActivationRequested for a TR WHEN the orchestrator handles it THEN it creates a PENDING WorkItem for that trustRelationshipId

#### A7.1.2 · `shouldSelectAggregateType_whenTrMetadataIsAggregate`

- [ ] covered by test

GIVEN an ActivationRequested for a TR whose metadata source is an aggregate WHEN the WorkItem is created THEN its type is PROCESS_AGGREGATE_METADATA

#### A7.1.3 · `shouldSelectIndividualType_whenTrMetadataIsIndividual`

- [ ] covered by test

GIVEN an ActivationRequested for a TR whose metadata is a single entity WHEN the WorkItem is created THEN its type is PROCESS_INDIVIDUAL_METADATA

#### A7.1.4 · `shouldSetCreatedItemAsCurrentForTr`

- [ ] covered by test

GIVEN an ActivationRequested WHEN the WorkItem is created THEN it becomes the TR's current WorkItem

### 7.2 Episode identity & the current pointer

#### A7.2.1 · `shouldPointToNewWorkItem_whenNewEpisodeRequested`

- [ ] covered by test

GIVEN a TR that already had an episode WHEN a further ActivationRequested arrives THEN a new WorkItem with a new WorkItemId is created and becomes current

#### A7.2.2 · `shouldTreatPriorWorkItemAsNotCurrent_afterNewEpisode`

- [ ] covered by test

GIVEN a new episode has started for a TR WHEN the previous episode's WorkItem is checked against the current pointer THEN it is no longer current

---

## Group 8 — Orchestrator: Claim & Assignment

*Depends on G5, G6, G7. Atomic claim gives at-most-one lease holder (§4.4, §7, §8 step 3).*

### 8.1 Claiming

#### A8.1.1 · `shouldAssignItemToAliveWorker_whenClaimed`

- [ ] covered by test

GIVEN a PENDING WorkItem and an alive Worker WHEN the Worker claims it THEN the item becomes ASSIGNED holding a lease for that Worker

#### A8.1.2 · `shouldRejectClaim_whenWorkerNotAlive`

- [ ] covered by test

GIVEN a PENDING WorkItem and an expired Worker WHEN the Worker attempts to claim it THEN the claim is rejected and the item stays PENDING

#### A8.1.3 · `shouldEmitWorkItemAssigned_whenClaimSucceeds`

- [ ] covered by test

GIVEN a successful claim WHEN it completes THEN a WorkItemAssigned event is emitted

### 8.2 Atomicity / mutual exclusion

#### A8.2.1 · `shouldLetOnlyOneWorkerWin_whenTwoClaimSameItem`

- [ ] covered by test

GIVEN two alive Workers attempting to claim the same PENDING WorkItem WHEN both perform the compare-and-set claim THEN exactly one succeeds and the other is rejected

#### A8.2.2 · `shouldKeepAtMostOneActiveLease_whenAssigned`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem WHEN its leases are inspected THEN it has at most one active lease

### 8.3 One Worker, many items

#### A8.3.1 · `shouldAllowWorkerToHoldManyItems`

- [ ] covered by test

GIVEN one alive Worker WHEN it claims several distinct PENDING WorkItems THEN it holds all of them concurrently

#### A8.3.2 · `shouldNameSingleWorkerPerItem`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem WHEN its lease is inspected THEN it names exactly one workerId

---

## Group 9 — Orchestrator: Heartbeat & Lease-Expiry Reclaim

*Depends on G8 and the TimeSource. Crash / silence handling via lease expiry → reclaim (§5.2, §7a, §9).*

### 9.1 Heartbeat renewal

#### A9.1.1 · `shouldRenewLease_whenHolderHeartbeats`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem WHEN its lease holder heartbeats before expiry THEN the lease is renewed and the item stays ASSIGNED

### 9.2 Expiry → reclaim

#### A9.2.1 · `shouldReclaimToPending_whenLeaseExpires`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem whose holder went silent past the lease TTL WHEN the orchestrator sweeps THEN the item is reclaimed to PENDING with lease Lease.NONE and the same WorkItemId

#### A9.2.2 · `shouldEmitLeaseExpired_whenReclaimed`

- [ ] covered by test

GIVEN a WorkItem reclaimed due to lease expiry WHEN the reclaim completes THEN a WorkItemLeaseExpired event is emitted

#### A9.2.3 · `shouldReassignAfterReclaim_whenAnotherWorkerClaims`

- [ ] covered by test

GIVEN a reclaimed PENDING WorkItem WHEN another alive Worker claims it THEN it becomes ASSIGNED again under the same WorkItemId and same episode

#### A9.2.4 · `shouldReclaimAllHeldItems_whenWorkerExpires`

- [ ] covered by test

GIVEN a Worker holding several WorkItems that then goes silent past the TTL WHEN the orchestrator sweeps THEN every one of its items is reclaimed to PENDING

---

## Group 10 — Orchestrator: Reporting & Fencing

*Depends on G8, G9 and the FinalizeActivationPort. The fence lives here, not in the TR aggregate (§7, §8 step 6).*

### 10.1 Authoritative report

#### A10.1.1 · `shouldFinalizeAndComplete_whenReportForCurrentItemByHolder`

- [ ] covered by test

GIVEN a report for the TR's current WorkItem from its lease holder WHEN the orchestrator processes it THEN it invokes finalizeActivation with the diagnostics and marks the WorkItem COMPLETED

#### A10.1.2 · `shouldResolveOpaqueReferenceToTrustId_atBoundary`

- [ ] covered by test

GIVEN an authoritative report carrying an opaque trustRelationshipId WHEN the orchestrator finalizes THEN it resolves that opaque value to the trust Id only at this boundary before calling finalizeActivation
*Guards §3 / §8 step 6 — the only place the opaque reference is mapped to the trust Id.*

### 10.2 Cross-episode fence (WorkItemId)

#### A10.2.1 · `shouldDropReport_whenForPriorEpisodeItem`

- [ ] covered by test

GIVEN a report naming a WorkItem that is no longer the TR's current one WHEN the orchestrator processes it THEN the report is dropped and finalizeActivation is not called

#### A10.2.2 · `shouldNotFinalizeSecondEpisode_whenSlowFirstEpisodeReportArrives`

- [ ] covered by test

GIVEN a TR that cycled into a second episode WHEN a slow report from the first episode arrives THEN it is discarded by the identity fence and does not finalize the second episode

### 10.3 Within-episode reassignment fence (lease ownership)

#### A10.3.1 · `shouldDropReport_whenReporterNoLongerHoldsLease`

- [ ] covered by test

GIVEN a report from a Worker whose lease expired and was reassigned WHEN the orchestrator processes it THEN it is dropped by the lease-ownership check

#### A10.3.2 · `shouldNotDoubleFinalize_whenLateIntraEpisodeReportSlipsThrough`

- [ ] covered by test

GIVEN the current WorkItem was already finalized WHEN a late intra-episode report is applied anyway THEN no second finalize takes effect because the TR already left ACTIVATING
*Documents the §7 "harmless" argument: the aggregate's ACTIVATING-only guard is the backstop.*

### 10.4 Terminal / duplicate reports

#### A10.4.1 · `shouldDropReport_whenWorkItemAlreadyCompleted`

- [ ] covered by test

GIVEN a report naming an already COMPLETED WorkItem WHEN the orchestrator processes it THEN it is dropped

#### A10.4.2 · `shouldDropReport_whenWorkItemCancelled`

- [ ] covered by test

GIVEN a report naming a CANCELLED WorkItem WHEN the orchestrator processes it THEN it is dropped

#### A10.4.3 · `shouldFinalizeEffectivelyOnce_whenDuplicateReportsArrive`

- [ ] covered by test

GIVEN duplicate reports for the same current WorkItem WHEN the orchestrator processes them THEN finalizeActivation takes effect exactly once

### 10.5 NO_DATA outcome

#### A10.5.1 · `shouldNotComplete_whenReportIsNoData`

- [ ] covered by test

GIVEN a report carrying NO_DATA WHEN the orchestrator processes it THEN finalizeActivation leaves the TR ACTIVATING and the WorkItem is not marked COMPLETED

#### A10.5.2 · `shouldKeepItemWorkable_whenNoData`

- [ ] covered by test

GIVEN a NO_DATA report was processed WHEN the WorkItem is inspected THEN it is still workable rather than terminal

---

## Group 11 — Orchestrator: Cancellation

*Depends on G7, G10. Cancellation is not a special case — the WorkItem simply stops being current (§9).*

#### A11.1.1 · `shouldCancelCurrentWorkItem_whenActivationCancelled`

- [ ] covered by test

GIVEN a TR that left ACTIVATING via cancelActivation WHEN ActivationCancelled is handled THEN the current WorkItem is marked CANCELLED and cleared as current

#### A11.1.2 · `shouldDiscardLateReport_afterCancellation`

- [ ] covered by test

GIVEN a WorkItem cancelled after a Worker was busy WHEN that Worker's eventual report arrives THEN it is discarded by the identity fence

#### A11.1.3 · `shouldStartFreshEpisode_whenActivatedAgainAfterCancel`

- [ ] covered by test

GIVEN a cancelled activation WHEN the TR is activated again THEN a new WorkItem with a new WorkItemId begins a fresh episode

---

## Group 12 — Cross-Cutting: Delivery, Lifecycle, Boundary

*Depends on everything above. System-wide properties once the pieces compose.*

### 12.1 Delivery semantics

#### A12.1.1 · `shouldProcessAtLeastOnce_whenWorkerRetried`

- [ ] covered by test

GIVEN a WorkItem whose first holder went silent WHEN it is reclaimed and reassigned THEN the same episode is processed more than once which is at-least-once delivery

#### A12.1.2 · `shouldFinalizeEffectivelyOnce_despiteRetries`

- [ ] covered by test

GIVEN at-least-once processing of an episode WHEN more than one Worker completes work THEN finalizeActivation takes effect exactly once

### 12.2 Full lifecycle happy paths

#### A12.2.1 · `shouldWalkFullActivationFlow_whenReportedSuccessfully`

- [ ] covered by test

GIVEN an ActivationRequested WHEN the WorkItem is created and claimed and heartbeated and reported successfully THEN it ends COMPLETED and finalizeActivation was invoked exactly once with success

#### A12.2.2 · `shouldReclaimThenComplete_whenFirstWorkerCrashes`

- [ ] covered by test

GIVEN an ASSIGNED WorkItem whose first Worker crashes WHEN the lease expires and a second Worker claims and reports THEN the item ends COMPLETED with a single finalize

#### A12.2.3 · `shouldReturnTrToReadyThenRetry_whenActivationFails`

- [ ] covered by test

GIVEN a reported failure for the current WorkItem WHEN finalize drives the TR to READY and a later episode succeeds THEN the second episode reaches a successful finalize

### 12.3 Boundary / architecture

#### A12.3.1 · `shouldNotDependOnTrustContext_fromActivationDomain`

- [ ] covered by test

GIVEN the activation model and workers packages WHEN their imports are scanned THEN they depend on no trust-context type except the ActivationDiagnostics finalize contract
*Architecture / dependency guard for §3 (import-scan or ArchUnit). The only permitted trust-side reference is the `ActivationDiagnostics` finalize contract (and its `ActivationStatus`); `Origin` lives in the shared kernel `io.jans.shibboleth.shared`, not the trust context.*

#### A12.3.2 · `shouldReferenceTrOnlyByOpaqueValue`

- [ ] covered by test

GIVEN the activation domain WHEN its TR references are inspected THEN they are opaque values and the trust Id type appears nowhere in the domain

---

## Coverage traceability (spec → tests)

| Spec item (source doc) | Covered by |
|------------------------|------------|
| §2 ubiquitous language — `WorkItemId` / `WorkerId` | A1.\* |
| §2 `WorkItemType` (aggregate + individual) | A2.1.1, A7.1.2, A7.1.3 |
| §3 boundary — TR referenced by opaque value | A4.2.1, A10.1.2, A12.3.\* |
| §3 shared contract — finalize via `ActivationDiagnostics` only | A10.1.1, A12.3.1 |
| §4.1 `WorkItem` fields / creation | A4.1.\* |
| §4.1 `Lease.NONE` null-object (no null / no Optional) | A3.3.\*, A4.1.2, A4.2.2 |
| §4.2 `Worker` liveness (derived) | A6.\* |
| §4.3 `Lease` value object | A3.1.\*, A3.2.\* |
| §4.4 invariant — one TR, ≤ one active lease | A4.1.4, A8.2.2 |
| §4.4 invariant — many items per Worker, one workerId per item | A8.3.\* |
| §4.4 invariant — report only if current WorkItem | A10.2.\*, A10.4.\* |
| §4.4 invariant — terminal at most once | A5.6.\* |
| §5.1 state machine (claim / heartbeat / report / reclaim / cancel) | A5.\* |
| §5.2 Worker liveness / reclaim of held items | A6.2.\*, A9.2.4 |
| §5.3 coordination drives TR via finalize | A10.1.1, A12.2.\* |
| §6 events — `ActivationRequested` | A7.1.\* |
| §6 events — `WorkItemAssigned` | A8.1.3 |
| §6 events — `WorkItemLeaseExpired` | A9.2.2 |
| §6 events — `ActivationReported` | A10.1.1 |
| §6 events — `ActivationCancelled` | A11.1.1 |
| §7a lease ownership / at-least-once | A9.\*, A12.1.1 |
| §7b cross-episode fence (identity) | A10.2.\* |
| §7b within-episode fence (lease ownership) | A10.3.\* |
| §7 atomic claim / mutual exclusion | A8.2.1 |
| §8 operation flow (end to end) | A12.2.1 |
| §8 step 6 opaque-reference resolution | A10.1.2 |
| §9 worker crash → reclaim → reassign | A9.2.\*, A12.2.2 |
| §9 slow report after reassignment (harmless) | A10.3.\* |
| §9 duplicate / late report dropped | A10.4.\* |
| §9 cancel while busy | A11.1.\* |
| §9 NO_DATA outcome | A10.5.\* |
| §10 configured lease TTL / heartbeat interval | A6.2.4 |
| §10 delivery semantics (at-least-once + effectively-once) | A12.1.\* |

---

## Test harness & fixtures to build

These support the suite and do not exist yet:

- **`TimeSource`** — an injectable clock (a fake advancing on demand). Every lease-expiry and liveness case
  drives it; no test touches the wall clock. This is the single most load-bearing piece of harness.
- **In-memory `WorkItemStore`** — holds WorkItems by `WorkItemId`, tracks the *"current WorkItem for this
  TR"* pointer, and supports an **atomic compare-and-set** claim (so A8.2.1 can assert mutual exclusion).
- **`FinalizeActivationPort` test double** — stands in for the TR side; records `finalizeActivation`
  invocations (count, diagnostics, resolved trust `Id`) and can be scripted to return success / failure /
  no-data, and to simulate "already left ACTIVATING" for A10.3.2.
- **`WorkItemFixtures` / `LeaseFixtures` / `WorkerFixtures`** — sample WorkItems per state, granted and
  expired leases, alive and expired Workers, all parameterized by the `TimeSource`.
- **`WorkItemAssert`** — fluent assertions: `isPending()`, `isAssigned()`, `isCompleted()`, `isCancelled()`,
  `hasNoLease()`, `hasLeaseHeldBy(workerId)`, `hasId(workItemId)`.
- **Event recorder** — captures emitted domain events (`WorkItemAssigned`, `WorkItemLeaseExpired`,
  `ActivationReported`, `ActivationCancelled`) for the §6 assertions.

---

## Open implementation choices surfaced by this plan

Decide these before/while implementing; each affects how the cases are written:

1. **Result / error type.** The activation context is result-oriented but must not use `TrustResult` (§3).
   Options: reuse the neutral `io.jans.common.Result` directly, or introduce an activation-local
   `ActivationResult` + error hierarchy. (Affects the assertion helpers used everywhere.)
2. **`WorkItem` mutability.** Immutable transitions (return a new `WorkItem`, matching the TR aggregate) vs
   in-place state mutation guarded by the store's atomic claim. The plan is written state-outcome-first so
   it holds either way, but the assertion style (new instance vs same instance) depends on this.
3. **Where fencing state lives.** The *"current WorkItem for this TR"* pointer and lease-holder check are
   orchestrator/store responsibilities (§7). The store contract (atomic claim, pointer, expiry sweep) is
   the seam the tests mock — pin it before G7.
4. **Event delivery mechanism.** In-process domain events vs an outbox — out of scope for the domain tests
   (§11), which only assert that events are *emitted*; the transport is covered elsewhere.
5. **Reclaim trigger.** A periodic sweep vs lazy on-access expiry check. The plan drives it via an explicit
   orchestrator sweep entry point so it stays deterministic under the `TimeSource`.
