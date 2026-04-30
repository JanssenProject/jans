# JWT Authorization Decision Matrix

This document is the authoritative human-readable summary of every Cedar authorization
rule enforced by the Cedarling-OPA JWT integration. Keep it in sync whenever a policy
file in `policy-store/policies/` is added, changed, or removed.

Unlike the [unsigned demo](../terraform/PERMISSIONS.md), this model has **no user roles**.
Authorization is based entirely on claims inside the GitHub Actions OIDC JWT:

| JWT claim      | What it proves                                                   |
|----------------|------------------------------------------------------------------|
| `iss`          | Token was issued by `https://token.actions.githubusercontent.com` |
| `repository`   | The GitHub repository that triggered the workflow                |
| `ref`          | The Git ref (branch/tag) the workflow ran on                     |
| `environment`  | GitHub Environment name (only present after a reviewer approved) |

Cedar is **deny-by-default**. A missing permit is a deny. No Cedar policy allows
`Destroy` for CI pipelines — that action always requires a human operator using the
unsigned demo.

---

## Policy files

| Policy file                                   | Cedar `@id`                              | What it grants                                            |
|-----------------------------------------------|------------------------------------------|-----------------------------------------------------------|
| `ci-permit-plan.cedar`                        | `ci-permit-plan`                         | `Plan` from a trusted repo, any branch, any workspace     |
| `ci-permit-apply-main-non-prod.cedar`         | `ci-permit-apply-main-non-prod`          | `Apply` from `main` on a trusted repo, non-prod workspace |
| `ci-permit-apply-prod-via-environment.cedar`  | `ci-permit-apply-prod-via-environment`   | `Apply` to `production` after a GitHub Environment review |

---

## Summary matrix

`✓` = ALLOWED  `✗` = DENIED

| Scenario                                               | Plan | Apply | Destroy |
|--------------------------------------------------------|:----:|:-----:|:-------:|
| Any branch, trusted repo                               |  ✓   |   ✗   |    ✗    |
| `main` branch, trusted repo, non-prod workspace        |  ✓   |   ✓   |    ✗    |
| `main` branch, trusted repo, `environment=production`  |  ✓   |   ✓   |    ✗    |
| Feature branch, trusted repo, any workspace            |  ✓   |   ✗   |    ✗    |
| Untrusted repo (any branch, any workspace)             |  ✗   |   ✗   |    ✗    |
| Any CI workflow                                        |  —   |   —   |    ✗    |

### Rule summaries

- **Plan** — any branch from a trusted repository may run `terraform plan` on any
  workspace. Branch and workspace are unconstrained; only the `repository` and `iss`
  claims are checked.
- **Apply (non-prod)** — `main` branch only, trusted repo, and the target workspace
  must not be `production`. Feature branches and untrusted repos are always denied.
- **Apply (production)** — `main` branch, trusted repo, target workspace `production`,
  **and** the token must carry `environment == "production"`. GitHub only injects that
  claim when a designated reviewer has approved the GitHub Environment gate before the
  job ran.
- **Destroy** — always denied for CI pipelines. No permit policy covers this action.
  A human operator using the unsigned demo is required.

---

## Complete Decision Table

Full enumeration of every tested scenario:

| Repository      | Ref                   | environment claim | Action  | Workspace   | Decision | Governing policy                          |
|-----------------|-----------------------|-------------------|---------|-------------|----------|-------------------------------------------|
| octoorg/myrepo  | refs/heads/main       | (absent)          | Plan    | staging     | ALLOWED  | ci-permit-plan                            |
| octoorg/myrepo  | refs/heads/feature-x  | (absent)          | Plan    | staging     | ALLOWED  | ci-permit-plan (no ref restriction)       |
| octoorg/myrepo  | refs/heads/main       | (absent)          | Plan    | production  | ALLOWED  | ci-permit-plan (no workspace restriction) |
| untrusted/repo  | refs/heads/main       | (absent)          | Plan    | staging     | DENIED   | repository not in trusted list            |
| octoorg/myrepo  | refs/heads/main       | (absent)          | Apply   | staging     | ALLOWED  | ci-permit-apply-main-non-prod             |
| octoorg/myrepo  | refs/heads/feature-x  | (absent)          | Apply   | staging     | DENIED   | ci-permit-apply-main-non-prod requires main |
| octoorg/myrepo  | refs/heads/main       | (empty string)    | Apply   | staging     | ALLOWED  | ci-permit-apply-main-non-prod (environment not checked for non-prod) |
| untrusted/repo  | refs/heads/main       | (absent)          | Apply   | staging     | DENIED   | repository not in trusted list            |
| octoorg/myrepo  | refs/heads/main       | production        | Apply   | production  | ALLOWED  | ci-permit-apply-prod-via-environment      |
| octoorg/myrepo  | refs/heads/main       | (absent)          | Apply   | production  | DENIED   | ci-permit-apply-main-non-prod excludes production; prod policy requires environment claim |
| octoorg/myrepo  | refs/heads/main       | (empty string)    | Apply   | production  | DENIED   | environment claim present but not equal to "production" |
| octoorg/myrepo  | refs/heads/main       | staging           | Apply   | production  | DENIED   | environment claim not equal to "production" |
| octoorg/myrepo  | refs/heads/feature-x  | production        | Apply   | production  | DENIED   | ci-permit-apply-prod-via-environment requires main |
| untrusted/repo  | refs/heads/main       | production        | Apply   | production  | DENIED   | repository not in trusted list            |
| octoorg/myrepo  | refs/heads/main       | (absent)          | Destroy | staging     | DENIED   | no policy grants Destroy to CI pipelines  |
| octoorg/myrepo  | refs/heads/main       | production        | Destroy | production  | DENIED   | no policy grants Destroy to CI pipelines  |
| octoorg/myrepo  | refs/heads/feature-x  | (absent)          | Destroy | staging     | DENIED   | no policy grants Destroy to CI pipelines  |

---

## Key rules to remember

1. **No implicit deny override** — Cedar is deny-by-default; a missing permit is a deny.
2. **Destroy is always denied for CI** — no permit policy covers the `Destroy` action.
   Infrastructure teardown must go through a human operator using the unsigned demo.
3. **Plan is branch-agnostic** — any branch from a trusted repository may plan. Only
   the `repository` and `iss` claims are checked; `ref` and `environment` are ignored.
4. **Apply requires `main`** — both non-prod and production apply policies require
   `ref == "refs/heads/main"`. Feature branches can never apply, regardless of the
   workspace or environment claim.
5. **Production apply requires the GitHub Environment gate** — the `environment` claim
   must be present **and** equal to `"production"`. An absent claim, an empty string,
   or any other value (e.g. `"staging"`) is a deny. GitHub only injects this claim after
   a designated reviewer approves the `production` GitHub Environment.
6. **Untrusted repositories are always denied** — every permit policy checks
   `repository` against the trusted list. A token from any other repository is denied
   for all three actions.
7. **JWT signature is the trust anchor** — in production mode, Cedarling validates the
   token signature against GitHub's JWKS endpoint. A locally crafted JWT with the
   correct claims but no valid signature is rejected before Cedar evaluation.

---

## Keeping this document in sync

When modifying Cedar policies:

1. Edit or add the `.cedar` file in `policy-store/policies/`.
2. Add the corresponding test cases to `test-cases.yml`.
3. Update the tables in this file to reflect the new grants or restrictions.

---

## How to extend the model

### Adding a new trusted repository

All three policy files contain a `repository` allowlist:

```cedar
(
    context.tokens.ci_githubworkflow.repository == "octoorg/myrepo" ||
    context.tokens.ci_githubworkflow.repository == "JanssenProject/jans"
)
```

Add an `||` clause in each of the three `.cedar` files. No schema change is needed.

---

### Adding a new non-production workspace

No policy change is required. `ci-permit-plan` has no workspace restriction, and
`ci-permit-apply-main-non-prod` only excludes `CI::TerraformWorkspace::"production"` —
any new workspace name is automatically permitted for plan and non-prod apply.

---

### Adding a new protected workspace (like a second production environment)

Extend the `resource !=` guard in `ci-permit-apply-main-non-prod` to exclude the new
workspace, then create a new policy file (modelled after
`ci-permit-apply-prod-via-environment`) that requires the matching `environment` claim:

```cedar
// ci-permit-apply-staging-prod-via-environment.cedar
@id("ci-permit-apply-staging-prod-via-environment")
permit (
    principal,
    action == CI::Action::"Apply",
    resource == CI::TerraformWorkspace::"staging-prod"
) when {
    context.tokens has ci_githubworkflow &&
    context.tokens.ci_githubworkflow.iss == CI::TrustedIssuer::"https://token.actions.githubusercontent.com" &&
    (
        context.tokens.ci_githubworkflow.repository == "octoorg/myrepo" ||
        context.tokens.ci_githubworkflow.repository == "JanssenProject/jans"
    ) &&
    context.tokens.ci_githubworkflow.ref == "refs/heads/main" &&
    context.tokens.ci_githubworkflow has environment &&
    context.tokens.ci_githubworkflow.environment == "staging-prod"
};
```

---

### Checklist

Before opening a pull request, confirm that you have:

- [ ] Created (or updated) the `.cedar` file in `policy-store/policies/`
- [ ] Updated `policy-store/schema.cedarschema` **only if** a new action was added
- [ ] Added ALLOWED and DENIED test cases in `test-cases.yml` for every new
      repository/ref/environment/workspace/action combination
- [ ] Updated the Policy files table, the Summary matrix, and the Complete Decision
      Table in this file
- [ ] Verified that the trusted `repository` allowlist is consistent across all
      policy files if you added a new repository
