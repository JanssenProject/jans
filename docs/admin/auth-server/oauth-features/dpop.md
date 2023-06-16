---
tags:
  - administration
  - auth-server
  - oauth
  - feature
  - dpop
---

# DPoP (Demonstrating Proof-of-Possession at the Application Layer)

Janssen Server supports DPoP, Demonstrating Proof-of-Possession at the Application Layer, which is OAuth 2.0 feature
to enhance security of resources protected by access token.

When DPoP is being used, the Janssen Server checks whether the presenter of the access token is the one to whom the 
access token was actually issued. Hence making sure that an stolen access token is not being used to access the 
protected resource. 

OAuth also provides [MTLS(Mutual TLS)](./mtls.md) as mechanism to ensure that the token presenting party is legitimate.
While MTLS should be preferred whenever it is possible to use it, for other cases like single
page application (SPA), DPoP can be used.

## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussion) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd 
like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).