---
tags:
- CI/CD
---

# Workflow Reference

One row per workflow under `.github/workflows/`. See
[CI/CD Architecture](architecture.md) for the chain and
[Security Scanning](security-scanning.md) for scan detail.

## Build & release

| Workflow | Trigger | Purpose |
|---|---|---|
| `build-publish.yml` | tag `v**`/`nightly`, dispatch | build Java modules, publish to GitHub Packages Maven + release assets (SLSA-signed). Chain hub. |
| `build-docker-images.yml` | `workflow_run` (Build & Publish), push/PR, dispatch | build & push the 14 container images to ghcr, cosign-signed. |
| `build-packages.yml` | `workflow_run` (Build & Publish), dispatch | deb/rpm, Python wheels, cedarling wasm/python/go/uniffi packages + SLSA provenance. |
| `build-nightly.yml` | cron 23:00, dispatch | recreate the `nightly` tag/release; call `release-cedarling`. |
| `build-sandbox.yml` | dispatch | build a branch and deploy to an ephemeral DigitalOcean VM. |
| `build-docs.yml` | push/PR to docs, release, dispatch | mkdocs + Helm chart publish to GitHub Pages. |
| `release-trigger.yml` | dispatch | version bump, tag `v<version>`, create release; call `release-cedarling`. |
| `release-cedarling.yml` | `workflow_call`, dispatch | publish the cedarling crate to crates.io (reusable). |
| `release-backport.yml` | `pull_request_target` | open backport PRs from a merged labelled PR. |

## Tests & checks

| Workflow | Trigger | Purpose |
|---|---|---|
| `ci-pr-checks.yml` | PR | yamllint, gradle-wrapper validation, `go vet`. |
| `lint-python.yml` | push/PR (py paths) | flake8 over pycloudlib/cli-tui/linux-setup. |
| `test-cedarling.yml` | PR (cedarling paths) | Rust/wasm/python/go/C/Java/pgrx test matrix. |
| `test-integration.yml` | cron 04:00, dispatch, PR (filtered paths) | full TestNG suite against source-built AIO on a DO droplet. PR runs only on the docker/service paths; the nightly cron and dispatch cover changes outside them. |
| `test-terraform-provider.yml` | push/PR/tag, cron, dispatch | provider acceptance tests against the prebuilt AIO compose stack. |
| `test-tf-authz-action.yml` | push/PR, `workflow_run` (Build Docker Images) | tf-authz composite-action + Cedar policy tests via OPA. |
| `test-tf-authz-jwt.yml` | push/PR, `workflow_run` (Build Docker Images) | self-hosted-OPA JWT allow/deny assertions (see `scripts/authz_assert.sh`). |
| `test-pycloudlib.yml` | push/PR (pycloudlib paths) | pytest matrix. |

## Scans

| Workflow | Trigger | Purpose |
|---|---|---|
| `scan-codeql.yml` | push/PR | CodeQL SAST (python/js/go). |
| `scan-dependency.yml` | PR | dependency-review of changed manifests. |
| `scan-sonar.yml` | push/PR, dispatch | SonarCloud quality/security scan per module. |
| `scan-scorecard.yml` | push main, weekly | OpenSSF Scorecard. |
| `scan-sbom.yml` | tag `v**`/`nightly` | enriched SBOM + compliance reports to release assets. |
| `scan-pentest.yml` | cron 05:00, tag `v**`, dispatch | DAST pen-test against the live AIO (report-only). |

## Ops

| Workflow | Trigger | Purpose |
|---|---|---|
| `ops-label.yml` | PR/issue events, dispatch | apply labels, add issues to the project board. |
| `ops-pr-ref-issue.yml` | PR opened, dispatch | ensure each PR references an open issue. |
| `ops-sync-tf.yml` | push main (tf paths), dispatch | subtree-sync the provider to its downstream repo. |
| `ops-cache-cleanup.yml` | PR closed, dispatch | delete Actions caches for the branch. |
| `ops-runs-cleanup.yml` | cron every 2 days, dispatch | prune old workflow runs. |
