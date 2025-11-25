---
tags:
  - administration
  - contribute
  - Testing
  - Quality Assurance
---

# Test And Quality Assurance For Janssen Project

The Janssen Project is a large, complex project with tons of inter-dependency. If we aren't test-driven, anarchy will ensue.

Every line of code at Janssen Project goes through various tests at different stages of development. Most of these tests
are automated and executed as part of our CI-CD pipeline, while others are executed manually.

## Unit Testing

Do not submit any code without a corresponding unit test. Also, any bug fixes should increment unit test coverage.

## Component Testing

Component testing uses real-world use cases to exercise a portion of the software, using typical data inputs.
Developers should document component stories and submit them to the component test library for the respective
repository. A tester should be able to run component tests manually. Component tests should run automatically with each
Jenkins build.

The OpenID Foundation [certification tests](https://openid.net/certification) supplement the component testing library,
and should be run for each major release of the software for which they are available.

## Performance Testing

Performance tests are critical to optimization of the persistence and caching implementation.
All major releases of the software should be tested for performance with all supported database and cache
configurations using the Cloud Native distribution. The VM distribution will not be performance tested, as the main
goal for this distribution is development and small deployments. The JMeter test tool should be used to generate the
load. These tests are published so community members can run their own benchmarking analysis.

In addition to standard performance testing, it’s highly recommended to perform full benchmarking to understand your
system’s performance. The [Benchmarking Documentation](../janssen-server/recipes/benchmark.md) provides a comprehensive
guide for testing with JMeter scripts and other load generation tools.

## HA Tests

HA tests should be run against the Cloud Native distribution, which by design is active-active with no single point of
failure. The HA testing should simulate taking down various pieces of infrastructure, to see if authentications can
still proceed. Also, what happens to transactions that were in progress during the crash?

## Penetration Tests

Penetration testing is highly deployment specific. Depending on different implementations of the Janssen Project
software, you may achieve different levels of risk mitigation. Thus it is important that organizations that operate
their own IAM platform based on Janssen perform their own penetration testing.

## Dependency Vulnerabilities

Dependency vulnerabilities are monitored by GitHub. In addition we plan to use the
[Linux Foundation Community Bridge](https://security.communitybridge.org) vulnerability detection platform.

## Contributing To The Documentation

While contributing documentation to official Janssen
Project [documentation](https://jans.io/docs/) it is important
to ensure the following.

- Changes adhere to the [style guidelines](../CONTRIBUTING.md#documentation-style-guide)
- Changes have been proofread to remove any typographical or grammatical errors
- If section titles are being updated or removed, check and update any reference
links to that section
- Changes render correctly on the documentation static site

The command below will checkout the documentation and deploy the static site
locally. Any changes made to the local documentation will be live reloaded
on the locally deployed static site.

```bash
git clone --depth 100 --filter blob:none --no-checkout https://github.com/janssenproject/jans \
    && cd jans \
    && git sparse-checkout init --cone \
    && git checkout main \
    && git sparse-checkout add docs \
    && git sparse-checkout add --skip-checks mkdocs.yml \
    && cd docs \
    && poetry install --no-root \
    && poetry run mkdocs serve -f ../mkdocs.yml
