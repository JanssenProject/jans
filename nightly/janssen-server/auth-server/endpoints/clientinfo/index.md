# Clientinfo Endpoint

`/clientinfo` endpoint is an OAuth2 protected endpoint that is used to retrieve claims about registered client.

URL to access clientinfo endpoint on Janssen Server is listed in the response of Janssen Server's well-known [configuration endpoint](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/configuration/index.md) given below.

```
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`clientinfo_endpoint` claim in the response specifies the URL for clientinfo endpoint. By default, clientinfo endpoint looks like below:

```
https://janssen.server.host/jans-auth/restv1/clientinfo
```

Since clientinfo endpoint is an OAuth2 protected resource, a valid access token with appropriate scope is required to access the endpoint. More information about request and response of the clientinfo endpoint can be found in the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/nightly/jans-auth-server/docs/swagger.yaml#/Client_Info).

## Disabling The Endpoint Using Feature Flag

`/clientinfo` endpoint can be enabled or disable using [clientinfo feature flag](https://docs.jans.io/nightly/janssen-server/reference/json/feature-flags/janssenauthserver-feature-flags/#clientinfo). Use [Janssen Text-based UI(TUI)](https://docs.jans.io/nightly/janssen-server/config-guide/config-tools/jans-tui/index.md) or [Janssen command-line interface](https://docs.jans.io/nightly/janssen-server/config-guide/config-tools/jans-cli/index.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or disable `clientinfo` flag as required.

## Configuration Properties

Clientinfo endpoint can be further configured using Janssen Server configuration properties listed below. When using [Janssen Text-based UI(TUI)](https://docs.jans.io/nightly/janssen-server/config-guide/config-tools/jans-tui/index.md) to configure the properties, navigate via `Auth Server`->`Properties`.

- [clientInfoEndpoint](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#clientinfoendpoint)
- [mtlsClientInfoEndpoint](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#mtlsclientinfoendpoint)

## Want to contribute?

If you'd like to contribute content to this page, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
