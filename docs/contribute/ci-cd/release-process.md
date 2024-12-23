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

# Future plans
We are planning a full move to SemVer for all Janssen projects that will be scheduled bi-weekly. In this move, the Google `release-please` GitHub workflow  will be activated to automatically release the projects according to the conventional commits submitted. 