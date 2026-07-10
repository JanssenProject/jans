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
| **ACTIVATING**    | `finalizeActivation(ActivationContext)`                  | **ACTIVE**       | On successful activation |
| **ACTIVATING**    | `finalizeActivation(ActivationContext)`                  | **READY**        | On failed activation |
| **ACTIVATING**    | `finalizeActivation(ActivationContext)`                  | **ACTIVATING**   | On no activation data | 
| **ACTIVE**        | Update metadata source (different & real)                | **ACTIVATING**   | - |
| **ACTIVE**        | Update metadata source to `NONE`                         | **DRAFT**        | - |
| **ACTIVE**        | `updateXXXProfileConfiguration` (different value, still ≥1 active profile) | **ACTIVATING** | Triggers re-activation |
| **ACTIVE**        | `deactivate()`                                           | **INACTIVE**     | - |
| **ACTIVE**        | Update metadata source to `NONE` or disable all profiles | **DRAFT**        | - |
| **INACTIVE**      | `activate()` + has real metadata source + ≥1 active profile | **ACTIVATING** | Success path |
| **INACTIVE**      | `activate()` when missing real metadata source or no active profiles | **DRAFT**      | Requirements not met after explicit `activate()` call |
| **INACTIVE**      | `updateMetadataSource()` (any change)                    | **INACTIVE**     | Version bumped if changed |
| **INACTIVE**      | `updateXXXProfileConfiguration()` (any change)              | **INACTIVE**     | Version bumped if changed |

**Clarification on updateXXXProfileConfiguration()**

**Important Note on Activation Path**:
All trust relationships follow the **same consistent path**:  
`READY` → `ACTIVATING` → `ACTIVE` (or back to `READY`/`DRAFT` if activation is cancelled or fails).  
There is **no** direct `READY` → `ACTIVE` transition for any nature.

---

#### 3. Global / Aggregate Invariants (Always True)

- `key` must not be null and must be assigned.
- `displayName` must not be null and must not be blank.
- `description` must not be null (can be blank).
- `nature` must not be null.
- `status` must not be null.
- `version` must not be null and must be ≥ `Version.initial()`.
- `metadataSource` must not be null.
- `discoveredEntityIds` must not be null.
- All profile configuration objects must not be null (they must be initialized to their default disabled state).
- `releasedAttributes` must not be null.

---

#### 4. Operations and Transition Rules

**4.1 `create(...)` (Static Factory Method)**
- Parameters: `displayName`, `description`, `nature`
- Target State: `DRAFT`
- Conditions: `displayName` (non-blank), `description` (not null), `nature` required.
- Post-conditions: All profiles initialized to default disabled state, `releasedAttributes` is empty, version = initial.

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

**4.9 `finalizeActivation(ActivationContext)`**
- Allowed from: `ACTIVATING`
- Parameters: `ActivationContext` (contains diagnostic data, verification results, errors, etc.)
- Returns: `TrustResult<TrustRelationship>`
- On Success → Target State: `ACTIVE`
- On Failure → Target State: `READY`
- On No Data → Target State: `ACTIVATING`
- Version bumped in both cases.

**4.10 `incorporateDiscoveredEntityIds(...)`**
- Allowed from: `ACTIVATING` (AGGREGATE only)
- Does **not** change state by itself (only updates data).
- Version bumped if the discovered entity IDs actually change.

**4.11 `updateReleasedAttributes(...)`**
- Allowed from: All states except `ACTIVATING`
- Target State: Same state
- Version bumped if changed.
