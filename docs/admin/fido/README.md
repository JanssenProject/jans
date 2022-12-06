---
tags:
  - administration
  - fido
---

# Overview

## Janssen's FIDO2 server

FIDO2 as an open standard for authentication is based on public key cryptography.

Janssen's FIDO2 server - a component inside the Janssen project enables users of RPs to enroll and authenticate themselves using U2F keys, FIDO2 keys or inbuilt platform authenticator.

1. The FIDO2 server uses REST endpoints to communicate with an RP via an https connection.
2. The FIDO2 server implements the [FIDO Metadata Service (MDS3)](https://fidoalliance.org/metadata/metadata-service-overview/) defined by FIDO Alliance.
3. The FIDO2 server stores user data into the same persistence store as the Jans-Auth server. (LDAP, MYSQL, Couchbase etc.)

Janssen's FIDO server is a standalone server communicates with the RP using an API which can be obtained by querying the following URL :

```
https://<myjans-server>/.well-known/fido2-configuration
```

Response:

    ```
    {
      "version": "1.1",
      "issuer": "https://<myjans-server>",
      "attestation": {
        "base_path": "https://<myjans-server>/jans-fido2/restv1/attestation",
        "options_enpoint": "https://<myjans-server>/jans-fido2/restv1/attestation/options",
        "result_enpoint": "https://<myjans-server>/jans-fido2/restv1/attestation/result"
      },
      "assertion": {
        "base_path": "https://<myjans-server>/jans-fido2/restv1/assertion",
        "options_enpoint": "https://<myjans-server>/jans-fido2/restv1/assertion/options",
        "result_enpoint": "https://<myjans-server>/jans-fido2/restv1/assertion/result"
      }
    }
    ```

## Customization authentication flow using Interception script
  
  In the Janssen ecosystem, the authentication flow that comprises of the calls to WebAuthn API and the FIDO server is achieved using an interception script, details of it can be found [here](../../script-catalog/person_authentication/fido2-external-authenticator/README).


## References
1. https://www.w3.org/TR/webauthn-2/
2. http://fidoalliance.org/specs/mds/fido-metadata-statement-v3.0-ps-20210518.html

## Tools
1. https://jwt.io/ – For JWT decoding and debugging
2. https://www.base64decode.org/ – For Decoding Base64 to UTF8
3. https://fidoalliance.org/certification/fido-certified-products/ - To browse authenticators listed with FIDO Alliance
