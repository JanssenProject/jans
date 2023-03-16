---
tags:
  - administration
  - client
  - sector-identifier
---

# Sector Identifier

Janssen Server supports sector identifier values for OpenId Connect relying party configuration. As defined in
[OpenId Connect core specification](https://openid.net/specs/openid-connect-core-1_0.html#Terminology), the
sector identifiers can be used by a group of websites under the same administrative control to receive same pairwise
subject identifiers. Using sector identifiers also allows clients to change host component of redirect URI and still
keep the subject identifiers unchanged. 

How sector identifier value is used to derive value for pairwise subject identifier is detailed in the 
[OIDC core specification](https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg).

## Using With Pairwise Subject Type

Janssen Server allows clients/RPs to set subject type. `public` subject type is default and client/RP can choose
to use `pairwise` type. When using TUI, this setting can be opted from client configuration screen below when 
creating or updating the client configuration.

![](../../../assets/image-tui-client-registration-basic.png)

When `pairwise` subject type is selected, the value for `Sector Identifier URI` can be left blank if the redirect
URI list for the client has the same host names. If the list of redirect URIs contain multiple host names, 
providing `Sector Identifier URI` is must. 

TODO: Do we validate the `Sector Identifier URI` by checking if it returns a JSON list of redirect URIs, or we 
don't validate?

TODO: what if the client/RP is of type which can not expose an API. Like native client. Then we can't validate 
the `Sector Identifier URI`. What do we do in that scenario.





## This content is in progress

The Janssen Project documentation is currently in development. Topic pages are being created in order of broadest relevance, and this page is coming in the near future.

## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussion) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).