[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-fido2&metric=bugs)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-fido2)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-fido2&metric=code_smells)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-fido2)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-fido2&metric=coverage)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-fido2)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-fido2&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-fido2)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-fido2&metric=ncloc)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-fido2)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-fido2&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-fido2)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-fido2&metric=alert_status)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-fido2)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-fido2&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-fido2)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-fido2&metric=security_rating)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-fido2)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-fido2&metric=sqale_index)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-fido2)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-fido2&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-fido2)

# Fido2

[FIDO 2.0 (FIDO2)](https://fidoalliance.org/fido2/) is an open authentication 
standard that enables leveraging common devices to authenticate to online services 
in both mobile and desktop environments.

FIDO2 is comprised of the [W3C’s Web Authentication specification (WebAuthn)](https://www.w3.org/TR/webauthn/) 
and FIDO’s corresponding [Client-to-Authenticator Protocol (CTAP)](https://fidoalliance.org/specs/fido-v2.0-ps-20170927/fido-client-to-authenticator-protocol-v2.0-ps-20170927.html). WebAuthn defines a standard web API 
that can be built into browsers and related web platform infrastructure to enable 
online services to use FIDO Authentication. CTAP enables external devices such as 
mobile handsets or FIDO Security Keys to work with WebAuthn and serve as 
authenticators to desktop applications and web services.

Janssen includes a FIDO2 component to implement a two-step, two-factor 
authentication (2FA) with username / password as the first step, and any FIDO2 
device as the second step. 

During Janssen installation, the administrator will have the option to also install 
the FIDO2 component. 

