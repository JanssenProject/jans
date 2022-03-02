---
layout: default
---

# Join the community

* Chat: We have an active [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). You can register for free their with your Github identity.
* Tweet: Janssen is on [Twitter](https://twitter.com/janssen_project) too. Follow us there to stay up to date on release announcements and news around Janssen.
* Email: We have an active mailing list also.
* Google group : You can subscribe to the [Janssen Google Group](https://groups.google.com/u/2/g/janssen_project)
  and post messages there.

# Contribute

Please go through our [contribution guidelines](#contribution-guidelines) so it'll be easy for you to make successful contributions.

In case you are _**first-time**_ contributor, then you can start with our [good first issues list](https://github.com/JanssenProject/home/labels/good%20first%20issue) These are issues where you can easily contribute and community members will guide and support your contribution as always.

If you need Janssen installation to test out your fix, here are the [steps](#janssen-quick-install).

There are many ways of contributing to Janssen. And it is not just limited to fixing issues and raising pull requests. Janssen welcomes you to [raise issues](#issues), respond to queries, suggest new features, tell us your experience with Janssen, be it good or bad. All these are contributions towards making Janssen better.

# Contribution guidelines

We are really glad you are reading this, because we need volunteer developers to help this project come to fruition.

- [Code of Conduct](#code-of-conduct)
- [Community](#community)
- [Issues](#issues)
- [Triaging](#triaging)
- [Coding Conventions](#code-conventions)
- [Developer Certificate Of Origin](#developer-certificate-of-origin)

## Code of Conduct

Janssen project has a
[Code of Conduct](code-of-conduct.md)
to which all contributors must adhere, please read it before interacting with the repository or the community in any way.

## Community

We have setup a [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Please raise discussion and **support requests** here first.

Join the [google group mailing list](https://groups.google.com/u/2/g/janssen_project) for news, announcements, and a Google calendar invite for our community open source meetings.

We also run monthly **community calls** which are open calls to discuss Janssen projects from an user perspective. In case you want to discuss a topic during those calls you can simply propose it opening an issue in the [community](https://github.com/JanssenProject/community) repository and join the call!

## Issues

There are four kinds of issues you can open:

- Bug report: you believe you found a problem in a project and you want to discuss and get it fixed,
  creating an issue with the **bug report template** is the best way to do so.
- Enhancement: any kind of new feature need to be discussed in this kind of issue, do you want a new rule or a new feature? This is the kind of issue you want to open. Be very good at explaining your intent, it's always important that others can understand what you mean in order to discuss, be open and collaborative in letting others help you getting this done!
- Vulnerability: If you identify a security problem, please report it immediately, providing details about the nature, and if applicable, how to reproduce it. If you want to report an issue privately, you can email security@gluu.org
- Failing tests: you noticed a flaky test or a problem with a build? This is the kind of issue to triage that!

The best way to get **involved** in the project is through issues, you can help in many ways:

- Issues triaging: participating in the discussion and adding details to open issues is always a good thing,
  sometimes issues need to be verified, you could be the one writing a test case to fix a bug!
- Helping to resolve the issue: you can help in getting it fixed in many ways, more often by opening a pull request. In case you are _**first-time**_ contributor, then you can start with our [good first issues list](https://github.com/JanssenProject/home/labels/good%20first%20issue) These are issues where you can easily contribute and community members will guide and support your contribution as always.

## Triaging

Triage is a process of evaluating issues and PRs in order to determine their characteristics and take quick actions if possible.

When you triage an issue, you:

* assess whether it has merit or not

* quickly close it by correctly answering a question

* point the reporter to a resource or documentation answering the issue

* tag it via labels, projects, or milestones

* take ownership submitting a PR for it, in case you want 😇

Here is how we [continously triage](triage.md) new issues and PRs so that contributors can contribute faster and better.


## Code conventions

### Commits
As commit convention, we adopt [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/), we have an history
of commits that do not adopt the convention but any new commit must follow it to be eligible for merge.

### Branch names
Branch name should have component name as prefix, eg `jans-core-mybranch`

### PR titles
PR titles should also follow [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/). This will help in keeping merge commit messages inline with commit message standards

## Developer Certificate Of Origin

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
