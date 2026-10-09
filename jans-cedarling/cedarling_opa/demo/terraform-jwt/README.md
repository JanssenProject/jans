# Terraform JWT Authorization Demo

This demo extends the [Terraform authorization demo](../terraform/) by replacing self-asserted identity with a **cryptographically verified GitHub Actions OIDC JWT**. Instead of trusting environment variables set by the caller, the Cedarling-OPA server validates the JWT signature against GitHub's JWKS endpoint, extracts the claims, and evaluates Cedar policies that check the repository name, Git ref, and GitHub Environment — with no secrets or service-account credentials required.

## Authorization Model

| Scenario | Plan | Apply | Destroy |
|---|:---:|:---:|:---:|
| Any branch, trusted repo | ✓ | ✗ | ✗ |
| `main` branch, trusted repo, non-prod workspace | ✓ | ✓ | ✗ |
| `main` branch, trusted repo, `production` GitHub Environment approved | ✓ | ✓ | ✗ |
| Any CI workflow | ✓ | see above | ✗ (never) |

`Destroy` is implicitly denied for all CI pipelines — no Cedar policy allows it. A human operator using the [unsigned demo](../terraform/) is required.

See [PERMISSIONS.md](./PERMISSIONS.md) for the complete decision matrix covering all claim combinations and edge cases.

## Directory Structure

```
terraform-jwt/
├── .github/
│   └── actions/
│       └── tf-jwt-authz/
│           └── action.yml             Reusable composite action (JWT auth + Terraform in one step)
├── policy-store/                      Cedar policy store (directory-based format)
│   ├── metadata.json                  Policy store metadata
│   ├── schema.cedarschema             Cedar entity schema (CI namespace)
│   ├── trusted-issuers/
│   │   └── github-actions.json        Trusted issuer config for authorize_multi_issuer
│   └── policies/
│       ├── ci-permit-plan.cedar                      Plan from trusted repo (any branch)
│       ├── ci-permit-apply-main-non-prod.cedar        Apply from main to non-prod
│       └── ci-permit-apply-prod-via-environment.cedar Apply to prod after Environment approval
├── rego/
│   └── terraform_jwt.rego             OPA policy (delegates to Cedarling multi-issuer)
├── docker-compose.yml                 One-command quick start (pre-built image, demo mode)
├── docker-compose.prod.yml            Production overlay — opt-in, swaps in opa-config.json for real JWT validation
├── opa-config.json                    Production OPA + Cedarling config (sig validation ON)
├── opa-config-demo.json               Demo config (sig validation OFF — local testing only)
├── tf_authz_jwt.sh                    JWT-aware authorization wrapper script
├── github-actions-example.yml         Full GitHub Actions workflow (script-based JWT wiring)
├── github-actions-example-composite.yml  Simplified workflow using the composite action
├── PERMISSIONS.md                     Complete decision matrix for all JWT authorization scenarios
└── example.tf                         Minimal Terraform config (no cloud credentials needed)
```

> **Trusted issuers for `authorize_multi_issuer`** are read from `policy-store/trusted-issuers/`, not from the OPA config file. The `github-actions.json` file there defines the issuer name, OIDC discovery endpoint, and token-to-entity mapping. The OPA config files (`opa-config.json`, `opa-config-demo.json`) only control Cedarling startup behaviour (log level, sig validation, policy store path).

## How It Works

```
GitHub Actions runner
  │  (permissions: id-token: write)
  │
  ├─► ACTIONS_ID_TOKEN_REQUEST_URL  ──► GitHub token endpoint
  │                                          │
  │   tf_authz_jwt.sh --fetch-token ◄────────┘
  │       │  JWT signed by GitHub
  │       ▼
  │   POST /v1/data/infra/terraform_jwt
  │   {
  │     tokens: [{ mapping: "CI::GitHubWorkflow", payload: "<jwt>" }],
  │     action:  "CI::Action::\"Apply\"",
  │     resource: { cedar_entity_mapping: { entity_type: "CI::TerraformWorkspace",
  │                                         id: "production" } },
  │     context: { current_time: 1776826458 }
  │   }
  │       │
  │       ▼
  │   OPA → Rego → cedarling.opa.authorize_multi_issuer(input)
  │                    │
  │                    ├─ Validate JWT signature (GitHub JWKS)
  │                    ├─ Map claims → CI::GitHubWorkflow entity
  │                    │    repository:  "octoorg/myrepo"
  │                    │    ref:         "refs/heads/main"
  │                    │    environment: "production"
  │                    └─ Evaluate Cedar policies
  │                            → ci-permit-apply-prod-via-environment: ALLOW
  │
  └─► terraform apply -auto-approve
```

## Quick Start with Docker

No build step required — pull the pre-built image and start the server in one command:

```bash
cd jans-cedarling/cedarling_opa/demo/terraform-jwt
docker compose up
```

OPA starts at `http://localhost:8181` with JWT signature validation **disabled** (`opa-config-demo.json`), so any JWT with the correct claims is accepted without needing real GitHub Actions credentials.

Once the server is healthy, jump straight to [step 3 (Generate a test JWT)](#3-generate-a-test-jwt) below.

> **Note:** The image is distroless and does not include a shell or curl. The healthcheck uses the bundled `opa-cedarling health` command, so `docker compose ps` will show `healthy` once OPA is ready.

To stop the server:

```bash
docker compose down
```

### Production mode (real GitHub Actions JWT)

By default, `docker compose up` starts OPA in **demo mode** — JWT signature validation is disabled and locally crafted tokens are accepted. To run against a **real GitHub Actions OIDC token** with full cryptographic validation, explicitly pass the production overlay file:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up
```

`docker-compose.prod.yml` is a non-auto-loading overlay (Docker Compose only auto-loads `docker-compose.override.yml`). A plain `docker compose up` always stays in demo mode. The overlay swaps `opa-config-demo.json` for `opa-config.json`, which:

- Enables JWT signature validation (`CEDARLING_JWT_SIG_VALIDATION: "enabled"`)
- Fetches GitHub's public keys from `https://token.actions.githubusercontent.com/.well-known/jwks` at startup
- Rejects any token whose signature cannot be verified against those keys

**Prerequisites for production mode:**

1. The job must declare `permissions: id-token: write` in its GitHub Actions workflow.
2. Export the OIDC token before calling the authorization wrapper:
   ```bash
   export TF_JWT=$(./tf_authz_jwt.sh --fetch-token)
   ```
3. The Docker host must have outbound HTTPS access to `https://token.actions.githubusercontent.com`.

Locally crafted JWTs (see step 3 below) will be **rejected** in production mode because they are signed with an arbitrary secret rather than GitHub's private key. Use the plain `docker compose up` (demo mode, no `-f docker-compose.prod.yml`) for local development and testing without real tokens.

---

## Running the Demo Locally

### 1. Build the opa-cedarling binary

From the `cedarling_opa` directory:

```bash
make
```

### 2. Start the Cedarling-OPA server (demo mode — sig validation off)

```bash
export LD_LIBRARY_PATH=$(pwd)/plugins/cedarling_opa:$LD_LIBRARY_PATH

./build/opa-cedarling run \
    --server \
    --config-file ./demo/terraform-jwt/opa-config-demo.json \
    ./demo/terraform-jwt/rego/
```

OPA starts at `http://localhost:8181`.

### 3. Generate a test JWT

Because demo mode has signature validation disabled, any JWT with the correct claims is accepted. Generate one with:

```bash
# Install python3-jwt if needed: pip install pyjwt
python3 - <<'EOF'
import jwt, time, json

claims = {
    "iss": "https://token.actions.githubusercontent.com",
    "sub": "repo:octoorg/myrepo:ref:refs/heads/main",
    "jti": "demo-plan-001",
    "repository": "octoorg/myrepo",
    "ref": "refs/heads/main",
    "workflow": ".github/workflows/terraform.yml",
    "environment": "",
    "iat": int(time.time()),
    "exp": int(time.time()) + 3600,
    "aud": "cedarling-terraform",
}
token = jwt.encode(claims, "demo-secret", algorithm="HS256")
print(token)
EOF
```

### 4. Test authorization scenarios

Export the token and target workspace:

```bash
export TF_JWT="<paste token from step 3>"
export TF_WORKSPACE=staging
```

**Plan from any branch (allowed):**
```bash
./demo/terraform-jwt/tf_authz_jwt.sh -chdir=./demo/terraform-jwt plan
```

Expected:
```
tf_authz_jwt: checking authorization...
  Repository: octoorg/myrepo
  Ref:        refs/heads/main
  ...

tf_authz_jwt: ALLOWED
  Granted by Cedar policy: ci-permit-plan
```

**Apply to staging from main (allowed):**
```bash
./demo/terraform-jwt/tf_authz_jwt.sh -chdir=./demo/terraform-jwt apply -auto-approve
```

Expected: `ALLOWED — ci-permit-apply-main-non-prod`

**Apply to production without Environment approval (denied):**
```bash
export TF_WORKSPACE=production
./demo/terraform-jwt/tf_authz_jwt.sh -chdir=./demo/terraform-jwt apply -auto-approve
```

Expected: `DENIED` — the `environment` claim is empty, so `ci-permit-apply-prod-via-environment` does not match.

**Apply to production with Environment approval:**

Regenerate the JWT with `"environment": "production"` (this claim is only present when a GitHub job targets a configured GitHub Environment and the required reviewers have approved):

```bash
python3 - <<'EOF'
import jwt, time
claims = {
    "iss": "https://token.actions.githubusercontent.com",
    "sub": "repo:octoorg/myrepo:environment:production",
    "jti": "demo-apply-prod-001",
    "repository": "octoorg/myrepo",
    "ref": "refs/heads/main",
    "workflow": ".github/workflows/terraform.yml",
    "environment": "production",
    "iat": int(time.time()),
    "exp": int(time.time()) + 3600,
    "aud": "cedarling-terraform",
}
print(jwt.encode(claims, "demo-secret", algorithm="HS256"))
EOF
```

```bash
export TF_JWT="<new token>"
export TF_WORKSPACE=production
./demo/terraform-jwt/tf_authz_jwt.sh -chdir=./demo/terraform-jwt apply -auto-approve
```

Expected: `ALLOWED — ci-permit-apply-prod-via-environment`

**Destroy (always denied for CI):**
```bash
./demo/terraform-jwt/tf_authz_jwt.sh -chdir=./demo/terraform-jwt destroy -auto-approve
```

Expected: `DENIED` — no Cedar policy allows Destroy for CI pipelines.

### 5. Query OPA directly with curl

```bash
# Plan — should be ALLOWED
curl -X POST http://localhost:8181/v1/data/infra/terraform_jwt \
    -H "Content-Type: application/json" \
    -d "{
      \"input\": {
        \"tokens\": [
          {
            \"mapping\": \"CI::GitHubWorkflow\",
            \"payload\": \"${TF_JWT}\"
          }
        ],
        \"action\": \"CI::Action::\\\"Plan\\\"\",
        \"resource\": {
          \"cedar_entity_mapping\": {
            \"entity_type\": \"CI::TerraformWorkspace\",
            \"id\": \"staging\"
          }
        },
        \"context\": { \"current_time\": $(date +%s) }
      }
    }" | jq .
```

## GitHub Actions Integration

### Configuring the production Environment gate with required reviewers

The `ci-permit-apply-prod-via-environment` Cedar policy permits production Apply **only** when the OIDC token contains an `environment` claim equal to `"production"`. GitHub only injects that claim into the token when **two conditions are both true**:

1. The workflow job declares `environment: production`.
2. A designated reviewer has **explicitly approved** the deployment in the GitHub Actions UI before the job starts.

Without reviewer approval the job is blocked by GitHub before it ever runs — the OIDC token is never issued, and the Cedar policy cannot be bypassed by simply adding `environment: production` to a workflow file.

**To configure required reviewers for the production Environment:**

1. Open the repository in GitHub and go to **Settings → Environments**.
2. Click **production** (create it if it does not exist yet).
3. Under **Deployment protection rules**, enable **Required reviewers**.
4. Add at least one GitHub user or team as a reviewer and click **Save protection rules**.

Once configured, every run of the `test-production-environment-apply` job (and any production deployment job in your own workflows) will pause at the environment gate and wait for a reviewer to approve before the runner starts and the OIDC token is minted. The approved token will carry `"environment": "production"`, satisfying the Cedar policy. A token issued without going through this gate — for example, from a job that does not target the Environment at all — will lack the claim and receive a DENY.

See [github-actions-example.yml](./github-actions-example.yml) for a complete workflow that:

1. Requests a GitHub OIDC token via `--fetch-token`
2. Runs `terraform plan` on every push/PR
3. Runs `terraform apply` against staging automatically on `main`
4. Runs `terraform apply` against production after a required reviewer approves the `production` GitHub Environment

> **Working directory**: The example paths (`./demo/terraform-jwt/tf_authz_jwt.sh`, etc.) assume the job runs from `jans-cedarling/cedarling_opa/`. If you copy the workflow into a repository with a different root, adjust the paths or add `defaults: run: working-directory: jans-cedarling/cedarling_opa` at the workflow level.

### Self-hosting OPA inside the CI job (production mode)

Instead of pointing `OPA_URL` at a persistent remote server, teams can spin up the OPA server **inside the CI job itself** using Docker Compose. `ubuntu-latest` runners include Docker, so no additional infrastructure is needed.

Each job in `github-actions-example.yml` contains a commented-out step that starts OPA in production mode and waits for it to become healthy before Terraform runs:

```yaml
# Option B — self-hosted OPA in production mode
# Add to each job BEFORE the Terraform steps; also change OPA_URL to http://localhost:8181.
- name: Start OPA (self-hosted, production mode)
  working-directory: demo/terraform-jwt
  run: |
    docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
    echo "Waiting for OPA to become healthy..."
    healthy=false
    for i in $(seq 1 30); do
      if curl -sf http://localhost:8181/health > /dev/null 2>&1; then
        echo "OPA is healthy"; healthy=true; break
      fi
      sleep 2
    done
    $healthy || { echo "ERROR: OPA did not become healthy in time"; exit 1; }
```

The `docker-compose.prod.yml` overlay swaps in `opa-config.json`, which enables full JWT signature validation against GitHub's JWKS endpoint (`https://token.actions.githubusercontent.com/.well-known/jwks`). Locally crafted tokens will be **rejected** — only real GitHub OIDC tokens issued for that runner are accepted.

**To activate self-hosted mode:**

1. Uncomment the `Start OPA (self-hosted, production mode)` step in each job.
2. Uncomment the matching `Stop OPA (self-hosted, production mode)` step that appears after the Terraform step in each job.
3. Change `OPA_URL` in the job's `env` block from `${{ vars.OPA_URL || 'http://opa-service:8181' }}` to `http://localhost:8181`.
4. Ensure the runner has outbound HTTPS access to `https://token.actions.githubusercontent.com` (required for JWKS fetch at startup).

Each job starts its own isolated OPA instance. The `Stop OPA` teardown step (which runs `docker compose … down`) uses `if: always()` so it executes even when an earlier step fails. On **ephemeral `ubuntu-latest` runners** the teardown is optional because the entire VM is discarded at the end of the job. On **persistent self-hosted runners**, however, omitting the teardown causes containers to accumulate across jobs and can exhaust runner resources over time — the teardown step is strictly necessary in that case.

## Production Configuration

Switch from `opa-config-demo.json` to `opa-config.json` to enable JWT signature validation against GitHub's JWKS endpoint. No other changes are required — the Cedar policies, Rego adapter, and wrapper script are identical.

```bash
./build/opa-cedarling run \
    --server \
    --config-file ./demo/terraform-jwt/opa-config.json \
    ./demo/terraform-jwt/rego/
```

## Adapting to Your Repository

1. Replace `"octoorg/myrepo"` with your actual repository name in all three `.cedar` policy files.
2. Adjust the ref guard (`"refs/heads/main"`) if your default branch has a different name.
3. Configure a GitHub Environment named `production` in your repository settings and add required reviewers.
4. Point `OPA_URL` at your deployed opa-cedarling server.

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| `TF_JWT is not set` | Forgot to export the token | Run `--fetch-token` and export the result |
| `DENIED` even with correct claims | Sig validation on, JWT crafted locally | Use `opa-config-demo.json` for local testing |
| `Cedarling uninitialized` in OPA logs | Policy store path wrong | Verify `CEDARLING_POLICY_STORE_LOCAL_FN` points to the `policy-store/` directory |
| JWT validation error in logs | JWKS fetch failed | Ensure the OPA server can reach `https://token.actions.githubusercontent.com` |
| `DENIED` for production apply | `environment` claim missing | Ensure the GitHub Actions job uses `environment: production` and has been approved |
