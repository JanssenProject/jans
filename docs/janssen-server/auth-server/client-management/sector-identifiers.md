---
tags:
- administration
- client
- sector-identifier
---

# Sector Identifier

Janssen Server supports sector identifier URI and pairwise subject IDs for OpenId Connect relying party.
As defined in [OpenId Connect core specification](https://openid.net/specs/openid-connect-core-1_0.html#Terminology), the
sector identifiers value is used to derive pairwise subject IDs. Janssen Server also supports `Sector Identifier URI` as
part of client configuration. `Sector Identifier URI` when used with `pairwise` subject type, enables a group of 
websites under the same administrative control to receive the same subject identifiers. 
`Sector Identifier URI` also allows clients to
change the host component of the redirect URI and still keep the subject identifiers unchanged.

## Configuring Sector Identifier

Janssen Server runs below mentioned checks on value configured for `Sector Identifier URI`:

- URI should have a `https` schema
- URI should be accessible to Janssen Server and the response should be a valid JSON array of redirect URIs
- All redirect URI received in response must exist in the list of the redirect URI provided by the client at the registration time

!!! Note
If the client can not host an endpoint that will be reachable by `Sector Identifier URI`, then in order to use the `pairwise`
subject IDs, the client must supply a `Redirect URI` list where URIs have the same host component. The host component
value will be used as the sector identifier.

## Configuration With Pairwise Subject Type

How sector identifier value is used to derive value for the pairwise subject identifier is detailed in the
[OIDC core specification](https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg).

Janssen Server allows clients/RPs to set subject type. The `public` subject type is the default and the client/RP can choose
to use the `pairwise` type. When using TUI, this can be configured from the client configuration screen below:

![](../../../assets/image-tui-client-registration-basic.png)

When the `pairwise` subject type is selected, the value for `Sector Identifier URI` can be left blank if all redirect
URIs have the same host component. If the list of redirect URIs contains multiple host names,
providing a `Sector Identifier URI` is a must. When `Sector Identifier URI` is provided, the host component of the URI is
used as a sector identifier.

## Configuration Properties

Janssen Server allows customization concerning sector identifiers using the properties below:

- [sectorIdentifierCacheLifetimeInMinutes](https://docs.jans.io/v1.0.10/admin/reference/json/properties/janssenauthserver-properties/#sectoridentifiercachelifetimeinminutes)
- [shareSubjectIdBetweenClientsWithSameSectorId](https://docs.jans.io/v1.0.10/admin/reference/json/properties/janssenauthserver-properties/#sharesubjectidbetweenclientswithsamesectorid)


## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
