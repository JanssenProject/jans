---
tags:
  - administration
  - auth-server
  - logging
---

# Logging Overview

All `jans-auth-server` log files are located in `/opt/jans/jetty/jans-auth/logs/`.

Use [log levels](log-levels.md) to control noise of logs in [standard logs](standard-logs.md).

Following AS configuration properties can be used to customize AS logging: 

- `loggingLevel` - Specify the [log levels](log-levels.md) of loggers
- `loggingLayout` - Logging layout used for Jans Authorization Server loggers
- `httpLoggingEnabled` - Enable/disable request/response logging filter. Disabled by default.
- `disableJdkLogger` - Choose whether to disable JDK loggers
- `enabledOAuthAuditLogging` - enable OAuth Audit Logging
- `externalLoggerConfiguration` - The path to the external log4j2 logging configuration
- `httpLoggingExcludePaths` - This list details the base URIs for which the request/response logging filter will not record activity

AS has pre-defined set of [standard logs](standard-logs.md) which can be overwritten by own [log4j2 xml](custom-logs.md).

AS under the hood is using `log4j2` and `slf4j` thus please reference [log4j configuration](https://logging.apache.org/log4j/2.x/manual/configuration.html) for available logging options.

AS support [audit logs](audit-logs.md).
 

## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).