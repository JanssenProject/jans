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

Audit events: 
- CLIENT_REGISTRATION
- CLIENT_UPDATE
- CLIENT_READ
- CLIENT_DELETE
- USER_AUTHORIZATION
- BACKCHANNEL_AUTHENTICATION
- BACKCHANNEL_DEVICE_REGISTRATION
- USER_INFO
- TOKEN_REQUEST
- TOKEN_VALIDATE
- TOKEN_REVOCATION
- SESSION_UNAUTHENTICATED
- SESSION_AUTHENTICATED
- SESSION_DESTROYED
- DEVICE_CODE_AUTHORIZATION
- SSA_CREATE
- SSA_READ 


# Enable/Disable Audit Logs on Jans TUI
Using Jans TUI we can easily `Enable` or `Disable` Audit Logs. Go to `jans tui` select `Auth Server` tab then `Properties` tab. Now set the value of  `enabledOAuthAuditLogging` pressing Enter button on your keyboard. Save it and let's see below image 

![auditLogs](https://github.com/JanssenProject/jans/assets/43112579/2bf87258-083c-47f5-bce5-13285582ec4b)

## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).