---
tags:
  - administration
  - auth-server
  - openidc
  - feature
  - ID Token
---

# ID Token

Janssen Server is an OpenID Connect Provider(OP). OpenID Connect extends OAuth 2.0 and adds user authentication
capabilities using ID token.

To add end-user authentication capabilities to an OAuth flow, it is important to use `openid` scope in the
request as specified [here](https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest). In absence of `openid`
scope, the Janssen Server will not treat the incoming request as OpenID Connect request.

For further information on how to configure and customize OpenID Connect using ID Token read
[here](../tokens/openid-id-token.md)

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).