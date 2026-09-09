# Janssen Auth Server Feature Flags

| Feature Flag Name              | Description                                                                                                                                                                                                                                                                                                                                               |                                            |
| ------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------ |
| ACCESS_EVALUATION              | Enables the AuthZEN Access Evaluation API in Janssen Server. For details about its behavior, requests, responses, and authorization decisions, see the [Access Evaluation endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/access-evaluation/index.md).                                                          | [Details](#access_evaluation)              |
| ACTIVE_SESSION                 | Enables the Active Session endpoint in Janssen Server. The endpoint allows authorized applications to retrieve information about the user's active authentication session.                                                                                                                                                                                | [Details](#active_session)                 |
| CIBA                           | Enables OpenID Connect Client Initiated Backchannel Authentication (CIBA) support in Janssen Server. For more details, see the [Janssen OIDC CIBA Documentation](https://docs.jans.io/nightly/janssen-server/auth-server/openid-features/ciba/index.md).                                                                                                  | [Details](#ciba)                           |
| CLIENT_ID_METADATA_DOCUMENT    | Enable/Disable OAuth Client ID Metadata Document support (URL-based client_id)                                                                                                                                                                                                                                                                            | [Details](#client_id_metadata_document)    |
| CLIENTINFO                     | Enables the OAuth 2.0-protected Clientinfo endpoint, which allows an authorized client to retrieve claims and information about a registered client. For more details, see the [ClientInfo Endpoint Documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/clientinfo/index.md).                                                | [Details](#clientinfo)                     |
| DEVICE_AUTHZ                   | Enables the OAuth 2.0 Device Authorization Grant in Janssen Server. For details about the device authorization flow, see the [Device Authorization endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/device-authorization/index.md).                                                                              | [Details](#device_authz)                   |
| END_SESSION                    | Enables the OpenID Connect RP-Initiated Logout end-session endpoint in Janssen Server. For more details, see the [End Session endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/end-session/index.md).                                                                                                            | [Details](#end_session)                    |
| GLOBAL_TOKEN_REVOCATION        | Enables the Global Token Revocation endpoint, which invalidates all tokens and sessions associated with a user. For more details, see the [Global Token Revocation endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/global-token-revocation/index.md).                                                           | [Details](#global_token_revocation)        |
| HEALTH_CHECK                   | Enables the Auth Server health-check endpoint, which reports the operational status of Janssen Server. For more details, see the [Health Check documentation](https://docs.jans.io/nightly/janssen-server/install/install-faq/#use-the-janssen-server-health-check-endpoint).                                                                             | [Details](#health_check)                   |
| ID_GENERATION                  | Enables custom ID generation support in Janssen Server. The ID Generator allows administrators to replace the default ID generation logic with custom rules for identifiers such as person and client entries. For more details, see the [ID Generator documentation](https://docs.jans.io/nightly/script-catalog/id_generator/id-generator/index.md).    | [Details](#id_generation)                  |
| IDENTITY_ASSERTION_AUTHZ_GRANT | Enables Identity Assertion Authorization Grant (Cross-App Access / ID-JAG) support in Janssen Server. It allows a client authenticated with one Identity Provider (IdP) to obtain an access token from a trusted Resource Authorization Server without starting a new browser-based SSO flow.                                                             | [Details](#identity_assertion_authz_grant) |
| INTROSPECTION                  | Enables the OAuth 2.0 Token Introspection endpoint in Janssen Server. For more details, see the [Introspection endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/introspection/index.md).                                                                                                                         | [Details](#introspection)                  |
| JANS_CONFIGURATION             | Enables the Janssen Server *.well-known* OpenID Connect configuration endpoint used for service discovery. For more details, see the [OpenID Configuration endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/configuration/index.md).                                                                             | [Details](#jans_configuration)             |
| LOGOUT_STATUS_JWT              | Enables Logout Status JWT support in Janssen Server. For more details, see the [Logout Status JWT documentation](https://docs.jans.io/nightly/janssen-server/auth-server/tokens/logout-status-jwt/index.md).                                                                                                                                              | [Details](#logout_status_jwt)              |
| METRIC                         | Enables metric reporting in Janssen Server. Metric data can be used to monitor and report on Authorization Server activity, including user activity, issued tokens, health checks, and audit information. For details, see the [Reporting and Metrics documentation](https://docs.jans.io/nightly/janssen-server/auth-server/reporting-metrics/index.md). | [Details](#metric)                         |
| PAR                            | Enables OAuth 2.0 Pushed Authorization Requests (PAR) in Janssen Server. For more details, see the [PAR endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/par/index.md).                                                                                                                                          | [Details](#par)                            |
| RATE_LIMIT                     | Enables request rate limiting in the Janssen Authorization Server. For more details, see the [Rate Limit Configuration](https://docs.jans.io/nightly/janssen-server/config-guide/auth-server-config/rate-limit/index.md).                                                                                                                                 | [Details](#rate_limit)                     |
| REGISTRATION                   | Enables the Client Registration endpoint in Janssen Server. For more details, see the [Client Registration endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/client-registration/index.md).                                                                                                                       | [Details](#registration)                   |
| REVOKE_TOKEN                   | Enables the OAuth 2.0 Token Revocation endpoint in Janssen Server. For more details, see the [Token Revocation endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/token-revocation/index.md).                                                                                                                      | [Details](#revoke_token)                   |
| SPIFFE_CLIENT_AUTH             | Enable/Disable SPIFFE-based client authentication (X.509-SVID mutual-TLS and JWT-SVID assertion), per draft-ietf-oauth-spiffe-client-auth                                                                                                                                                                                                                 | [Details](#spiffe_client_auth)             |
| SSA                            | Enables the Software Statement Assertion (SSA) endpoint in Janssen Server. For more details, see the [SSA endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/ssa/index.md).                                                                                                                                        | [Details](#ssa)                            |
| STAT                           | Enables the Authorization Server Statistic service, which provides statistical data such as monthly active users and issued-token information. For more details, see the [Statistic endpoint](https://docs.jans.io/nightly/janssen-server/auth-server/reporting-metrics/#statistic-endpoint).                                                             | [Details](#stat)                           |
| STATUS_LIST                    | Enables the Token Status List endpoint in Janssen Server, which enables the client to query token status. For more details, see the [Logout Status JWT documentation](https://docs.jans.io/nightly/janssen-server/auth-server/tokens/logout-status-jwt/index.md).                                                                                         | [Details](#status_list)                    |
| STATUS_SESSION                 | Enables the session status check endpoint in Janssen Server, which allows an application to check the current status of an authenticated user session.                                                                                                                                                                                                    | [Details](#status_session)                 |
| U2F                            | Enables support for FIDO U2F in Janssen Server, allowing applications to use legacy U2F authenticators for registration and authentication. For more details, see the [FIDO Administration Guide](https://docs.jans.io/nightly/contribute/implementation-design/jans-fido2-design/index.md).                                                              | [Details](#u2f)                            |
| UMA                            | Enables User-Managed Access (UMA) support in Janssen Server. For more details, see the [UMA documentation](https://docs.jans.io/nightly/janssen-server/auth-server/uma-features/index.md).                                                                                                                                                                | [Details](#uma)                            |
| USERINFO                       | Enables the OpenID Connect UserInfo endpoint in Janssen Server. For more details, see the [UserInfo endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/userinfo/index.md).                                                                                                                                         | [Details](#userinfo)                       |

## ACCESS_EVALUATION

- Description: Enables the AuthZEN Access Evaluation API in Janssen Server. For details about its behavior, requests, responses, and authorization decisions, see the [Access Evaluation endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/access-evaluation/index.md).
- Required: No
- Default value: Enabled

## ACTIVE_SESSION

- Description: Enables the Active Session endpoint in Janssen Server. The endpoint allows authorized applications to retrieve information about the user's active authentication session.
- Required: No
- Default value: Enabled

## CIBA

- Description: Enables OpenID Connect Client Initiated Backchannel Authentication (CIBA) support in Janssen Server. For more details, see the [Janssen OIDC CIBA Documentation](https://docs.jans.io/nightly/janssen-server/auth-server/openid-features/ciba/index.md).
- Required: No
- Default value: Enabled

## CLIENT_ID_METADATA_DOCUMENT

- Description: Enable/Disable OAuth Client ID Metadata Document support (URL-based client_id)
- Required: No
- Default value: Enabled

## CLIENTINFO

- Description: Enables the OAuth 2.0-protected Clientinfo endpoint, which allows an authorized client to retrieve claims and information about a registered client. For more details, see the [ClientInfo Endpoint Documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/clientinfo/index.md).
- Required: No
- Default value: Enabled

## DEVICE_AUTHZ

- Description: Enables the OAuth 2.0 Device Authorization Grant in Janssen Server. For details about the device authorization flow, see the [Device Authorization endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/device-authorization/index.md).
- Required: No
- Default value: Enabled

## END_SESSION

- Description: Enables the OpenID Connect RP-Initiated Logout end-session endpoint in Janssen Server. For more details, see the [End Session endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/end-session/index.md).
- Required: No
- Default value: Enabled

## GLOBAL_TOKEN_REVOCATION

- Description: Enables the Global Token Revocation endpoint, which invalidates all tokens and sessions associated with a user. For more details, see the [Global Token Revocation endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/global-token-revocation/index.md).
- Required: No
- Default value: Enabled

## HEALTH_CHECK

- Description: Enables the Auth Server health-check endpoint, which reports the operational status of Janssen Server. For more details, see the [Health Check documentation](https://docs.jans.io/nightly/janssen-server/install/install-faq/#use-the-janssen-server-health-check-endpoint).
- Required: No
- Default value: Enabled

## ID_GENERATION

- Description: Enables custom ID generation support in Janssen Server. The ID Generator allows administrators to replace the default ID generation logic with custom rules for identifiers such as person and client entries. For more details, see the [ID Generator documentation](https://docs.jans.io/nightly/script-catalog/id_generator/id-generator/index.md).
- Required: No
- Default value: Enabled

## IDENTITY_ASSERTION_AUTHZ_GRANT

- Description: Enables Identity Assertion Authorization Grant (Cross-App Access / ID-JAG) support in Janssen Server. It allows a client authenticated with one Identity Provider (IdP) to obtain an access token from a trusted Resource Authorization Server without starting a new browser-based SSO flow.
- Required: No
- Default value: Disabled

## INTROSPECTION

- Description: Enables the OAuth 2.0 Token Introspection endpoint in Janssen Server. For more details, see the [Introspection endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/introspection/index.md).
- Required: No
- Default value: Enabled

## JANS_CONFIGURATION

- Description: Enables the Janssen Server *.well-known* OpenID Connect configuration endpoint used for service discovery. For more details, see the [OpenID Configuration endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/configuration/index.md).
- Required: No
- Default value: Enabled

## LOGOUT_STATUS_JWT

- Description: Enables Logout Status JWT support in Janssen Server. For more details, see the [Logout Status JWT documentation](https://docs.jans.io/nightly/janssen-server/auth-server/tokens/logout-status-jwt/index.md).
- Required: No
- Default value: Enabled

## METRIC

- Description: Enables metric reporting in Janssen Server. Metric data can be used to monitor and report on Authorization Server activity, including user activity, issued tokens, health checks, and audit information. For details, see the [Reporting and Metrics documentation](https://docs.jans.io/nightly/janssen-server/auth-server/reporting-metrics/index.md).
- Required: No
- Default value: Enabled

## PAR

- Description: Enables OAuth 2.0 Pushed Authorization Requests (PAR) in Janssen Server. For more details, see the [PAR endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/par/index.md).
- Required: No
- Default value: Enabled

## RATE_LIMIT

- Description: Enables request rate limiting in the Janssen Authorization Server. For more details, see the [Rate Limit Configuration](https://docs.jans.io/nightly/janssen-server/config-guide/auth-server-config/rate-limit/index.md).
- Required: No
- Default value: Enabled

## REGISTRATION

- Description: Enables the Client Registration endpoint in Janssen Server. For more details, see the [Client Registration endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/client-registration/index.md).
- Required: No
- Default value: Enabled

## REVOKE_TOKEN

- Description: Enables the OAuth 2.0 Token Revocation endpoint in Janssen Server. For more details, see the [Token Revocation endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/token-revocation/index.md).
- Required: No
- Default value: Enabled

## SPIFFE_CLIENT_AUTH

- Description: Enable/Disable SPIFFE-based client authentication (X.509-SVID mutual-TLS and JWT-SVID assertion), per draft-ietf-oauth-spiffe-client-auth
- Required: No
- Default value: Disabled

## SSA

- Description: Enables the Software Statement Assertion (SSA) endpoint in Janssen Server. For more details, see the [SSA endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/ssa/index.md).
- Required: No
- Default value: Enabled

## STAT

- Description: Enables the Authorization Server Statistic service, which provides statistical data such as monthly active users and issued-token information. For more details, see the [Statistic endpoint](https://docs.jans.io/nightly/janssen-server/auth-server/reporting-metrics/#statistic-endpoint).
- Required: No
- Default value: Enabled

## STATUS_LIST

- Description: Enables the Token Status List endpoint in Janssen Server, which enables the client to query token status. For more details, see the [Logout Status JWT documentation](https://docs.jans.io/nightly/janssen-server/auth-server/tokens/logout-status-jwt/index.md).
- Required: No
- Default value: Enabled

## STATUS_SESSION

- Description: Enables the session status check endpoint in Janssen Server, which allows an application to check the current status of an authenticated user session.
- Required: No
- Default value: Enabled

## U2F

- Description: Enables support for FIDO U2F in Janssen Server, allowing applications to use legacy U2F authenticators for registration and authentication. For more details, see the [FIDO Administration Guide](https://docs.jans.io/nightly/contribute/implementation-design/jans-fido2-design/index.md).
- Required: No
- Default value: Disabled

## UMA

- Description: Enables User-Managed Access (UMA) support in Janssen Server. For more details, see the [UMA documentation](https://docs.jans.io/nightly/janssen-server/auth-server/uma-features/index.md).
- Required: No
- Default value: Disabled

## USERINFO

- Description: Enables the OpenID Connect UserInfo endpoint in Janssen Server. For more details, see the [UserInfo endpoint documentation](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/userinfo/index.md).
- Required: No
- Default value: Enabled
