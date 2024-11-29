---
tags:
- Casa
- Introduction
---

# Jans Casa Documentation

## Overview

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
- Gluu's FIDO push-notification mobile app, [Super Gluu](https://docs.gluu.org/head/supergluu/)    
- OTP hardware cards like [these](https://www.ftsafe.com/Products/Power_Card/Standard) or dongles like [these](https://www.ftsafe.com/Products/OTP/Single_Button_OTP)      
- OTP mobile apps like Google Authenticator, FreeOTP, etc.       
- Mobile phone numbers able to receive OTPs via SMS   
- Passwords      

Additional authenticators and use cases can be supported via [custom plugins](#existing-plugins).

## 2FA enrollment APIs

To facilitate 2FA device enrollment during account registration, or elsewhere in an application ecosystem, Casa exposes [APIs](https://github.com/JanssenProject/jans/raw/vreplace-janssen-version/jans-casa/app/src/main/webapp/enrollment-api.yaml)  for enrolling the following types of authenticators:   

- Phone numbers for SMS OTP
- OTP apps, cards, or dongles  
- FIDO security keys

## Configuration via APIs

Besides a comprehensive graphical [admin console](./administration/admin-console.md), application settings can also be manipulated by means of a configuration [API](https://github.com/JanssenProject/jans/raw/vreplace-janssen-version/jans-casa/app/src/main/webapp/admin-api.yaml).

## Existing plugins

Casa is a plugin-oriented, Java web application. Existing functionality can be extended and new functionality and APIs can be introduced through plugins. Currently, there are plugins available for the following:

- [2FA settings](./plugins/2fa-settings.md)
- [Accounts linking](./plugins/accts-linking/account-linking-index.md)
- [Consent management](./plugins/consent-management.md)
- [Custom branding](./plugins/custom-branding.md)
- [BioID](./plugins/bioid.md)
- [Email OTP](./plugins/email-otp.md)

If you are interested in onboarding additional authentication methods to Casa, read this [guide](./developer/add-authn-methods.md).

## User roles

There are two types of users in Jans Casa:

- **Regular users**: Any user in the Janssen Server  

- **Admin users**: Users having the `CasaAdmin` role 

Admin users have access to the Casa [admin console](./administration/admin-console.md). All users can manage their 2FA credentials, as outlined in the [user guide](./user-guide.md). 

A user can be "turned" into an administrator by editing his profile - in [TUI](../janssen-server/config-guide/config-tools/jans-tui/README.md) for instance - ensuring `CasaAdmin` is part of his `role` attribute.

## Get started

Use the following links to get started with Casa:  

### Admin Guide

  - [Quick start](./administration/quick-start.md)
  - [Admin console](./administration/admin-console.md)     
  - [Custom branding](./administration/custom-branding.md)        
  - [FAQs](./administration/faq.md)            

### User Guide

- [Home](./user-guide.md)

### Developer guide

- [Home](./developer/overview.md)
- [Adding authentication methods](./developer/add-authn-methods.md)
