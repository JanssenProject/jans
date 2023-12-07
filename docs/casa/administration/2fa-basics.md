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

!!! Note
    The number of credentials required for two-factor authentication (2FA) can be customized with 
    the [Strong authentication settings plugin](../plugins/2fa-settings.md)

## Supported types of 2FA

Users will only be able to add credentials with a type matching one of the already enabled authentication methods in 
the admin console. See the [Admin console page](./admin-console.md#enabled-methods) to learn more. Out of the box, 
all the following authentication methods are supported:

- FIDO 2 security keys
- [Super Gluu](https://super.gluu.org/) for push notifications 
- HOTP/TOTP apps, cards, "dongles"
- OTP via SMS (using Twilio or an SMPP server)

Other methods may be supported via [plugins](../index.md#existing-plugins).

## Resetting a user's 2FA availability

In the event a user loses access to his account, admins can revert the user's authentication method to "password only" 
by following the steps shown in the [troubleshooting guide](./faq.md).

## Associated "strength" of credentials

When authenticating, a user with 2FA turned on, will be challenged to present the credential matching the "strongest" 
authentication method. The strength is a numerical value assigned via the "level" property of the custom script that 
is tied to the authentication method. The higher the value, the stronger it is considered. Thus, if a user has several 
credentials enrolled, he will be asked to present the one of them having the highest strength associated. 

Particularly, if the device used to access is a mobile browser, only the methods listed in the property 
"mobile_methods" of casa script will be accounted to determine the strongest credential. Admins can modify this 
property at will if the default value does not meet their expectations.

Note there are ways to override the rule of the "strongest" method; see the docs of 
the [Strong authentication settings plugin](../plugins/2fa-settings.md).

## Forcing users to enroll a specific credential before 2FA is available

To further reduce the likelihood of lockouts, you can force users to initially enroll, for instance, one OTP 
credential before any other. OTP credentials are generally more accessible than their counterparts (like Fido 2) since 
they normally don't demand special conditions from the device used to access, like having a USB port.

To do so, just add a new configuration property named `2fa_requisite` to the custom interception script corresponding 
to the authentication method, and assign `true` as its value. It may take more than one minute for Casa to pick up the 
changes. To add the property, open oxTrust web console and navigate to `Configuration` > `Manage custom scripts`, 
collapse the method you want to set as requisite for 2FA, and click on `Add new property`.

You can flag more than one method as requisite. In this case users will be encouraged to enroll one credential 
associated to any of the flagged methods.

If you are using an authentication method you added your own (via plugin), ensure the corresponding plugin 
implements the `mayBe2faActivationRequisite` method.

If a user attempts to delete their only available credential matching the requisite method, a prompt will appear 
warning that doing so will disable 2FA, that is, resetting to password authentication.

## Enrolling credentials upon registration or first login

If the previous scenario is not enough for your needs, you can force users to enroll credentials and turn 2FA on 
before the first usage of the application. This can be done in two ways:

- Altering the login flow to check for presence of credentials and then redirect to custom pages that implement enrollment
- Making enrollments occur at registration time (through the application you use for this purpose)
