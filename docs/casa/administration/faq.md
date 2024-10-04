---
tags:
- Casa
- administration
- faq
---


# Frequently Asked Questions

## Common administrative tasks

### Where are the logs?

The application logs are located at `/opt/jans/jetty/jans-casa/logs`. By default, Casa uses the INFO level for messages. You can change the log level at will using the app's admin UI.

### How do I custom brand Casa?

We have a dedicated page covering the topic of custom branding [here](./custom-branding.md).

### What ports are used by the application?

Casa uses port `8080`. One way to see if the app is up and running is by checking whether this port is open by issuing a command like `netstat -nltp`.

### How to turn 2FA off for a user?

If a user has been locked out for any reason (e.g. lost devices), you can reset his "authentication method" to password by accessing the admin console and choosing the menu item labelled "Reset users preference". Type the username (or part of) in the text field and then press search. Once you locate the user in the result grid, click the corresponding row and then hit "Change to password". The row will become disabled, and you'll see a success message.

If you've followed the steps as described above, next time he attempts to log in, he won't be asked to present any credentials other than password to enter.

### How to adjust the issuer for OTP tokens

When people add OTP mobile apps, the enrollment appears in the device associated with an "issuer", so it is easy to recognize where the OTPs generated can be used. To keep track of which OTPs are valid for which IDPs, the issuer property can be adjusted in flow `io.jans.casa.authn.otp` part of Casa Agama project - you can use [TUI](../../janssen-server/config-guide/auth-server-config/agama-project-configuration.md#agama-project-configuration-screen) for this purpose. For example, you might want to set the `issuer` property to `ACME Dev` on your dev server, and `ACME, Inc.` on your production server. 

## Errors shown in the UI

### A page with a "Service Temporarily Unavailable" message appears when accessing the application
This is the 503 HTTP error. There is an Apache server in front of the application and this means the reverse proxy couldn't establish a communication internally with the app. This usually happens when Casa hasn't started completely, so it's usually a matter of waiting a few seconds.

### An "Unauthorized access" error is shown when accessing the application

This is caused by an unauthorized access attempt (e.g. users requesting URLs without ever logging in or after session has expired).

### "An error occurred: Casa did not start properly" is shown when accessing the application

This occurs whenever the application failed to start successfully. Check the log to diagnose the problem. Try to find a message like "WEBAPP INITIALIZATION FAILED" and see the traces above it. Often, error messages are self-explanatory.

Once fixed, please restart the application. You will have to see a "WEBAPP INITIALIZED SUCCESSFULLY" message to know that it's working.

## Miscellanenous

### Admin console is not shown 

If you have logged in using an administrative account and cannot find any admin features in the UI ensure you have gone through these [steps](./quick-start.md#finish-configuration).

### A previously enabled method is not available anymore

Check if the plugin that contributed the given authentication method was removed. The out-of-the-box methods will always appear listed.

### What kind of TOTP/HOTP devices are supported?

Both soft (mobile apps) or hard tokens (keyfobs, cards, etc.) are supported. Supported algorithms are `HmacSHA1`, `HmacSHA256`, and `HmacSHA512`.

### Authentication fails when using TOTP or HOTP with no apparent reason

For Time-based OTP, ensure the time of your server is correctly synchronized (use NTP for instance). The time lag of the authentication device used (for instance, a mobile phone) with respect to server time should not be representative. 

Big time differences can cause unsuccessful attempts to enroll TOTP credentials in Casa.

For Event-based OTP (HOTP), ensure you are using a suitable value for `look ahead window` (we suggest at least 10). Check the configuration of flow `io.jans.casa.authn.otp` part of Casa Agama project - you can use [TUI](../../janssen-server/config-guide/auth-server-config/agama-project-configuration.md#agama-project-configuration-screen) for this purpose.

### The user interface is not showing any means to enroll credentials

In the administration console, ensure one or more authentication methods have been enabled.

### A user cannot turn 2FA on

To turn 2FA on, the user has to have enrolled at least a certain number of credentials through the app. Only after this is met, he will be able to perform this action. 

In the administration console you can specify the minimum number of enrolled credentials needed to enable second factor authentication for users. Please check the [2FA Settings plugin](../plugins/2fa-settings.md) for more details.

## My problem is not listed here

Feel free to [ask Janssen Server community](https://github.com/JanssenProject/jans/discussions).
