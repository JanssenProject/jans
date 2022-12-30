---
tags:
  - administration
  - auth-server
  - endpoint
---

## OpenID Configuration Endpoint aka `.well-known/openid-configuration`

The Configuration Endpoint returns both the OP server metadata, and OAuth
AS metdata, most of which
is defined in the [OpenID Discovery Spec](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata), although other configuration metadata is defined
in other OpenID specifications, or in OAuth specifications.

If you want to customize the configuration response, you can use the
[OpenID Config Interception Script](../../developer/scripts/config/openid-config.md),
which enables you to filter the results, modify claim values, or add claims.

If you want to explicitly allow only certain OpenID Metadata claims, you can
supply a list of the claims in the `opConfigMetadataAllowList` Auth Server
Property.

Below is a list of all the current available claims, and where they are specified.

| Claim | Origin |
| ----- | -----:|
|  access_token_signing_alg_values_supported | ? |
|  acr_values_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  authorization_encryption_alg_values_supported | ? |
|  authorization_encryption_enc_values_supported | ? |
|  authorization_endpoint | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  authorization_signing_alg_values_supported | ? |
|  backchannel_authentication_endpoint | ? |
|  backchannel_logout_session_supported | ? |
|  backchannel_logout_supported | ? |
|  backchannel_token_delivery_modes_supported | ? |
|  backchannel_user_code_parameter_supported | ? |
|  check_session_iframe | ? |
|  claim_types_supported | ? |
|  claims_locales_supported | ? |
|  claims_parameter_supported | ? |
|  claims_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  clientinfo_endpoint | ? |
|  device_authorization_endpoint | ? |
|  display_values_supported | ? |
|  dpop_signing_alg_values_supported | ? |
|  end_session_endpoint | ? |
|  frontchannel_logout_session_supported | ? |
|  frontchannel_logout_supported | ? |
|  grant_types_supported | ? |
|  id_token_encryption_alg_values_supported | ? |
|  id_token_encryption_enc_values_supported | ? |
|  id_token_signing_alg_values_supported | ? |
|  id_token_token_binding_cnf_values_supported | ? |
|  introspection_endpoint | ? |
|  issuer | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  jwks_uri | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  op_tos_uri | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  pushed_authorization_request_endpoint | ? |
|  registration_endpoint | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  request_object_encryption_alg_values_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  request_object_encryption_enc_values_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  request_object_signing_alg_values_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  request_parameter_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  request_uri_parameter_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  require_pushed_authorization_requests | ? |
|  require_request_uri_registration | ? |
|  response_modes_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  response_types_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  revocation_endpoint | ? |
|  scopes_supported | ? |
|  service_documentation | ? |
|  session_revocation_endpoint | ? |
|  ssa_endpoint | Janssen |
|  subject_types_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  tls_client_certificate_bound_access_tokens | ? |
|  token_endpoint | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  token_endpoint_auth_methods_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  token_endpoint_auth_signing_alg_values_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  ui_locales_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  userinfo_encryption_alg_values_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  userinfo_encryption_enc_values_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  userinfo_endpoint | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |
|  userinfo_signing_alg_values_supported | [OpenID](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata) |

## Notes on specific OP Server Metadata claims

* **claims_supported** Each user claim (in Jans jargon, "Attribute") has a property called `jansHideOnDiscovery`--if you don't want a claim to appear in `.well-known/openid-configuration`, set this to `true` for the Attribute entity.

* **ssa_endpoint** This is the endpoint which issues Software Statement
Assertions JWT's. It is an OAuth protected endpoint.
