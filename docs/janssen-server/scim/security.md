---
tags:
  - administration
  - scim
---

## Security considerations

SCIM API allows administrators and developers manage a key asset in an IAM deployment, namely users. Additionally, other resources like groups and registered fido devices can be managed in Janssen Server via SCIM.

Clearly, this kind of API must not be anonymously accessible. However, the SCIM standard does not define a specific mechanism to prevent unauthorized requests to endpoints. There are just a few guidelines in section 2 of [RFC 7644](https://tools.ietf.org/html/rfc7644) concerned with authentication and authorization.

Currently, the Janssen server supports an OAuth-based mechanism to protect these endpoints; you can find more information in this regard [here](./oauth-protection.md). Nonetheless, it is recommended not to expose this API outside the organization's network. Ideally administrators should block any external attempt to access URLs under the path `https://your-jans-server/jans-scim`.
