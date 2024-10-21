---
tags:
  - administration
  - auth-server
  - logging
---

# Standard Logs

All `jans-auth-server` log files are located in `/opt/jans/jetty/jans-auth/logs/`.

1. `jans-auth.log` - main AS log file. All logs during execution goes here.
1. `jans-auth_script.log`: - contains [custom interception scripts](../../developer/interception-scripts.md) related log messages. Use `scriptLogger` inside custom script to log messages to this log file.
1. `jans-auth_persistence.log` - log which contains entries from AS persistance layer 
1. `jans-auth_persistence_duration.log` - log which contains duration of query execution. It can be useful during performance issues investigation 
1. `app_persistence_orm_statistics.log` - contains statistics about persistence layer activities
1. `jans-auth_audit.log` - contains audit log information (audit is disabled by default and can be enabled via `enabledOAuthAuditLogging` AS configuration property) 
1. `http_request_response.log` - contains http request/response data. It can be enabled via `httpLoggingEnabled` AS configuration property.

## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).