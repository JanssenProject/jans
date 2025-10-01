---
tags:
- Casa
- 2FA
- two-factor
---

# About Two-Factor Authentication (2FA)

!!! Note
    You can tailor different aspects of 2FA behavior in Casa using the [plugin](../plugins/2fa-settings.md) developed 
    for this purpose.

## Availability

By default, strong (two-factor) authentication will be available to users of Casa once they have added at least two 
credentials. By requiring users to register two strong credentials, the number of account lockouts that require admin 
intervention will be greatly reduced. If one credential is lost, there will be at least one fallback mechanism 
available. Avoiding user lockout is important because it prevents a serious burden for IT administrators.

There is no limit to the number of credentials a user can enroll, and credentials do not need to be of the same type: 
any combination is valid. 

## Supported types of 2FA

Users will only be able to add credentials with a type matching one of the already enabled authentication methods in 
the [admin console](./admin-console.md#authentication-methods). Other methods may be supported via [plugins](../index.md#existing-plugins).

## Resetting a user's 2FA availability

In the event a user loses access to his account, admins can revert the user's authentication method to "password only" 
by following the steps shown in the [troubleshooting guide](./faq.md).

## Associated "strength" of credentials

When authenticating, a user with 2FA turned on, will be challenged to present the credential having the "strongest" 
authentication method (from his already available enrolled credentials). The relative strength of methods can be assigned in the [authentication methods](./admin-console.md#authentication-methods) screen of the admin console.

## Forcing users to enroll a specific credential before 2FA is available

To further reduce the likelihood of lockouts, you can force users to initially enroll, for instance, one OTP 
credential before any other. OTP credentials are generally more accessible than their counterparts (like Fido) since 
they normally don't demand special conditions from the device used to access, like having a USB port.

To do so, add a property named `2fa_requisite` to the configuration of the Agama flow that backs the given authentication method, and assign `true` as its value. You can do this via [TUI](../../janssen-server/config-guide/auth-server-config/agama-project-configuration.md#agama-project-configuration-screen). Note
this mechanism is applicable for the out-of-the box authentication methods supported by Casa. For other methods contributed via plugins, consult their respective documentation.

You can flag more than one method as requisite. In this case users will be encouraged to enroll one credential 
associated to any of the flagged methods.

If a user attempts to delete their only available credential matching the requisite method, a prompt will appear 
warning that doing so will disable 2FA, that is, resetting to password authentication.

If you are a developer coding a plugin that adds an authentication method, you can make the method a requisite by properly implementing method `mayBe2faActivationRequisite` part of interface `AuthnMethod`.

## Enrolling credentials upon registration or first login

If the previous scenario is not enough for your needs, you can force users to enroll credentials and turn 2FA on 
before the first usage of the application. This can be done in two ways:

- Creating a new Agama project that takes charge of credentials enrollment and then reuses the flows bundled with the standard Agama project which performs the actual authentication. Learn more about this in the developer pages

- Making enrollments occur at registration time (through the application you use for this purpose)
