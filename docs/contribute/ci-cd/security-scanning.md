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
| CBOM (cbomkit) | `scan-cbom.yml` | crypto algorithms, protocols, certs, keys | signed CycloneDX 1.6 JSON | release asset `jans-cbom.json`; image CBOM workflow artifact only |
| Pen-test (DAST) | `scan-pentest.yml` | live endpoints | PDF/JSON/MD/SARIF | workflow artifact only — CONFIDENTIAL, not attached to the public release |

## CBOM

`scan-cbom.yml` produces a cryptographic bill of materials (CycloneDX 1.6) for
post-quantum readiness reviews.

- **Source** (after each tag-push "Build: Publish" succeeds, or dispatch): builds
  the Maven modules, then
  [cbomkit-action](https://github.com/cbomkit/cbomkit-action) scans Java, Python
  and Go code for crypto API use. Cedarling is Rust, which cbomkit does not scan,
  so `scripts/cbom.py rust` maps the crypto crates in `jans-cedarling/Cargo.lock`
  to the algorithms they provide. Both merge into `jans-cbom.json`, stamped with
  the scanned commit (`jans:commit`). After a tag-push publish it is
  cosign-signed and attached to the release, unless the tag has since moved or
  the release already holds a complete pair for that commit. A manual run only
  uploads an unsigned `jans-cbom` workflow artifact.
- **Images** (manual dispatch only): [cbomkit-theia](https://github.com/cbomkit/cbomkit-theia)
  scans each published image for certificates, keys, secrets and OpenSSL config.
  Base-image CA trust stores are skipped; Janssen certs under `/etc/certs` are
  not. Image digests are resolved once per run, so a partial re-run scans the
  same images. Each platform of a multi-arch image is scanned by its own digest.
  Every finding carries `jans:image`; findings from multi-platform scans also
  carry `jans:platform`, and each container component records its
  `jans:image-index`. Output is a workflow artifact (`jans-image-cbom`), not a
  release asset, until reviewed. If some images fail, the artifact is
  `jans-image-cbom-partial` and lists them in `jans:missing-images`. If none
  succeed, no artifact is produced and `image-publish` fails at "Build
  jans-image-cbom.json".

`scripts/cbom.py gate` runs on every CBOM before upload. It strips key values and
fails the job if it finds private keys, secret or generic keys, passwords,
credentials or tokens, at any nesting depth, printing only the type, finding name
(e.g. `generic-api-key`) and file path. The gitleaks false positive on the stock
JDK `java.security` is allowed. theia stderr is discarded because
it can echo scanned file contents into the public run log.

## Pen-test (DAST)

`scan-pentest.yml` is dispatched by "Build: Docker Images" at the release ref once
the images are published for a nightly or tagged (`v**`) release — so it scans the
freshly published all-in-one image — and can also be dispatched manually (pass
`release_tag` to ingest that release's SBOM/Trivy assets; leave it empty for an
artifact-only, best-effort run). It runs the full DAST template set within a bounded time
window (a per-scan shell timeout under a step `timeout-minutes` backstop); if the
limit is reached the scan stops and the report is built from partial results. It
does **not fail on findings** (severity never breaks the build), but a run with a
non-empty `release_tag` **hard-fails** if that release's SBOM/Trivy assets never
publish. An empty `release_tag` skips that wait and is best-effort.

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
3. **DAST** — a ZAP baseline scan, ZAP active API scans of every in-repo OpenAPI
   spec (auth-server, config-api, fido2, scim, lock) targeting the live instance,
   and the full nuclei template set against every discovered edge; raw output
   (baseline + per-spec API + nuclei) is uploaded per backend.
4. **Ingest** — the `report` job runs `scripts/pentest_ingest_scans.py` once,
   pulling every available data source into `context.json`: open code-scanning
   alerts (CodeQL SAST + Scorecard), Dependabot advisories, the Trivy container-image
   CVE report, and the enriched CycloneDX SBOM (component inventory + per-component
   vulnerabilities). Whenever `release_tag` is set it first waits for that release's
   SBOM and Trivy assets to be published by their own workflows and hard-fails if they
   never appear; only an empty `release_tag` skips the wait. The ingest step itself is
   best-effort — a source that errors is logged and dropped, never fatal — so the wait
   guarantees the assets exist, not that every source landed in the report. These
   become findings in the
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
   artifacts restrict access to users with repository access. The PDF and a link to
   the run are also posted to Zulip (`#bot_reporter`, topic `pen-test`) when
   `ZULIP_API_KEY` is set.

### Configuration

| Secret / Var | Required | Use |
|---|---|---|
| `PENTEST_AI_ENDPOINT` (secret) | optional | Messages API URL, e.g. `https://api.anthropic.com/v1/messages`; step skipped if unset |
| `PENTEST_AI_TOKEN` (secret) | optional | API key, sent as the `x-api-key` header |
| `PENTEST_AI_MODEL` (var) | optional | model id (default `claude-opus-5`) |
| `PENTEST_AI_API_VERSION` (var) | optional | `anthropic-version` header (default `2023-06-01`) |
| `ZULIP_API_KEY` (secret) | optional | post the PDF + run link to Zulip `#bot_reporter` / `pen-test`; skipped if unset |

The analysis step calls the Messages API directly: it sends the DAST output and
ingested context as a single user message with a system prompt instructing the
model to return `{ "findings": [...] }`, then parses `.content[0].text`. Set
`PENTEST_AI_MODEL` / `PENTEST_AI_API_VERSION` repository variables to override the
defaults.
