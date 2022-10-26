# Fido2

[FIDO 2.0 (FIDO2)](https://fidoalliance.org/fido2/) is an open authentication 
standard that enables leveraging common devices to authenticate to online services 
in both mobile and desktop environments.

FIDO2 comprises the [W3C’s Web Authentication specification (WebAuthn)](https://www.w3.org/TR/webauthn/) 
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
