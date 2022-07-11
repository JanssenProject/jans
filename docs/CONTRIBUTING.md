# Contributing to Janssen Project

Purpose of this guide is to provide necessary information and resources to community in order to make successful contribution to the Janssen Project.

There are many ways you can contribute. Of course you can contribute code. But we also need people to write documentation and guides, to help us with testing, to answer questions on the forums and chat, to review PR's, to help us with devops and CI/CD, to provide feedback on usability, and to promote the project through outreach. Also, by sharing metrics with us, we can gain valuable insights into how the software performs in the wild. 

- [Join the Community](#join-the-community)
- [First Time Contributors](#first-time-contributors)
- [Contribution Guidelines](#contribution-guidelines)
  - [Code of Conduct](#code-of-conduct)
  - [About Issues](#about-issues)
  - [Triaging](#triaging)
  - [Code Conventions and Guidelines](#code-conventions-and-guidelines)
    - [Commits](#commits)
    - [Branches](#branches)
    - [PRs](#prs)
    - [Issues](#issues)
    - [Contributing to the documentation](#contributing-to-the-documentation)
- [Contribution Workflow](#contribution-workflow)
  - [Find Something To Work On](#find-something-to-work-on)
  - [Start a Discussion](#start-a-discussion)
  - [Implement the Change](#implement-the-change)
  - [Document](#document)
  - [Raise a PR](#raise-a-pr)
  - [Follow Through](#follow-through)
  

# Join the Community

* **Repo**: Watch and Star [Janssen repository](https://github.com/JanssenProject/jans) on Github
* **Discussions**: Join interesting discussions at [Github Discussions](https://github.com/JanssenProject/jans/discussions)
* **Chat**: We have an active [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). You can register for free their with your Github identity.
* **Tweet**: Janssen is on [Twitter](https://twitter.com/janssen_project) too. Follow us there to stay up to date on release announcements and news around Janssen.

# First Time Contributors

In case you are _**first-time**_ contributor, then you can start with our [good first issues list](https://github.com/JanssenProject/jans/issues?q=is%3Aissue+is%3Aopen++label%3A%22good+first+issue%22) These are issues where you can easily contribute and community members will guide and support your contribution as always.

If you need Janssen installation to test out your fix, here are the [steps](https://github.com/JanssenProject/jans/wiki#janssen-installation).


# Contribution Guidelines

We are really glad you are reading this, because we need volunteer developers to help this project come to fruition.

- [Code of Conduct](#code-of-conduct)
- [Issues](#issues)
- [Triaging](#triaging)
- [Coding Conventions](#code-conventions-and-guidelines)

## Code of Conduct

Janssen project has a
[Code of Conduct](CODE_OF_CONDUCT.md)
to which all contributors must adhere, please read it before interacting with the repository or the community in any way.

## About Issues

There are four kinds of issues you can open:

- **Bug report**: you believe you found a problem in a project and you want to discuss and get it fixed,
  creating an issue with the **bug report template** is the best way to do so.
- **Feature Request**: any kind of new feature need to be discussed in this kind of issue, do you want a new rule or a new feature? This is the kind of issue you want to open. Be very good at explaining your intent, it's always important that others can understand what you mean in order to discuss, be open and collaborative in letting others help you getting this done!
- **Security Vulnerability**: If you identify a security problem, please report it immediately, providing details about the nature, and if applicable, how to reproduce it. If you want to report an issue privately, you can email security@gluu.org
- **Failing tests**: you noticed a flaky test or a problem with a build? This is the kind of issue to triage that!

The best way to get **involved** in the project is through issues, you can help in many ways:

- Issues triaging: participating in the discussion and adding details to open issues is always a good thing,
  sometimes issues need to be verified, you could be the one writing a test case to fix a bug!
- Fix an issue: you can help in getting issue fixed in many ways. More often by opening a pull request. In case you are _**first-time**_ contributor, then you can start with our [good first issues list](https://github.com/JanssenProject/jans/issues?q=is%3Aissue+is%3Aopen++label%3A%22good+first+issue%22) These are issues where you can easily contribute and community members will guide and support your contribution as always.

## Triaging

Triage is a process of evaluating issues and PRs in order to determine their characteristics and take quick actions if possible.

When you triage an issue, you:

* assess whether it has merit or not

* quickly close it by correctly answering a question

* point the reporter to a resource or documentation answering the issue

* tag it via labels, projects, or milestones

* take ownership submitting a PR for it, in case you want 😇

Here is how we [continously triage](governance/triage.md) new issues and PRs so that contributors can contribute faster and better.


## Code Conventions and Guidelines

### Commits

Janssen Project mandates all commits to follow guidelines as below.

- **Commit messages**

  As commit convention, we adopt [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/), we have an history
  of commits that do not adopt the convention but any new commit must follow it to be eligible for merge.

- **Add GPG signature to your commit**

  To ensure that contribution is coming for a trusted source, all commits should be signed using GPG key and verified by Github. If you have GPG key    setup already then just use `-S` switch with you commit to sign it. If you need to setup your GPG key and verification, then you can find detailed instructions [here](https://docs.github.com/en/authentication/managing-commit-signature-verification)

- **Add DCO sign-off**
  
  The [Developer Certificate of Origin (DCO)](https://developercertificate.org/) is a lightweight way for contributors to certify that they wrote or otherwise have the right to submit the code they are contributing to the project.

  Contributors to the Janssen project sign-off that they adhere to these requirements by adding a `Signed-off-by` line to commit messages.

  ```
  This is a commit message

  Signed-off-by: Foo Bar <foobar@spam.org>
  ```

  Git even has a `-s` command line option to append this automatically to your commit message:

  ```
  $ git commit -s -m 'This is my commit message'
  ```

In all, if you have your GPG verification setup, your commit command should look like 

```
git commit -S -s -m 'message that follows conventional commit style'
```

### Branches
Branch name should have component name as prefix, eg `jans-core-mybranch`

### PRs
- PR titles should also follow [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/). This will help in keeping merge commit messages inline with commit message standards
- Squash commits into small number of cohesive commits before raising a PR
- PR should be rebased on main branch so that there are minimal or no conflicts at the time of merge
- PR should only have changes related to target feature or issue. Create a separate PR for formatting or other quick bug fixes
- PR should include relevent documentaton changes
- PR should include unit and integration tests

### Issues 
- Issue titles should follow [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/)

## Contributing to the documentation
Great documentation is a reflection of software's maturity and the great community that stands behind it. Contributing to the Janssen Project documentation is the easiest way to learn about the Janssen Project and to get involved in the community process. 

In order to ensure consistency of style, language, format, and terminology across all documents, please follow the guidelines below:

### Glossary of terms

This glossary helps to keep terms and their meanings consistent across documentation.

- `Janssen Project` or `Jans`: 

  Refers to the official project name under Linux Foundation that seeks to build the world’s fastest and most comprehensive cloud native identity and access management software platform

- `Janssen Server`:

  Refers to a set of software components developed under the Janssen Project . Components of the Janssen Server include client and server implementations of the OAuth, OpenID Connect, SCIM and FIDO standards. The term `Janssen Server` is used to refer to these components as a group. 

- `jans-auth-server`: 

  Refers to a module within the Janssen Server named `jans-auth-server`. This is one of the significant modules of the Janssen Server that has an implementation for OAuth and OpenId Connect. 

- Janssen Server module names:

  For correct naming of other modules of the Janssen Server, please refer to [README](https://github.com/JanssenProject/jans#janssen-modules)

# Contribution Workflow

## Find something to work on
Best place to find something to work on is to look at [currently open issues](https://github.com/JanssenProject/jans/issues). If you are a first time contributor then starting with [list of good first issues](https://github.com/JanssenProject/jans/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) is best.

## Start a discussion
Start a [Github Discussion](https://github.com/JanssenProject/jans/discussions) about what you are planning to contribute. Explain the feature or issue that you are planning to contribute and what your solution or implementation approach. Janssen is a community driven project and it'll be helpful to get community's view about it.

## Create an issue
Take your time to write a proper issue including a good summary and description. Outcome of Github discussion about your contribution can help you create good content for the issue. As a first step when creating a new issue, an issue template has to be selected. Select appropriate `issue template` and it'll help you create an issue with right content.

Remember that issue may be the first thing a reviewer of your PR will look at to get an idea of what you are proposing. It will also be used by the community in the future to find about what new features and enhancements are included in new releases.

## Implement the change
All contributions to Janssen Project should be made via Github pull requests(PR). 

> New to PR workflow?? Learn and practice it at [first-contributions](https://github.com/firstcontributions/first-contributions)

### Create a Fork
Fork [Janssen repository](https://github.com/JanssenProject/jans). And create a clone.

### Implement the Change

Start working on changes as required. 

- Make sure the [code conventions](#code-conventions-and-guidelines) are being followed.
- Use static code analysis and linting tools to make sure the code is high-quality.
- Write tests first and then code. Ensure that integration tests that cover your code are appropriately updated and reviewed.
- Create PR early and push often. 
- Janssen uses Github actions to run automated checks on PR changes. Ensure that these checks are passing with every push.
- Engage PR reviewers at the start so that they can continue to reivew code as it is developed and in small chunks.
- For a change that is non-trivial(an enhancement or a new feature), design should be reviewed. This should be done via PR by adding appropriate code owners.

<!-- TODO: enable this once the workspace guide is ready and developers can test locally from workspace
## Test 

To run tests locally before pushing your code, refer to [TESTING] guide.
-->

## Document

PR should include changes in relevent documentation along with code changes.

## Raise a PR
- Make sure that PR title follows these [guidelines](#prs)
- Janssen uses Github PR template. Template provides helpful instructions to ensure new PRs are complete in details and easy to review. Github will populate new PR's description with these instructions. You can edit PR description as per your requirements.
- When PR is raised, Github will automatically assign reviewer to the PR based changed files and [CODEOWNERS](https://github.com/JanssenProject/jans/blob/main/.github%2FCODEOWNERS) list.
- Once PR is raised, ensure that PR passes all the mandatory Github actions checks available on Github PR page. Github will not allow PR to be merged if any of the mandatory check is failing.

## Follow Through
Once the PR is raised, wait for reviewers to start review. Reviewers will start review at the first opportunity available. If you want to draw attention, give a gentle reminder in PR comments. But please be patient. 

Follow activities on your PR closely till the time PR is merged. PR reviewer may want to suggest a change or may need to ask a question to get more clarity. Make sure you are actively collaborating. Once Reviewer has completed the review and approved the changes, the PR will be merged.

Thats it!! Congratulations on successful contribution. 🥳 🤟
