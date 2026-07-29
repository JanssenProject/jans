---
tags:
  - administration
  - client
  - schema
---

# Client Schema

## Overview

The Client Schema defines the metadata associated with an OAuth 2.0/OpenID Connect client registered in Janssen Server. This metadata describes the client's identity, authentication methods, redirect URIs, supported grant types, cryptographic settings, logout behavior, and other configuration used during client authentication and authorization.

Janssen Server implements the standard OpenID Connect Dynamic Client Registration metadata and extends it with additional properties that support Jans-specific features. 

This page documents the metadata supported by Janssen Server. For the complete OpenID Connect metadata specification, refer to the [OpenID Connect Dynamic Client Registration specification](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata).

## OpenID Connect Client Metadata Supported by Janssen Server

Janssen Server supports the core client metadata defined by the OpenID Connect Dynamic Client Registration specification. These metadata fields are used when registering and managing OAuth 2.0/OpenID Connect clients and are persisted as part of the client object.

The following table maps the standard OpenID Connect client metadata to the corresponding Janssen Server client properties.

| OpenID Metadata | Janssen Property | Description |
|-----------------|---------------|-------------|
| `client_id` | `clientId` | Unique identifier assigned to the client. |
| `client_secret` | `clientSecret` | Secret used by confidential clients for authentication. |
| `redirect_uris` | `redirectUris` | Registered callback URIs used during authorization. |
| `response_types` | `responseTypes` | OAuth 2.0 response types supported by the client. |
| `grant_types` | `grantTypes` | OAuth 2.0 grant types allowed for the client. |
| `application_type` | `applicationType` | Specifies whether the client is a `web` or `native` application. |
| `contacts` | `contacts` | Contact email addresses for the client administrators. |
| `client_name` | `clientName` | Human-readable name of the client application. |
| `logo_uri` | `logoUri` | URI pointing to the client application's logo. |
| `client_uri` | `clientUri` | URI of the client application's home page. |
| `policy_uri` | `policyUri` | URI of the client's privacy policy. |
| `tos_uri` | `tosUri` | URI of the client's terms of service. |
| `jwks_uri` | `jwksUri` | URI of the client's JSON Web Key Set (JWKS). |
| `jwks` | `jwks` | JSON Web Key Set provided directly in the client metadata. |
| `sector_identifier_uri` | `sectorIdentifierUri` | URI used when calculating pairwise subject identifiers. |
| `subject_type` | `subjectType` | Subject identifier type (`public` or `pairwise`). |
| `id_token_signed_response_alg` | `idTokenSignedResponseAlg` | Signing algorithm used for ID Tokens. |
| `id_token_encrypted_response_alg` | `idTokenEncryptedResponseAlg` | Encryption algorithm used for ID Tokens. |
| `id_token_encrypted_response_enc` | `idTokenEncryptedResponseEnc` | Content encryption algorithm used for ID Tokens. |
| `userinfo_signed_response_alg` | `userInfoSignedResponseAlg` | Signing algorithm used for UserInfo responses. |
| `userinfo_encrypted_response_alg` | `userInfoEncryptedResponseAlg` | Encryption algorithm used for UserInfo responses. |
| `userinfo_encrypted_response_enc` | `userInfoEncryptedResponseEnc` | Content encryption algorithm used for UserInfo responses. |
| `request_object_signing_alg` | `requestObjectSigningAlg` | Signing algorithm required for Request Objects. |
| `request_object_encryption_alg` | `requestObjectEncryptionAlg` | Encryption algorithm used for Request Objects. |
| `request_object_encryption_enc` | `requestObjectEncryptionEnc` | Content encryption algorithm for Request Objects. |
| `token_endpoint_auth_method` | `tokenEndpointAuthMethod` | Authentication method used at the token endpoint. |
| `token_endpoint_auth_signing_alg` | `tokenEndpointAuthSigningAlg` | Signing algorithm used for client authentication JWTs. |
| `default_max_age` | `defaultMaxAge` | Maximum authentication age requested by the client. |
| `default_acr_values` | `defaultAcrValues` | Default Authentication Context Class Reference (ACR) values. |
| `initiate_login_uri` | `initiateLoginUri` | URI used to initiate login from a third party. |
| `request_uris` | `requestUris` | Pre-registered request object URIs. |
| `post_logout_redirect_uris` | `postLogoutRedirectUris` | Redirect URIs used after logout. |
| `client_id_issued_at` | `clientIdIssuedAt` | Timestamp indicating when the client identifier was issued. |
| `client_secret_expires_at` | `clientSecretExpiresAt` | Timestamp indicating when the client secret expires. A value of `0` indicates that the client secret never expires. |

## Janssen Server-specific Client Metadata

In addition to the standard OpenID Connect client metadata, Janssen Server defines several client properties that support operational, security, and administrative capabilities beyond the OpenID Connect specification.

| Property | Purpose |
|----------|---------|
| `claimRedirectUris` | Redirect URIs used for UMA claims gathering. |
| `registrationAccessToken` | Access token used to manage dynamically registered clients. |
| `scopes` | Specifies the scopes available to the client. |
| `claims` | Associates claims with the client configuration. |
| `trustedClient` | Indicates whether the client is trusted by the authorization server. |
| `persistClientAuthorizations` | Persists user authorization decisions for subsequent requests. |
| `includeClaimsInIdToken` | Includes configured claims in issued ID Tokens. |
| `accessTokenLifetime` | Overrides the default access token lifetime for the client. |
| `refreshTokenLifetime` | Overrides the default refresh token lifetime for the client. |
| `accessTokenAsJwt` | Issues access tokens as JWTs. |
| `accessTokenSigningAlg` | Signing algorithm used for JWT access tokens. |
| `rptAsJwt` | Issues Requesting Party Tokens (RPTs) as JWTs. |
| `authorizedOrigins` | Defines the allowed origins for browser-based requests. |
| `customAttributes` | Stores administrator-defined client attributes. |
| `attributes` | Stores additional Janssen-specific client configuration. |
| `frontChannelLogoutUri` | Front-channel logout endpoint for the client. |
| `frontChannelLogoutSessionRequired` | Indicates whether session information is included during front-channel logout. |
| `softwareId` | Software identifier associated with the client. |
| `softwareVersion` | Version of the registered software. |
| `softwareStatement` | Software statement presented during client registration. |
| `backchannelTokenDeliveryMode` | Token delivery mode used for CIBA. |
| `backchannelClientNotificationEndpoint` | Notification endpoint used for CIBA. |
| `backchannelAuthenticationRequestSigningAlg` | Signing algorithm used for CIBA authentication requests. |
| `backchannelUserCodeParameter` | Indicates whether a user code is required for CIBA authentication. |
| `groups` | Associates the client with one or more administrative groups. |
| `organization` | Organization associated with the client. |
| `description` | Human-readable description of the client. |
| `disabled` | Enables or disables the client without deleting it. |
| `ttl` | Time-to-live value used for client object expiration. |
| `idTokenTokenBindingCnf` | Stores Token Binding confirmation information associated with the client. |
| `clientNameLocalized` | Stores localized values for the client display name. |
| `logoUriLocalized` | Stores localized logo URIs for different languages. |
| `clientUriLocalized` | Stores localized client home page URIs. |
| `policyUriLocalized` | Stores localized privacy policy URIs. |
| `tosUriLocalized` | Stores localized Terms of Service URIs. |
| `lastAccessTime` | Records the timestamp of the client's most recent access. |
| `lastLogonTime` | Records the timestamp of the client's most recent successful authentication. |
| `customObjectClasses` | Specifies additional LDAP object classes associated with the client entry. |

These properties extend the standard OpenID Connect client metadata model and provide capabilities that are specific to Janssen Server.

## Update Client Metadata

### Using Jans TUI

Client metadata can be viewed and updated using the Jans Text User Interface (TUI). The TUI provides an interactive interface for managing registered clients, including adding, modifying, and removing client metadata properties.

For detailed instructions on navigating the TUI and managing client metadata, refer to the [Using Text-based UI](https://docs.jans.io/v2.2.0/janssen-server/config-guide/auth-server-config/openid-connect-client-configuration/#using-text-based-ui)
