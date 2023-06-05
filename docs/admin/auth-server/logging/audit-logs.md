---
tags:
  - administration
  - auth-server
  - logging
---

# Audit Logs

Audit logs are located in `jans-auth_audit.log` log file.
All `jans-auth-server` log files are located in `/opt/jans/jetty/jans-auth/logs/`.
 
Audit is disabled by default on AS and can be enabled via `enabledOAuthAuditLogging` AS configuration property.

Audit logs are logged to file when enabled, however if JMS configuration is specified it will be logged to JMS as well.

JMS Configuration must be set inside AS global configuration

| Name            | Description         |  
|---------------- |---------------------|  
|jmsBrokerURISet  | JMS Broker URI Set  |
|jmsUserName      | JMS UserName        |  
|jmsPassword      | JMS Password.       |  

## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussion) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).