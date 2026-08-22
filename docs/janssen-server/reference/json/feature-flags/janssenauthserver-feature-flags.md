---
tags:
- administration
- reference
- json
- feature-flags
---

# Janssen Auth Server Feature Flags

| Feature Flag Name | Description |  | 
|-----|-----|-----|
| ACCESS_EVALUATION | Enable/Disable Access Evaluation Endpoint | [Details](#access_evaluation) |
| ACTIVE_SESSION | Enable/Disable active session endpoint | [Details](#active_session) |
| CIBA | Enable/Disable OpenID Connect Client Initiated Backchannel Authentication Flow(CIBA) flow support | [Details](#ciba) |
| CLIENT_ID_METADATA_DOCUMENT | Enable/Disable OAuth Client ID Metadata Document support (URL-based client_id) | [Details](#client_id_metadata_document) |
| CLIENTINFO | Enable/Disable client info endpoint | [Details](#clientinfo) |
| DEVICE_AUTHZ | Enable/Disable support for device authorization | [Details](#device_authz) |
| END_SESSION | Enable/Disable end session endpoint | [Details](#end_session) |
| GLOBAL_TOKEN_REVOCATION | Enable/Disable global token revocation endpoint | [Details](#global_token_revocation) |
| HEALTH_CHECK | Enable/Disable health-check endpoint | [Details](#health_check) |
| ID_GENERATION | Enable/Disable ID Generation endpoint | [Details](#id_generation) |
| IDENTITY_ASSERTION_AUTHZ_GRANT | Enable/Disable Identity Assertion Authorization Grant (Cross-App Access / ID-JAG) support | [Details](#identity_assertion_authz_grant) |
| INTROSPECTION | Enable/Disable token introspection endpoint | [Details](#introspection) |
| JANS_CONFIGURATION | Enable/Disable *.well-known* configuration endpoint | [Details](#jans_configuration) |
| LOGOUT_STATUS_JWT | Enable/Disable logout status jwt | [Details](#logout_status_jwt) |
| METRIC | Enable/Disable metric reporter feature | [Details](#metric) |
| PAR | Enable/Disable Pushed Authorization Requests(PAR) feature | [Details](#par) |
| RATE_LIMIT | Enable/Disable Rate Limit | [Details](#rate_limit) |
| REGISTRATION | Enable/Disable client registration endpoint | [Details](#registration) |
| REVOKE_TOKEN | Enable/Disable token revocation endpoint | [Details](#revoke_token) |
| SPIFFE_CLIENT_AUTH | Enable/Disable SPIFFE-based client authentication (X.509-SVID mutual-TLS and JWT-SVID assertion), per draft-ietf-oauth-spiffe-client-auth | [Details](#spiffe_client_auth) |
| SSA | Enable/Disable Software Statement Assertion(SSA) feature | [Details](#ssa) |
| STAT | Enable/Disable Stat service | [Details](#stat) |
| STATUS_LIST | Enable/Disable status list endpoint | [Details](#status_list) |
| STATUS_SESSION | Enable/Disable session status check endpoint | [Details](#status_session) |
| U2F | Enable/Disable support for Universal 2nd Factor(U2F) protocol | [Details](#u2f) |
| UMA | Enable/Disable support for User-Managed Access (UMA) | [Details](#uma) |
| USERINFO | Enable/Disable OpenID Connect [userinfo endpoint](https://openid.net/specs/openid-connect-core-1_0.html#UserInfo) | [Details](#userinfo) |


## ACCESS_EVALUATION

- Description: Enable/Disable Access Evaluation Endpoint

- Required: No

- Default value: Enabled


## ACTIVE_SESSION

- Description: Enable/Disable active session endpoint

- Required: No

- Default value: Enabled


## CIBA

- Description: Enable/Disable OpenID Connect Client Initiated Backchannel Authentication Flow(CIBA) flow support

- Required: No

- Default value: Enabled


## CLIENT_ID_METADATA_DOCUMENT

- Description: Enable/Disable OAuth Client ID Metadata Document support (URL-based client_id)

- Required: No

- Default value: Disabled


## CLIENTINFO

- Description: Enable/Disable client info endpoint

- Required: No

- Default value: Enabled


## DEVICE_AUTHZ

- Description: Enable/Disable support for device authorization

- Required: No

- Default value: Enabled


## END_SESSION

- Description: Enable/Disable end session endpoint

- Required: No

- Default value: Enabled


## GLOBAL_TOKEN_REVOCATION

- Description: Enable/Disable global token revocation endpoint

- Required: No

- Default value: Enabled


## HEALTH_CHECK

- Description: Enable/Disable health-check endpoint

- Required: No

- Default value: Enabled


## ID_GENERATION

- Description: Enable/Disable ID Generation endpoint

- Required: No

- Default value: Enabled


## IDENTITY_ASSERTION_AUTHZ_GRANT

- Description: Enable/Disable Identity Assertion Authorization Grant (Cross-App Access / ID-JAG) support

- Required: No

- Default value: Disabled


## INTROSPECTION

- Description: Enable/Disable token introspection endpoint

- Required: No

- Default value: Enabled


## JANS_CONFIGURATION

- Description: Enable/Disable *.well-known* configuration endpoint

- Required: No

- Default value: Enabled


## LOGOUT_STATUS_JWT

- Description: Enable/Disable logout status jwt

- Required: No

- Default value: Enabled


## METRIC

- Description: Enable/Disable metric reporter feature

- Required: No

- Default value: Enabled


## PAR

- Description: Enable/Disable Pushed Authorization Requests(PAR) feature

- Required: No

- Default value: Enabled


## RATE_LIMIT

- Description: Enable/Disable Rate Limit

- Required: No

- Default value: Enabled


## REGISTRATION

- Description: Enable/Disable client registration endpoint

- Required: No

- Default value: Enabled


## REVOKE_TOKEN

- Description: Enable/Disable token revocation endpoint

- Required: No

- Default value: Enabled


## SPIFFE_CLIENT_AUTH

- Description: Enable/Disable SPIFFE-based client authentication (X.509-SVID mutual-TLS and JWT-SVID assertion), per draft-ietf-oauth-spiffe-client-auth

- Required: No

- Default value: Disabled


## SSA

- Description: Enable/Disable Software Statement Assertion(SSA) feature

- Required: No

- Default value: Enabled


## STAT

- Description: Enable/Disable Stat service

- Required: No

- Default value: Enabled


## STATUS_LIST

- Description: Enable/Disable status list endpoint

- Required: No

- Default value: Enabled


## STATUS_SESSION

- Description: Enable/Disable session status check endpoint

- Required: No

- Default value: Enabled


## U2F

- Description: Enable/Disable support for Universal 2nd Factor(U2F) protocol

- Required: No

- Default value: Disabled


## UMA

- Description: Enable/Disable support for User-Managed Access (UMA)

- Required: No

- Default value: Disabled


## USERINFO

- Description: Enable/Disable OpenID Connect [userinfo endpoint](https://openid.net/specs/openid-connect-core-1_0.html#UserInfo)

- Required: No

- Default value: Enabled


