# Fido2

[FIDO 2.0 (FIDO2)](https://fidoalliance.org/fido2/) is an open authentication 
standard that enables leveraging common devices to authenticate to online services 
in both mobile and desktop environments.

Janssen includes a FIDO2 component (or FIDO server) to provide the following infrastructure:
- **Secure authentication** using public-key cryptography, eliminating the need for passwords.
- **Cross-platform authentication** across different devices and platforms that support the FIDO standard.
- Management of the **registration process of passkeys**, where public keys are registered and stored.
- Management of the **authentication process of passkeys**, verifying user identity through challenges and responses.
- **Trusted repository for public keys** used in the authentication process, ensuring secure, private, and passwordless authentication.

During Janssen installation, the administrator will install the FIDO2 component in order to enable passkeys as an authentication mechanism.
