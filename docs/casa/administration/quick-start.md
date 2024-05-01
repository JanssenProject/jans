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

Jans Casa can be used with [Janssen Server](https://jans.io) or [Gluu Flex Server](https://gluu.org). At [installation](https://docs.jans.io/head/admin/install/) time (applies to any of these two products), you will be prompted if you desire to include Casa. If you want to add Casa post-installation, you will simply have to re-run the installer and ensure to select Casa.

## Configuration

The "out-of-the-box" login experience in Casa consists of the usual username and password prompt. To start leveraging a stronger authentication, consider the authentication methods you would like to support, for instance:

- One Time Passcodes (OTP) sent via SMS
- One Time Passcodes (OTP) obtained through physical OTP devices or apps  
- FIDO devices
- [Super Gluu](https://docs.gluu.org/head/supergluu/) mobile notifications

You can choose one or more of these, or create plugins to support more authentication mechanisms. 

### Enable scripts

First, the custom authentication scripts corresponding to the authentication methods of your preference must be enabled in the server. For this purpose you can use [TUI](../../admin/config-guide/config-tools/jans-tui/README.md) or the [Admin UI](https://docs.gluu.org/head/admin/admin-ui/introduction/) if you have Gluu Flex Server installed.

Scripts can be more easily looked up by display name. The below table shows the display names of the scripts previously listed:

|Script|Display name|
|-|-|
|OTP (SMS)|twilio_sms|
|OTP (apps or hardware)|otp|
|FIDO devices|fido2|
|Super gluu|super_gluu|

As an example, connect to your server and run `python3 /opt/jans/jans-cli/jans_cli_tui.py`), follow the prompts, and navigate to the _scripts_ tab. There, search for the  script matching the type of 2FA credential you want to support, e.g. `fido2`, `otp`, etc. Press enter to open the script details page. Select the "Enabled" field and press space (an asterisk will be displayed). Finally, press the "save" button.
 
**Important notes**:

- Usage of OTP via SMS requires the setup of a [Twilio](https://twilio.com) account and extra configuration of the custom script. Check [this](https://github.com/JanssenProject/jans/blob/vreplace-janssen-version/docs/script-catalog/person_authentication/twilio-2fa/README.md) document for reference
- Usage of Super Gluu has some [preliminar requisites](https://docs.gluu.org/head/supergluu/admin-guide/) and requires extra configuration of the custom script 

### Enable methods in Casa

Once scripts are enabled and configured, login to Casa as administrator (visit `https://<your-server-name>/jans-casa`) and [enable the methods](./admin-console.md#enabled-authentication-methods) you want to offer Casa users.

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
Janssen Server using [TUI](../../admin/config-guide/config-tools/jans-tui/README.md) 
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
