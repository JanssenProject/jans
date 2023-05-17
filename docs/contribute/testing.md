# Test Driven development

The Janssen Project is a large, complex project with tons of inter-dependency.  If we aren't test-driven, anarchy will ensue.

### Unit testing
Do not submit any code without a corresponding unit test. Also, any bug fixes should increment unit test coverage. All unit tests are executed with any subsequent Jenkins build.

### Component testing
Component testing uses real world use cases to exercise a portion of the software, using typical data inputs. Developers should document component stories and submit them to the component test library for the respective repository. A tester should be able to run component tests manually. Component tests should run automatically with each Jenkins build.

The OpenID Foundation [certification tests](https://openid.net/certification) supplement the component testing library, and should be run for each major release of the software for which they are available.

### Performance testing
Performance tests are critical to optimization of the persistence and caching implementation. All major releases of the software should be tested for performance with all supported database and cache configurations using the Cloud Native distribution. The VM distribution will not be performance tested, as the main goal for this distribution is development and small deployments. The JMeter test tool should be used to generate the load. These tests are published so community members can run their own bench-marking analysis.

## HA Tests
HA tests should be run against the Cloud Native distribution, which by design is active-active with no single point of failure. The HA testing should simulate taking down various pieces of infrastructure, to see if authentications can still proceed. Also, what happens to transactions that were in progress during the crash?

# Penetration tests
Penetration testing is highly deployment specific. Depending on different implementations of the Janssen Project software, you may achieve different levels of risk mitigation. Thus it is important that organizations that operate their own IAM platform based on Janssen perform their own penetration
testing.

# Dependency Vulnerabilities
Dependency vulnerabilities are monitored by Gihub. In addition we plan to use the [Linux Foundation Community Bridge](https://security.communitybridge.org) vulnerability detection platform.

# Documentation local testing

While contributing documentation to official Janssen Project [documentation](https://jans.io/docs/) it is important to make sure that documents meet [style guidelines](../CONTRIBUTING.md#documentation-style-guide) and have been proofread to remove any typographical or grammatical errors.
Janssen Project uses [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/) to create the documenation site. Before new content is pushed to the repository on GitHub, it should be tested locally by the author. Author can do this by deploying Material for MkDocs locally.

High-level steps involve:

1. [Install Material for MkDocs](https://squidfunk.github.io/mkdocs-material/getting-started/#installation)
2. Install required plugins
3. [Preview as you write](https://squidfunk.github.io/mkdocs-material/creating-your-site/#previewing-as-you-write)

# Open Banking

We are working on developing this content under [issue 2548](https://github.com/JanssenProject/jans/issues/2548). If you’d like to contribute the content, get started with the [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/#contributing-to-the-documentation) 
## How to test OpenBanking?

This test uses a Gluu Testing Certificate.

### device authentication

		After installation, we have to complete device authentication to use OpenBanking.

### Testing using IM mode
	launch jans-cli using the below command.

```
/opt/jans/jans-cli/config-cli.py -CC /opt/jans/jans-setup/output/CA/client.crt -CK /opt/jans/jans-setup/output/CA/client.key
```

		further testing is same as jans server

###  Testing using commnd line mode

We can run the below command on the command line..
for ex:
```
/opt/jans/jans-cli/config-cli.py -CC /opt/jans/jans-setup/output/CA/client.crt -CK /opt/jans/jans-setup/output/CA/client.key –operation-id get-oauth-openid-clients
```

in the same way we can run other commands.

		rest is same for jans and openbanking
