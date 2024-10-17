---
tags:
- Casa
- quick start
---

# Jans Casa Quick Start Guide

!!! Note
    This document is intended for administrators only. Learn [here](../index.md#user-roles) how to "grant" administrative privileges for Casa.

Use this guide to install and configure your Casa deployment.

## Installation

Jans Casa can be used with [Janssen Server](https://jans.io) or [Gluu Flex Server](https://gluu.org). At [installation](https://docs.jans.io/vreplace-janssen-version/admin/install/) time (applies to any of these two products), you will be prompted if you desire to include Casa. If you want to add Casa post-installation, you will simply have to re-run the installer and ensure to select Casa.

## Configuration

### Enable authentication methods

The "out-of-the-box" login experience in Casa consists of the usual username and password prompt. To start leveraging a stronger authentication login to Casa with an administrative account (visit `https://<your-server-name>/jans-casa`) and activate the [methods](./admin-console.md#authentication-methods) you want to offer Casa users.

**Important notes**:

- Usage of OTP via SMS requires the setup of a [Twilio](https://twilio.com) account and populating configuration properties of 
flow `io.jans.casa.authn.twilio_sms` found in Casa Agama project. You can do the latter via [TUI](../../janssen-server/config-guide/auth-server-config/agama-project-configuration.md#agama-project-configuration-screen). We encourage you to use the online Twilio testing tools beforehand to ensure you can send SMS to the countries you are targetting

- Usage of Super Gluu has some preliminar requisites described [here](https://docs.gluu.org/head/supergluu/admin-guide/)

### Add the strong authentication settings plugin

This step is optional. Check [this](../plugins/2fa-settings.md) page for more information. Use this plugin if you need to exercise an advanced control on how 2FA behaves in your Casa deployment.
 
### Test enrollment and 2FA

Do the following steps using a testing account with no administrative privileges:

- Login to Casa. Only username and password should be prompted
- Use the menu on the left to access the screens from which enrollment of credentials can be performed
- Ensure at least two credentials have been added. In the home page, [turn on](../user-guide.md#turn-2fa-onoff) 2FA 
- Log off and log back in. From now on, besides username and password, one credential has to be presented to get access

Click [here](./2fa-basics.md) to learn more about 2FA in Casa.

## Finish configuration

Once you are done with testing, you may use casa as the default authentication method of 
Janssen Server using [TUI](../../janssen-server/config-guide/config-tools/jans-tui/README.md) 
 to log in users via Casa for all applications the server protects.
 
Finally, as a security measure you can thoroughly disable access to the administrative console of Casa by following the steps below:

1. Connect to your server
1. Navigate to `/opt/jans/jetty/jans-casa`
1. remove file `.administrable` (ie. `rm .administrable`)

If you want to make the admin console available again you need to recreate the marker file:

1. Create an empty file (eg. `touch .administrable`)
1. Run `chown casa:casa .administrable` (do this only if you are on FIPS environment)
1. Logout in case you have an open browser session, and login again

## Check out available plugins

Browse our [catalog of plugins](../index.md#existing-plugins) to add features and expand Casa!.
