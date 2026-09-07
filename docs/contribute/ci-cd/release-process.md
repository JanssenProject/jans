---
tags:
- Release
- faq
---

# Release Process

----------------------------

The release process starts on scheduled times detailed in the [milestones](https://github.com/JanssenProject/jans/milestones) of the Janssen project. The release manager is responsible for coordinating the release process and ensuring that all steps are completed. The process is as follows:

1. **Release Planning**: The release manager creates a release plan that normally includes the release date, the version number, and the main features that will be included in the release. The release plan is shared with the team for review and feedback.
2. **Feature Freeze**: The release manager announces the feature freeze date. After this date, no new features will be added to the release. The team focuses on fixing bugs and improving the quality of the release. QA starts testing the release.
3. **Code Freeze**: The release manager announces the code freeze date. After this date, no new code will be added to the release. The team focuses on fixing bugs and improving the quality of the release. QA team verifies testing the release.
4. **Release Candidate**: The release manager creates a release candidate and shares it with the team for testing. The team tests the release candidate and reports any issues found. The release manager fixes the issues and creates a new release candidate. This process continues until the release candidate is stable. This is normally done via a PR process and release branch following the structure `release-<version>`.
5. **Release**: The release manager creates the final release and shares it with the team. The team tests the final release and reports any issues found. The release manager fixes the issues and creates a new release. This process continues until the final release is stable.
6. **Release Notes**: The release manager creates the release notes and shares them with the team. The release notes include the version number, the main features included in the release, and any known issues. The release notes are shared with the community. This process is automated and picked up through conventional commits developers submit.
7. **Release Announcement**: The release manager announces the release to the community. The announcement includes the version number, the main features included in the release, and any known issues. The announcement is shared on the Janssen website, the Janssen blog, and social media. The release manager also sends an email to the Janssen mailing list.
8. **Post-Release**: The release manager monitors the release and addresses any issues that arise. The team continues to work on the next release.
9. **Release Retrospective**: The release manager conducts a retrospective to review the release process and identify areas for improvement. The team provides feedback on the release process. The release manager uses this feedback to improve the release process for future releases.
10. **Next Release Planning**: The release manager starts planning the next release. The process starts again from step 1. A branch `release-<version>` is created for the next dev and snapshot release with a similar process from step 1 and is merged into `main`.

# Tooling

Releases are cut by the `release-trigger.yml` workflow (version bump, tag
`v<version>`, GitHub Release), which starts the [build chain](architecture.md).
Nightly prereleases are produced by `build-nightly.yml`.

# Terraform provider

`terraform-provider-jans/` in this repo is the source of truth. The standalone
[terraform-provider-jans](https://github.com/JanssenProject/terraform-provider-jans)
repo is a generated mirror of that folder; it exists only because the Terraform and
OpenTofu registries require one repo per provider. Its `.github/` is owned downstream
(the goreleaser + GPG release workflow lives there) and is excluded from the mirror.
Nothing else downstream is edited by hand — a direct commit there is overwritten by the
next sync.

Two workflows, no downstream PR:

- `ops-sync-tf.yml` — rsyncs the folder onto downstream `main` on every push to
  monorepo `main` that touches it, and, when called with a `tag`, pushes that tag.
- `release-terraform-provider.yml` — listens for `test-terraform-provider.yml`
  concluding at a `v*` ref (the tail of the release chain: images built, provider
  tested against them), then pings Zulip and waits for approval on the
  `terraform-provider-release` environment. On approval it calls `ops-sync-tf.yml`
  with the tag; the downstream tag push runs goreleaser, which signs and publishes
  the version the registries index.

The provider version therefore always equals the Janssen release version. Re-runs are
idempotent: an existing downstream tag makes the workflow skip. If the provider tests
are red at a release tag the workflow reports to Zulip instead of publishing; publish
anyway with the `Release: Terraform Provider` dispatch (`ignore_tests`).

One-time setup: an environment named `terraform-provider-release` with @moabu as a
required reviewer. Without it the approval gate is a no-op and the release publishes
unattended.

# Future plans

We are planning a full move to SemVer for all Janssen projects that will be scheduled bi-weekly, releasing automatically from the conventional commits submitted.
