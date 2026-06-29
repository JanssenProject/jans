# Jans Casa Documentation

Jans Casa ("Casa") is a self-service web portal for end-users to manage authentication and authorization preferences for their account in a Janssen Server.

For example, as people interact with an organization's digital services, they may need to:

- Enroll, delete and manage two-factor authentication (2FA) credentials for their account (e.g. FIDO security keys, mobile apps, phone numbers, etc.)
- Turn 2FA on and off
- View and manage which external apps have been authorized to access what personal data
- View trusted devices

Casa provides a platform for people to perform these account security functions and more in a friendly, straightforward manner.

## Two-factor authentication

The core use case for Casa is self-service 2FA. If people need to call the helpdesk every time they get a new phone or security key, supporting strong authentication becomes prohibitively expensive.

Out-of-the-box, Casa can be used to enroll and manage the following authenticators:

- FIDO2 security keys like [Yubikeys](https://www.yubico.com/products/)
- OTP hardware cards like [these](https://www.ftsafe.com/Products/Power_Card/Standard) or dongles like [these](https://www.ftsafe.com/Products/OTP/Single_Button_OTP)
- OTP mobile apps like Google Authenticator, FreeOTP, etc.
- Mobile phone numbers able to receive OTPs via SMS
- Passwords

Additional authenticators and use cases can be supported via [custom plugins](#existing-plugins).

## 2FA enrollment APIs

To facilitate 2FA device enrollment during account registration, or elsewhere in an application ecosystem, Casa exposes [APIs](https://github.com/JanssenProject/jans/raw/vreplace-janssen-version/jans-casa/app/src/main/webapp/enrollment-api.yaml) for enrolling the following types of authenticators:

- Phone numbers for SMS OTP
- OTP apps, cards, or dongles
- FIDO security keys

## Configuration via APIs

Besides a comprehensive graphical [admin console](https://docs.jans.io/head/casa/administration/admin-console/index.md), application settings can also be manipulated by means of a configuration [API](https://github.com/JanssenProject/jans/raw/vreplace-janssen-version/jans-casa/app/src/main/webapp/admin-api.yaml).

## Existing plugins

Casa is a plugin-oriented, Java web application. Existing functionality can be extended and new functionality and APIs can be introduced through plugins. Currently, there are plugins available for the following:

- [2FA settings](https://docs.jans.io/head/casa/plugins/2fa-settings/index.md)
- [Accounts linking](https://docs.jans.io/head/casa/plugins/accts-linking/account-linking-index/index.md)
- [Consent management](https://docs.jans.io/head/casa/plugins/consent-management/index.md)
- [Custom branding](https://docs.jans.io/head/casa/plugins/custom-branding/index.md)
- [BioID](https://docs.jans.io/head/casa/plugins/bioid/index.md)
- [Email OTP](https://docs.jans.io/head/casa/plugins/email-otp/index.md)
- [Certificate authentication](https://docs.jans.io/head/casa/plugins/cert-authn/index.md)

If you are interested in onboarding additional authentication methods to Casa, read this [guide](https://docs.jans.io/head/casa/developer/add-authn-methods/index.md).

## User roles

There are two types of users in Jans Casa:

- **Regular users**: Any user in the Janssen Server
- **Admin users**: Users having the `CasaAdmin` role

Admin users have access to the Casa [admin console](https://docs.jans.io/head/casa/administration/admin-console/index.md). All users can manage their 2FA credentials, as outlined in the [user guide](https://docs.jans.io/head/casa/user-guide/index.md).

A user can be "turned" into an administrator by editing his profile - in [TUI](https://docs.jans.io/head/janssen-server/config-guide/config-tools/jans-tui/index.md) for instance - ensuring `CasaAdmin` is part of his `role` attribute.

## Get started

Use the following links to get started with Casa:

### Admin Guide

- [Quick start](https://docs.jans.io/head/casa/administration/quick-start/index.md)
- [Admin console](https://docs.jans.io/head/casa/administration/admin-console/index.md)
- [Custom branding](https://docs.jans.io/head/casa/administration/custom-branding/index.md)
- [FAQs](https://docs.jans.io/head/casa/administration/faq/index.md)

### User Guide

- [Home](https://docs.jans.io/head/casa/user-guide/index.md)

### Developer guide

- [Home](https://docs.jans.io/head/casa/developer/overview/index.md)
- [Adding authentication methods](https://docs.jans.io/head/casa/developer/add-authn-methods/index.md)
