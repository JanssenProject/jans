---
tags:
  - administration
  - client
  - configuration
---

# Client Configuration

This document covers some important configuration elements of client configuration. How these elements are configured
for a client has an impact on aspects like client security.

## Redirect URI

Redirect URI is the most basic and at times the only parameter needed for registering a client. It is defined in [OAuth
framework](https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.1) and
[OpenId Connect specification](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication).

Client can register a list of URIs as value for redirect URI parameter. Redirect URI can be any
[valid URI](https://www.ietf.org/rfc/rfc2396.txt).  

- When using client type as `web`, the redirect URI generally takes the form of `<schema>://<host-name>:<port>/<path>`. 
  It must use `https` schema. Exception to schema rule is made when using 
  `localhost` or loopback ip `127.0.0.1` as hostname to facilitate local testing. When possible, use the loopback IP 
  instead of `localhost` as hostname as 
  recommended in [OAuth 2.0 native app specification section 8.3](https://www.rfc-editor.org/rfc/rfc8252#section-8.3)
- Janssen Server supports all [methods of redirection used by
  native apps](https://datatracker.ietf.org/doc/html/rfc8252#section-7). Use of Private-Use URI (or custom URL) is 
  supported by allowing redirect URI to take form of reverse DNS name, for example, ` com.example.app`. URLs for
  loopback interface redirection are also supported.
  **TODO**: confirm what this [code comment](https://github.com/JanssenProject/jans/blob/82a1046bf4a14a2ae191251e4fc874ccf7c612ec/jans-auth-server/server/src/main/java/io/jans/as/server/model/registration/RegisterParamsValidator.java#L284-L285) means 
- When client registers multiple redirect URIs (Janssen Server accepts a list URIs separated by space), be aware that
  Janssen Server will use these one by one for validation purpose and the validation stops at the first match. 
  URIs are used for validation of authorization request
  for authorization code and implicit grant type. URIs are also used for validation when the application exchanges an 
  authorization code for an access token.  
- Redirect URI should be an absolute URI. For instance, URI should not have wildcard characters. As recommended 
  [here](https://www.rfc-editor.org/rfc/rfc6749#section-3.1.2)
- Redirect Regex: **TODO**
- **TODO**: Max number of redirect uris that can be configured, plus, should we document [this approach](https://learn.microsoft.com/en-us/azure/active-directory/develop/reply-url#use-a-state-parameter)?
- If there are multiple registered redirect_uris, and the client is using `pairwise` subject 
  identifiers, the Client MUST also register a sector_identifier_uri. This is required to keep the pairwise subject
  identifiers consistent across various domains under same administrative control. Refer to [pairwise algorithm 
  section](https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg) of OpenId Connect specification for more
  details.

## Cryptography 



## Grants

## Pre-authorization

If the OAuth authorization prompt should not be displayed to end users, set this field to True. This is useful for SSO 
to internal clients (not third party) where there is no need to prompt the person to approve the release of information.

## Response Types



Please use the left navigation menu to browse the content of this section while we are still working on developing content for `Overview` page.

!!! Contribute
If you’d like to contribute to this document, get started with the [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/#contributing-to-the-documentation)