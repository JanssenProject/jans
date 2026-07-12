### **TrustRelationship Invariants & State Transition Rules**

#### 1. Supported Metadata Sources by Nature

**1.1 INDIVIDUAL**
- `NONE` (default)
- `FILE`
- `URI`
- `UPSTREAM`
- `MANUAL`

**1.2 AGGREGATE**
- `NONE` (default)
- `FILE`
- `URI`
- `MDQ`

---

#### 2. State Transitions

| Current State     | Operation / Trigger                                      | Target State     | Conditions / Notes |
|-------------------|----------------------------------------------------------|------------------|--------------------|
| **DRAFT**         | Update metadata source (real) + has active profile       | **READY**        | - |
| **DRAFT**         | `updateXXXProfileConfiguration` (enables ≥1 profile) + has real metadata source | **READY** | - |
| **DRAFT**         | Any other update                                         | **DRAFT**        | - |
| **READY**         | `activate()`                                             | **ACTIVATING**   | clears previous diagnostics |
| **READY**         | Update metadata source to `NONE` or disable all profiles | **DRAFT**        | - |
| **ACTIVATING**    | `cancelActivation()`                                     | **READY**        | Aborts pending activation |
| **ACTIVATING**    | `finalizeActivation(ActivationDiagnostics)`                  | **ACTIVE**       | On successful activation |
| **ACTIVATING**    | `finalizeActivation(ActivationDiagnostics)`                  | **READY**        | On failed activation |
| **ACTIVATING**    | `finalizeActivation(ActivationDiagnostics)`                  | **ACTIVATING**   | On no activation data | 
| **ACTIVE**        | Update metadata source (different & real)                | **ACTIVATING**   | - |
| **ACTIVE**        | `updateXXXProfileConfiguration` (different value, still ≥1 active profile) | **ACTIVATING** | Triggers re-activation |
| **ACTIVE**        | `deactivate()`                                           | **INACTIVE**     | - |
| **ACTIVE**        | Update metadata source to `NONE` or disable all profiles | **DRAFT**        | - |
| **INACTIVE**      | `activate()` + has real metadata source + ≥1 active profile | **ACTIVATING** | Success path |
| **INACTIVE**      | `activate()` when missing real metadata source or no active profiles | **DRAFT**      | Requirements not met after explicit `activate()` call |
| **INACTIVE**      | `updateMetadataSource()` (any change)                    | **INACTIVE**     | Version bumped if changed |
| **INACTIVE**      | `updateXXXProfileConfiguration()` (any change)              | **INACTIVE**     | Version bumped if changed |

**Important Note on Activation Path**:
All trust relationships follow the **same consistent path**:  
`READY` → `ACTIVATING` → `ACTIVE` (or back to `READY`/`DRAFT` if activation is cancelled or fails).  
There is **no** direct `READY` → `ACTIVE` transition for any nature.

---

#### 3. Global / Aggregate Invariants (Always True)

- `id` must not be null. (It is **unassigned** on a freshly created TR — see §4.1; assignment happens at persistence, not creation.)
- `displayName` must not be null and must not be blank.
- `description` must not be null (can be blank).
- `nature` must not be null.
- `status` must not be null.
- `version` must not be null and must be ≥ `Version.initial()`.
- `metadataSource` must not be null.
- `discoveredEntityIds` must not be null.
- All profile configuration objects must not be null (they must be initialized to their default disabled state).
- `releasedAttributes` must not be null.

> **Enforced by** `ValidityInvariants.VersionAtLeastInitial` — any build with `version < Version.initial()` fails with `InvalidVersion` (wrapped in `DomainObjectCreationFailed`/`DomainObjectUpdateFailed`), on both the `create()` and builder/reconstruction paths. Covered by `shouldFailWhenVersionIsBelowInitialDuringBuild`.

---

#### 4. Operations and Transition Rules

**4.1 `create(...)` (Static Factory Method)**
- Parameters: `displayName`, `description`, `nature`
- Target State: `DRAFT`
- Conditions: `displayName` (non-blank), `description` (not null), `nature` required.
- Post-conditions: `id` unassigned, `status` = `DRAFT`, `metadataSource` = `NONE`, `discoveredEntityIds` empty, all profiles initialized to default disabled state, `releasedAttributes` empty, `activationDiagnostics` = none, version = initial.

**4.2 `updateDisplayName(DisplayName)`**
- Allowed from: All states
- Target State: Same state
- Version bumped only if value actually changes.

**4.3 `updateDescription(Description)`**
- Allowed from: All states
- Target State: Same state
- Version bumped only if value actually changes.

**4.4 `updateMetadataSource(MetadataSource)`**
- Allowed from: `DRAFT`, `READY`, `ACTIVE`, `INACTIVE`
- Forbidden from: `ACTIVATING`
- Must respect nature restrictions (see section 1).
- In `INACTIVE`: State remains `INACTIVE`, version bumped if changed.
- From `DRAFT`/`READY`/`ACTIVE`: target state per section 2 (`DRAFT` when set to `NONE` or left with no active profile; otherwise `READY` from `DRAFT`, `ACTIVATING` from `ACTIVE` on a real change). Version bumped if changed.

> **By design:** an `INACTIVE` TR is intentionally inert — it can't *do* anything until an explicit `activate()`, at which point readiness is re-evaluated and the appropriate state (`ACTIVATING` if requirements are met, else `DRAFT`) is assigned. Consequently, edits never auto-demote an `INACTIVE` TR: setting the metadata source to `NONE` or disabling all profiles leaves it `INACTIVE`, whereas the same edits demote `DRAFT`/`READY`/`ACTIVE` toward `DRAFT`.

**4.5 `updateXXXProfileConfiguration(XXXProfileConfiguration)`**
- Allowed from: `DRAFT`, `READY`, `ACTIVE`, `INACTIVE`
- Forbidden from: `ACTIVATING`
- In `INACTIVE`: State remains `INACTIVE`, version bumped if changed.
- From `DRAFT`/`READY`/`ACTIVE`: target state per section 2 (`DRAFT` when no profile remains active; `READY` from `DRAFT` once a profile is enabled alongside a real metadata source; `ACTIVATING` from `ACTIVE` if ≥1 profile still active). Version bumped if changed.

**4.6 `activate()`**
- Allowed from: `READY`, `INACTIVE`
- Target State: `ACTIVATING` (on success) or `DRAFT` (if called from `INACTIVE` but requirements are not met)
- Version bumped.
- Clears any previous activation diagnostics.
- Note: a failed readiness check from `INACTIVE` demotes to `DRAFT` (not back to `INACTIVE`) because the TR no longer meets activation prerequisites; `activate()` thus re-validates readiness on every call.

**4.7 `cancelActivation()`**
- Allowed from: `ACTIVATING`
- Target State: `READY`
- Version bumped.
- Used to abort a pending activation attempt.

**4.8 `deactivate()`**
- Allowed from: `ACTIVE`
- Target State: `INACTIVE`
- Version bumped.

**4.9 `finalizeActivation(ActivationDiagnostics)`**
- Allowed from: `ACTIVATING`
- Parameters: `ActivationDiagnostics` (contains diagnostic data, verification results, errors, etc.)
- Returns: `TrustResult<TrustRelationship>`
- On Success → Target State: `ACTIVE`
- On Failure → Target State: `READY`
- On No Data → Target State: `ACTIVATING`
- Version bumped when the aggregate is effectively modified — i.e., on success (→ `ACTIVE`) and failure (→ `READY`). The no-data outcome does not change state and does not bump the version when diagnostics are unchanged.

> **By design:** the "No Data → `ACTIVATING`" outcome is a deliberate no-op. Entering `ACTIVATING` sets the activation diagnostics to `NO_DATA`, and finalizing with `NO_DATA` leaves them unchanged — so the TR stays pending. This forces API clients to **explicitly report success or failure** in order to drive a transition out of `ACTIVATING`. (Mechanically it is a transition-rule fall-through: no rule matches `NO_DATA`, so the status defaults to unchanged.)

**4.10 `incorporateDiscoveredEntityIds(...)`**
- Allowed from: `ACTIVATING` (AGGREGATE only)
- Does **not** change state by itself (only updates data).
- Version bumped if the discovered entity IDs actually change.

**4.11 `updateReleasedAttributes(...)`**
- Allowed from: All states except `ACTIVATING`
- Target State: Same state
- Version bumped if changed.
