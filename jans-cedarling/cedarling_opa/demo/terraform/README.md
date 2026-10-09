# Terraform Authorization Demo

This demo shows how to use the **Cedarling-OPA plugin** to gate `terraform plan`, `terraform apply`, and `terraform destroy` with fine-grained Cedar policies. Before any Terraform command reaches the cloud, a shell wrapper queries OPA (backed by a Cedarling instance) and enforces a role-based authorization model:

| Role      |       Plan        |       Apply       | Destroy |
|-----------|:-----------------:|:-----------------:|:-------:|
| Developer |         ✓         |         ✗         |    ✗    |
| Ops       | ✓ (non-prod only) | ✓ (non-prod only) |    ✗    |
| Admin     |         ✓         |         ✓         |    ✓    |

See [PERMISSIONS.md](./PERMISSIONS.md) for the full permission matrix, including multi-role combinations and a complete decision table.

## Quick Start with Docker

No build step required — pull the pre-built image and start the server in one command:

```bash
cd jans-cedarling/cedarling_opa/demo/terraform
docker compose up
```

The `opa-cedarling` service uses the pre-built image published at
[`ghcr.io/janssenproject/jans/cedarling-opa`](https://github.com/JanssenProject/jans/pkgs/container/jans%2Fcedarling-opa).
To pin to a specific release instead of `latest`, edit the `image:` line in
`docker-compose.yml` (e.g. `ghcr.io/janssenproject/jans/cedarling-opa:2.0.0-1`).

OPA will be available at `http://localhost:8181`. Once the server is healthy, jump
straight to [step 3 (Query OPA directly)](#3-query-opa-directly-with-curl) below.

> **Note:** The image is distroless and does not include a shell or curl. The healthcheck uses the bundled `opa-cedarling health` command, so `docker compose ps` will show `healthy` once OPA is ready.

To run the full suite of allow/deny scenarios automatically, use the `demo` profile:

```bash
docker compose --profile demo run --rm tf-demo
```

This spins up a lightweight Alpine container that drives `tf_authz.sh` against the
running OPA server and prints a pass/fail summary for every role combination (Developer,
Ops, Admin) without requiring a real Terraform installation.

To stop and remove the containers:

```bash
docker compose down
```

---

## Prerequisites

### Docker path (recommended)
- Docker Engine 24+ with the Compose plugin — no other local tooling needed.

### Manual / local path
- The **opa-cedarling** binary built from the parent `cedarling_opa` directory (see the main [README](../../README.md))
- `curl` and `jq` installed on your system
- `terraform` installed (only needed to run the actual Terraform steps)

## Directory Structure

```
terraform/
├── policy-store/               Cedar policy store (directory-based format)
│   ├── metadata.json           Policy store metadata
│   ├── schema.cedarschema      Cedar entity schema
│   └── policies/
│       ├── admin-permit-all.cedar
│       ├── ops-permit-plan-apply.cedar
│       └── developer-permit-plan.cedar
├── rego/
│   └── terraform.rego          OPA policy (delegates to Cedarling)
├── docker-compose.yml          One-command Docker setup
├── Dockerfile.demo             Lightweight demo runner image (Alpine + curl/jq)
├── demo-runner.sh              Scripted allow/deny scenario suite
├── opa-config.json             OPA + Cedarling plugin configuration
├── tf_authz.sh                 Authorization wrapper script
├── github-actions-example.yml  Full GitHub Actions workflow showing role-based wiring
└── example.tf                  Minimal Terraform config (no cloud credentials needed)
```

## Running the Demo

### 1. Start the Cedarling-OPA server

From the `cedarling_opa` directory, set the library path and start the server pointing at the Terraform demo files:

```bash
export LD_LIBRARY_PATH=$(pwd)/plugins/cedarling_opa:$LD_LIBRARY_PATH

./build/opa-cedarling run \
    --server \
    --config-file ./demo/terraform/opa-config.json \
    ./demo/terraform/rego/
```

OPA will start at `http://localhost:8181`.

### 2. Test authorization with the wrapper

The wrapper reads three environment variables:

| Variable       | Description                                    | Example             |
|----------------|------------------------------------------------|---------------------|
| `TF_WORKSPACE` | Target environment (`dev`, `staging`, `production`) | `production`   |
| `TF_USER_ID`   | Operator identity (username or service account) | `alice`            |
| `TF_USER_ROLES`| Comma-separated Cedar roles                    | `Developer`         |

**Developer planning production (allowed):**
```bash
export TF_WORKSPACE=production
export TF_USER_ID=alice
export TF_USER_ROLES=Developer

# Global flags before the sub-command are supported:
./demo/terraform/tf_authz.sh -chdir=./demo/terraform plan
# Or equivalently:
./demo/terraform/tf_authz.sh plan -chdir=./demo/terraform
```

Expected output:
```
tf_authz: checking authorization...
  User:      alice (roles: Developer)
  Action:    terraform plan  →  Infra::Action::"Plan"
  Workspace: production  →  Infra::TerraformWorkspace::"production"

tf_authz: ALLOWED
  Granted by Cedar policy: developer-permit-plan

(terraform plan output follows...)
```

**Developer trying to apply production (denied):**
```bash
export TF_WORKSPACE=production
export TF_USER_ID=alice
export TF_USER_ROLES=Developer

./demo/terraform/tf_authz.sh -chdir=./demo/terraform apply -auto-approve
```

Expected output:
```
tf_authz: checking authorization...
  User:      alice (roles: Developer)
  Action:    terraform apply  →  Infra::Action::"Apply"
  Workspace: production  →  Infra::TerraformWorkspace::"production"

tf_authz: DENIED

  alice is not permitted to run 'terraform apply' on workspace 'production'.

  No Cedar policy allows this operation.
```

**Ops applying to staging (allowed):**
```bash
export TF_WORKSPACE=staging
export TF_USER_ID=bob
export TF_USER_ROLES=Ops

./demo/terraform/tf_authz.sh apply -chdir=./demo/terraform -auto-approve
```

**Ops trying to apply to production (denied):**
```bash
export TF_WORKSPACE=production
export TF_USER_ID=bob
export TF_USER_ROLES=Ops

./demo/terraform/tf_authz.sh apply -chdir=./demo/terraform -auto-approve
```

**Admin destroying production (allowed):**
```bash
export TF_WORKSPACE=production
export TF_USER_ID=carol
export TF_USER_ROLES=Admin

./demo/terraform/tf_authz.sh destroy -chdir=./demo/terraform -auto-approve
```

### 3. Query OPA directly with curl

You can also query OPA directly without the wrapper:

```bash
curl -X POST http://localhost:8181/v1/data/infra/terraform \
    -H "Content-Type: application/json" \
    -d '{
      "input": {
        "principal": {
          "cedar_entity_mapping": {
            "entity_type": "Infra::User",
            "id": "alice"
          },
          "sub": "alice",
          "role": ["Developer"]
        },
        "action": "Infra::Action::\"Apply\"",
        "resource": {
          "cedar_entity_mapping": {
            "entity_type": "Infra::TerraformWorkspace",
            "id": "production"
          }
        },
        "context": {
          "current_time": 1776826458
        }
      }
    }'
```

Response (denied):
```json
{
  "result": {
    "allow": false,
    "reasons": [],
    "result": {
      "decision": false,
      "errors": [],
      "reasons": [],
      "request_id": "..."
    }
  }
}
```

## Adding More Policies

Place additional `.cedar` files in `policy-store/policies/`. The Cedarling policy store loader picks up all `.cedar` files in the directory automatically.

Example — allow Ops to destroy dev only:
```cedar
@id("ops-permit-destroy-dev")
permit (
    principal,
    action == Infra::Action::"Destroy",
    resource == Infra::TerraformWorkspace::"dev"
) when {
    principal.role.contains("Ops")
};
```

> **Note on role checking:** The policies use `principal.role.contains("RoleName")` rather
> than `principal in Infra::Role::"RoleName"`. The Cedarling unsigned-authorize path passes
> role values as a `Set<String>` attribute on the User entity; it does not build Cedar
> entity-hierarchy parent relationships from the `role` array at runtime. Attribute
> containment is therefore the correct and working approach.

## GitHub Actions Integration

### Full self-contained workflow

See [github-actions-example.yml](./github-actions-example.yml) for a complete workflow that:

1. Sets `TF_USER_ID`, `TF_USER_ROLES`, and `TF_WORKSPACE` from repository variables
2. Runs `terraform plan` on every push/PR (any role with Plan permission)
3. Runs `terraform apply` against staging automatically on `main` (Ops or Admin roles)
4. Runs `terraform apply` against production after a required reviewer approves the `production` GitHub Environment (Admin role)

Configure the identity variables in **GitHub Settings → Secrets and variables → Actions → Variables**:

| Variable        | Example value | Notes                                      |
|-----------------|---------------|--------------------------------------------|
| `TF_USER_ID`    | `ci-bot`      | Service-account name evaluated by Cedar    |
| `TF_USER_ROLES` | `Ops`         | Must match a Cedar role in the policy store |
| `TF_WORKSPACE`  | `staging`     | Override for the plan job; apply jobs hardcode the target |
| `OPA_URL`       | `http://opa-service:8181` | URL of the running opa-cedarling server |

> **Working directory**: The example paths (`./demo/terraform/tf_authz.sh`, etc.) assume the job runs from `jans-cedarling/cedarling_opa/`. If you copy the workflow into a repository with a different root, adjust the paths or add `defaults: run: working-directory: jans-cedarling/cedarling_opa` at the workflow level.

### Reusable composite action

If you already have a Terraform workflow and only need to add the Cedar authorization check, use the composite action instead of copying the full workflow.  The action is self-contained — it embeds the authorization logic inline so no additional scripts need to be present on the runner.

The action lives at:
```
.github/actions/tf-authz/action.yml
```
inside this repository (`JanssenProject/jans`).

**Single-step authorization check:**

```yaml
- name: Authorize terraform plan via Cedarling-OPA
  uses: JanssenProject/jans/jans-cedarling/cedarling_opa/demo/terraform/.github/actions/tf-authz@main
  with:
    tf_user_id:    ${{ vars.TF_USER_ID }}
    tf_user_roles: ${{ vars.TF_USER_ROLES }}
    tf_workspace:  ${{ vars.TF_WORKSPACE || 'staging' }}
    opa_url:       ${{ vars.OPA_URL || 'http://opa-service:8181' }}
    tf_subcommand: plan
```

**Complete minimal example — adding authorization to an existing plan job:**

```yaml
jobs:
  terraform-plan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@de0fac2e4500dabe0009e67214ff5f5447ce83dd

      - uses: hashicorp/setup-terraform@v3

      - name: Terraform init
        run: terraform init

      - name: Authorize terraform plan via Cedarling-OPA
        uses: JanssenProject/jans/jans-cedarling/cedarling_opa/demo/terraform/.github/actions/tf-authz@main
        with:
          tf_user_id:    ${{ vars.TF_USER_ID }}
          tf_user_roles: ${{ vars.TF_USER_ROLES }}
          tf_workspace:  ${{ vars.TF_WORKSPACE || 'staging' }}
          opa_url:       ${{ vars.OPA_URL || 'http://opa-service:8181' }}
          tf_subcommand: plan

      - name: Terraform plan
        run: terraform plan
```

The authorization step must succeed (exit 0) before the `terraform plan` step runs.  If Cedar denies the request the job fails immediately and Terraform is never invoked.

**Action inputs:**

| Input           | Required | Default                 | Description                                                                         |
|-----------------|----------|-------------------------|-------------------------------------------------------------------------------------|
| `tf_user_id`    | yes      | —                       | Identity of the operator or CI service account (Cedar principal)                    |
| `tf_user_roles` | yes      | —                       | Comma-separated Cedar roles (e.g. `"Developer"` or `"Ops,Admin"`)                  |
| `tf_workspace`  | yes      | —                       | Target workspace evaluated as the Cedar resource                                    |
| `opa_url`       | no       | `http://localhost:8181` | Base URL of the running opa-cedarling server                                        |
| `tf_subcommand` | yes      | —                       | Terraform subcommand to authorize (`plan`, `apply`, `destroy`); other values skip the check |

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `could not reach OPA server` | opa-cedarling not running | Start it (step 1) |
| `DENIED` for admin user | `TF_USER_ROLES` not set to `Admin` | Check env var spelling and case |
| `Cedarling uninitialized` in OPA logs | Policy store path wrong | Verify `CEDARLING_POLICY_STORE_LOCAL_FN` points to the `policy-store/` directory |
| OPA returns empty result | Rego package mismatch | Confirm the OPA endpoint is `/v1/data/infra/terraform` |
