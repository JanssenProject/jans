---
tags:
  - administration
  - client
  - schema
---

# Client Schema

## Overview

The Client Schema defines the metadata associated with an OAuth 2.0 or OpenID Connect client registered in Janssen Server. This metadata describes the client's identity, authentication methods, redirect URIs, supported grant types, cryptographic settings, logout behavior, and other configuration used during client authentication and authorization.

Janssen Server supports standard client metadata defined by OpenID Connect, OAuth, and related specifications, and extends the client schema with additional Janssen-specific properties.

This page documents the metadata supported by Janssen Server. For the complete OpenID Connect metadata specification, refer to the [OpenID Connect Dynamic Client Registration specification](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata).

## Client Metadata Supported by Janssen Server

Janssen Server supports client metadata defined by the OpenID Connect and OAuth specifications and extends the standard client metadata model with additional Janssen-specific properties.

The following table lists the client properties supported by Janssen Server and identifies whether each property is defined by a relevant specification or is specific to Janssen Server.

| Janssen Property | Description | Specification / Origin |
|------------------|-------------|------------------------|
| `clientId` | Unique identifier assigned to the client. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#RegistrationResponse) as `client_id`. |
| `clientSecret` | Secret used by confidential clients for authentication. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#RegistrationResponse) as `client_secret`. |
| `redirectUris` | Registered callback URIs used during authorization. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `redirect_uris`. |
| `responseTypes` | OAuth 2.0 response types supported by the client. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `response_types`. |
| `grantTypes` | OAuth 2.0 grant types allowed for the client. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `grant_types`. |
| `applicationType` | Specifies whether the client is a `web` or `native` application. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `application_type`. |
| `contacts` | Contact email addresses for the client administrators. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `contacts`. |
| `clientName` | Human-readable name of the client application. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `client_name`. |
| `logoUri` | URI pointing to the client application's logo. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `logo_uri`. |
| `clientUri` | URI of the client application's home page. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `client_uri`. |
| `policyUri` | URI of the client's privacy policy. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `policy_uri`. |
| `tosUri` | URI of the client's terms of service. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `tos_uri`. |
| `jwksUri` | URI of the client's JSON Web Key Set (JWKS). | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `jwks_uri`. |
| `jwks` | JSON Web Key Set provided directly in the client metadata. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `jwks`. |
| `sectorIdentifierUri` | URI used when calculating pairwise subject identifiers. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `sector_identifier_uri`. |
| `subjectType` | Subject identifier type (`public` or `pairwise`). | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `subject_type`. |
| `idTokenSignedResponseAlg` | Signing algorithm used for ID Tokens. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `id_token_signed_response_alg`. |
| `idTokenEncryptedResponseAlg` | Encryption algorithm used for ID Tokens. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `id_token_encrypted_response_alg`. |
| `idTokenEncryptedResponseEnc` | Content encryption algorithm used for ID Tokens. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `id_token_encrypted_response_enc`. |
| `userInfoSignedResponseAlg` | Signing algorithm used for UserInfo responses. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `userinfo_signed_response_alg`. |
| `userInfoEncryptedResponseAlg` | Encryption algorithm used for UserInfo responses. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `userinfo_encrypted_response_alg`. |
| `userInfoEncryptedResponseEnc` | Content encryption algorithm used for UserInfo responses. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `userinfo_encrypted_response_enc`. |
| `requestObjectSigningAlg` | Signing algorithm required for Request Objects. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `request_object_signing_alg`. |
| `requestObjectEncryptionAlg` | Encryption algorithm used for Request Objects. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `request_object_encryption_alg`. |
| `requestObjectEncryptionEnc` | Content encryption algorithm for Request Objects. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `request_object_encryption_enc`. |
| `tokenEndpointAuthMethod` | Authentication method used at the token endpoint. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `token_endpoint_auth_method`. |
| `tokenEndpointAuthSigningAlg` | Signing algorithm used for client authentication JWTs. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `token_endpoint_auth_signing_alg`. |
| `defaultMaxAge` | Maximum authentication age requested by the client. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `default_max_age`. |
| `defaultAcrValues` | Default Authentication Context Class Reference (ACR) values. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `default_acr_values`. |
| `initiateLoginUri` | URI used to initiate login from a third party. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `initiate_login_uri`. |
| `requestUris` | Pre-registered request object URIs. | Defined in [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) as `request_uris`. |
| `postLogoutRedirectUris` | Redirect URIs used after logout. | Defined in [OpenID Connect RP-Initiated Logout 1.0](https://openid.net/specs/openid-connect-rpinitiated-1_0.html#ClientMetadata) as `post_logout_redirect_uris`. |
| `clientIdIssuedAt` | Timestamp indicating when the client identifier was issued. | Defined in [OAuth 2.0 Dynamic Client Registration Protocol (RFC 7591)](https://www.rfc-editor.org/rfc/rfc7591.html) as `client_id_issued_at`. |
| `clientSecretExpiresAt` | Timestamp indicating when the client secret expires. A value of `0` indicates that the client secret never expires. | Defined in [OAuth 2.0 Dynamic Client Registration Protocol (RFC 7591)](https://www.rfc-editor.org/rfc/rfc7591.html) as `client_secret_expires_at`. |
| `registrationAccessToken` | Access token used to manage dynamically registered clients. | Defined in  [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#RegistrationResponse) as `registration_access_token`. |
| `scopes` | Specifies the scopes available to the client. | Defined in [OAuth 2.0 Dynamic Client Registration Protocol (RFC 7591)](https://datatracker.ietf.org/doc/html/rfc7591#section-2) as `scope`. |
| `frontChannelLogoutUri` | Front-channel logout endpoint for the client. | Defined in [OpenID Connect Front-Channel Logout 1.0](https://openid.net/specs/openid-connect-frontchannel-1_0.html#RPLogout) as `frontchannel_logout_uri`. |
| `frontChannelLogoutSessionRequired` | Indicates whether session information is included during front-channel logout. | Defined in [OpenID Connect Front-Channel Logout 1.0](https://openid.net/specs/openid-connect-frontchannel-1_0.html#RPLogout) as `frontchannel_logout_session_required`. |
| `softwareId` | Software identifier associated with the client. | Defined in [OAuth 2.0 Dynamic Client Registration Protocol (RFC 7591)](https://datatracker.ietf.org/doc/html/rfc7591#section-2) as `software_id`. |
| `softwareVersion` | Version of the registered software. | Defined in [OAuth 2.0 Dynamic Client Registration Protocol (RFC 7591)](https://datatracker.ietf.org/doc/html/rfc7591#section-2) as `software_version`. |
| `softwareStatement` | Software statement presented during client registration. | Defined in [OAuth 2.0 Dynamic Client Registration Protocol (RFC 7591)](https://datatracker.ietf.org/doc/html/rfc7591#section-2) as `software_statement`. |
| `backchannelTokenDeliveryMode` | Token delivery mode used for CIBA. | Defined in [OpenID Connect Client-Initiated Backchannel Authentication (CIBA) Core 1.0](https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#registration) as `backchannel_token_delivery_mode`. |
| `backchannelClientNotificationEndpoint` | Notification endpoint used for CIBA. | Defined in [OpenID Connect Client-Initiated Backchannel Authentication (CIBA) Core 1.0](https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#registration) as `backchannel_client_notification_endpoint`. |
| `backchannelAuthenticationRequestSigningAlg` | Signing algorithm used for CIBA authentication requests. | Defined in [OpenID Connect Client-Initiated Backchannel Authentication (CIBA) Core 1.0](https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#registration) as `backchannel_authentication_request_signing_alg`. |
| `backchannelUserCodeParameter` | Indicates whether a user code is required for CIBA authentication. | Defined in [OpenID Connect Client-Initiated Backchannel Authentication (CIBA) Core 1.0](https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#registration) as `backchannel_user_code_parameter`. |
| `idTokenTokenBindingCnf` | Stores Token Binding confirmation information associated with the client. | Defined in [OpenID Connect Token Bound Authentication 1.0](https://openid.net/specs/openid-connect-token-bound-authentication-1_0-ID1.html#RPMetadata) as `id_token_token_binding_cnf`. |
| `clientNameLocalized` | Stores localized values for the client display name. | Defined by [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#LanguagesAndScripts) through the `#` language-tag syntax for `client_name`. |
| `logoUriLocalized` | Stores localized logo URIs for different languages. | Defined by [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#LanguagesAndScripts) through the `#` language-tag syntax for `logo_uri`. |
| `clientUriLocalized` | Stores localized client home page URIs. | Defined by [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#LanguagesAndScripts) through the `#` language-tag syntax for `client_uri`. |
| `policyUriLocalized` | Stores localized privacy policy URIs. | Defined by [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#LanguagesAndScripts) through the `#` language-tag syntax for `policy_uri`. |
| `tosUriLocalized` | Stores localized Terms of Service URIs. | Defined by [OpenID Connect Dynamic Client Registration 1.0](https://openid.net/specs/openid-connect-registration-1_0.html#LanguagesAndScripts) through the `#` language-tag syntax for `tos_uri`. |
| `claimRedirectUris` | Redirect URIs used for UMA claims gathering. | Janssen-specific custom client property. |
| `claims` | Specifies claims associated with the client configuration in Janssen Server. | Janssen-specific custom client property. |
| `trustedClient` | Indicates whether the client is trusted by the authorization server. | Janssen-specific custom client property. |
| `persistClientAuthorizations` | Persists user authorization decisions for subsequent requests. | Janssen-specific custom client property. |
| `includeClaimsInIdToken` | Includes configured claims in issued ID Tokens. | Janssen-specific custom client property. |
| `accessTokenLifetime` | Overrides the default access token lifetime for the client. | Janssen-specific custom client property. |
| `refreshTokenLifetime` | Overrides the default refresh token lifetime for the client. | Janssen-specific custom client property. |
| `accessTokenAsJwt` | Issues access tokens as JWTs. | Janssen-specific custom client property. |
| `accessTokenSigningAlg` | Signing algorithm used for JWT access tokens. | Janssen-specific custom client property. |
| `rptAsJwt` | Issues Requesting Party Tokens (RPTs) as JWTs. | Janssen-specific custom client property. |
| `authorizedOrigins` | Defines the allowed origins for browser-based requests. | Janssen-specific custom client property. |
| `customAttributes` | Defines additional custom attributes for the client, with each attribute identified by a name and associated value or values. | Janssen-specific custom client property. |
| `attributes` | Stores additional structured Janssen-specific client configuration, including authentication, token, authorization, scripting, and other client settings. | Janssen-specific custom client property. |
| `groups` | Associates the client with one or more administrative groups. | Janssen-specific custom client property. |
| `organization` | Organization associated with the client. | Janssen-specific custom client property. |
| `description` | Human-readable description of the client. | Janssen-specific custom client property. |
| `disabled` | Enables or disables the client without deleting it. | Janssen-specific custom client property. |
| `ttl` | Time-to-live value used for client object expiration. | Janssen-specific custom client property. |
| `lastAccessTime` | Records the timestamp of the client's most recent access. | Janssen-specific custom client property. |
| `lastLogonTime` | Records the timestamp of the client's most recent successful authentication. | Janssen-specific custom client property. |
| `customObjectClasses` | Specifies additional LDAP object classes associated with the client entry. | Janssen-specific custom client property. |

## Update Client Metadata

### Using Jans TUI

Client metadata can be viewed and updated using the Jans Text User Interface (TUI). The TUI provides an interactive interface for managing registered clients, including adding, modifying, and removing client metadata properties.

For detailed instructions on navigating the TUI and managing client metadata, refer to the [Using Text-based UI](../../config-guide/auth-server-config/openid-connect-client-configuration.md#using-text-based-ui).