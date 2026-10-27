# OAuth 2.0 Implicit Grant

The [Implicit Grant](https://tools.ietf.org/html/rfc6749#section-1.3.2) :

!!! [OAuth 2.0 Security Best Current Practice](https://tools.ietf.org/html/draft-ietf-oauth-security-topics) absolutely discourages the use of Implicit flow.
Instead, use Authorization code flow with PKCE -[OAuth 2.0 for Browser-Based Apps](https://tools.ietf.org/html/draft-ietf-oauth-browser-based-apps).
[Further reading](https://oauth.net/2/grant-types/implicit/)


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
