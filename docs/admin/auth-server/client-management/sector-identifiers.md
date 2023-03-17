---
tags:
  - administration
  - client
  - sector-identifier
---

# Sector Identifier

Janssen Server supports sector identifier URI and pairwise subject IDs for OpenId Connect relying party configuration. 
As defined in [OpenId Connect core specification](https://openid.net/specs/openid-connect-core-1_0.html#Terminology), the
sector identifiers value is used to derive pairwise subject IDs. Janssen Server also supports `Sector Identifier URI` as
part of client configuration. `Sector Identifier URI` enables a group of websites under the same 
administrative control to receive same pairwise subject identifiers. `Sector Identifier URI` also allows clients to 
change host component of redirect URI and still keep the subject identifiers unchanged. 

## Configuring Sector Identifier

Janssen Server needs the `Sector Identifier URI` to fulfil below-mentioned checks:
- URI should have `https` schema
- URI should be accessible to Janssen Server and response should be a valid JSON array of redirect URIs
- All redirect URI received in response must exist in the list of redirect URI provided by client at the registration time

!!! Note
    If the client can not host an endpoint which will be reachable by `Sector Identifier URI`, then to use `pairwise` 
    subject IDs, the client must supply `Redirect URI` list where URI's have the same host component. The host component
    will be used as sector identifier.

## Configuration With Pairwise Subject Type

How sector identifier value is used to derive value for pairwise subject identifier is detailed in the
[OIDC core specification](https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg).

Janssen Server allows clients/RPs to set subject type. `public` subject type is the default and client/RP can choose
to use `pairwise` type. When using TUI, this setting can be opted from client configuration screen below when 
creating or updating the client configuration.

![](../../../assets/image-tui-client-registration-basic.png)

When `pairwise` subject type is selected, the value for `Sector Identifier URI` can be left blank if the all redirect
URIs for the client use the same host name. If the list of redirect URIs contain multiple host names,
providing `Sector Identifier URI` is must. When `Sector Identifier URI` is provided, the host component of the URI is
used as sector identifier.

### Pairwise Identifier Generation

Janssen Server uses the `Sector Identifier URI` host name string, local user ID and a salt string as 
initial input. This input is then signed with HS256 signing algorithm to generating pairwise identifier. 

## Configuration Properties

Janssen Server allows customisation with respect to sector identifiers using properties below:

 - [sectorIdentifierCacheLifetimeInMinutes](https://docs.jans.io/v1.0.10/admin/reference/json/properties/janssenauthserver-properties/#sectoridentifiercachelifetimeinminutes)
 - [shareSubjectIdBetweenClientsWithSameSectorId](https://docs.jans.io/v1.0.10/admin/reference/json/properties/janssenauthserver-properties/#sharesubjectidbetweenclientswithsamesectorid)


## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).