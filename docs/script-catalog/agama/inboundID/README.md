# Inbound identity using Agama

With Agama administrators can delegate authorization to external services like social sites. In a typical scenario users click on a "Sign in with..." button for authentication to take place at a given 3rd party. Finally, users get access to a target application.

This kind of solution is usually referred to as "inbound identity". In this document the steps required to setup inbound identity in your Janssen server are presented.

## Terminology

- Provider: An external identity provider, often a social site. Every provider is associated with a unique identifier

- Provider preselection: A process that designates the provider to employ for authentication without the end-user  making a explicit decision, i.e. without a "Sign in with" button. Here the provider may be inferred from contextual data elicited in earlier stages, for instance. 

## Requisites

- A Janssen server with config-api installed
- Agama engine [enabled](https://jans.io/docs/admin/developer/agama/quick-start/#enable-the-engine)
- Understanding of how to [add and modify Agama flows](https://jans.io/docs/admin/developer/agama/quick-start/#add-the-flow-to-the-server) in your server
- Starter knowledge of [OAuth2](https://www.ietf.org/rfc/rfc6749)
)/[OIDC](http://openid.net/specs/openid-connect-core-1_0.html)

## Scope

Inbound identity is a broad topic in the sense that there is a great amount of different mechanisms providers may employ to materialize the authentication process. The current offering in Agama is focused on OAuth2-compliant providers, more specifically those supporting the `code` authorization grant. This does not mean other grants or even different protocols such as SAML cannot be supported, however this would entail a considerable development effort.

If the provider of interest already supports the `code` authorization grant, the amount of work is minimal.

## Flows

### Provider 

Every provider to support must have an associated Agama flow which has to:

- Redirect the browser to the provider's site for the user to enter his login credentials
- Return the associated profile data of given user

Note the scope here is limited: no login process takes place in the Janssen server; this occurs later in a parent flow.

To facilitate administrators' work, the following flows are already implemented:

- Apple
- Facebook
- Github
- Google

Later, we'll see how to deploy one of these.

### Utility flows
 
A couple of utility flows are available for developers writing new flows:

- Authorization Code flow: This flow implements the OAuth 2.0 authorization code grant where client authentication at the token endpoint occurs as described in section 2.3.1 of [RFC 6749](https://www.ietf.org/rfc/rfc6749) (HTTP basic authentication scheme). In summary, this flow redirects the browser to the external provider's site where the user will enter his credentials, then back at the Janssen redirect URL a `code` is obtained which is employed to issue an access token request. The flow returns the token response as received by the provider  

- Authorization Code flow with userInfo request: This flow reuses the previous flow and additionally issues a request to a given userInfo URL passing the access token in the HTTP Authorization header. The response obtained (the profile data of the user) is returned in conjuction with the token response of the authorization code flow

The above means that often, when writing a new flow for a provider, the task boils down to calling the latter flow and retrieving the profile data only
<!--
Default inbound identity flow

The Agama flow `io.jans.inbound.ExternalSiteLogin` implements a generic inbound identity flow. This is a summary of the steps involved:

1. If the flow was passed a provider identifier as input, the given provider's data is looked up and its corresponding flow .  wi
-->