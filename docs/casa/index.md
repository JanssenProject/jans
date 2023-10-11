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

Casa provides a platform for people to perform these account security functions and more. 

## Two-factor authentication

The core use case for Casa is self-service 2FA. If people need to call the helpdesk every time they get a new phone or security key, supporting strong authentication becomes prohibitively expensive. 

Out-of-the-box, Casa can be used to enroll and manage the following authenticators:    

- FIDO2 security keys like [Yubikeys](https://www.yubico.com/products/yubikey-hardware/)       
- Gluu's FIDO push-notification mobile app, [Super Gluu](https://super.gluu.org)    
- OTP hardware cards like [these](https://www.ftsafe.com/Products/Power_Card/Standard) or dongles like [these](https://www.ftsafe.com/Products/OTP/Single_Button_OTP)      
- OTP mobile apps like Google Authenticator, FreeOTP, etc.       
- Mobile phone numbers able to receive OTPs via SMS   
- Passwords (if stored in the corresponding Janssen Server's local database, i.e. not a backend LDAP like AD)      

Additional authenticators and use cases can be supported via [custom plugins](#plugin-oriented). 

## 2FA enrollment APIs

To facilitate 2FA device enrollment during account registration, or elsewhere in an application ecosystem, Casa exposes APIs for enrolling the following types of authenticators:   

- Phone numbers for SMS OTP   
- OTP apps, cards or dongles        
- [Super Gluu](https://super.gluu.org) Android and iOS devices  
- FIDO2 security keys

## Configuration via APIs

Besides a comprehensive graphical admin console, application settings can also be manipulated by means of a configuration API.

## Plugin oriented

Casa is a plugin-oriented, Java web application. Existing functionality can be extended and new functionality and APIs can be introduced through plugins. 

## Existing plugins

Gluu has written a number of plugins to extend Casa, including plugins for:

- [Consent management](./plugins/consent-management.md) 
- [Custom branding](./plugins/custom-branding.md)  
- [2FA settings](./plugins/2fa-settings.md)
- [BioID authentication](./plugins/bioid.md)

## Janssen Server integration

Janssen Server relies on "interception scripts" to implement user authentication. Casa itself has an interception script which defines authentication logic and routes authentications to specific 2FA mechanisms which also have their own scripts. All scripts must be enabled in the Janssen Server.        

## User roles

There are two types of users in Jans Casa:

- **Admin users**: Janssen Server admin user

- **Regular users**: Any user in the Janssen Server  

Admin users have access to the Casa [admin console](./administration/admin-console.md). All users can manage their 2FA credentials, as outlined in the [user guide](./user-guide.md).  

## Get started

Use the following links to get started with Casa:  

### Admin Guide

  - [Administration](./administration/README.md)
  - [Admin console](./administration/admin-console.md)
  - [Credentials storage](./administration/credentials-stored.md)        
  - [Custom branding](./administration/custom-branding.md)        
  - [FAQs](./administration/faq.md)            

### User Guide

- [Home](./user-guide.md)


