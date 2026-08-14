---
tags:
- CI/CD
- security
---

# Security Scanning

The project runs layered scanning across CI. This page maps each scanner, its
output, and where results land, then describes the pen-test that correlates them.

## Scan topology

| Scanner | Workflow | Scans | Output | Destination |
|---|---|---|---|---|
| CodeQL | `scan-codeql.yml` | source SAST (python/js/go) | SARIF | GitHub code scanning |
| Dependency Review | `scan-dependency.yml` | changed dependency manifests (PR) | PR annotations | inline PR only |
| Sonar | `scan-sonar.yml` | quality + security hotspots per module | Sonar report | SonarCloud (off-platform) |
| Scorecard | `scan-scorecard.yml` | supply-chain posture | SARIF | code scanning + artifact + OpenSSF |
| SBOM (Parlay + sbomqs) | `scan-sbom.yml` | dependency graph + compliance | signed JSON | release assets |
| Pen-test (DAST) | `scan-pentest.yml` | live endpoints | PDF/JSON/MD/SARIF | workflow artifact only — CONFIDENTIAL, not attached to the public release |

## Pen-test (DAST)

`scan-pentest.yml` runs after "Build Docker Images" completes for a nightly or
tagged (`v**`) release — so it scans the freshly published all-in-one image — and
on manual dispatch. It runs the full DAST template set within a bounded time
window (a per-scan shell timeout under a step `timeout-minutes` backstop); if the
limit is reached the scan stops and the report is built from partial results. It
It does **not fail on findings** (severity never breaks the build), but a release
run **hard-fails** if the required upstream release assets (SBOM/Trivy) never
publish, so a release report is never silently incomplete.

Two-stage: a `scan` matrix runs the DAST tooling against a fresh AIO for each
persistence backend (`MYSQL`, `PGSQL`) in parallel; a `report` job then produces
**one consolidated report** across both, with a Backend column distinguishing the
findings.

Flow:

1. **Target (per backend)** — the `scan` matrix brings up the prebuilt AIO compose
   stack (the same one `test-terraform-provider.yml` uses) via
   `automation/ci/run_aio_for_tf.sh`, once per persistence backend.
2. **Discover** — `scripts/pentest_discover_endpoints.py` reads the OpenID
   discovery document and known service edges into `targets.json`.
3. **DAST** — an open baseline scan (plus an OpenAPI-seeded API scan on
   `full_scan`) and the full nuclei template set run against every discovered edge;
   raw output is uploaded per backend.
4. **Ingest** — the `report` job runs `scripts/pentest_ingest_scans.py` once,
   pulling every available data source into `context.json`: open code-scanning
   alerts (CodeQL SAST + Scorecard), Dependabot advisories, the Trivy container-image
   CVE report, and the enriched CycloneDX SBOM (component inventory + per-component
   vulnerabilities). On a release run it first waits for the SBOM and Trivy assets
   to be published by their own workflows and hard-fails if they never appear, so a
   release report is always complete; manual dispatch skips the wait and is
   best-effort. These become findings in the
   report alongside DAST, so one document covers DAST + SAST + SCA + container image
   + supply-chain.
5. **Analysis (optional)** — if `PENTEST_AI_ENDPOINT` / `PENTEST_AI_TOKEN` secrets
   are configured, the consolidated findings (all dimensions) and ingested context
   are sent to a Messages API endpoint for prioritisation; the model returns a
   `{ "findings": [...] }` object. Absent the secrets, the report keeps the
   tool-derived findings only.
6. **Report** — `scripts/pentest_report.py` merges every backend and every ingested
   source into `pentest-report.{pdf,json,md,sarif}`, laid out like a professional
   assessment: cover (logo, CONFIDENTIAL, run metadata, automated-DAST disclaimer),
   executive summary with a severity chart, scope/methodology with an
   assessment-coverage table (findings per dimension), a findings register (with a
   Backend column), detailed write-ups with recommendations for medium+ findings,
   and appendices for run metadata and the SBOM component inventory. All formats
   are uploaded **only** as a workflow artifact (30-day retention) — the report is
   CONFIDENTIAL and is never attached to the public release; GitHub Actions
   artifacts restrict access to users with repository access.

### Configuration

| Secret / Var | Required | Use |
|---|---|---|
| `PENTEST_AI_ENDPOINT` (secret) | optional | Messages API URL, e.g. `https://api.anthropic.com/v1/messages`; step skipped if unset |
| `PENTEST_AI_TOKEN` (secret) | optional | API key, sent as the `x-api-key` header |
| `PENTEST_AI_MODEL` (var) | optional | model id (default `claude-opus-5`) |
| `PENTEST_AI_API_VERSION` (var) | optional | `anthropic-version` header (default `2023-06-01`) |

The analysis step calls the Messages API directly: it sends the DAST output and
ingested context as a single user message with a system prompt instructing the
model to return `{ "findings": [...] }`, then parses `.content[0].text`. Set
`PENTEST_AI_MODEL` / `PENTEST_AI_API_VERSION` repository variables to override the
defaults.
