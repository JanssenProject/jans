---
tags:
  - administration
  - fido
---

Apple Anonymous Attestation is first of its kind, providing a service like an Anonymization CA, where the authenticator works with a cloud operated CA owned by its manufacturer to dynamically generate per-credential attestation certificates such that no identification information of the authenticator will be revealed to websites in the attestation statement. Furthermore, among data relevant to the registration ceremony, only the public key of the credential along with a hash of the concatenated authenticator data and client data are sent to the CA for attestation, and the CA will not store any of these. This approach makes the whole attestation process privacy preserving. In addition, this approach avoids the security pitfall of Basic Attestation that the compromising of a single device results in revoking certificates from all devices with the same attestation certificate.

https://medium.com/webauthnworks/webauthn-fido2-verifying-apple-anonymous-attestation-5eaff334c849
