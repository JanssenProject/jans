---
tags:
- administration
- client-management
---

# Client Management

Janssen server client management enables users to create(register), update, remove and get information about OAuth 2.0 
clients(or relying parties in context of OpenId Connect). 

OAuth framework defines the term `client` [here](https://datatracker.ietf.org/doc/html/rfc6749#section-1.1) and OpenId 
Connect [defines relying party](https://openid.net/specs/openid-connect-core-1_0.html#Introduction) as 
`OAuth 2.0 Clients using OpenID Connect are also referred to as Relying Parties (RPs)`

## Ways To Perform Client Management

### Config-API and Tools

Janssen Server [config-API](../../config-guide/config-api/README.md) module includes [API for client management](../../config-guide/config-api/openid-client.md). 

Janssen Server tools like [Janssen Text-based UI(TUI)](../../config-guide/tui.md) and [jans-CLI](../../config-guide/jans-cli/README.md)
provide a way to perform client management operations. These tools use the same APIs provided by `config-API` module mentioned above. 

### API For Dynamic Client Registration

Janssen Server provides client management API for dynamic client registration using 
`/registration` endpoint. The OpenApi specification for
[/registration](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml#/Registration)
endpoint contains details on how to use this endpoint to perform various client management operations along with defining
the request and response parameters. Also refer to 
[documentation for /registration endpoint](../endpoints/client-registration.md) to 
understand steps involved in dynamic client registration.

Below section is a step-by-step guide to for client management using [jans-CLI](../../config-guide/jans-cli/README.md) and 
[Janssen Text-based UI(TUI)](../../config-guide/tui.md). 

## Client Registration

In order to get token from token endpoint, a `confidential` client has to be registered with authorization server(AS),
also called OpenID Connect Provider(OP) in OpenID Connect context. Registration allow clients to specify some
vital details about itself, like [redirect_uri](./configuration/redirect-uris.md), plus information that will define
the interaction between the client and the AS/OP.

### Using Jans-CLI

TODO: add sample command

### Using TUI

To start registering a new client, navigate to `Auth Server`->`Clients`->`Add Client`.  This brings up a screen as show below with various sections to input client details.

![](../../../assets/Jans_TUI_Auth_Server_Add_new_client.png)

#### Basic

| Parameter (TUI)             | Request Parameter (CLI) | Description                                                                                                                                                                                                                                                                                                                                                              | Required | Default                               |
|-----------------------------|-------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|---------------------------------------|
| Client_ID                   |                         | Janssen Server generates and assigns a unique identifier to a successfully registered client                                                                                                                                                                                                                                                                             | No       | Server Generated                      |
| Active                      |                         | Flag to activate or deactivate the client                                                                                                                                                                                                                                                                                                                                | No       | Active                                |
| Client Name                 |                         | User provided name of the client                                                                                                                                                                                                                                                                                                                                         | No       | Blank                                 |
| Client Secret               |                         | User provided secret for client authentication                                                                                                                                                                                                                                                                                                                           | No       | Server Generated                      |
| Description                 |                         | User provided description of the client                                                                                                                                                                                                                                                                                                                                  | No       | Blank                                 |
| Authn Method Token Endpoint |                         | Method used for authenticating client at token endpoint. It can be any one of the [client authentication methods](./client-authn.md) supported by Janssen Server                                                                                                                                                                                                         | No       | `none`                                |
| Subject Type                |                         | Type of identifier used for subject(end-user). Janssen Server supports Subject identifier [type as defined in OIDC core specification](https://openid.net/specs/openid-connect-core-1_0.html#SubjectIDTypes)                                                                                                                                                             | No       | `public`                              |
| Sector Identifier URI       |                         | User provided URI to be used in pairwise identifier algorithm [as specified in OIDC core specification](https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg)                                                                                                                                                                                               | No       | Blank                                 |
| Grant                       |                         | Enroll client for One or more [supported grant types](./configuration/grants.md)                                                                                                                                                                                                                                                                                         | No       | `Authorization Code`, `Refresh Token` |
| Response Types              |                         | [Response Type](./configuration/response-types.md) value that determines the authorization processing flow to be used, including what parameters are returned from the endpoints used.                                                                                                                                                                                   | No       | `code`                                |
| Suppress Authorization      |                         | TODO                                                                                                                                                                                                                                                                                                                                                                     | No       | False                                 |
| Application Type            |                         | Kind of the application. The defined values are `native` or `web`. Web Clients using the OAuth Implicit Grant Type must only register URLs using the HTTPS scheme as redirect_uris; they must not use localhost as the hostname. Native Clients must only register redirect_uris using custom URI schemes or URLs using the http: scheme with localhost as the hostname. | No       | `web`                                 |
| Redirect Uris               |                         | [Redirection URI](./configuration/redirect-uris.md) values used by the Client. One of these registered Redirection URI values must exactly match the redirect_uri parameter value used in each Authorization Request                                                                                                                                                     | Yes      | No defaults                           |
| Redirect Regex              |                         | TODO                                                                                                                                                                                                                                                                                                                                                                     | No       | Blank                                 |
| Scopes                      |                         | List of scopes that a client can request. Refer to Janssen Server [scope documentation](../scopes/README.md) for more details                                                                                                                                                                                                                                            |          |                                       |




#### Token

TODO: add details here

#### Logout

TODO: add details here

#### Software Info

TODO: add details here

#### CIBA-PAR-UMA

TODO: add details here

#### Encryption-Signing

TODO: add details here

#### Advanced Client Properties

TODO: add details here

#### Client Scripts

TODO: add details here

## Getting Information About Existing Clients

Use `search` to retrieve information about a particular client, while `Get Clients` will show the list of all
available clients.

Upon selecting the client, the client details screen will shown.

![](../../../assets/Jans_TUI_Auth_Server_Client_detail.png)

## Updating Client Configuration

- Open the client details page using steps mentioned in 
[getting information](#getting-information-about-existing-clients) section.
- Update the information as required and select `save` to persist the updates.

## Deactivating A Client

Use instructions in [updating client configuration](#updating-client-configuration) to open client details 
page. A client can be deactivated by unchecking the `Active` attribute.

## Interception Scripts

TODO: add more details

REF:
https://docs.jans.io/v1.0.6/script-catalog/client_registration/OpenBanking/client-registration/



!!! Contribute
If you’d like to contribute to this document, get started with the [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/#contributing-to-the-documentation)