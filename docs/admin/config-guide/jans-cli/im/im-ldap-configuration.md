---
tags:
  - administration
  - configuration
  - cli
  - interactive
---

# LDAP Configuration

!!! Important
    The interactive mode of the CLI will be deprecated upon the full release of the Configuration TUI in the coming months.

> Prerequisite: Know how to use the Janssen CLI in [interactive mode](im-index.md)

Using Janssen CLI, the Following list of actions can be performed in LDAP.
```text
Database - LDAP configuration
-----------------------------
1 Gets list of existing LDAP configurations
2 Adds a new LDAP configuration
3 Updates LDAP configuration
4 Gets an LDAP configuration by name
5 Deletes an LDAP configuration
6 Partially modify an LDAP configuration
7 Tests an LDAP configuration
```

## Gets List of Existing LDAP Configuration

To get a list of existing LDAP configurations, select option 1 and press enter, you will get a list of existing LDAP configurations in your Janssen server.

```text
Gets list of existing LDAP configurations
-----------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/database/ldap.readonly

[
  {
    "configId": "auth_ldap_server",
    "bindDN": "cn=directory manager",
    "bindPassword": "gD63aUTvvS4=",
    "servers": [
      "localhost:1636"
    ],
    "maxConnections": 1000,
    "useSSL": true,
    "baseDNs": [
      "ou=people,o=jans"
    ],
    "primaryKey": "uid",
    "localPrimaryKey": "uid",
    "useAnonymousBind": false,
    "enabled": false,
    "version": 0,
    "level": 0
  }
]
```
## Adding new LDAP Configuration

To add a new LDAP configuration, choose option 2 and add the following properties:
```json5
{
  "configId":,
  "bindDN": ,
  "bindPassword":,
  "servers": [],
  "maxConnections": 2,
  "useSSL": false,
  "baseDNs": [],
  "primaryKey":,
  "localPrimaryKey":,
  "useAnonymousBind": false,
  "enabled": false,
  "version": null,
  "level": null
}
```
Then enter `y` to confirm.

## Update an Existing LDAP configuration

To update an existing LDAP configuration, select option 3 and enter the LDAP configuration name. If it matches to the existing configuration then It will ask to enter a value for each properties.

## Delete an Existing LDAP configuration

To delete an existing ldap configuration, enter a name of an existing ldap configuration and enter `yes/y` to confirm.


