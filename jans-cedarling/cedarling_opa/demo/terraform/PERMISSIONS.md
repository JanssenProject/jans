# Role Permission Matrix

This document is the authoritative human-readable summary of every Cedar authorization
rule enforced by the Cedarling-OPA integration. Keep it in sync whenever a policy file
in `policy-store/policies/` is added, changed, or removed.

## Roles

| Role      | Cedar entity                  | Governing policy file                  |
|-----------|-------------------------------|----------------------------------------|
| Developer | `Infra::Role::"Developer"`    | `developer-permit-plan.cedar`          |
| Ops       | `Infra::Role::"Ops"`          | `ops-permit-plan-apply.cedar`          |
| Admin     | `Infra::Role::"Admin"`        | `admin-permit-all.cedar`               |

Users may hold more than one role simultaneously (comma-separated in `TF_USER_ROLES`).
Cedar evaluates all role memberships together; the **union** of grants applies — a single
matching permit is sufficient for the decision to be ALLOWED.

---

## Single-Role Matrix

`✓` = ALLOWED  `✗` = DENIED

| Role      | plan · staging | plan · production | apply · staging | apply · production | destroy · staging | destroy · production |
|-----------|:--------------:|:-----------------:|:---------------:|:------------------:|:-----------------:|:--------------------:|
| Developer |       ✓        |         ✓         |        ✗        |         ✗          |         ✗         |          ✗           |
| Ops       |       ✓        |         ✗         |        ✓        |         ✗          |         ✗         |          ✗           |
| Admin     |       ✓        |         ✓         |        ✓        |         ✓          |         ✓         |          ✓           |

### Rule summaries

- **Developer** — `terraform plan` on **any** workspace; no apply or destroy anywhere.
- **Ops** — `terraform plan` and `terraform apply` on **non-production** workspaces only;
  no destroy anywhere; no access to production at all.
- **Admin** — all three actions (`plan`, `apply`, `destroy`) on **any** workspace,
  including production.

---

## Multi-Role Matrix

When a user holds more than one role the effective permission is the union of all
individual role grants. Admin's permit-all policy dominates every combination that
includes the Admin role.

| Role combination          | plan · staging | plan · production | apply · staging | apply · production | destroy · staging | destroy · production |
|---------------------------|:--------------:|:-----------------:|:---------------:|:------------------:|:-----------------:|:--------------------:|
| Developer + Ops           |       ✓        |         ✓         |        ✓        |         ✗          |         ✗         |          ✗           |
| Developer + Admin         |       ✓        |         ✓         |        ✓        |         ✓          |         ✓         |          ✓           |
| Ops + Admin               |       ✓        |         ✓         |        ✓        |         ✓          |         ✓         |          ✓           |
| Developer + Ops + Admin   |       ✓        |         ✓         |        ✓        |         ✓          |         ✓         |          ✓           |

### Rule summaries

- **Developer + Ops** — plan anywhere (Developer covers production; Ops covers staging);
  apply on staging only (Ops grant); no destroy anywhere; no apply on production.
- **Developer + Admin** — unrestricted (Admin permit-all dominates).
- **Ops + Admin** — unrestricted (Admin permit-all dominates).
- **Developer + Ops + Admin** — unrestricted (Admin permit-all dominates).

---

## Complete Decision Table

Full enumeration of every scenario covered by `test-cases.yml`:

| Role(s)           | Action  | Workspace  | Decision | Governing policy                          |
|-------------------|---------|------------|----------|-------------------------------------------|
| Admin             | plan    | staging    | ALLOWED  | admin-permit-all                          |
| Admin             | apply   | staging    | ALLOWED  | admin-permit-all                          |
| Admin             | destroy | staging    | ALLOWED  | admin-permit-all                          |
| Admin             | plan    | production | ALLOWED  | admin-permit-all                          |
| Admin             | apply   | production | ALLOWED  | admin-permit-all                          |
| Admin             | destroy | production | ALLOWED  | admin-permit-all                          |
| Ops               | plan    | staging    | ALLOWED  | ops-permit-plan-apply-non-prod            |
| Ops               | apply   | staging    | ALLOWED  | ops-permit-plan-apply-non-prod            |
| Ops               | destroy | staging    | DENIED   | no policy grants Ops the Destroy action   |
| Ops               | plan    | production | DENIED   | ops-permit-plan-apply-non-prod excludes production |
| Ops               | apply   | production | DENIED   | ops-permit-plan-apply-non-prod excludes production |
| Ops               | destroy | production | DENIED   | no policy grants Ops the Destroy action   |
| Developer         | plan    | staging    | ALLOWED  | developer-permit-plan                     |
| Developer         | apply   | staging    | DENIED   | no policy grants Developer the Apply action |
| Developer         | destroy | staging    | DENIED   | no policy grants Developer the Destroy action |
| Developer         | plan    | production | ALLOWED  | developer-permit-plan (no workspace restriction) |
| Developer         | apply   | production | DENIED   | no policy grants Developer the Apply action |
| Developer         | destroy | production | DENIED   | no policy grants Developer the Destroy action |
| Developer + Ops   | plan    | staging    | ALLOWED  | developer-permit-plan and ops-permit-plan-apply-non-prod |
| Developer + Ops   | apply   | staging    | ALLOWED  | ops-permit-plan-apply-non-prod            |
| Developer + Ops   | destroy | staging    | DENIED   | neither Developer nor Ops grants Destroy  |
| Developer + Ops   | plan    | production | ALLOWED  | developer-permit-plan (no workspace restriction) |
| Developer + Ops   | apply   | production | DENIED   | Ops excludes production; Developer does not grant Apply |
| Developer + Ops   | destroy | production | DENIED   | neither Developer nor Ops grants Destroy  |
| Ops + Admin       | plan    | staging    | ALLOWED  | admin-permit-all (and ops-permit-plan-apply-non-prod) |
| Ops + Admin       | apply   | production | ALLOWED  | admin-permit-all                          |
| Ops + Admin       | destroy | production | ALLOWED  | admin-permit-all                          |
| Developer + Admin | plan    | staging    | ALLOWED  | admin-permit-all (and developer-permit-plan) |
| Developer + Admin | apply   | staging    | ALLOWED  | admin-permit-all                          |
| Developer + Admin | destroy | staging    | ALLOWED  | admin-permit-all                          |
| Developer + Admin | plan    | production | ALLOWED  | admin-permit-all (and developer-permit-plan) |
| Developer + Admin | apply   | production | ALLOWED  | admin-permit-all                          |
| Developer + Admin | destroy | production | ALLOWED  | admin-permit-all                          |

---

## Key rules to remember

1. **No implicit deny override** — Cedar is deny-by-default; a missing permit is a deny.
2. **Admin dominates all combinations** — any identity that holds the Admin role is
   effectively unrestricted.
3. **Ops is non-production only** — the `ops-permit-plan-apply-non-prod` policy
   explicitly excludes `Infra::TerraformWorkspace::"production"`.
4. **Developer cannot mutate** — plan only, regardless of workspace.
5. **Destroy requires Admin** — no other role (or combination of non-Admin roles) grants
   the Destroy action.

---

## Keeping this document in sync

When modifying Cedar policies:

1. Edit or add the `.cedar` file in `policy-store/policies/`.
2. Add the corresponding test cases to `test-cases.yml`.
3. Update the tables in this file to reflect the new grants or restrictions.

---

## How to extend the model

This section is a step-by-step guide for the two most common extension tasks: adding a
**new role** and adding a **new workspace**. Both examples follow the same four-step
pattern.

### Step 1 — Edit the Cedar schema (only when needed)

The schema lives in `policy-store/schema.cedarschema`.

**You need to touch the schema when you are adding a new action** (e.g. a
`Refresh` sub-command). Roles and workspaces are untyped entity literals in
Cedar — they do not need to be listed in the schema ahead of time.

```cedar
# schema.cedarschema — adding a hypothetical new action
action Plan, Apply, Destroy, Refresh      # ← add the new action name here
    appliesTo {
        principal: [User],
        resource:  [TerraformWorkspace],
    };
```

For a new role or a new workspace, **skip this step** — no schema change is
required.

---

### Step 2 — Write the Cedar policy file

Create a new `.cedar` file in `policy-store/policies/`. Name it after the role
or the permission it grants, following the existing kebab-case convention.

#### Example A — Adding a "ReadOnly" role

`policy-store/policies/readonly-permit-plan.cedar`

```cedar
// ReadOnly engineers may only run terraform plan on any workspace.
// They cannot apply or destroy any environment.
@id("readonly-permit-plan")
permit (
    principal,
    action == Infra::Action::"Plan",
    resource
) when {
    principal.role.contains("ReadOnly")
};
```

Key points:
- The role string (`"ReadOnly"`) is the value that must appear in the user's
  `TF_USER_ROLES` environment variable at runtime.
- Roles are checked via `principal.role.contains(...)` because Cedarling's
  unsigned-authorize path stores the `role` array as a `Set<String>` attribute
  on the User entity rather than as Cedar entity-hierarchy parents.
- Cedar is deny-by-default, so you only need to write permits — anything not
  explicitly allowed is denied automatically.

#### Example B — Adding a "dev" workspace restriction

Workspaces are opaque string identifiers; no schema change is needed. To
restrict an existing role to only non-production **and** non-dev workspaces,
extend the `when` clause of its policy:

```cedar
// Ops engineers may plan or apply only on non-production, non-dev workspaces.
@id("ops-permit-plan-apply-non-prod")
permit (
    principal,
    action in [
        Infra::Action::"Plan",
        Infra::Action::"Apply"
    ],
    resource
) when {
    principal.role.contains("Ops") &&
    resource != Infra::TerraformWorkspace::"production" &&
    resource != Infra::TerraformWorkspace::"dev"
};
```

To **grant** a role access to the new workspace, no policy change may be needed
at all — a role whose policy has no workspace restriction (like `Developer` or
`Admin`) automatically covers any workspace string, including `"dev"`.

---

### Step 3 — Add test cases to `test-cases.yml`

Every new role, workspace, or action combination must have explicit test cases.
The test runner picks them up automatically — no changes to the workflow YAML
or the runner script are required.

Add one entry per scenario. Cover both the expected **ALLOWED** and **DENIED**
paths:

```yaml
# ── ReadOnly role ────────────────────────────────────────────────────────────

- name:        "ReadOnly — plan on staging"
  user_id:     ci-readonly-bot
  roles:       ReadOnly
  workspace:   staging
  subcommand:  plan
  expected:    ALLOWED
  policy_note: readonly-permit-plan

- name:        "ReadOnly — apply on staging (must be DENIED)"
  user_id:     ci-readonly-bot
  roles:       ReadOnly
  workspace:   staging
  subcommand:  apply
  expected:    DENIED
  policy_note: no policy grants ReadOnly the Apply action

- name:        "ReadOnly — plan on dev"
  user_id:     ci-readonly-bot
  roles:       ReadOnly
  workspace:   dev
  subcommand:  plan
  expected:    ALLOWED
  policy_note: readonly-permit-plan has no workspace restriction
```

Guidelines for good test coverage:
- One case for each action the role **should** be allowed to perform.
- One case for each action the role **should not** be able to perform.
- At least one case for every workspace the role is expected to reach.
- At least one case confirming access is denied for workspaces the role must
  not reach.
- If the role can be combined with existing roles, add multi-role cases too
  (see the `Developer+Ops` block in `test-cases.yml` for the pattern).

---

### Step 4 — Update this document

Update the tables in PERMISSIONS.md so the human-readable view stays in sync
with the Cedar policies:

1. **Roles table** — add a row for the new role with its Cedar entity string and
   governing policy file name.
2. **Single-Role Matrix** — add a column for each new workspace, or a row for
   each new role, and fill in `✓`/`✗` for every cell.
3. **Rule summaries** — add a bullet that describes in plain English what the
   new role or workspace restriction allows and forbids.
4. **Complete Decision Table** — add one row per new test case, matching the
   entries you just added to `test-cases.yml`.
5. **Key rules to remember** — add a bullet if the new role introduces a
   behavioural invariant that reviewers should know about (e.g.
   _"ReadOnly can never mutate"_).

---

### Checklist

Before opening a pull request, confirm that you have:

- [ ] Created (or updated) the `.cedar` file in `policy-store/policies/`
- [ ] Updated `policy-store/schema.cedarschema` **only if** a new action was added
- [ ] Added ALLOWED and DENIED test cases in `test-cases.yml` for every new
      role/workspace/action combination
- [ ] Updated the Roles table, the Single-Role Matrix, the Rule summaries, and
      the Complete Decision Table in this file
- [ ] Verified that the `TF_USER_ROLES` documentation (README or workflow
      comments) mentions the new role name if operators need to configure it

---

## Worked example — adding a "dev" workspace and a "ReadOnly" role together

This example walks through all four steps end-to-end for a realistic scenario:
a new `dev` workspace is introduced alongside a new `ReadOnly` role, and the
multi-role interaction between `ReadOnly` and the existing `Developer` and `Ops`
roles is spelled out explicitly.

**Desired behaviour after the change**

| Role         | plan · dev | apply · dev | destroy · dev |
|--------------|:----------:|:-----------:|:-------------:|
| ReadOnly     |     ✓      |      ✗      |       ✗       |
| Developer    |     ✓      |      ✗      |       ✗       |
| Ops          |     ✓      |      ✓      |       ✗       |
| Admin        |     ✓      |      ✓      |       ✓       |
| ReadOnly+Ops |     ✓      |      ✓      |       ✗       |
| ReadOnly+Developer |     ✓      |      ✗      |       ✗       |

Key decisions:
- `ReadOnly` may only run `terraform plan`, on any workspace — same as
  `Developer` but without any mutation capability now or in the future.
- `Ops` already covers the `dev` workspace because its `when` clause only
  excludes `production`; no policy change is needed for Ops.
- `Developer` and `Admin` are also unaffected — their policies have no
  workspace restriction.
- Adding `ReadOnly+Ops` in a multi-role combination grants `Apply` on `dev`
  (and `staging`) because Ops already permits that on non-production workspaces
  and Cedar unions the grants.

---

### Step 1 — Edit the Cedar schema

**No schema change is needed.** Roles and workspaces are opaque entity
literals in Cedar — they do not need to be declared in the schema ahead of
time. The schema only needs to change when a brand-new *action* is added (e.g.
a `Refresh` sub-command).

The existing schema already covers the `dev` workspace and the `ReadOnly` role
without any modification:

```cedar
# policy-store/schema.cedarschema — unchanged
namespace Infra {
    entity Role;
    entity User in [Role] { sub: String, role: Set<String> };
    entity TerraformWorkspace;

    action Plan, Apply, Destroy
        appliesTo {
            principal: [User],
            resource:  [TerraformWorkspace],
        };
}
```

---

### Step 2 — Write the Cedar policy file

Create one new policy file for the `ReadOnly` role.  No existing policy files
need to change because `Developer`, `Ops`, and `Admin` already handle the `dev`
workspace correctly through their existing `when` conditions (or lack thereof).

**`policy-store/policies/readonly-permit-plan.cedar`**

```cedar
// ReadOnly engineers may only run terraform plan on any workspace.
// They cannot apply or destroy any environment, including dev and production.
@id("readonly-permit-plan")
permit (
    principal,
    action    == Infra::Action::"Plan",
    resource
) when {
    principal.role.contains("ReadOnly")
};
```

Key points:
- The role string `"ReadOnly"` must match the value in `TF_USER_ROLES` exactly
  (case-sensitive).
- Roles are checked via `principal.role.contains(...)` because Cedarling's
  unsigned-authorize path stores the `role` array as a `Set<String>` attribute.
- Cedar is deny-by-default: only the `Plan` action is granted; `Apply` and
  `Destroy` are automatically denied.

You do **not** need to edit `ops-permit-plan-apply.cedar` because its existing
`when` condition already covers `dev`:

```cedar
// ops-permit-plan-apply.cedar (existing — no change required)
@id("ops-permit-plan-apply-non-prod")
permit (
    principal,
    action in [Infra::Action::"Plan", Infra::Action::"Apply"],
    resource
) when {
    principal.role.contains("Ops") &&
    resource != Infra::TerraformWorkspace::"production"
};
```

The condition excludes only `production`, so `dev` (and `staging`, and any
future non-production workspace) is automatically included.

---

### Step 3 — Add test cases to `test-cases.yml`

Add the following block to `test-cases.yml`.  Cover every action for the new
role on both the new workspace and the existing workspaces, and include at
least one multi-role combination.

```yaml
  # ── ReadOnly role ─────────────────────────────────────────────────────────
  # readonly-permit-plan: ReadOnly engineers may only run terraform plan,
  # on any workspace.  Apply and destroy are never permitted.

  - name:        "ReadOnly — plan on dev"
    user_id:     ci-readonly-bot
    roles:       ReadOnly
    workspace:   dev
    subcommand:  plan
    expected:    ALLOWED
    policy_note: readonly-permit-plan has no workspace restriction

  - name:        "ReadOnly — apply on dev (must be DENIED)"
    user_id:     ci-readonly-bot
    roles:       ReadOnly
    workspace:   dev
    subcommand:  apply
    expected:    DENIED
    policy_note: no policy grants ReadOnly the Apply action

  - name:        "ReadOnly — destroy on dev (must be DENIED)"
    user_id:     ci-readonly-bot
    roles:       ReadOnly
    workspace:   dev
    subcommand:  destroy
    expected:    DENIED
    policy_note: no policy grants ReadOnly the Destroy action

  - name:        "ReadOnly — plan on staging"
    user_id:     ci-readonly-bot
    roles:       ReadOnly
    workspace:   staging
    subcommand:  plan
    expected:    ALLOWED
    policy_note: readonly-permit-plan has no workspace restriction

  - name:        "ReadOnly — apply on staging (must be DENIED)"
    user_id:     ci-readonly-bot
    roles:       ReadOnly
    workspace:   staging
    subcommand:  apply
    expected:    DENIED
    policy_note: no policy grants ReadOnly the Apply action

  - name:        "ReadOnly — plan on production"
    user_id:     ci-readonly-bot
    roles:       ReadOnly
    workspace:   production
    subcommand:  plan
    expected:    ALLOWED
    policy_note: readonly-permit-plan has no workspace restriction

  - name:        "ReadOnly — apply on production (must be DENIED)"
    user_id:     ci-readonly-bot
    roles:       ReadOnly
    workspace:   production
    subcommand:  apply
    expected:    DENIED
    policy_note: no policy grants ReadOnly the Apply action

  # ── Multi-role: ReadOnly + Ops ─────────────────────────────────────────────
  # ReadOnly grants Plan anywhere; Ops grants Plan+Apply on non-production.
  # Union: the combined user can plan anywhere, apply on dev and staging, but
  # cannot destroy anywhere and cannot apply on production.

  - name:        "ReadOnly+Ops — plan on dev"
    user_id:     ci-readonly-ops-bot
    roles:       "ReadOnly,Ops"
    workspace:   dev
    subcommand:  plan
    expected:    ALLOWED
    policy_note: both readonly-permit-plan and ops-permit-plan-apply-non-prod allow this

  - name:        "ReadOnly+Ops — apply on dev (ALLOWED via Ops)"
    user_id:     ci-readonly-ops-bot
    roles:       "ReadOnly,Ops"
    workspace:   dev
    subcommand:  apply
    expected:    ALLOWED
    policy_note: ops-permit-plan-apply-non-prod grants Apply on dev; ReadOnly alone cannot

  - name:        "ReadOnly+Ops — destroy on dev (must be DENIED)"
    user_id:     ci-readonly-ops-bot
    roles:       "ReadOnly,Ops"
    workspace:   dev
    subcommand:  destroy
    expected:    DENIED
    policy_note: neither ReadOnly nor Ops grants the Destroy action

  - name:        "ReadOnly+Ops — apply on production (must be DENIED)"
    user_id:     ci-readonly-ops-bot
    roles:       "ReadOnly,Ops"
    workspace:   production
    subcommand:  apply
    expected:    DENIED
    policy_note: Ops excludes production; ReadOnly does not grant Apply; no policy covers this

  # ── Multi-role: ReadOnly + Developer ──────────────────────────────────────
  # Both roles grant only Plan.  The union is still Plan-only on any workspace.

  - name:        "ReadOnly+Developer — plan on dev"
    user_id:     ci-readonly-dev-bot
    roles:       "ReadOnly,Developer"
    workspace:   dev
    subcommand:  plan
    expected:    ALLOWED
    policy_note: both readonly-permit-plan and developer-permit-plan allow this

  - name:        "ReadOnly+Developer — apply on dev (must be DENIED)"
    user_id:     ci-readonly-dev-bot
    roles:       "ReadOnly,Developer"
    workspace:   dev
    subcommand:  apply
    expected:    DENIED
    policy_note: neither ReadOnly nor Developer grants the Apply action

  - name:        "ReadOnly+Developer — apply on staging (must be DENIED)"
    user_id:     ci-readonly-dev-bot
    roles:       "ReadOnly,Developer"
    workspace:   staging
    subcommand:  apply
    expected:    DENIED
    policy_note: neither ReadOnly nor Developer grants the Apply action

  # ── Ops on dev (existing role, new workspace — no policy change needed) ───

  - name:        "Ops — plan on dev"
    user_id:     ci-ops-bot
    roles:       Ops
    workspace:   dev
    subcommand:  plan
    expected:    ALLOWED
    policy_note: ops-permit-plan-apply-non-prod; dev is not production

  - name:        "Ops — apply on dev"
    user_id:     ci-ops-bot
    roles:       Ops
    workspace:   dev
    subcommand:  apply
    expected:    ALLOWED
    policy_note: ops-permit-plan-apply-non-prod; dev is not production

  - name:        "Ops — destroy on dev (must be DENIED)"
    user_id:     ci-ops-bot
    roles:       Ops
    workspace:   dev
    subcommand:  destroy
    expected:    DENIED
    policy_note: no policy grants Ops the Destroy action

  # ── Developer on dev (existing role, new workspace — no policy change) ────

  - name:        "Developer — plan on dev"
    user_id:     ci-dev-bot
    roles:       Developer
    workspace:   dev
    subcommand:  plan
    expected:    ALLOWED
    policy_note: developer-permit-plan has no workspace restriction

  - name:        "Developer — apply on dev (must be DENIED)"
    user_id:     ci-dev-bot
    roles:       Developer
    workspace:   dev
    subcommand:  apply
    expected:    DENIED
    policy_note: no policy grants Developer the Apply action
```

---

### Step 4 — Update PERMISSIONS.md

Apply the following four edits to this file:

#### 4a — Roles table

Add a row for `ReadOnly`:

```markdown
| ReadOnly  | `Infra::Role::"ReadOnly"` | `readonly-permit-plan.cedar`           |
```

#### 4b — Single-Role Matrix

Add a `ReadOnly` row and a `dev` workspace column set.  The full updated table
(showing all four workspaces including the new `dev` column) would look like:

```markdown
| Role      | plan · dev | plan · staging | plan · prod | apply · dev | apply · staging | apply · prod | destroy · dev | destroy · staging | destroy · prod |
|-----------|:----------:|:--------------:|:-----------:|:-----------:|:---------------:|:------------:|:-------------:|:-----------------:|:--------------:|
| ReadOnly  |     ✓      |       ✓        |      ✓      |      ✗      |        ✗        |      ✗       |       ✗       |         ✗         |       ✗        |
| Developer |     ✓      |       ✓        |      ✓      |      ✗      |        ✗        |      ✗       |       ✗       |         ✗         |       ✗        |
| Ops       |     ✓      |       ✓        |      ✗      |      ✓      |        ✓        |      ✗       |       ✗       |         ✗         |       ✗        |
| Admin     |     ✓      |       ✓        |      ✓      |      ✓      |        ✓        |      ✓       |       ✓       |         ✓         |       ✓        |
```

#### 4c — Rule summaries

Add a bullet for `ReadOnly`:

> **ReadOnly** — `terraform plan` on **any** workspace; no apply or destroy
> anywhere.  Effectively a view-only role.  When combined with `Ops`, the `Ops`
> grant promotes the combined user to plan+apply on non-production workspaces.

Add a note under Key rules:

> **ReadOnly cannot destroy** — the `readonly-permit-plan` policy grants only
> the `Plan` action.  `Apply` can be unlocked when combined with a role that
> permits it (e.g. `ReadOnly+Ops` grants `Apply` on non-production workspaces
> via the Ops policy), but `Destroy` always requires the `Admin` role — no
> combination of non-Admin roles can ever grant it.

#### 4d — Complete Decision Table

Add one row per new test case following the same format as the existing rows,
for example:

```markdown
| ReadOnly            | plan    | dev        | ALLOWED  | readonly-permit-plan                                    |
| ReadOnly            | apply   | dev        | DENIED   | no policy grants ReadOnly the Apply action              |
| ReadOnly            | destroy | dev        | DENIED   | no policy grants ReadOnly the Destroy action            |
| ReadOnly            | plan    | staging    | ALLOWED  | readonly-permit-plan                                    |
| ReadOnly            | apply   | staging    | DENIED   | no policy grants ReadOnly the Apply action              |
| ReadOnly            | plan    | production | ALLOWED  | readonly-permit-plan (no workspace restriction)         |
| ReadOnly            | apply   | production | DENIED   | no policy grants ReadOnly the Apply action              |
| ReadOnly + Ops      | plan    | dev        | ALLOWED  | readonly-permit-plan and ops-permit-plan-apply-non-prod |
| ReadOnly + Ops      | apply   | dev        | ALLOWED  | ops-permit-plan-apply-non-prod (dev is not production)  |
| ReadOnly + Ops      | destroy | dev        | DENIED   | neither ReadOnly nor Ops grants Destroy                 |
| ReadOnly + Ops      | apply   | production | DENIED   | Ops excludes production; ReadOnly does not grant Apply  |
| ReadOnly + Developer| plan    | dev        | ALLOWED  | both readonly-permit-plan and developer-permit-plan     |
| ReadOnly + Developer| apply   | dev        | DENIED   | neither ReadOnly nor Developer grants Apply             |
| Ops                 | plan    | dev        | ALLOWED  | ops-permit-plan-apply-non-prod (dev is not production)  |
| Ops                 | apply   | dev        | ALLOWED  | ops-permit-plan-apply-non-prod (dev is not production)  |
| Ops                 | destroy | dev        | DENIED   | no policy grants Ops the Destroy action                 |
| Developer           | plan    | dev        | ALLOWED  | developer-permit-plan (no workspace restriction)        |
| Developer           | apply   | dev        | DENIED   | no policy grants Developer the Apply action             |
```

---

### What this example shows about multi-role semantics

The `ReadOnly+Ops` combination is the most instructive case:

1. `ReadOnly` grants `Plan` on **any** workspace — including `production`.
2. `Ops` grants `Plan` and `Apply` on **non-production** workspaces only.
3. Cedar takes the **union**: the combined principal can `Plan` anywhere
   (from `ReadOnly`) *and* `Apply` on non-production workspaces (from `Ops`).
4. Neither role grants `Destroy`, so destroy remains denied everywhere.
5. `Apply` on `production` is still denied because no individual policy permits
   it: `ReadOnly` never grants `Apply`, and `Ops` explicitly excludes
   `production`.

The `ReadOnly+Developer` combination shows the opposite: when two roles that
each grant only `Plan` are combined, the union is still just `Plan` — holding
more roles of the same capability does not elevate privileges.

This confirms the general invariant: **the only way to gain a new action is for
at least one held role's policy to explicitly permit that action.**
