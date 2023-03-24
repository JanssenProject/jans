---
tags:
  - administration
  - auth-server
  - oauth
  - feature
---

# OAuth 2.0 Implicit Grant 

The [Implicit Grant](tools.ietf.org/html/rfc6749#section-1.3.2) :

!!! [OAuth 2.0 Security Best Current Practice](https://tools.ietf.org/html/draft-ietf-oauth-security-topics) absolutely discourages the use of Implicit flow.
Instead, use Authorization code flow with PKCE -[OAuth 2.0 for Browser-Based Apps](https://tools.ietf.org/html/draft-ietf-oauth-browser-based-apps).
[Further reading](https://oauth.net/2/grant-types/implicit/)


### Sequence Diagram

```mermaid
sequenceDiagram

title Implicit flow
autonumber 1

participant Resource owner User
Client->>Jans AS:Authorization Request
activate Client
activate Jans AS
Resource owner User ->>Jans AS:User login and consent
Jans AS-->>Client:Access Token in the URI fragment
deactivate Client
deactivate Jans AS

Client ->>Jans AS:Validate Access Token
activate Client
activate Jans AS
Jans AS-->>Client:Validate response
deactivate Client
deactivate Jans AS

Client ->>Web - Hosted client resource:Call API with Access Token
activate Client
activate Jans AS
Jans AS-->>Client:Protected resource
deactivate Client
deactivate Jans AS

```

