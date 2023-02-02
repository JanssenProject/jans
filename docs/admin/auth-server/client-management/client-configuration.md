---
tags:
  - administration
  - client
  - configuration
---

# Client Configuration

## Redirect URI

- Redirect URI must use `https` schema except when using hostname as `localhost` or loopback ip `127.0.0.1` for the purpose of locally testing `web` client. Between loopback IP and `localhost` as hostname, use of loopback IP is recommended as described in [OAuth 2.0 native app specification section 8.3](https://www.rfc-editor.org/rfc/rfc8252#section-8.3)
-  TODO: if we support custom URI for native apps [here](https://github.com/JanssenProject/jans/blob/82a1046bf4a14a2ae191251e4fc874ccf7c612ec/jans-auth-server/server/src/main/java/io/jans/as/server/model/registration/RegisterParamsValidator.java#L284-L285)

- TODO: when multiple redirect URIs match, what does AS do

- TODO: what if someone registers URIs where just ports are different [here](https://learn.microsoft.com/en-us/azure/active-directory/develop/reply-url#localhost-exceptions)

- wild cards in URIs

- query params 

- [sector_identifier_uri](https://github.dev/JanssenProject/jans/blob/82a1046bf4a14a2ae191251e4fc874ccf7c612ec/jans-auth-server/server/src/main/java/io/jans/as/server/model/registration/RegisterParamsValidator.java#L298)

- Using `state` param

- where all its validated [here](https://www.oauth.com/oauth2-servers/redirect-uris/redirect-uri-validation/)

## Cryptography 



## Grants

## Pre-authorization

## Response Types



Please use the left navigation menu to browse the content of this section while we are still working on developing content for `Overview` page.

!!! Contribute
If you’d like to contribute to this document, get started with the [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/#contributing-to-the-documentation)