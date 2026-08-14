# CI/CD Architecture

Overview of the GitHub Actions workflows under `.github/workflows/` and how they chain. See [Workflow Reference](https://docs.jans.io/nightly/contribute/ci-cd/workflows/index.md) for a per-workflow table and [Security Scanning](https://docs.jans.io/nightly/contribute/ci-cd/security-scanning/index.md) for the scan and pen-test topology.

## Naming convention

Workflows are named `<domain>-<purpose>.yml` with a matching `name:` field:

| Prefix          | Purpose                                                                                   |
| --------------- | ----------------------------------------------------------------------------------------- |
| `build-`        | produce/publish artifacts (Maven, containers, packages, docs, sandbox)                    |
| `test-`         | acceptance/integration/unit tests                                                         |
| `scan-`         | security & quality scanning (CodeQL, Sonar, Scorecard, Dependency Review, SBOM, pen-test) |
| `release-`      | release orchestration (bump/tag, backport, crates.io)                                     |
| `ci-` / `lint-` | PR gate checks and linters                                                                |
| `ops-`          | repository automation and housekeeping                                                    |

## Release / build chain

The build hub is `build-publish.yml` (`name: Build & Publish`). A tag push starts it; on completion two `workflow_run` listeners fan out to the container and package builds.

```
flowchart TD
  RT[release-trigger.yml<br/>bump & tag] -->|push v** tag| BP
  BN[build-nightly.yml<br/>cron 23:00] -->|recreate nightly tag| BP
  RT -->|workflow_call| RC[release-cedarling.yml]
  BN -->|workflow_call| RC
  BP[build-publish.yml<br/>Build & Publish] -->|workflow_call| SLSA[slsa-github-generator]
  BP -->|workflow_run: completed| BDI[build-docker-images.yml]
  BP -->|workflow_run: completed| BPK[build-packages.yml]
  BDI -->|workflow_run: completed| TA[test-tf-authz-action.yml]
  BDI -->|workflow_run: completed| TJ[test-tf-authz-jwt.yml]
  REL[release published] -.waits on run.-> BD[build-docs.yml]
```

## Trigger mechanisms

| Mechanism           | Where                                                                                                               | Note                                                                                                      |
| ------------------- | ------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| tag push (PAT)      | `release-trigger`, `build-nightly`                                                                                  | a `GITHUB_TOKEN`-pushed tag does not trigger workflows, so a PAT (`MOAUTO_WORKFLOW_TOKEN`) pushes the tag |
| `workflow_run`      | `build-docker-images`, `build-packages` listen on `Build & Publish`; tf-authz tests listen on `Build Docker Images` | loose coupling by workflow `name:`; renaming a `name:` breaks its listeners                               |
| `workflow_call`     | `release-cedarling` (reusable), `slsa-github-generator`                                                             | true reusable workflows                                                                                   |
| `workflow_dispatch` | most build/release workflows                                                                                        | manual entry points                                                                                       |

Renaming caution

`workflow_run` and branch-protection required-status checks both key off the workflow `name:`. When a `name:` changes, update the listeners and the repository's required-check settings in the same cutover.

## Ephemeral environments

- `test-integration.yml` — builds AIO from source on an ephemeral DigitalOcean droplet.
- `test-terraform-provider.yml` and `scan-pentest.yml` — bring up the prebuilt AIO compose stack (consul+vault+traefik+DB+AIO) on the runner.
- `test-tf-authz-*` — start the demo OPA via the shared `.github/actions/opa-up` composite action.
- `build-sandbox.yml` — provisions a developer DigitalOcean VM via easycloud.
