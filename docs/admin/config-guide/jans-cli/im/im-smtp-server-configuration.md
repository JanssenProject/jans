---
tags:
  - administration
  - configuration
  - cli
  - interactive
---

# SMTP Configuration

!!! Important
    The interactive mode of the CLI will be deprecated upon the full release of the Configuration TUI in the coming months.

> Prerequisite: Know how to use the Janssen CLI in [interactive mode](im-index.md)

Janssen CLI also supports SMTP configuration. You can do the following things as stated below:
- `View/Get`
- `Add/Delete`
- `Update`
- `Test`

Simply select option '10' from Main Menu, It will show some options as below:
```text
Configuration – SMTP
--------------------
1 Returns SMTP server configuration
2 Adds SMTP server configuration
3 Updates SMTP server configuration
4 Deletes SMTP server configuration
5 Test SMTP server configuration
```
Just go with the option and perform operation.

## Get Current SMTP Server Configuration

To view the current SMTP server configuration on your Janssen server, please select option 1, it will return as below:

```text
Returns SMTP server configuration
---------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/smtp.readonly

{
  "host": null,
  "port": 0,
  "requiresSsl": null,
  "serverTrust": null,
  "fromName": null,
  "fromEmailAddress": null,
  "requiresAuthentication": null,
  "userName": null,
  "password": null
}
```
## Setup new SMTP server

To add a smtp server, chose option 2 from SMTP Configuration Menu. It will ask few things to fill each property.

- host
- port
- requiresSsl[true, false]
- serverTrust[true, false]
- fromName
- fromEmailAddress
- requireAuthentication [true, false]
- username
- password

```text
Obtained Data:

{
  "host": null,
  "port": null,
  "requiresSsl": false,
  "serverTrust": false,
  "fromName": null,
  "fromEmailAddress": null,
  "requiresAuthentication": false,
  "userName": null,
  "password": null
}

Continue? 
```


## Test SMTP Configuration

If the server is running, and all the information you have entered is correct. You can test SMTP server from the following option 5, it will respond if the server is configured properly.

