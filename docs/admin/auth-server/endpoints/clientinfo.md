---
tags:
- administration
- auth-server
- clientinfo
- endpoint
---

# Overview

`/clientinfo` endpoint is an OAuth2 protected endpoint that is used to retrieve claims about registered client.

URL to access clientinfo endpoint on Janssen Server is listed in the response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`clientinfo_endpoint` claim in the response specifies the URL for clientinfo endpoint. By default, clientinfo endpoint looks
like below:

```
https://janssen.server.host/jans-auth/restv1/clientinfo
```

Since clientinfo endpoint is an OAuth2 protected resource, a valid access token with appropriate scope is required to
access the endpoint. More information about request and response of the clientinfo endpoint can be found in
the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml#/Client_Info).

## Disabling The Endpoint Using Feature Flag

`/clientinfo` endpoint can be enabled or disable using [clientinfo feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#clientinfo).
Use [Janssen Text-based UI(TUI)](../../config-guide/tui.md) or [Janssen command-line interface](../../config-guide/jans-cli/README.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or
disable `clientinfo` flag as required.

![](../../../assets/image-tui-enable-components.png)

## Configuration Properties

Clientinfo endpoint can be further configured using Janssen Server configuration properties listed below. When using
[Janssen Text-based UI(TUI)](../../config-guide/tui.md) to configure the properties,
navigate via `Auth Server`->`Properties`.

- [clientInfoEndpoint](../../reference/json/properties/janssenauthserver-properties.md#clientinfoendpoint)
- [mtlsClientInfoEndpoint](../../reference/json/properties/janssenauthserver-properties.md#mtlsclientinfoendpoint)

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).