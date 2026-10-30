---
tags:
  - administration
  - auth-server
  - oauth
  - feature
  - grant
  - implicit
---

# OAuth 2.0 Implicit Grant

!!! warning "Legacy grant"
    The Implicit Grant is not included in OAuth 2.1 and is not enabled in
    Janssen's default supported grant types. It is not recommended for new
    integrations. Legacy deployments may explicitly configure support for
    this grant. For new applications, use the Authorization Code Grant with
    PKCE (`S256`) instead. Verify the grant types advertised in the
    deployment's discovery metadata.


The [Implicit Grant](https://datatracker.ietf.org/doc/html/rfc6749#section-4.2) 
is a legacy OAuth 2.0 flow in which an access token is returned directly
in the authorization response, without exchanging an authorization code
at the token endpoint.

The [OAuth 2.0 Security Best Current Practice (RFC 9700)](https://datatracker.ietf.org/doc/html/rfc9700)
recommends against using the Implicit Grant because of security risks
associated with access tokens exposed through the authorization response.

For additional background, see
[OAuth 2.0 Implicit Grant](https://oauth.net/2/grant-types/implicit/).


### Sequence Diagram

```mermaid

sequenceDiagram
    title Implicit flow
    autonumber 1

    participant User as Resource owner User
    participant Client
    participant JansAS as Jans AS
    participant WebResource as Web - Hosted client resource

    Client->>JansAS:Authorization Request
    activate Client
    activate JansAS
    User->>JansAS:User login and consent
    JansAS-->>Client:Access Token in the URI fragment
    deactivate Client
    deactivate JansAS

    Client->>JansAS:Validate Access Token
    activate Client
    activate JansAS
    JansAS-->>Client:Validate response
    deactivate Client
    deactivate JansAS

    Client->>WebResource:Call API with Access Token
    activate Client
    activate JansAS
    JansAS-->>Client:Protected resource
    deactivate Client
    deactivate JansAS


```
