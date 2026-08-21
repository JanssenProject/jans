# TrustRelationship — Test Plan

Companion to [`tr_invariants_and_state_transition_rules.md`](./tr_invariants_and_state_transition_rules.md).
This plan drives an **incremental, dependency-ordered** implementation of the `TrustRelationship`
aggregate: each group only relies on behaviour proven by an **earlier** group, so you can implement
and green-bar the suite top-to-bottom without forward references.

---

## Conventions

- **Each case = a method name + a `@DisplayName`.** The `####` heading holds the T-ID and the generated
  method name; the single line beneath it is a `GIVEN … WHEN … THEN …` sentence written to be **pasted
  verbatim into the JUnit 5 `@DisplayName`** — plain text, ALL-CAPS keywords, no markdown so nothing
  needs stripping. It also serves as the case's description.
- **Naming.** Test methods follow the existing suite convention:
  `should<Outcome>_when<Condition>` (camelCase, `should` prefix, `_when` separating the condition;
  the `_when…` clause is dropped when there is no meaningful condition). Method names that match a
  test already present in `TrustRelationshipTests.java` are reused verbatim.
- **Result-oriented, not exception-oriented.** Every mutating operation returns
  `Result<TrustRelationship>` (and `create(...)` / `finalizeActivation(...)` likewise).
  Success is asserted with `result.isSuccess()` + `TrustRelationshipAssert.assertThat(result.getValue())…`;
  failure is asserted with `result.isFailure()` and the concrete `TrustError` subtype via
  `result.getError()`. **No test asserts a thrown exception** for a domain rule violation.
- **Structure.** `Group → Subgroup → Case`, IDs of the form `T<group>.<subgroup>.<case>`.
- **Fixtures & asserts.** Reuse `TrustRelationshipFixtures` (sample states per nature) and the fluent
  `TrustRelationshipAssert` / `ProfileConfigurationAssert`. New fixtures noted where needed.
- **Version rule (global).** Version is bumped **iff the aggregate is effectively modified**; a
  no-op / idempotent update leaves the version unchanged. Asserted throughout with `isVersion(...)`.
- **Immutability.** Operations never mutate the receiver; they return a new instance (or a failure).
- **Coverage checkbox.** Each case carries a `- [ ] covered by existing test` box. Tick it (`- [x]`)
  once you have confirmed a test in `TrustRelationshipTests.java` exercises that case. Boxes already
  ticked were mapped from the current suite via its method names and are worth a quick line-by-line
  confirm — treat a pre-tick as "very likely covered", not gospel. A case left unticked here is either
  a genuine gap or covered only with a weaker assertion than this plan asks for (see the *Gaps* section).

### Definitions (used verbatim across cases)

| Term | Meaning |
|------|---------|
| **Real metadata source** | Any `MetadataSource` whose type is **not** `NONE` (i.e. `FILE`, `URI`, `UPSTREAM`, `MANUAL`, `MDQ`). |
| **Active profile** | A profile configuration whose `ProfileStatus` is `ACTIVE` (enabled). |
| **No active profile** | Every one of the six profile configurations is `INACTIVE`. |
| **Effective change** | The candidate differs from the original in at least one persisted field. |

### Nature → supported metadata sources

| Nature | Supported | Rejected |
|--------|-----------|----------|
| `INDIVIDUAL` | `NONE`, `FILE`, `URI`, `UPSTREAM`, `MANUAL` | `MDQ` |
| `AGGREGATE` | `NONE`, `FILE`, `URI`, `MDQ` | `UPSTREAM`, `MANUAL` |

### Dependency order (why the groups are numbered this way)

```
G1  create + invariants ........... nothing (foundation; makes DRAFT reachable)
G2  displayName/description + version ... G1
G3  metadata nature support/validation .. G1
G4  profile configuration basics ........ G1
G5  DRAFT transitions ................... G3, G4       (makes READY reachable)
G6  READY transitions .................. G5            (makes ACTIVATING reachable)
G7  ACTIVATING ops + finalization ....... G6           (makes ACTIVE reachable)
G8  ACTIVE transitions ................. G7            (makes INACTIVE reachable)
G9  INACTIVE transitions ............... G8
G10 released attributes ............... G5–G9 (needs every state)
G11 cross-cutting / lifecycle ......... all of the above
```

A test that *exercises* state **S** never appears before the group that first *produces* state **S**.

---

## Group 1 — Creation & Global Invariants

*Foundation. Depends on nothing. Establishes `create(...)` and the always-true structural invariants.*

### 1.1 Successful creation

#### T1.1.1 · `shouldCreateIndividualInDraftState`

- [x] covered by existing test

GIVEN valid parameters and INDIVIDUAL nature WHEN create() is called THEN it creates a new INDIVIDUAL trust relationship in DRAFT status with defaults

#### T1.1.2 · `shouldCreateAggregateInDraftState`

- [x] covered by existing test

GIVEN valid parameters and AGGREGATE nature WHEN create() is called THEN it creates a new AGGREGATE trust relationship in DRAFT status with defaults

#### T1.1.3 · `shouldCreate_whenDescriptionIsBlank`

- [x] covered by existing test

GIVEN a valid displayName and a blank description WHEN create() is called THEN it succeeds because a blank description is allowed

### 1.2 Creation input validation (return errors, never throw)

#### T1.2.1 · `shouldFailCreation_whenDisplayNameIsNull`

- [x] covered by existing test

GIVEN a null displayName WHEN create() is called THEN it fails with DomainObjectCreationFailed caused by RequiredValueMissing

#### T1.2.2 · `shouldFailCreation_whenDisplayNameIsBlank`

- [x] covered by existing test

GIVEN a blank displayName WHEN create() is called THEN it fails because the display name must not be blank

*N/A at `create()` — `DisplayName.of(blank)` rejects blank/null at construction (`RequiredValueMissing`, field `rawValue`), so a blank `DisplayName` cannot be handed to `create()`. The invariant lives at the `DisplayName` value-object boundary, not in the aggregate; would be covered by a `DisplayNameTests` unit test if added.*

#### T1.2.3 · `shouldFailCreation_whenDescriptionIsNull`

- [x] covered by existing test

GIVEN a null description WHEN create() is called THEN it fails with DomainObjectCreationFailed

#### T1.2.4 · `shouldFailCreation_whenNatureIsNull`

- [x] covered by existing test

GIVEN a null nature WHEN create() is called THEN it fails with DomainObjectCreationFailed

### 1.3 Post-creation defaults / invariants

#### T1.3.1 · `shouldBeInDraftState_whenCreated`

- [x] covered by existing test

GIVEN valid creation parameters WHEN create() is called THEN the new trust relationship is in DRAFT status

#### T1.3.2 · `shouldHaveInitialVersion_whenCreated`

- [x] covered by existing test

GIVEN valid creation parameters WHEN create() is called THEN its version equals the initial version

#### T1.3.3 · `shouldHaveUnassignedId_whenCreated`

- [x] covered by existing test

GIVEN valid creation parameters WHEN create() is called THEN its id is unassigned and the trust relationship is new

#### T1.3.4 · `shouldHaveNoRealMetadataSource_whenCreated`

- [x] covered by existing test

GIVEN valid creation parameters WHEN create() is called THEN its metadata source is NONE (no real metadata source)

#### T1.3.5 · `shouldHaveEmptyDiscoveredEntityIds_whenCreated`

- [x] covered by existing test

GIVEN valid creation parameters WHEN create() is called THEN its discovered entity IDs are empty and non-null

#### T1.3.6 · `shouldHaveAllProfilesDisabled_whenCreated`

- [x] covered by existing test

GIVEN valid creation parameters WHEN create() is called THEN all six profile configurations are initialized, non-null, and disabled

#### T1.3.7 · `shouldHaveEmptyReleasedAttributes_whenCreated`

- [x] covered by existing test

GIVEN valid creation parameters WHEN create() is called THEN its released attributes are empty and non-null

#### T1.3.8 · `shouldHaveNoActivationDiagnostics_whenCreated`

- [x] covered by existing test

GIVEN valid creation parameters WHEN create() is called THEN it has no activation diagnostics

### 1.4 Global null-argument rejection (invariants preserved on every mutator)

#### T1.4.1 · `shouldFail_whenUpdateDisplayNameWithNull`

- [x] covered by existing test

GIVEN any trust relationship WHEN updateDisplayName() is called with null THEN it fails with DomainObjectUpdateFailed and the original is unchanged

#### T1.4.2 · `shouldFail_whenUpdateDescriptionWithNull`

- [x] covered by existing test

GIVEN any trust relationship WHEN updateDescription() is called with null THEN it fails and the original is unchanged

#### T1.4.3 · `shouldFail_whenUpdateMetadataSourceWithNull`

- [x] covered by existing test

GIVEN any trust relationship WHEN updateMetadataSource() is called with null THEN it fails and the original is unchanged

#### T1.4.4 · `shouldFail_whenUpdateReleasedAttributesWithNull`

- [x] covered by existing test

GIVEN any trust relationship WHEN updateReleasedAttributes() is called with null THEN it fails and the original is unchanged

#### T1.4.5 · `shouldFail_whenUpdateProfileConfigurationWithNull`

- [x] covered by existing test

GIVEN any trust relationship WHEN updateXXXProfileConfiguration() is called with null THEN it fails and the original is unchanged
*Parametrized over the six profiles.*

#### T1.4.6 · `shouldFail_whenFinalizeActivationWithNull`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN finalizeActivation() is called with null THEN it fails

### 1.5 Structural invariants enforced at build() (builder / reconstruction path)

#### T1.5.1 · `shouldFailWhenVersionIsBelowInitialDuringBuild`

- [x] covered by existing test

GIVEN a builder with version set below Version.initial() WHEN build() is called THEN it fails with InvalidVersion
*Enforces the §3 invariant "version ≥ Version.initial()" via `ValidityInvariants.VersionAtLeastInitial`; guards the builder/reconstruction path, not just `create()`.*

#### T1.5.2 · `shouldFailWhenRequiredFieldsAreNullOrInvalidDuringBuild`

- [x] covered by existing test

GIVEN a builder with one required field set to null or invalid WHEN build() is called THEN it fails with RequiredValueMissing as the cause
*Parametrized over an invalidator per required field.*

#### T1.5.3 · `shouldFailWhenMetadataSourceIsNull`

- [x] covered by existing test

GIVEN a builder with metadataSource set to null WHEN build() is called THEN it fails with RequiredValueMissing as the cause

#### T1.5.4 · `shouldFailWhenAnyProfileConfigurationIsNull`

- [x] covered by existing test

GIVEN a builder with a profile configuration set to null WHEN build() is called THEN it fails with RequiredValueMissing naming the offending field
*Parametrized over the six profiles.*

#### T1.5.5 · `shouldFailWhenDiscoveredEntityIdsIsNull`

- [x] covered by existing test

GIVEN a builder with discoveredEntityIds set to null WHEN build() is called THEN it fails with RequiredValueMissing as the cause

---

## Group 2 — Descriptive Updates & Version Semantics

*Depends on G1. Establishes the version-bump-on-effective-change convention on the simplest mutators,
against the DRAFT baseline only. "From every state" preservation is deferred to G11.*

### 2.1 `updateDisplayName` (from DRAFT)

#### T2.1.1 · `shouldUpdateDisplayNameAndRemainInDraft_whenDifferentNameProvided`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN updateDisplayName() is called with a different name THEN the display name is updated and it remains in DRAFT

#### T2.1.2 · `shouldIncrementVersion_whenDisplayNameChanged`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN updateDisplayName() is called with a different name THEN the version is incremented

#### T2.1.3 · `shouldNotChangeStateOrVersion_whenDisplayNameUnchanged`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN updateDisplayName() is called with the same name THEN neither the state nor the version changes

### 2.2 `updateDescription` (from DRAFT)

#### T2.2.1 · `shouldUpdateDescriptionAndRemainInDraft_whenDifferentDescriptionProvided`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN updateDescription() is called with a different description THEN the description is updated and it remains in DRAFT

#### T2.2.2 · `shouldIncrementVersion_whenDescriptionChanged`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN updateDescription() is called with a different description THEN the version is incremented

#### T2.2.3 · `shouldNotChangeStateOrVersion_whenDescriptionUnchanged`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN updateDescription() is called with the same description THEN neither the state nor the version changes

#### T2.2.4 · `shouldUpdateDescription_whenNewDescriptionIsBlank`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN updateDescription() is called with a blank description THEN it succeeds because a blank description is allowed

### 2.3 `updateBasicInfo` (from DRAFT)

*Combined descriptive update (display name + description). Its defining guarantee over calling the two
single-field updates in sequence: it is a single build, so it bumps the version at most once.*

#### T2.3.1 · `shouldUpdateBothFieldsAndRemainInDraft_whenBasicInfoChanged`

- [x] covered by test

GIVEN a DRAFT trust relationship WHEN updateBasicInfo() is called with a different display name and description THEN both are updated and it remains in DRAFT

#### T2.3.2 · `shouldIncrementVersionExactlyOnce_whenBothBasicInfoFieldsChanged`

- [x] covered by test

GIVEN a DRAFT trust relationship WHEN updateBasicInfo() changes both the display name and the description THEN the version is incremented by exactly one

#### T2.3.3 · `shouldIncrementVersionExactlyOnce_whenOnlyOneBasicInfoFieldChanged`

- [x] covered by test

GIVEN a DRAFT trust relationship WHEN updateBasicInfo() changes only the description and leaves the display name unchanged THEN the version is incremented by exactly one

#### T2.3.4 · `shouldNotChangeStateOrVersion_whenBasicInfoUnchanged`

- [x] covered by test

GIVEN a DRAFT trust relationship WHEN updateBasicInfo() is called with the same display name and description THEN neither the state nor the version changes

#### T2.3.5 · `shouldFail_whenBasicInfoDisplayNameIsNull`

- [x] covered by test

GIVEN a DRAFT trust relationship WHEN updateBasicInfo() is called with a null display name THEN it fails with DomainObjectUpdateFailed and the original is unchanged

#### T2.3.6 · `shouldNormaliseDescriptionToEmpty_whenBasicInfoDescriptionIsNull`

- [x] covered by test

GIVEN a DRAFT trust relationship WHEN updateBasicInfo() is called with a null description THEN it succeeds and the description becomes empty

### 2.4 Version-bump semantics (the shared rule)

#### T2.4.1 · `shouldIncrementVersionByOne_whenEffectivelyModified`

- [x] covered by existing test

GIVEN a trust relationship at a given version WHEN any field is effectively changed THEN the version is incremented exactly once

#### T2.4.2 · `shouldMaintainVersion_whenNotEffectivelyModified`

- [x] covered by existing test

GIVEN a trust relationship at a given version WHEN an idempotent update is applied THEN the version is unchanged

---

## Group 3 — Metadata Source: Nature Support & Validation

*Depends on G1. Validates the nature→source matrix and metadata-specific idempotency, without relying
on any transition beyond DRAFT. All cases start from a DRAFT TR with no active profile, so a successful
update stays DRAFT.*

### 3.1 Nature support matrix

#### T3.1.1 · `shouldAcceptMetadataSource_whenIndividualAndFile`

- [x] covered by existing test

GIVEN a DRAFT individual trust relationship WHEN updateMetadataSource() is called with a FILE source THEN it succeeds and stores the source

#### T3.1.2 · `shouldAcceptMetadataSource_whenIndividualAndUri`

- [x] covered by existing test

GIVEN a DRAFT individual trust relationship WHEN updateMetadataSource() is called with a URI source THEN it succeeds and stores the source

#### T3.1.3 · `shouldAcceptMetadataSource_whenIndividualAndUpstream`

- [x] covered by existing test

GIVEN a DRAFT individual trust relationship WHEN updateMetadataSource() is called with an UPSTREAM source THEN it succeeds and stores the source

#### T3.1.4 · `shouldAcceptMetadataSource_whenIndividualAndManual`

- [x] covered by existing test

GIVEN a DRAFT individual trust relationship WHEN updateMetadataSource() is called with a MANUAL source THEN it succeeds and stores the source

#### T3.1.5 · `shouldRejectMetadataSource_whenIndividualAndMdq`

- [x] covered by existing test

GIVEN a DRAFT individual trust relationship WHEN updateMetadataSource() is called with an MDQ source THEN it fails with IncompatibleMetadataSourceForNature and the original is unchanged

#### T3.1.6 · `shouldAcceptMetadataSource_whenAggregateAndFile`

- [x] covered by existing test

GIVEN a DRAFT aggregate trust relationship WHEN updateMetadataSource() is called with a FILE source THEN it succeeds and stores the source

#### T3.1.7 · `shouldAcceptMetadataSource_whenAggregateAndUri`

- [x] covered by existing test

GIVEN a DRAFT aggregate trust relationship WHEN updateMetadataSource() is called with a URI source THEN it succeeds and stores the source

#### T3.1.8 · `shouldAcceptMetadataSource_whenAggregateAndMdq`

- [x] covered by existing test

GIVEN a DRAFT aggregate trust relationship WHEN updateMetadataSource() is called with an MDQ source THEN it succeeds and stores the source

#### T3.1.9 · `shouldRejectMetadataSource_whenAggregateAndUpstream`

- [x] covered by existing test

GIVEN a DRAFT aggregate trust relationship WHEN updateMetadataSource() is called with an UPSTREAM source THEN it fails with IncompatibleMetadataSourceForNature and the original is unchanged

#### T3.1.10 · `shouldRejectMetadataSource_whenAggregateAndManual`

- [x] covered by existing test

GIVEN a DRAFT aggregate trust relationship WHEN updateMetadataSource() is called with a MANUAL source THEN it fails with IncompatibleMetadataSourceForNature and the original is unchanged

### 3.2 Metadata source value semantics

#### T3.2.1 · `shouldIncrementVersion_whenRealMetadataSourceSetFromNone`

- [x] covered by existing test

GIVEN a DRAFT trust relationship with a NONE source WHEN updateMetadataSource() is called with a real source THEN it succeeds and the version is incremented

#### T3.2.2 · `shouldMaintainVersionAndState_whenMetadataSourceUnchanged`

- [x] covered by existing test

GIVEN a DRAFT trust relationship with a real source WHEN updateMetadataSource() is called with the same source THEN neither the state nor the version changes

#### T3.2.3 · `shouldMaintainVersion_whenMetadataSetToNoneOnNoneSource`

- [x] covered by existing test

GIVEN a DRAFT trust relationship with a NONE source WHEN updateMetadataSource() is called with NONE THEN it succeeds and the version is unchanged

---

## Group 4 — Profile Configuration Fundamentals

*Depends on G1. Enable/disable semantics against DRAFT (no real metadata ⇒ stays DRAFT), for each of
the six profiles. Parametrize with a `ProfileConfigurationAccessor` over all six profile types.*

### 4.1 Enable / disable a single profile

#### T4.1.1 · `shouldMarkProfileActive_whenProfileEnabled`

- [x] covered by existing test

GIVEN a DRAFT trust relationship with all profiles disabled WHEN a profile configuration is enabled THEN that profile becomes ACTIVE and at least one profile is active
*Parametrized over the six profiles.*

#### T4.1.2 · `shouldIncrementVersion_whenProfileEnabled`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN a profile configuration is enabled THEN the version is incremented

#### T4.1.3 · `shouldMarkProfileInactive_whenProfileDisabled`

- [x] covered by existing test

GIVEN a DRAFT trust relationship with one active profile WHEN that profile configuration is disabled THEN the profile becomes INACTIVE

#### T4.1.4 · `shouldMaintainVersion_whenProfileUnchanged`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN a profile configuration is updated with the same value THEN the version is unchanged

### 4.2 Multiple profiles / counting

#### T4.2.1 · `shouldReflectActiveCount_whenSeveralProfilesEnabled`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN several distinct profiles are enabled THEN the active profile count reflects them

#### T4.2.2 · `shouldHaveNoActiveProfile_whenAllProfilesDisabled`

- [x] covered by existing test

GIVEN a DRAFT trust relationship with active profiles WHEN all profiles are disabled THEN no profile is active

---

## Group 5 — DRAFT State Transitions

*Depends on G3 + G4 (needs real metadata and active-profile machinery). First group that produces READY.*

### 5.1 DRAFT → READY

#### T5.1.1 · `shouldTransitionToReady_whenRealMetadataSourceAddedWithActiveProfile`

- [x] covered by existing test

GIVEN a DRAFT trust relationship with an active profile and a NONE source WHEN a real metadata source is added THEN it transitions to READY and bumps the version

#### T5.1.2 · `shouldTransitionToReady_whenProfileConfigurationEnabledWithRealMetadataSource`

- [x] covered by existing test

GIVEN a DRAFT trust relationship with a real source and no active profile WHEN a profile configuration is enabled THEN it transitions to READY and bumps the version

### 5.2 DRAFT stays DRAFT

#### T5.2.1 · `shouldRemainInDraft_whenMetadataSourceAddedButNoActiveProfile`

- [x] covered by existing test

GIVEN a DRAFT trust relationship with no active profile and a NONE source WHEN a real metadata source is added THEN it remains in DRAFT

#### T5.2.2 · `shouldRemainInDraft_whenProfileEnabledButNoRealMetadataSource`

- [x] covered by existing test

GIVEN a DRAFT trust relationship with a NONE source WHEN a profile configuration is enabled THEN it remains in DRAFT

#### T5.2.3 · `shouldRemainInDraft_whenDescriptiveFieldUpdated`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN a descriptive field (displayName or description) is updated THEN it remains in DRAFT

#### T5.2.4 · `shouldRemainInDraft_whenMetadataSourceSetToNone`

- [x] covered by existing test

GIVEN a DRAFT trust relationship with a real source WHEN the metadata source is set to NONE THEN it remains in DRAFT

### 5.3 Operations invalid from DRAFT (return errors)

#### T5.3.1 · `shouldFailActivate_whenCalledFromDraft`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN activate() is called THEN it fails with OperationForbiddenFromStatus and the original is unchanged

#### T5.3.2 · `shouldFailCancelActivation_whenCalledFromDraft`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN cancelActivation() is called THEN it fails and the original is unchanged

#### T5.3.3 · `shouldFailDeactivate_whenCalledFromDraft`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN deactivate() is called THEN it fails and the original is unchanged

#### T5.3.4 · `shouldFailFinalizeActivation_whenCalledFromDraft`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN finalizeActivation() is called THEN it fails and the original is unchanged

#### T5.3.5 · `shouldFailIncorporateDiscoveredEntityIds_whenCalledFromDraft`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN incorporateDiscoveredEntityIds() is called THEN it fails and the original is unchanged

---

## Group 6 — READY State Transitions

*Depends on G5 (READY reachable). Produces ACTIVATING.*

### 6.1 READY → ACTIVATING via `activate()`

#### T6.1.1 · `shouldTransitionToActivating_whenActivateCalledFromReady`

- [x] covered by existing test

GIVEN a READY trust relationship WHEN activate() is called THEN it transitions to ACTIVATING and bumps the version

#### T6.1.2 · `shouldClearPreviousDiagnostics_whenActivateIsCalledFromReady`

- [x] covered by existing test

GIVEN a READY trust relationship carrying stale diagnostics WHEN activate() is called THEN the previous activation diagnostics are cleared

#### T6.1.3 · `shouldIncrementVersion_whenActivateCalledFromReady`

- [x] covered by existing test

GIVEN a READY trust relationship WHEN activate() is called THEN the version is incremented

### 6.2 READY → DRAFT (demotion)

#### T6.2.1 · `shouldTransitionToDraft_whenMetadataSourceSetToNoneFromReady`

- [x] covered by existing test

GIVEN a READY trust relationship WHEN the metadata source is set to NONE THEN it transitions to DRAFT and bumps the version

#### T6.2.2 · `shouldTransitionToDraft_whenAllProfilesDisabledFromReady`

- [x] covered by existing test

GIVEN a READY trust relationship with one active profile WHEN that profile is disabled THEN it transitions to DRAFT and bumps the version

### 6.3 READY stays READY

#### T6.3.1 · `shouldRemainInReady_whenDescriptiveFieldUpdated`

- [x] covered by existing test

GIVEN a READY trust relationship WHEN a descriptive field is updated THEN it remains in READY

#### T6.3.2 · `shouldRemainInReady_whenMetadataSourceChangedToAnotherRealSource`

- [x] covered by existing test

GIVEN a READY trust relationship WHEN the metadata source is changed to another real source THEN it remains in READY and bumps the version

#### T6.3.3 · `shouldRemainInReady_whenAnotherProfileEnabled`

- [x] covered by existing test

GIVEN a READY trust relationship WHEN a second profile is enabled THEN it remains in READY

### 6.4 Operations invalid from READY (return errors)

#### T6.4.1 · `shouldFailCancelActivation_whenCalledFromReady`

- [x] covered by existing test

GIVEN a READY trust relationship WHEN cancelActivation() is called THEN it fails and the original is unchanged

#### T6.4.2 · `shouldFailDeactivate_whenCalledFromReady`

- [x] covered by existing test

GIVEN a READY trust relationship WHEN deactivate() is called THEN it fails and the original is unchanged

#### T6.4.3 · `shouldFailFinalizeActivation_whenCalledFromReady`

- [x] covered by existing test

GIVEN a READY trust relationship WHEN finalizeActivation() is called THEN it fails and the original is unchanged

#### T6.4.4 · `shouldFailIncorporateDiscoveredEntityIds_whenCalledFromReady`

- [x] covered by existing test

GIVEN a READY trust relationship WHEN incorporateDiscoveredEntityIds() is called THEN it fails and the original is unchanged

---

## Group 7 — ACTIVATING: Operations & Finalization

*Depends on G6 (ACTIVATING reachable). Produces ACTIVE (via successful finalize).*

### 7.1 `cancelActivation()`

#### T7.1.1 · `shouldTransitionToReady_whenCancelActivationCalledFromActivating`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN cancelActivation() is called THEN it transitions to READY and bumps the version

#### T7.1.2 · `shouldIncrementVersion_whenCancelActivationCalledFromActivating`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN cancelActivation() is called THEN the version is incremented

### 7.2 `finalizeActivation(ActivationDiagnostics)` outcomes

#### T7.2.1 · `shouldTransitionToActive_whenFinalizeActivationSucceeds`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN finalizeActivation() is called with successful diagnostics THEN it transitions to ACTIVE and bumps the version

#### T7.2.2 · `shouldTransitionToReady_whenFinalizeActivationFails`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN finalizeActivation() is called with failed diagnostics THEN it transitions to READY and bumps the version

#### T7.2.3 · `shouldRemainInActivating_whenFinalizeActivationHasNoData`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN finalizeActivation() is called with no activation data THEN it remains in ACTIVATING

#### T7.2.4 · `shouldStoreDiagnostics_whenFinalizeActivationCompletes`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN finalizeActivation() completes THEN it stores the corresponding activation diagnostics (successful or failed)

#### T7.2.5 · `shouldFailFinalizeActivation_whenActivationContextIsNull`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN finalizeActivation() is called with null THEN it fails and the original is unchanged

### 7.3 Operations forbidden from ACTIVATING (return errors)

#### T7.3.1 · `shouldFailUpdateMetadataSource_whenCalledFromActivating`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN updateMetadataSource() is called THEN it fails with OperationForbiddenFromStatus and the original is unchanged

#### T7.3.2 · `shouldFailUpdateProfileConfiguration_whenCalledFromActivating`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN updateXXXProfileConfiguration() is called THEN it fails and the original is unchanged
*Parametrized over the six profiles.*

#### T7.3.3 · `shouldFailUpdateReleasedAttributes_whenCalledFromActivating`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN updateReleasedAttributes() is called THEN it fails and the original is unchanged

#### T7.3.4 · `shouldFailActivate_whenCalledFromActivating`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN activate() is called THEN it fails and the original is unchanged

#### T7.3.5 · `shouldFailDeactivate_whenCalledFromActivating`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN deactivate() is called THEN it fails and the original is unchanged

### 7.4 `incorporateDiscoveredEntityIds(...)` (AGGREGATE only, ACTIVATING)

#### T7.4.1 · `shouldIncorporateDiscoveredEntityIds_whenInActivatingStateForAggregate`

- [x] covered by existing test

GIVEN an ACTIVATING aggregate trust relationship WHEN incorporateDiscoveredEntityIds() is called THEN it succeeds and records the discovered entity IDs

#### T7.4.2 · `shouldRemainInActivating_whenIncorporateDiscoveredEntityIds`

- [x] covered by existing test

GIVEN an ACTIVATING aggregate trust relationship WHEN incorporateDiscoveredEntityIds() is called THEN it remains in ACTIVATING

#### T7.4.3 · `shouldIncrementVersion_whenDiscoveredEntityIdsChanged`

- [x] covered by existing test

GIVEN an ACTIVATING aggregate trust relationship WHEN new discovered entity IDs are incorporated THEN the version is incremented

#### T7.4.4 · `shouldBeIdempotent_whenIncorporateDiscoveredEntityIdsWithSameValue`

- [x] covered by existing test

GIVEN an ACTIVATING aggregate that already has discovered entity IDs WHEN the same IDs are incorporated THEN the version is unchanged

#### T7.4.5 · `shouldRejectIncorporateDiscoveredEntityIds_whenTrustIsIndividual`

- [x] covered by existing test

GIVEN an ACTIVATING individual trust relationship WHEN incorporateDiscoveredEntityIds() is called THEN it fails with OperationRestrictedToNature and the original is unchanged

### 7.5 Descriptive updates remain allowed from ACTIVATING

#### T7.5.1 · `shouldRemainInActivating_whenDisplayNameUpdated`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN updateDisplayName() is called with a different name THEN it succeeds, remains in ACTIVATING, and bumps the version

#### T7.5.2 · `shouldRemainInActivating_whenDescriptionUpdated`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN updateDescription() is called with a different description THEN it succeeds and remains in ACTIVATING

---

## Group 8 — ACTIVE State Transitions

*Depends on G7 (ACTIVE reachable via successful finalize). Produces INACTIVE.*

### 8.1 ACTIVE → ACTIVATING (re-activation)

#### T8.1.1 · `shouldTransitionToActivating_whenMetadataSourceUpdatedFromActive`

- [x] covered by existing test

GIVEN an ACTIVE trust relationship WHEN the metadata source is changed to a different real source THEN it transitions to ACTIVATING and bumps the version

#### T8.1.2 · `shouldRemainInActive_whenMetadataSourceUpdateIsNoOp`

- [x] covered by existing test

GIVEN an ACTIVE trust relationship WHEN updateMetadataSource() is called with the same source THEN it remains in ACTIVE and the version is unchanged

#### T8.1.3 · `shouldTransitionToActivating_whenProfileConfigurationActuallyChangedFromActive`

- [x] covered by existing test

GIVEN an ACTIVE trust relationship with at least one active profile WHEN a profile configuration is actually changed while leaving at least one active THEN it transitions to ACTIVATING and bumps the version

### 8.2 ACTIVE → DRAFT

#### T8.2.1 · `shouldTransitionToDraft_whenMetadataSourceSetToNoneFromActive`

- [x] covered by existing test

GIVEN an ACTIVE trust relationship WHEN the metadata source is set to NONE THEN it transitions to DRAFT and bumps the version

#### T8.2.2 · `shouldTransitionToDraft_whenAllProfilesDisabledFromActive`

- [x] covered by existing test

GIVEN an ACTIVE trust relationship WHEN all profiles are disabled THEN it transitions to DRAFT and bumps the version

### 8.3 ACTIVE → INACTIVE

#### T8.3.1 · `shouldTransitionToInactive_whenDeactivateCalledFromActive`

- [x] covered by existing test

GIVEN an ACTIVE trust relationship WHEN deactivate() is called THEN it transitions to INACTIVE and bumps the version

### 8.4 ACTIVE stays ACTIVE

#### T8.4.1 · `shouldRemainInActive_whenDescriptiveFieldUpdated`

- [x] covered by existing test

GIVEN an ACTIVE trust relationship WHEN a descriptive field is updated THEN it remains in ACTIVE

#### T8.4.2 · `shouldRemainInActive_whenReleasedAttributesUpdated`

- [x] covered by existing test

GIVEN an ACTIVE trust relationship WHEN updateReleasedAttributes() is called THEN it remains in ACTIVE

### 8.5 Operations invalid from ACTIVE (return errors)

#### T8.5.1 · `shouldFailActivate_whenCalledFromActive`

- [x] covered by existing test

GIVEN an ACTIVE trust relationship WHEN activate() is called THEN it fails and the original is unchanged

#### T8.5.2 · `shouldFailCancelActivation_whenCalledFromActive`

- [x] covered by existing test

GIVEN an ACTIVE trust relationship WHEN cancelActivation() is called THEN it fails and the original is unchanged

#### T8.5.3 · `shouldFailFinalizeActivation_whenCalledFromActive`

- [x] covered by existing test

GIVEN an ACTIVE trust relationship WHEN finalizeActivation() is called THEN it fails and the original is unchanged

#### T8.5.4 · `shouldFailIncorporateDiscoveredEntityIds_whenCalledFromActive`

- [x] covered by existing test

GIVEN an ACTIVE aggregate trust relationship WHEN incorporateDiscoveredEntityIds() is called THEN it fails and the original is unchanged

---

## Group 9 — INACTIVE State Transitions

*Depends on G8 (INACTIVE reachable via deactivate).*

### 9.1 `activate()` from INACTIVE (re-validates readiness)

#### T9.1.1 · `shouldTransitionToActivatingFromInactive_whenRequirementsMet`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship with a real metadata source and at least one active profile WHEN activate() is called THEN it transitions to ACTIVATING and bumps the version

#### T9.1.2 · `shouldTransitionToDraft_whenActivateCalledFromInactiveWithoutRealMetadataSource`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship with a NONE source and an active profile WHEN activate() is called THEN it transitions to DRAFT because the requirements are not met

#### T9.1.3 · `shouldTransitionToDraft_whenActivateCalledFromInactiveWithoutActiveProfile`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship with a real source and no active profile WHEN activate() is called THEN it transitions to DRAFT because the requirements are not met

#### T9.1.4 · `shouldClearDiagnosticsAndIncrementVersion_whenActivateCalledFromInactive`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship carrying diagnostics WHEN activate() is called THEN the diagnostics are cleared and the version is incremented

### 9.2 `updateMetadataSource(...)` keeps INACTIVE

#### T9.2.1 · `shouldRemainInInactive_whenMetadataSourceChangedToAnotherRealSource`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship with a real source WHEN the metadata source is changed to another real source THEN it remains in INACTIVE and bumps the version

#### T9.2.2 · `shouldRemainInInactive_whenMetadataSourceSetToNone`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship WHEN the metadata source is set to NONE THEN it remains in INACTIVE and bumps the version

#### T9.2.3 · `shouldMaintainVersion_whenMetadataSourceUnchangedFromInactive`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship WHEN updateMetadataSource() is called with the same source THEN the version is unchanged

#### T9.2.4 · `shouldRejectMetadataSource_whenInactiveIndividualAndMdq`

- [x] covered by existing test

GIVEN an INACTIVE individual trust relationship WHEN updateMetadataSource() is called with an MDQ source THEN it fails with IncompatibleMetadataSourceForNature

### 9.3 `updateXXXProfileConfiguration(...)` keeps INACTIVE

#### T9.3.1 · `shouldRemainInInactive_whenProfileConfigurationChanged`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship WHEN a profile configuration is changed THEN it remains in INACTIVE and bumps the version

#### T9.3.2 · `shouldRemainInInactive_whenAllProfilesDisabled`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship WHEN all profiles are disabled THEN it remains in INACTIVE

#### T9.3.3 · `shouldMaintainVersion_whenProfileConfigurationUnchangedFromInactive`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship WHEN a profile configuration is updated with the same value THEN the version is unchanged

### 9.4 Operations invalid from INACTIVE (return errors)

#### T9.4.1 · `shouldFailCancelActivation_whenCalledFromInactive`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship WHEN cancelActivation() is called THEN it fails and the original is unchanged

#### T9.4.2 · `shouldFailDeactivate_whenCalledFromInactive`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship WHEN deactivate() is called THEN it fails and the original is unchanged

#### T9.4.3 · `shouldFailFinalizeActivation_whenCalledFromInactive`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship WHEN finalizeActivation() is called THEN it fails and the original is unchanged

#### T9.4.4 · `shouldFailIncorporateDiscoveredEntityIds_whenCalledFromInactive`

- [x] covered by existing test

GIVEN an INACTIVE aggregate trust relationship WHEN incorporateDiscoveredEntityIds() is called THEN it fails and the original is unchanged

---

## Group 10 — Released Attributes

*Depends on G5–G9 (needs every non-ACTIVATING state). `updateReleasedAttributes` is allowed from all
states **except** ACTIVATING and never changes state.*

### 10.1 Allowed from all non-ACTIVATING states (state preserved)

#### T10.1.1 · `shouldRemainInDraft_whenReleasedAttributesUpdated`

- [x] covered by existing test

GIVEN a DRAFT trust relationship WHEN updateReleasedAttributes() is called THEN it succeeds, stores the attributes, and remains in DRAFT

#### T10.1.2 · `shouldRemainInReady_whenReleasedAttributesUpdated`

- [x] covered by existing test

GIVEN a READY trust relationship WHEN updateReleasedAttributes() is called THEN it succeeds and remains in READY

#### T10.1.3 · `shouldRemainInActive_whenReleasedAttributesUpdated`

- [x] covered by existing test

GIVEN an ACTIVE trust relationship WHEN updateReleasedAttributes() is called THEN it succeeds and remains in ACTIVE
*Mirrors T8.4.2 — implement once.*

#### T10.1.4 · `shouldRemainInInactive_whenReleasedAttributesUpdated`

- [x] covered by existing test

GIVEN an INACTIVE trust relationship WHEN updateReleasedAttributes() is called THEN it succeeds and remains in INACTIVE

### 10.2 Version semantics

#### T10.2.1 · `shouldIncrementVersion_whenReleasedAttributesChanged`

- [x] covered by existing test

GIVEN a trust relationship at a given version WHEN updateReleasedAttributes() changes the attributes THEN the version is incremented

#### T10.2.2 · `shouldMaintainVersion_whenReleasedAttributesUnchanged`

- [x] covered by existing test

GIVEN a trust relationship with released attributes WHEN updateReleasedAttributes() is called with the same attributes THEN the version is unchanged

### 10.3 Forbidden from ACTIVATING (cross-reference)

#### T10.3.1 · `shouldFailUpdateReleasedAttributes_whenCalledFromActivating`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN updateReleasedAttributes() is called THEN it fails and the original is unchanged
*Mirrors T7.3.3 — implement once.*

---

## Group 11 — Cross-Cutting Invariants & Lifecycle

*Depends on everything above. Verifies system-wide properties once all states are constructible.*

### 11.1 Immutability

#### T11.1.1 · `shouldReturnNewInstanceAndLeaveOriginalUnchanged_whenOperationSucceeds`

- [x] covered by existing test

GIVEN any trust relationship WHEN a successful mutation is applied THEN a new instance is returned and the original is left unchanged

#### T11.1.2 · `shouldLeaveOriginalUnchanged_whenOperationFails`

- [x] covered by existing test

GIVEN any trust relationship WHEN a failing operation is attempted THEN it fails and the original is left unchanged

### 11.2 `equals` / `hashCode`

#### T11.2.1 · `shouldBeEqual_whenAllFieldsMatch`

- [x] covered by existing test

GIVEN two trust relationships built with identical fields WHEN they are compared THEN they are equal and share the same hashCode

#### T11.2.2 · `shouldNotBeEqual_whenAnyFieldDiffers`

- [x] covered by existing test

GIVEN two trust relationships differing in a single field WHEN they are compared THEN they are not equal

### 11.3 Version monotonicity

#### T11.3.1 · `shouldNeverDecreaseVersion_acrossSuccessfulMutations`

- [x] covered by existing test

GIVEN a fresh trust relationship WHEN a chain of effective mutations is applied THEN the version never decreases and strictly increases on each effective change

#### T11.3.2 · `shouldMaintainVersion_acrossIdempotentUpdates`

- [x] covered by existing test

GIVEN a fresh trust relationship WHEN a chain of idempotent updates is applied THEN the version stays constant

### 11.4 Descriptive updates preserve state from *every* state

#### T11.4.1 · `shouldPreserveState_whenDisplayNameUpdatedFromAnyState`

- [x] covered by existing test

GIVEN a trust relationship in any state WHEN updateDisplayName() is called with a different name THEN it succeeds and remains in the same state
*Parametrized over DRAFT / READY / ACTIVATING / ACTIVE / INACTIVE.*

#### T11.4.2 · `shouldPreserveState_whenDescriptionUpdatedFromAnyState`

- [x] covered by existing test

GIVEN a trust relationship in any state WHEN updateDescription() is called with a different description THEN it succeeds and remains in the same state
*Parametrized over DRAFT / READY / ACTIVATING / ACTIVE / INACTIVE.*

### 11.5 Full lifecycle happy paths

#### T11.5.1 · `shouldReachEveryState_whenIndividualFollowsFullLifecycle`

- [x] covered by existing test

GIVEN a new individual trust relationship WHEN it walks the full lifecycle from create through READY, ACTIVATING, ACTIVE, INACTIVE, and back to ACTIVATING THEN every waypoint reaches the expected status and the version stays monotonic

#### T11.5.2 · `shouldReachEveryState_whenAggregateFollowsFullLifecycleWithDiscovery`

- [x] covered by existing test

GIVEN a new aggregate trust relationship WHEN it walks the full lifecycle and incorporates discovered entity IDs while ACTIVATING THEN every waypoint reaches the expected status and the discovered IDs are present at ACTIVE

#### T11.5.3 · `shouldReturnToReadyThenRetry_whenActivationFails`

- [x] covered by existing test

GIVEN a READY trust relationship WHEN activation fails and is then retried successfully THEN it returns to READY on failure and reaches ACTIVE on the retry

### 11.6 Activation diagnostics lifecycle

#### T11.6.1 · `shouldClearDiagnostics_whenActivateCalled`

- [x] covered by existing test

GIVEN a READY or INACTIVE trust relationship carrying diagnostics WHEN activate() is called THEN the activation diagnostics are cleared

#### T11.6.2 · `shouldRetainSuccessfulDiagnostics_afterFinalizeActivationSucceeds`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN finalizeActivation() succeeds THEN it retains successful activation diagnostics

#### T11.6.3 · `shouldRetainFailedDiagnostics_afterFinalizeActivationFails`

- [x] covered by existing test

GIVEN an ACTIVATING trust relationship WHEN finalizeActivation() fails THEN it retains failed activation diagnostics

---

## Coverage traceability (spec → tests)

| Spec item (source doc) | Covered by |
|------------------------|------------|
| §1 metadata sources by nature | T3.1.* , T9.2.4 |
| §2 DRAFT→READY (metadata / profile) | T5.1.1, T5.1.2 |
| §2 DRAFT→DRAFT (other update) | T5.2.* |
| §2 READY→ACTIVATING (`activate`) | T6.1.1 |
| §2 READY→DRAFT (NONE / disable all) | T6.2.1, T6.2.2 |
| §2 ACTIVATING→READY (`cancelActivation`) | T7.1.1 |
| §2 ACTIVATING→ACTIVE / READY / ACTIVATING (`finalizeActivation`) | T7.2.1, T7.2.2, T7.2.3 |
| §2 ACTIVE→ACTIVATING (metadata / profile change) | T8.1.1, T8.1.3 |
| §2 ACTIVE→DRAFT (NONE / disable all) | T8.2.1, T8.2.2 |
| §2 ACTIVE→INACTIVE (`deactivate`) | T8.3.1 |
| §2 INACTIVE→ACTIVATING (`activate`, requirements met) | T9.1.1 |
| §2 INACTIVE→DRAFT (`activate`, requirements not met) | T9.1.2, T9.1.3 |
| §2 INACTIVE stays INACTIVE (metadata / profile updates) | T9.2.*, T9.3.* |
| §3 global structural invariants | T1.3.*, T1.4.*, T1.5.* |
| §4.1 `create` | T1.1.*, T1.2.*, T1.3.* |
| §4.2/4.3 `updateDisplayName` / `updateDescription` | T2.1.*, T2.2.*, T11.4.* |
| §4.4 `updateMetadataSource` (incl. forbidden from ACTIVATING) | T3.*, T5.*, T6.3.2, T7.3.1, T8.1.*, T9.2.* |
| §4.5 `updateXXXProfileConfiguration` (incl. forbidden from ACTIVATING) | T4.*, T5.1.2, T6.2.2, T7.3.2, T8.1.3, T9.3.* |
| §4.6 `activate` (READY / INACTIVE, re-validation) | T6.1.*, T9.1.* |
| §4.7 `cancelActivation` | T7.1.* |
| §4.8 `deactivate` | T8.3.1 |
| §4.9 `finalizeActivation` (3 outcomes) | T7.2.* |
| §4.10 `incorporateDiscoveredEntityIds` (AGGREGATE/ACTIVATING) | T7.4.* |
| §4.11 `updateReleasedAttributes` (all except ACTIVATING) | T10.* |
| version-bump-on-change rule | T2.3.*, T11.3.* |
| immutability / equality | T11.1.*, T11.2.* |

---

## Gaps in the current `TrustRelationshipTests.java` this plan closes

The existing suite (12 `@Nested` groups, mostly parameterized) already covers creation, the core
transitions, ACTIVE updates, discovered-entity-ids, and diagnostics behaviour. Against it, this plan
adds or tightens the following — each worth implementing even if the rest is deferred:

1. **Specific error subtypes.** The current suite asserts only the outer `DomainObjectCreationFailed` /
   `DomainObjectUpdateFailed` (and sometimes the generic `DomainObjectConsistencyFailed` cause). This
   plan pins the *concrete* cause — `OperationForbiddenFromStatus` (T5.3.\*, T6.4.\*, T7.3.\*, T8.5.\*,
   T9.4.\*), `IncompatibleMetadataSourceForNature` (T3.1.5/9/10, T9.2.4), and `OperationRestrictedToNature`
   (T7.4.5, the `incorporate`-on-individual case). *Decision point:* if you
   prefer to keep asserting only the outer type, relax these `THEN` clauses — but the specific subtype
   is more valuable as a regression guard.
2. **Descriptive updates from non-DRAFT states** — only DRAFT is asserted today. Added: T7.5.\*
   (ACTIVATING) and T11.4.\* (parametrized over every state).
3. **INACTIVE direct updates** — INACTIVE is currently reached only to test `activate()`. Added
   dedicated `updateMetadataSource` / `updateXXXProfileConfiguration` cases: T9.2.\*, T9.3.\*.
4. **`updateReleasedAttributes` from READY / ACTIVE / INACTIVE** — only DRAFT + null are covered today.
   Added: T10.1.2–T10.1.4, plus explicit ACTIVATING-forbidden T10.3.1.
5. **Failed operation leaves original untouched (incl. no version bump)** — not asserted today.
   Added: T11.1.2 (and the "original is unchanged" clause attached to every failure case).
6. **`equals`/`hashCode` and version monotonicity** — no direct coverage today. Added: T11.2.\*, T11.3.\*.
7. **Full end-to-end lifecycle walks** — the current suite tests transitions in isolation. Added:
   T11.5.\* (individual, aggregate-with-discovery, and failed-then-retry activation).

Also flagged in the current file (not part of this plan, but worth fixing while you are in there):
`NatureRestrictionsTests.shouldRejectIncorporateDiscoveredEntityIds_whenTrustIsIndividual` is annotated
`@Test` while carrying an (inert) `@MethodSource`, and two ACTIVE-update method names are duplicated
across `UpdatesFromActiveStateTests` and `ComplexStateTransitionsAndInteractionsTests`.
</content>
