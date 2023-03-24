---
tags:
  - administration
  - auth-server
  - token
---

## Background

The UMA requesting party token (RPT) is an OAuth access token associated with
the UMA grant. An RPT is unique to a requesting party, client, authorization
server, resource server, and resource owner.

The client uses the RPT in the Authorization header of an API request.
The RPT bearer token conveys that  UMA policy evaluation was successful on Auth
Server.

The Client may also present the RPT token while making a request to Auth Server
for a new RPT, for example to up-scope or down-scope a token.

For more information on UMA, see the specifications:
* [User-Managed Access (UMA) 2.0 Grant for OAuth 2.0 Authorization](https://gluu.co/uma-grant)
* [Federated Authorization for User-Managed Access (UMA) 2.0](https://gluu.co/uma-authz)

### Server properties

`umaRptAsJwt` *Default: False* -
: Reference token is the default

`umaRptLifetime` *Default: 3600 seconds* -
: 60 minutes

`introspectionAccessTokenMustHaveUmaProtectionScope` *Default: False* -
:

`umaGrantAccessIfNoPolicies` *Default: False* -
:

`umaTicketLifetime` *Default: 3600 seconds* -
: 60 minutes - Returned by the Resource Server to the Client for presentation
at the UMA token endpoint. The ticket is the equivalent of the code in the
OAuth code flow.

`umaAddScopesAutomatically` *Default: True* -
:

`umaPctLifetime` *Default: 1728000 seconds* -
: The PCT is a pushed claim token that references a previous claims gathering
flow, so you don't have to bother the subject again for this information.

`umaValidateClaimToken` *Default: False* -
:

`umaRestrictResourceToAssociatedClient` *Default: False* -
:
