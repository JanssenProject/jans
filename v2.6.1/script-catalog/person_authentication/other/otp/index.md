# One-Time Password (OTP) Authentication

Gluu's OTP interception script uses the two-factor event/counter-based HOTP algorithm [RFC4226](https://tools.ietf.org/html/rfc4226) and the time-based TOTP algorithm [RFC6238](https://tools.ietf.org/html/rfc6238).

In order to use this authentication mechanism users will need to install a mobile authenticator, like [Google Authenticator 2](https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2), that supports HOTP/TOTP.

# External Jars

This script uses the [otp-java](https://github.com/BastiaanJansen/otp-java)
library (RFC 4226 HOTP + RFC 6238 TOTP), available on Maven Central:

    <groupId>com.github.bastiaanjansen</groupId>
    <artifactId>otp-java</artifactId>
    <version>2.1.0</version>
