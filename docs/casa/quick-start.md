---
tags:
- Casa
- quick start
---

# Jans Casa Quick Start Guide

Jans Casa is a self-service web portal for end-users to manage security preferences for their accounts. Jans Casa can
be used to with [Janssen Server](https://jans.io) or [Gluu Flex Server](https://gluu.org).

Use this guide to install and configure a deployment of Casa.

## Installation

Follow the Jans Casa [installation guide](./administration/README.md) to install Gluu Casa.

## Configuration

Configuring Casa for usage requires you to enable interception scripts in the Janssen Server, activate the 
authentication methods in Casa, and install and configure the 2FA settings plugin. 

- Enable authentication interception scripts using [TUI](../admin/config-guide/config-tools/jans-tui/README.md) 
- Activate authentication methods in Casa Once the interception scripts have been enabled. To do this 
Log in to Casa as an administrator and [enable the desired methods](./administration/admin-console.md#configure-casa).

- Setup 2FA preferences using the [2FA Settings plugin](./plugins/2fa-settings.md) to set the 
[minimum number of credentials](./administration/admin-console.md#2fa-settings) a user must enroll among others.

### Test enrollment and 2FA

- [Enroll](./user-guide.md#2fa-credential-details--enrollment) at least two credentials on a non-administrator user.

- [Turn on](./user-guide.md#turn-2fa-onoff) 2FA for the account.

- Test 2FA Authentication by logging off and logging back in. Application access should now require a second 
authentication factor.

### Finish configuration

Once satisfied with testing, configure default authentication method of 
Janssen Server using [TUI](../admin/config-guide/config-tools/jans-tui/README.md) 
 to log in users via Casa for all applications the server protects.

### Check out available plugins

Browse our [catalog of plugins](./plugins) to add features and expand Casa!.
