# End Session Endpoint

Janssen Server's `/end_session` endpoint supports logout using [OpenId Connect RP-initiated Logout](https://openid.net/specs/openid-connect-rpinitiated-1_0.html) mechanism. When using OpenID Connect Logout, it is recommended to use Front-Channel Logout. In Front-Channel Logout the browser receives a page with a list of application logout urls within an iframe. This prompts the browser to call each application logout individually and the OpenID Connect end-session endpoint via Javascript.

URL to access end session endpoint on Janssen Server is listed in the response of Janssen Server's well-known [configuration endpoint](https://docs.jans.io/head/janssen-server/auth-server/endpoints/configuration/index.md) given below.

```
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`end_session_endpoint` claim in the response specifies the URL for end session endpoint. By default, end session endpoint looks like below:

```
https://janssen.server.host/jans-auth/restv1/end_session
```

Refer to [this](https://gluu.org/docs/gluu-server/4.4/operation/logout/#openid-connect-single-log-out-slo) article from Gluu Server documentation to understand how end session endpoint works in Janssen Server.

More information about request and response of the end session endpoint can be found in the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml).

## Disabling The Endpoint Using Feature Flag

`/end_session` endpoint can be enabled or disable using [END_SESSION feature flag](https://docs.jans.io/head/janssen-server/reference/json/feature-flags/janssenauthserver-feature-flags/#end_session). Use [Janssen Text-based UI(TUI)](https://docs.jans.io/head/janssen-server/config-guide/config-tools/jans-tui/index.md) or [Janssen command-line interface](https://docs.jans.io/head/janssen-server/config-guide/config-tools/jans-cli/index.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or disable `END_SESSION` flag as required.

## Configuration Properties

End session endpoint can be further configured using Janssen Server configuration properties listed below. When using [Janssen Text-based UI(TUI)](https://docs.jans.io/head/janssen-server/config-guide/config-tools/jans-tui/index.md) to configure the properties, navigate via `Auth Server`->`Properties`.

- [allowEndSessionWithUnmatchedSid](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#allowendsessionwithunmatchedsid)
- [endSessionEndpoint](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#endsessionendpoint)
- [endSessionWithAccessToken](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#endsessionwithaccesstoken)
- [mtlsEndSessionEndpoint](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#mtlsendsessionendpoint)
- [rejectEndSessionIfIdTokenExpired](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#rejectendsessionifidtokenexpired)
- [allowPostLogoutRedirectWithoutValidation](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#allowpostlogoutredirectwithoutvalidation)
- [forceIdTokenHintPresence](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#forceidtokenhintpresence)

Apart from the above-mentioned server properties, the properties relevant to individual clients can be configured during client registration or can be edited later. When using [Janssen Text-based UI(TUI)](https://docs.jans.io/head/janssen-server/config-guide/config-tools/jans-tui/index.md) to configure the properties, navigate via `Auth Server`-> `Clients`->`logout` as show in image below:

### Interception Scripts

Response from end session endpoint can be further customized using [end session](https://docs.jans.io/head/script-catalog/end_session/end-session/index.md) interception script.

This script can be used to customize the HTML response generated from end session endpoint.

## Difference with Global Token Revocation

The End Session endpoint is used to terminate a user's session from a single application (log out), while the Global Token Revocation endpoint (`/global-token-revocation`) is used to invalidate all of a user's sessions and tokens across all applications. This is a more forceful measure that ensures all access is revoked, which is useful in situations where a user's account may have been compromised.

For more details, see [Global Token Revocation](https://docs.jans.io/head/janssen-server/auth-server/endpoints/global-token-revocation/index.md).

## Want to contribute?

If you have content you'd like to contribute to this page, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
