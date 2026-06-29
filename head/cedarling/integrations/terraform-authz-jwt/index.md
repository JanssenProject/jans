# Terraform Authorization with GitHub Actions OIDC JWTs

Not sure which flow to use?

See the [Terraform authorization overview](https://docs.jans.io/head/cedarling/integrations/terraform-authz-overview/index.md) for a side-by-side comparison of the unsigned and JWT flows before diving into implementation details.

This guide shows a CI/CD-first variant of the [Terraform authorization demo](https://docs.jans.io/head/cedarling/integrations/terraform-authz/index.md). Instead of asserting the operator's identity through environment variables (as in the unsigned demo), the pipeline authenticates itself with a **signed GitHub Actions OIDC token**. Cedarling validates the JWT cryptographically and evaluates Cedar policies that check JWT claims — no service-account secrets or long-lived credentials required.

## Why JWTs for CI/CD?

The unsigned demo works well for human operators running Terraform locally. In an enterprise CI/CD pipeline, a different model is more appropriate:

| Concern                  | Unsigned demo                          | JWT demo                                   |
| ------------------------ | -------------------------------------- | ------------------------------------------ |
| Identity source          | Environment variables (self-asserted)  | GitHub-signed OIDC token                   |
| Secret management        | Requires secrets/service account creds | No secrets needed                          |
| Claim verification       | Trust the caller                       | Cedarling verifies signature via JWKS      |
| Branch enforcement       | Manual, honor-system                   | Verified `ref` claim in JWT                |
| Production approval gate | Not built-in                           | `environment` claim proves GitHub approval |

GitHub Actions automatically issues an OIDC token to every job that has `permissions: id-token: write`. The token is signed by GitHub's private key and contains claims that describe the workflow:

| Claim         | Example value                                 | Meaning                                                |
| ------------- | --------------------------------------------- | ------------------------------------------------------ |
| `iss`         | `https://token.actions.githubusercontent.com` | Issuer — GitHub's OIDC endpoint                        |
| `sub`         | `repo:org/myrepo:ref:refs/heads/main`         | Subject — identifies the workflow                      |
| `repository`  | `org/myrepo`                                  | Source repository                                      |
| `ref`         | `refs/heads/main`                             | Git ref that triggered the run                         |
| `workflow`    | `.github/workflows/terraform.yml`             | Workflow file path                                     |
| `environment` | `production`                                  | GitHub Environment (only present after human approval) |

## Authorization Model

The demo implements a graduated trust model for CI pipelines:

| Trigger                                            | Workspace     | Plan | Apply     | Destroy   |
| -------------------------------------------------- | ------------- | ---- | --------- | --------- |
| Any branch, trusted repo                           | any           | ✓    | ✗         | ✗         |
| `main` branch, trusted repo                        | dev / staging | ✓    | ✓         | ✗         |
| `main` branch, trusted repo + Environment approved | production    | ✓    | ✓         | ✗         |
| Any CI workflow                                    | any           | ✓    | see above | ✗ (never) |

`Destroy` is permanently off-limits for CI. A human operator using the [unsigned wrapper](https://docs.jans.io/head/cedarling/integrations/terraform-authz/index.md) with Admin-role membership is required to destroy infrastructure.

```
sequenceDiagram
    participant GH as GitHub Actions runner
    participant GHTOKEN as GitHub token endpoint
    participant Wrapper as tf_authz_jwt.sh
    participant OPA
    participant Rego
    participant Cedarling
    participant JWKS as GitHub JWKS
    participant Cedar as Cedar Policies

    GH->>GHTOKEN: Request OIDC token (id-token: write)
    GHTOKEN-->>GH: Signed JWT { repository, ref, environment, ... }

    GH->>Wrapper: terraform apply (TF_JWT=<jwt>)
    Wrapper->>OPA: POST /v1/data/infra/terraform_jwt
    OPA->>Rego: evaluate infra.terraform_jwt
    Rego->>Cedarling: cedarling.opa.authorize_multi_issuer(input)
    Cedarling->>JWKS: Fetch public keys (cached)
    JWKS-->>Cedarling: JWK set
    Cedarling->>Cedarling: Verify JWT signature + expiry
    Cedarling->>Cedarling: Map claims → CI::GitHubWorkflow entity
    Cedarling->>Cedar: Evaluate policies
    Cedar-->>Cedarling: decision + matching policy IDs
    Cedarling-->>Rego: { decision, reasons, errors }
    Rego-->>OPA: { allow, reasons }
    OPA-->>Wrapper: HTTP 200 { result: { allow: true|false } }
    alt ALLOWED
        Wrapper->>GH: terraform apply proceeds
    else DENIED
        Wrapper->>GH: exit 1 + deny message with JWT claims
    end
```

## Cedar Schema

The schema defines the principal entity that Cedarling populates from the verified JWT claims:

```
namespace CI {
    // URL type for TrustedIssuer's issuer_entity_id attribute.
    type Url = {"host": String, "path": String, "protocol": String};

    // Cedarling builds one TrustedIssuer entity per issuer in policy-store/trusted-issuers/.
    // The issuer name field must equal the Cedar namespace ("CI") so the entity type resolves correctly.
    entity TrustedIssuer = {"issuer_entity_id": Url};

    // A GitHub Actions workflow run authenticated via OIDC JWT.
    // Cedarling maps verified JWT claims to these attributes when the token mapping
    // is "CI::GitHubWorkflow" (see policy-store/trusted-issuers/github-actions.json).
    entity GitHubWorkflow {
        iss:          TrustedIssuer,
        sub:          String,
        repository:   String,
        ref:          String,
        workflow:     String,
        // Optional: only present when the job targets a GitHub Environment and
        // required reviewers have approved. Always use `has environment` before accessing.
        environment?: String,
    } tags Set<String>;

    // A named Terraform workspace / environment target, e.g. "dev", "staging", "production".
    entity TerraformWorkspace;

    // Injected automatically by Cedarling's multi-issuer context builder.
    // Access token claims in Cedar policies via context.tokens.ci_githubworkflow.<attr>.
    type TokensContext = {
        ci_githubworkflow?: GitHubWorkflow,
        total_token_count: Long,
    };

    // Actions map to Terraform sub-commands:
    //   Plan → terraform plan  |  Apply → terraform apply  |  Destroy → terraform destroy
    action Plan, Apply, Destroy
        appliesTo {
            principal: [GitHubWorkflow],
            resource:  [TerraformWorkspace],
            context: {
                current_time: Long,
                tokens: TokensContext,
            }
        };
}
```

The `environment` attribute is the key to production gating: GitHub only populates the `environment` claim when the job targets a configured GitHub Environment **and** the required reviewers have approved. Cedarling verifies this claim cryptographically, so the approval is unforgeable.

Note that Cedar policies access JWT claims through `context.tokens.ci_githubworkflow` (the `TokensContext` injected by Cedarling's multi-issuer context builder) rather than directly on the `principal`.

## Cedar Policies

Three policy files cover all CI scenarios.

JWT claims are accessed through `context.tokens.ci_githubworkflow` — the token context injected by Cedarling's multi-issuer context builder. Each policy also guards on the `iss` claim to ensure the token comes from the expected GitHub OIDC endpoint.

**Plan — allowed from the trusted repo on any branch:**

```
@id("ci-permit-plan")
permit (
    principal,
    action == CI::Action::"Plan",
    resource
) when {
    context.tokens has ci_githubworkflow &&
    context.tokens.ci_githubworkflow.iss == CI::TrustedIssuer::"https://token.actions.githubusercontent.com" &&
    (
        context.tokens.ci_githubworkflow.repository == "octoorg/myrepo" ||
        context.tokens.ci_githubworkflow.repository == "JanssenProject/jans"
    )
};
```

**Apply — allowed from main to non-production workspaces:**

```
@id("ci-permit-apply-main-non-prod")
permit (
    principal,
    action == CI::Action::"Apply",
    resource
) when {
    context.tokens has ci_githubworkflow &&
    context.tokens.ci_githubworkflow.iss == CI::TrustedIssuer::"https://token.actions.githubusercontent.com" &&
    (
        context.tokens.ci_githubworkflow.repository == "octoorg/myrepo" ||
        context.tokens.ci_githubworkflow.repository == "JanssenProject/jans"
    ) &&
    context.tokens.ci_githubworkflow.ref == "refs/heads/main" &&
    resource != CI::TerraformWorkspace::"production"
};
```

**Apply to production — requires `main` branch and GitHub Environment approval:**

```
@id("ci-permit-apply-prod-via-environment")
permit (
    principal,
    action == CI::Action::"Apply",
    resource == CI::TerraformWorkspace::"production"
) when {
    context.tokens has ci_githubworkflow &&
    context.tokens.ci_githubworkflow.iss == CI::TrustedIssuer::"https://token.actions.githubusercontent.com" &&
    (
        context.tokens.ci_githubworkflow.repository == "octoorg/myrepo" ||
        context.tokens.ci_githubworkflow.repository == "JanssenProject/jans"
    ) &&
    context.tokens.ci_githubworkflow.ref == "refs/heads/main" &&
    // `has` guard required: `environment` is optional — absent on plan and non-prod-apply tokens.
    context.tokens.ci_githubworkflow has environment &&
    context.tokens.ci_githubworkflow.environment == "production"
};
```

No `permit` for `Destroy` means all CI destroy attempts are implicitly denied.

## OPA Rego Policy

The Rego adapter calls `cedarling.opa.authorize_multi_issuer` instead of `authorize_unsigned`:

```
package infra.terraform_jwt

default allow := false

result := cedarling.opa.authorize_multi_issuer(input)

allow if {
    result.decision == true
}

decision    := result.decision
reasons     := result.reasons
```

The input payload structure differs from the unsigned variant — the JWT is passed in a `tokens` array:

```
{
  "input": {
    "tokens": [
      {
        "mapping": "CI::GitHubWorkflow",
        "payload": "<base64url-encoded OIDC JWT>"
      }
    ],
    "action": "CI::Action::\"Apply\"",
    "resource": {
      "cedar_entity_mapping": {
        "entity_type": "CI::TerraformWorkspace",
        "id": "production"
      }
    },
    "context": {
      "current_time": 1776826458
    }
  }
}
```

The `mapping` field tells Cedarling which Cedar entity type to instantiate from the JWT claims. Cedarling validates the JWT, maps the verified claims to `CI::GitHubWorkflow` attributes (as declared in the Cedar schema and trusted issuer configuration), and then makes the entity available to Cedar policies through `context.tokens.ci_githubworkflow`.

## OPA Configuration

```
{
  "plugins": {
    "cedarling_opa": {
      "stderr": false,
      "bootstrap_config": {
        "CEDARLING_APPLICATION_NAME": "TerraformJwtAuthz",
        "CEDARLING_LOG_TYPE": "std_out",
        "CEDARLING_LOG_TTL": 60,
        "CEDARLING_LOG_LEVEL": "INFO",
        "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": ["RS256"],
        "CEDARLING_JWT_SIG_VALIDATION": "enabled",
        "CEDARLING_POLICY_STORE_LOCAL_FN": "/app/demo/terraform-jwt/policy-store"
      }
    }
  }
}
```

Key differences from the unsigned config:

- `CEDARLING_JWT_SIG_VALIDATION: "enabled"` — Cedarling fetches GitHub's JWKS and validates every token's signature.
- Trusted issuer configuration lives in `policy-store/trusted-issuers/github-actions.json`, not in the OPA bootstrap config. That file declares GitHub's OIDC endpoint (`https://token.actions.githubusercontent.com`) as the trusted issuer and specifies which entity type (`CI::GitHubWorkflow`) to build from the token claims. Cedarling discovers the JWKS automatically via the issuer's OpenID Connect discovery document.

### Trusted issuer file format

`policy-store/trusted-issuers/github-actions.json`:

```
{
  "name": "CI",
  "description": "GitHub Actions OIDC token issuer. The name 'CI' must match the Cedar namespace so Cedarling can build the CI::TrustedIssuer entity. Cedarling locates this issuer by matching the JWT iss claim against the origin of configuration_endpoint.",
  "configuration_endpoint": "https://token.actions.githubusercontent.com/.well-known/openid-configuration",
  "token_metadata": {
    "CI::GitHubWorkflow": {
      "trusted": true,
      "entity_type_name": "CI::GitHubWorkflow",
      "token_id": "jti",
      "required_claims": []
    }
  }
}
```

Field reference:

| Field                    | Description                                                                                                                                                                                                                                                                                                     |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `name`                   | Must match the Cedar namespace used in your policies and schema (e.g. `CI` maps to `CI::*` entity types). Cedarling also uses this value when constructing the `CI::TrustedIssuer` entity.                                                                                                                      |
| `configuration_endpoint` | URL of the issuer's OpenID Connect discovery document (`.well-known/openid-configuration`). Cedarling fetches the `jwks_uri` from this document and uses it to validate JWT signatures automatically. To use a different issuer (GitLab, Okta, etc.) replace this URL with the issuer's own discovery endpoint. |
| `token_metadata`         | A map from Cedar entity type name to per-token settings. Each key must be a fully-qualified Cedar entity type (e.g. `CI::GitHubWorkflow`).                                                                                                                                                                      |
| `entity_type_name`       | The Cedar entity type Cedarling instantiates from the verified JWT claims. Must match the key in `token_metadata` and the type declared in your Cedar schema.                                                                                                                                                   |
| `token_id`               | JWT claim used as the Cedar entity's unique ID (typically `jti`).                                                                                                                                                                                                                                               |
| `required_claims`        | List of JWT claim names that must be present for the token to be considered valid. Set to `[]` to enforce no additional claims beyond signature validation.                                                                                                                                                     |

To adapt this configuration for another issuer, create a new JSON file in `policy-store/trusted-issuers/` (one file per issuer), update `name` to the Cedar namespace you want to use, point `configuration_endpoint` at the new issuer's discovery URL, and adjust `entity_type_name` to match the Cedar entity type declared in your schema.

## The Authorization Wrapper

`tf_authz_jwt.sh` has the same interface as `tf_authz.sh` but sources identity from the JWT instead of environment variables:

### Fetching the GitHub OIDC token

```
export TF_JWT="$(./demo/terraform-jwt/tf_authz_jwt.sh --fetch-token)"
```

This calls the `ACTIONS_ID_TOKEN_REQUEST_URL` endpoint using the `ACTIONS_ID_TOKEN_REQUEST_TOKEN` credential that GitHub injects automatically into any runner job with `permissions: id-token: write`.

### Environment variables

| Variable        | Required | Default                 | Description                                       |
| --------------- | -------- | ----------------------- | ------------------------------------------------- |
| `TF_JWT`        | **yes**  | —                       | Signed OIDC JWT (GitHub or custom issuer)         |
| `TF_WORKSPACE`  | no       | `dev`                   | Target workspace (`dev`, `staging`, `production`) |
| `OPA_URL`       | no       | `http://localhost:8181` | OPA server base URL                               |
| `TERRAFORM_BIN` | no       | `terraform`             | Path to the terraform binary                      |

### Usage

```
export TF_JWT="$(./demo/terraform-jwt/tf_authz_jwt.sh --fetch-token)"
export TF_WORKSPACE=staging

./demo/terraform-jwt/tf_authz_jwt.sh plan
./demo/terraform-jwt/tf_authz_jwt.sh apply -auto-approve
./demo/terraform-jwt/tf_authz_jwt.sh destroy -auto-approve  # always DENIED for CI
```

## GitHub Actions Integration

The following excerpt from `github-actions-example.yml` shows the production-deploy job:

```
terraform-apply-production:
  runs-on: ubuntu-latest
  needs: terraform-apply-staging
  if: github.ref == 'refs/heads/main'
  # GitHub only sets environment: "production" in the OIDC token after the
  # required reviewers approve this environment gate.
  environment: production
  permissions:
    id-token: write
    contents: read

  steps:
    - uses: actions/checkout@de0fac2e4500dabe0009e67214ff5f5447ce83dd
    - uses: hashicorp/setup-terraform@v3

    - name: Fetch GitHub OIDC token
      id: get-token
      run: |
        TOKEN=$(./demo/terraform-jwt/tf_authz_jwt.sh --fetch-token)
        echo "token=$TOKEN" >> "$GITHUB_OUTPUT"
        echo "::add-mask::$TOKEN"

    - name: Terraform apply — production
      env:
        TF_JWT: ${{ steps.get-token.outputs.token }}
        TF_WORKSPACE: production
      run: ./demo/terraform-jwt/tf_authz_jwt.sh apply -auto-approve
```

See the [full workflow file](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/cedarling_opa/demo/terraform-jwt/github-actions-example.yml) for the plan and staging jobs.

## Composite Action (Reusable)

For teams that want to adopt JWT authorization without copying the wrapper script into every repository, a pre-built GitHub Actions composite action is available at `demo/terraform-jwt/.github/actions/tf-jwt-authz/`. It encapsulates the full flow — OIDC token fetch, OPA query, decision enforcement, and Terraform execution — in a single `uses:` step.

### Inputs

| Input                         | Required | Default               | Description                                                                       |
| ----------------------------- | -------- | --------------------- | --------------------------------------------------------------------------------- |
| `opa_url`                     | **yes**  | —                     | Base URL of the Cedarling-OPA server (e.g. `http://opa-service:8181`)             |
| `workspace`                   | **yes**  | —                     | Target Terraform workspace (`dev`, `staging`, `production`)                       |
| `terraform_command`           | **yes**  | —                     | Sub-command (and flags) to authorize and run (e.g. `plan`, `apply -auto-approve`) |
| `opa_policy_path`             | no       | `infra/terraform_jwt` | OPA path appended to `/v1/data/`                                                  |
| `terraform_working_directory` | no       | `.`                   | Directory from which to run Terraform (relative to the repo root)                 |

The action requires the parent job to have `permissions: id-token: write` so that GitHub can issue the OIDC token.

### Usage

Replace the manual token-fetch and `tf_authz_jwt.sh` call with a single step:

```
jobs:
  terraform-plan:
    runs-on: ubuntu-latest
    permissions:
      id-token: write   # Required — GitHub issues the OIDC token for this job
      contents: read

    steps:
      - uses: actions/checkout@de0fac2e4500dabe0009e67214ff5f5447ce83dd
      - uses: hashicorp/setup-terraform@v3

      - name: Terraform init
        run: terraform init

      - name: Terraform plan (JWT-authorized)
        uses: JanssenProject/jans/jans-cedarling/cedarling_opa/demo/terraform-jwt/.github/actions/tf-jwt-authz@main
        with:
          opa_url: ${{ vars.OPA_URL }}
          workspace: staging
          terraform_command: plan
```

See [`github-actions-example-composite.yml`](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/cedarling_opa/demo/terraform-jwt/github-actions-example-composite.yml) for a complete workflow with plan, staging apply, and production apply jobs.

### How it works

The composite action runs two steps internally:

1. **Fetch GitHub OIDC token** — calls `ACTIONS_ID_TOKEN_REQUEST_URL` (automatically set by GitHub when `id-token: write` is present), extracts the signed JWT, and immediately masks it in the log.
1. **Authorize and run Terraform** — maps the sub-command to a Cedar action (`Plan`, `Apply`, or `Destroy`), builds the `authorize_multi_issuer` OPA payload, queries the Cedarling-OPA server, and either runs `terraform <command>` (ALLOWED) or fails the step with the denial reason (DENIED). Non-auth commands (`init`, `fmt`, `validate`) are forwarded to Terraform directly without an OPA call.

## Running the Demo Locally

See the [demo README](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/cedarling_opa/demo/terraform-jwt/README.md) for step-by-step instructions including:

- Starting the Cedarling-OPA server in demo mode (signature validation off for local testing)
- Generating self-signed test JWTs with Python
- Testing all authorization scenarios with the wrapper and with raw `curl`

## Comparing the Two Terraform Demos

|                           | [Unsigned demo](https://docs.jans.io/head/cedarling/integrations/terraform-authz/index.md) | JWT demo (this page)                                                              |
| ------------------------- | ------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------- |
| **Identity source**       | Environment variables (self-asserted)                                                      | GitHub OIDC token (cryptographically signed)                                      |
| **Signature validation**  | None — `CEDARLING_JWT_SIG_VALIDATION: "disabled"`                                          | Enabled — Cedarling fetches GitHub's JWKS and verifies every token                |
| **Trusted issuer config** | Not required — no `trusted-issuers/` directory needed                                      | Required — `policy-store/trusted-issuers/github-actions.json` declares the issuer |
| **Cedarling built-in**    | `authorize_unsigned`                                                                       | `authorize_multi_issuer`                                                          |
| **OPA endpoint**          | `/v1/data/infra/terraform`                                                                 | `/v1/data/infra/terraform_jwt`                                                    |
| **Cedar namespace**       | `Infra`                                                                                    | `CI`                                                                              |
| **Principal entity**      | `Infra::User` (role-based)                                                                 | `CI::GitHubWorkflow` (claim-based)                                                |
| **Secret management**     | Requires `TF_USER_ID` / `TF_USER_ROLES`                                                    | No secrets — OIDC token issued automatically                                      |
| **Prod approval gate**    | Role-based policy only                                                                     | `environment` JWT claim proves GitHub Environment approval                        |
| **Best suited for**       | Human operators, local development                                                         | CI/CD pipelines requiring cryptographic identity                                  |

Both demos use the same Cedar policy store format, the same OPA plugin, and the same `opa-cedarling` binary — only the Rego built-in and the principal model differ.
