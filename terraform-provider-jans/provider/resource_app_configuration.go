package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceAuthenticationFilter() *schema.Resource {
	return &schema.Resource{
		Schema: map[string]*schema.Schema{
			"filter": {
				Type:     schema.TypeString,
				Required: true,
			},
			"bind": {
				Type:     schema.TypeBool,
				Optional: true,
			},
			"bind_password_attribute": {
				Type:     schema.TypeString,
				Optional: true,
			},
			"base_dn": {
				Type:     schema.TypeString,
				Optional: true,
			},
		},
	}
}

func resourceAppConfiguration() *schema.Resource {

	return &schema.Resource{
		Description: "Resource for managing all global application configurations for the Janssen Server. This resource cannot " +
			"be created or deleted, only imported and updated.",
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceAppConfigurationRead,
		UpdateContext: resourceAppConfigurationUpdate,
		DeleteContext: resourceUntrackOnDelete,
		Schema: map[string]*schema.Schema{
			"issuer": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL using the https scheme that OP asserts as Issuer identifier. Example: https://server.example.com/",
				ValidateDiagFunc: validateURL,
			},
			"base_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "The base URL for endpoints. Example: https://server.example.com/restv1",
				ValidateDiagFunc: validateURL,
			},
			"authorization_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "The authorization endpoint URL. Example: https://server.example.com/restv1/authorize",
				ValidateDiagFunc: validateURL,
			},
			"token_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "The token endpoint URL. Example: https://server.example.com/restv1/token",
				ValidateDiagFunc: validateURL,
			},
			"token_revocation_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "The URL for the access_token or refresh_token revocation endpoint. Example: https://server.example.com/restv1/revoke",
				ValidateDiagFunc: validateURL,
			},
			"userinfo_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "The User Info endpoint URL. Example: https://server.example.com/restv1/userinfo",
				ValidateDiagFunc: validateURL,
			},
			"client_info_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "The Client Info endpoint URL. Example: https://server.example.com/restv1/clientinfo",
				ValidateDiagFunc: validateURL,
			},
			"check_session_iframe": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL for an OP IFrame that supports cross-origin communications for session state information with the RP Client using the HTML5 postMessage API. Example: https://server.example.com/opiframe.htm",
				ValidateDiagFunc: validateURL,
			},
			"end_session_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL at the OP to which an RP can perform a redirect to request that the end user be logged out at the OP. Example: https://server.example.com/restv1/end_session",
				ValidateDiagFunc: validateURL,
			},
			"jwks_uri": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL of the OP's JSON Web Key Set (JWK) document. This contains the signing key(s) the RP uses to validate signatures from the OP. Example: https://server.example.com/restv1/jwks",
				ValidateDiagFunc: validateURL,
			},
			"registration_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL of the Registration Endpoint. Example: https://server.example.com/restv1/register",
				ValidateDiagFunc: validateURL,
			},
			"openid_discovery_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL of the OpenID Discovery Endpoint. Example: https://server.example.com/.well-known/webfinger",
				ValidateDiagFunc: validateURL,
			},
			"openid_configuration_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL for the Open ID Connect Configuration Endpoint. Example: https://server.example.com/.well-known/openid-configuration",
				ValidateDiagFunc: validateURL,
			},
			"id_generation_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL for the ID Generation Endpoint. Example: https://server.example.com/restv1/id",
				ValidateDiagFunc: validateURL,
			},
			"introspection_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL for the Introspection Endpoint. Example: https://server.example.com/restv1/introspection",
				ValidateDiagFunc: validateURL,
			},
			"par_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL for Pushed Authorisation Request (PAR) Endpoint. Example: https://server.example.com/jans-auth/restv1/par",
				ValidateDiagFunc: validateURL,
			},
			"require_par": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value to indicate if Pushed Authorisation Request(PAR) is required",
			},
			"mtls_authorization_endpoint": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `URL for Mutual TLS Client Authentication and Certificate-Bound Access Tokens (MTLS) Endpoint.
							Example: 'https://server.example.com/jans-auth/restv1/mtls'`,
				ValidateDiagFunc: validateURL,
			},
			"mtls_token_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      `URL for MTLS Authorization token Endpoint. Example: 'https://server.example.com/jans-auth/restv1/mtls/token'`,
				ValidateDiagFunc: validateURL,
			},
			"mtls_token_revocation_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      `URL for MTLS Authorization token revocation endpoint. Example: 'https://server.example.com/jans-auth/restv1/mtls/revoke'`,
				ValidateDiagFunc: validateURL,
			},
			"mtls_user_info_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      `URL for MTLS User Info endpoint. Example: 'https://server.example.com/jans-auth/restv1/mtls/userinfo'`,
				ValidateDiagFunc: validateURL,
			},
			"mtls_client_info_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      `URL for MTLS Client Info endpoint. Example: 'https://server.example.com/jans-auth/restv1/mtls/clientinfo'`,
				ValidateDiagFunc: validateURL,
			},
			"mtls_check_session_iframe": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `URL for MTLS IFrame that supports cross-origin communications for session state information with the RP 
							Client using the HTML5 postMessage API. Example: 'https://server.example.com/jans-auth/restv1/mtls/opiframe.htm'`,
				ValidateDiagFunc: validateURL,
			},
			"mtls_end_session_endpoint": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `URL for MTLS to which an RP can perform a redirect to request that the end user be logged out at the OP.
           		Example: 'https://server.example.com/jans-auth/restv1/mtls/end_session'`,
				ValidateDiagFunc: validateURL,
			},
			"mtls_jwks_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: `URL for MTLS of the OP\'s JSON Web Key Set (JWK) document. Example: 'https://server.example.com/jans-auth/restv1/mtls/jwks'`,
			},
			"mtls_registration_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      `URL for MTLS Registration endpoint. Example: 'https://server.example.com/jans-auth/restv1/mtls/register'`,
				ValidateDiagFunc: validateURL,
			},
			"mtls_id_generation_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      `URL for MTLS Id generation endpoint. Example: 'https://server.example.com/jans-auth/restv1/mtls/id'`,
				ValidateDiagFunc: validateURL,
			},
			"mtls_introspection_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      `URL for MTLS Introspection endpoint. Example: 'https://server.example.com/jans-auth/restv1/mtls/introspection'`,
				ValidateDiagFunc: validateURL,
			},
			"mtls_par_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      `URL for MTLS Pushed Authorisation Request (PAR) endpoint. Example: 'https://server.example.com/jans-auth/restv1/mtls/par'`,
				ValidateDiagFunc: validateURL,
			},
			"mtls_device_authz_endpoint": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: `URL for MTLS Device Authorization endpoint. Example: 'https://server.example.com/jans-auth/restv1/mtls/device_authorization'`,
			},
			"device_authz_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL for the Device Authorization Endpoint. Example: https://server.example.com/restv1/device_authorization",
				ValidateDiagFunc: validateURL,
			},
			"session_as_jwt": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value true saves session data as a JWT.",
			},
			"require_request_object_encryption": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value true encrypts request object",
			},
			"require_pkce": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value true check for Proof Key for Code Exchange (PKCE).",
			},
			"allow_all_value_for_revoke_endpoint": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value true allow all value for revoke endpoint.",
			},
			"sector_identifier_cache_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The cache lifetime in minutes of the sector identifier.",
			},
			"uma_configuration_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL for the UMA Configuration Endpoint. Example: https://server.example.com/restv1/uma2-configuration",
				ValidateDiagFunc: validateURL,
			},
			"uma_rpt_as_jwt": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Issue RPT as JWT or as random string.",
			},
			"uma_rpt_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "UMA RPT lifetime.",
			},
			"uma_ticket_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "UMA Ticket lifetime.",
			},
			"uma_pct_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "UMA PCT lifetime.",
			},
			"uma_resource_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "UMA Resource lifetime.",
			},
			"uma_add_scopes_automatically": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Add scopes automatically.",
			},
			"uma_validate_claim_token": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Validate claim_token as id_token assuming it is issued by local idp.",
			},
			"uma_grant_access_if_no_policies": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies whether to grant access to resources if there are no any policies associated with scopes.",
			},
			"uma_restrict_resource_to_associated_client": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Restrict access to resource by associated client.",
			},
			"openid_sub_attribute": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Specifies which LDAP attribute is used for the subject identifier claim. Example: inum",
			},
			"public_subject_identifier_per_client_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies whether public subject identifier is allowed per client.",
			},
			"subject_identifiers_per_client_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of the subject identifiers supported per client.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"mail", "uid"}

						return validateEnum(v, enums)
					},
				},
			},
			"response_types_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: `A list of the OAuth 2.0 response_type values that this OP supports. One of "code", "token", "id_token".`,
				Elem: &schema.Schema{
					Type: schema.TypeList,
					Elem: &schema.Schema{
						Type: schema.TypeString,
						ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

							enums := []string{"code", "token", "id_token"}

							return validateEnum(v, enums)
						},
					},
				},
			},
			"response_modes_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: `A list of the OAuth 2.0 Response Mode values that this OP supports. One of "query¡¡", "fragment", "form_post".`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"query¡¡", "fragment", "form_post"}

						return validateEnum(v, enums)
					},
				},
			},
			"grant_types_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of the OAuth 2.0 Grant Type values that this OP supports. One of "authorization_code",
							"implicit", "password", "client_credentials", "refresh_token", "urn:ietf:params:oauth:grant-type:uma-ticket",
							"urn:openid:params:grant-type:ciba".`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"authorization_code", "implicit", "password",
							"client_credentials", "refresh_token",
							"urn:ietf:params:oauth:grant-type:uma-ticket",
							"urn:openid:params:grant-type:ciba",
						}

						return validateEnum(v, enums)
					},
				},
			},
			"subject_types_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of the Subject Identifier types that this OP supports. Valid types include pairwise and public.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"pairwise", "public"}

						return validateEnum(v, enums)
					},
				},
			},
			"default_subject_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Default Subject Type used for Dynamic Client Registration. Valid types include pairwise and public.",
			},
			"authorization_signing_alg_values_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of the authorization signing algorithms supported.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						return validateEnum(v, signingAlgs)
					},
				},
			},
			"authorization_encryption_alg_values_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of the authorization encryption algorithms supported.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						return validateEnum(v, encryptionAlgs)
					},
				},
			},
			"authorization_encryption_enc_values_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of the authorization encryption algorithms supported.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						return validateEnum(v, encryptionEnc)
					},
				},
			},
			"user_info_signing_alg_values_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of the JWS signing algorithms (alg values) JWA supported by the UserInfo Endpoint to encode 
								the Claims in a JWT. One of "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", 
								"ES512", "PS256", "PS384", "PS512".`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						return validateEnum(v, signingAlgs)
					},
				},
			},
			"user_info_encryption_alg_values_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of the JWE encryption algorithms (alg values) JWA supported by the UserInfo 
							Endpoint to encode the Claims in a JWT. One of "RSA1_5", "RSA-OAEP", "A128KW", "A256KW"`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						return validateEnum(v, encryptionAlgs)
					},
				},
			},
			"user_info_encryption_enc_values_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of the JWE encryption algorithms (enc values) JWA supported by the UserInfo Endpoint 
							to encode the Claims in a JWT. One of "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM".`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						return validateEnum(v, encryptionEnc)
					},
				},
			},

			"id_token_signing_alg_values_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of the JWS signing algorithms (alg values) supported by the OP for the ID 
							Token to encode the Claims in a JWT. One of "none", "HS256", "HS384", "HS512", "RS256", 
							"RS384", "RS512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512"`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						enums := []string{"none"}
						enums = append(enums, signingAlgs...)
						return validateEnum(v, enums)
					},
				},
			},
			"id_token_encryption_alg_values_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of the JWE encryption algorithms (alg values) supported by the OP 
							for the ID Token to encode the Claims in a JWT. One of "RSA1_5", "RSA-OAEP", "A128KW", "A256KW".`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						return validateEnum(v, encryptionAlgs)
					},
				},
			},
			"id_token_encryption_enc_values_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of the JWE encryption algorithms (enc values) supported by the OP for 
							the ID Token to encode the Claims in a JWT. One of "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM".`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						return validateEnum(v, encryptionEnc)
					},
				},
			},
			"force_signed_request_object": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value true indicates that signed request object is mandatory.",
			},
			"request_object_signing_alg_values_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of the JWS signing algorithms (alg values) supported by the OP for Request Objects. One of "none",
							"HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512".`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						enums := []string{"none"}
						enums = append(enums, signingAlgs...)
						return validateEnum(v, enums)
					},
				},
			},
			"request_object_encryption_alg_values_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of the JWE encryption algorithms (alg values) supported by the OP for Request Objects. 
							One of "RSA1_5", "RSA-OAEP", "A128KW", "A256KW".`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						return validateEnum(v, encryptionAlgs)
					},
				},
			},
			"request_object_encryption_enc_values_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of the JWE encryption algorithms (enc values) supported by the OP for Request Objects.
							One of "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM".`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						return validateEnum(v, encryptionEnc)
					},
				},
			},
			"token_endpoint_auth_methods_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of Client Authentication methods supported by this Token Endpoint. One of 'client_secret_basic', 'client_secret_post', 'client_secret_jwt', 'private_key_jwt'.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"client_secret_basic", "client_secret_post", "client_secret_jwt", "private_key_jwt"}

						return validateEnum(v, enums)
					},
				},
			},
			"token_endpoint_auth_signing_alg_values_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of the JWS signing algorithms (alg values) supported by the Token Endpoint for the signature 
							on the JWT used to authenticate the Client at the Token Endpoint for the private_key_jwt and client_secret_jwt 
							authentication methods. One of 'HS256', 'HS384', 'HS512', 'RS256", 'RS384', 'RS512', 'ES256', 'ES384', 'ES512', 'PS256', 'PS384', 'PS512'.`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
						return validateEnum(v, signingAlgs)
					},
				},
			},
			"display_values_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of the display parameter values that the OpenID Provider supports. One of 'page', 'popup'.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"page", "popup"}

						return validateEnum(v, enums)
					},
				},
			},
			"claim_types_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of the Claim Types that the OpenID Provider supports. One of 'normal'",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"normal"}

						return validateEnum(v, enums)
					},
				},
			},
			"jwks_algorithms_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of algorithms that will be used in JWKS endpoint. One of 'RS256', 'RS384', 'RS512', 
							'ES256', 'ES384", 'ES512', 'PS256', 'PS384', 'PS512', 'RSA1_5', 'RSA-OAEP'.`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{
							"RS256", "RS384", "RS512", "ES256",
							"ES384", "ES512", "PS256", "PS384",
							"PS512", "RSA1_5", "RSA-OAEP",
						}

						return validateEnum(v, enums)
					},
				},
			},
			"service_documentation": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `URL of a page containing human-readable information that developers might want or need to know 
							when using the OpenID Provider. Example: http://gluu.org/docs`,
				ValidateDiagFunc: validateURL,
			},
			"claims_locales_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Languages and scripts supported for values in Claims being returned. One of 'en'.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"en"}

						return validateEnum(v, enums)
					},
				},
			},
			"id_token_token_binding_cnf_values_supported": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `Array containing a list of the JWT Confirmation Method member names supported by the OP for Token 
								Binding of ID Tokens. The presence of this parameter indicates that the OpenID Provider supports Token Binding of ID Tokens. 
								If omitted, the default is that the OpenID Provider does not support Token Binding of ID Tokens. One of 'tbh'.`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"tbh"}

						return validateEnum(v, enums)
					},
				},
			},
			"ui_locales_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Languages and scripts supported for the user interface. One of 'en', 'es'.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"en", "es"}

						return validateEnum(v, enums)
					},
				},
			},
			"claims_parameter_supported": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies whether the OP supports use of the claim’s parameter.",
			},
			"request_parameter_supported": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether the OP supports use of the request parameter.",
			},
			"request_uri_parameter_supported": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether the OP supports use of the request_uri parameter.",
			},
			"request_uri_block_list": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Block list for requestUri that can come to Authorization Endpoint (e.g. 'localhost')",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"request_uri_hash_verification_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether the OP supports use of the request_uri hash verification.",
			},
			"require_request_uri_registration": {
				Type:     schema.TypeBool,
				Optional: true,
				Description: `Boolean value specifying whether the OP requires any request_uri values used to be 
								pre-registered using the request_uris registration parameter.`,
			},
			"op_policy_uri": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `URL that the OpenID Provider provides to the person registering the Client to read about the OP's requirements on how the 
								Relying Party can use the data provided by the OP. Example: http://ox.gluu.org/doku.php?id=jans:policy`,
				ValidateDiagFunc: validateURL,
			},
			"op_tos_uri": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `URL that the OpenID Provider provides to the person registering the Client to read about OpenID Provider's terms of 
								service. Example: http://ox.gluu.org/doku.php?id=jans:tos`,
				ValidateDiagFunc: validateURL,
			},
			"authorization_code_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The lifetime of the Authorization Code.",
			},
			"refresh_token_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The lifetime of the Refresh Token.",
			},
			"id_token_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The lifetime of the ID Token. Example: 3600",
			},
			"id_token_filter_claims_based_on_access_token": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether idToken filters claims based on accessToken.",
			},
			"access_token_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The lifetime of the short-lived Access Token. Example: 3600",
			},
			"clean_service_interval": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Time interval for the Clean Service in seconds. Example: 60",
			},
			"clean_service_batch_chunk_size": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Each clean up iteration fetches chunk of expired data per base dn and removes it from storage. Example: 10000",
			},
			"key_regeneration_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to regenerate keys.",
			},
			"key_regeneration_interval": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The interval for key regeneration in hours. Example: 48",
			},
			"default_signature_algorithm": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `The default signature algorithm to sign ID Tokens. One of "HS256", "HS384", "HS512", 
							"RS256", "RS384", "RS512", "ES256", "ES384", "ES512"`,
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512"}

					return validateEnum(v, enums)
				},
			},
			"ox_open_id_connect_version": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "OpenID Connect Version. Example: openidconnect-1.0",
			},
			"ox_id": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "URL for the Inum generator Service. Example: https://server.example.com/oxid/service/jans/inum",
			},
			"trusted_client_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether a client is trusted and no authorization is required.",
			},
			"skip_authorization_for_open_id_scope_and_pairwise_id": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "If a client has only openid scope and pairwise id, person should not have to authorize.",
			},
			"dynamic_registration_expiration_time": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Expiration time in seconds for clients created with dynamic registration, -1 means never expire. Example: -1",
			},
			"dynamic_registration_persist_client_authorizations": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to persist client authorizations.",
			},
			"dynamic_registration_custom_attributes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Custom attributes for the Dynamic registration. One of 'jansTrustedClnt'.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"dynamic_registration_allowed_password_grant_scopes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of grant scopes for dynamic registration.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"dynamic_registration_custom_object_class": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "LDAP custom object class for dynamic registration.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"dynamic_registration_scopes_param_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to enable scopes parameter in dynamic registration.",
			},
			"dynamic_registration_password_grant_type_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to enable Password Grant Type during Dynamic Registration.",
			},
			"persist_id_token_in_ldap": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies whether to persist id_token into LDAP (otherwise saves into cache).",
			},
			"persist_refresh_token_in_ldap": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies whether to persist refresh_token into LDAP (otherwise saves into cache).",
			},
			"allow_post_logout_redirect_without_validation": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Allows post logout redirect without validation for End Session Endpoint.",
			},
			"invalidate_session_cookies_after_authorization_flow": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value to specify whether to invalidate 'session_id' and 'consent_session_id' cookies right after successful or unsuccessful authorization.",
			},
			"return_client_secret_on_read": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether a client_secret is returned on client GET or PUT. False value means not to return secret.",
			},
			"reject_jwt_with_none_alg": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether reject JWT requested or validated with algorithm None.",
			},
			"expiration_notificator_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether expiration notificator is enabled (used to identify expiration for persistence that support TTL, like Couchbase).",
			},
			"use_nested_jwt_during_encryption": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to use nested Jwt during encryption.",
			},
			"expiration_notificator_map_size_limit": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The expiration notificator maximum size limit. Example: 100000",
			},
			"expiration_notificator_interval_in_seconds": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The expiration notificator interval in seconds. Example: 600",
			},
			"consent_gathering_script_backward_compatibility": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether turn on Consent Gathering Script backward compatibility mode. If true AS will pick up script with higher level globally. If false AS will pick up script based on client configuration.",
			},
			"client_authentication_filters_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to enable client authentication filters.",
			},
			"configuration_update_interval": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The interval for configuration update in seconds.",
			},
			"ciba_max_expiration_time_allowed_sec": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Specifies the CIBA token expiration time in seconds.",
			},
			"dpop_signing_alg_values_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Demonstration of Proof-of-Possession (DPoP) authorization signing algorithms supported.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"RS256", "RS384", "RS512", "ES256",
							"ES384", "ES512", "PS256", "PS384", "PS512"}

						return validateEnum(v, enums)
					},
				},
			},
			"dpop_timeframe": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Demonstration of Proof-of-Possession (DPoP) timeout.",
			},
			"dpop_jti_cache_time": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Demonstration of Proof-of-Possession (DPoP) cache time.",
				Default:     3600,
			},
			"allow_id_token_without_implicit_grant_type": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies if a token without implicit grant types is allowed.",
			},
			"error_handling_method": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "A list of possible error handling methods.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"internal", "remote"}

					return validateEnum(v, enums)
				},
			},
			"keep_authenticator_attributes_on_acr_change": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to keep authenticator attributes on ACR change.",
			},
			"client_authentication_filters": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of client authentication filters.",
				Elem:        resourceAuthenticationFilter(),
			},
			"front_channel_logout_session_supported": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value to specify support for front channel logout session.",
			},
			"share_subject_id_between_clients_with_same_sector_id": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Share Subject ID between clients with same Sector ID.",
			},
			"remove_refresh_tokens_for_client_on_logout": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to remove refresh tokens on logout.",
			},
			"backchannel_authentication_response_expires_in": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Backchannel Authentication Response Expires In.",
			},
			"pairwise_id_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The pairwise ID type.",
			},
			"enable_client_grant_type_update": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value to specify if client can update Grant Type values.",
			},
			"dn_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "DN of certificate issuer.",
			},
			"fapi_compatibility": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether turn on FAPI compatibility mode. If true AS behaves in more strict mode.",
			},
			"session_id_persist_on_prompt_none": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to persist session ID on prompt none.",
			},
			"key_store_file": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The Key Store File (JKS). Example: /etc/certs/jans-auth-keys.jks",
			},
			"legacy_id_token_claims": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Include Claims in ID Token.",
			},
			"force_offline_access_scope_to_enable_refresh_token": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether force offline_access scope to enable refresh_token grant type.",
			},
			"refresh_token_extend_lifetime_on_rotation": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to extend refresh tokens on rotation.",
			},
			"device_authz_response_type_to_process_authz": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Response type used to process device authz requests.",
			},
			"logging_layout": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Logging layout used for Jans Authorization Server loggers. - text - json",
			},
			"agama_configuration": {
				Type:        schema.TypeList,
				Optional:    true,
				MaxItems:    1,
				Description: "Engine Config which offers an alternative way to build authentication flows in Janssen server",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"enabled": {
							Type:     schema.TypeBool,
							Optional: true,
						},
						"root_dir": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"templates_path": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"scripts_path": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"serializer_type": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"max_items_logged_in_collections": {
							Type:     schema.TypeInt,
							Optional: true,
						},
						"page_mismatch_error_page": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"interruption_error_page": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"crash_error_page": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"finished_flow_page": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"bridge_script_page": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"default_response_headers": {
							Type:     schema.TypeMap,
							Optional: true,
						},
					},
				},
			},
			"server_session_id_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The sessionId lifetime in seconds for sessionId. By default same as sessionIdLifetime.",
			},
			"active_session_authorization_scope": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Authorization Scope for active session.",
			},
			"redirect_uris_regex_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Enable/Disable redirect uris validation using regular expression.",
			},
			"cors_configuration_filters": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "CORS Configuration filters.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"filter_name": {
							Type:     schema.TypeString,
							Required: true,
						},
						"cors_enabled": {
							Type:     schema.TypeBool,
							Optional: true,
						},
						"cors_allowed_origins": {
							Type:     schema.TypeString,
							Required: true,
						},
						"cors_allowed_methods": {
							Type:     schema.TypeString,
							Required: true,
						},
						"cors_allowed_headers": {
							Type:     schema.TypeString,
							Required: true,
						},
						"cors_support_credentials": {
							Type:     schema.TypeBool,
							Optional: true,
						},
						"cors_logging_enabled": {
							Type:     schema.TypeBool,
							Optional: true,
						},
						"cors_preflight_max_age": {
							Type:     schema.TypeInt,
							Optional: true,
						},
						"cors_request_decorate": {
							Type:     schema.TypeBool,
							Optional: true,
						},
					},
				},
			},
			"backchannel_requests_processor_job_chunk_size": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Each backchannel request processor iteration fetches chunk of data to be processed.",
			},
			"discovery_allowed_keys": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `List of configuration response claim allowed to be displayed in discovery endpoint. Example: authorization_endpoint, 
				token_endpoint, jwks_uri, scopes_supported, response_types_supported, response_modes_supported, etc..`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"discovery_cache_lifetime_in_minutes": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Lifetime of discovery cache.",
			},
			"discovery_deny_keys": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `List of configuration response claims which must not be displayed in discovery endpoint response. 
								Example: id_generation_endpoint, auth_level_mapping, etc.`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"session_id_unused_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The lifetime for unused session states.",
			},
			"backchannel_redirect_uri": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "Backchannel Redirect Uri. Example: https://server.example.com/oxeleven/rest/backchannel/backchannelRedirectUri",
				ValidateDiagFunc: validateURL,
			},
			"backchannel_binding_message_pattern": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Backchannel Binding Message Pattern.",
			},
			"web_keys_storage": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Web Key Storage Type.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"keystore", "pkcs11"}
					return validateEnum(v, enums)
				},
			},
			"key_store_secret": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The password of the Key Store.",
			},
			"authorization_request_custom_allowed_parameters": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Authorization Request Custom Allowed Parameters.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"param_name": {
							Type:     schema.TypeString,
							Required: true,
						},
						"return_in_response": {
							Type:     schema.TypeBool,
							Required: true,
						},
					},
				},
			},
			"backchannel_login_hint_claims": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Backchannel Login Hint Claims.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"ciba_end_user_notification_config": {
				Type:        schema.TypeList,
				Optional:    true,
				MaxItems:    1,
				Description: "CIBA End User Notification Config.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"api_key": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"auth_domain": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"database_url": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"project_id": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"storage_bucket": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"messaging_sender_id": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"app_id": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"notification_url": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"notification_key": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"public_vapid_key": {
							Type:     schema.TypeString,
							Optional: true,
						},
					},
				},
			},
			"backchannel_requests_processor_job_interval_sec": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Specifies the allowable elapsed time in seconds backchannel request processor executes.",
			},
			"session_id_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The lifetime of session id in seconds. If 0 or -1 then expiration is not set. 'session_id' cookie expires when browser session ends.",
			},
			"key_selection_strategy": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Key Selection Strategy.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"OLDER", "NEWER", "FIRST"}

					return validateEnum(v, enums)
				},
			},
			"custom_headers_with_authorization_response": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to enable Custom Response Header parameter to return custom headers with the Authorization Response.",
			},
			"session_id_persist_in_cache": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to persist session_id in cache.",
			},
			"include_sid_in_response": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to include sessionId in response.",
			},
			"dcr_signature_validation_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value enables DCR signature validation. Default is false.",
			},
			"dcr_signature_validation_shared_secret": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Specifies shared secret for Dynamic Client Registration.",
			},
			"dcr_signature_validation_software_statement_jwks_uri_claim": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Specifies claim name inside software statement. Value of claim should point to JWKS URI.",
			},
			"dcr_signature_validation_software_statement_jwks_claim": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Specifies claim name inside software statement. Value of claim should point to inlined JWKS.",
			},
			"dcr_signature_validation_jwks": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Specifies JWKS for all DCR's validations.",
			},
			"dcr_signature_validation_jwks_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Specifies JWKS URI for all DCR's validations.",
			},
			"dcr_authorization_with_client_credentials": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value indicating if DCR authorization to be performed using client credentials.",
			},
			"dcr_authorization_with_mtls": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value indicating if DCR authorization allowed with MTLS.",
			},
			"dcr_issuers": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of DCR issuers.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"change_session_id_on_authentication": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to change session_id on authentication.",
			},
			"force_id_token_hint_precense": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether force id_token_hint parameter presence.",
			},
			"backchannel_device_registration_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "Backchannel Device Registration Endpoint. Example: https://server.example.com/oxeleven/rest/backchannel/backchannelDeviceRegistrationEndpoint",
				ValidateDiagFunc: validateURL,
			},
			"software_statement_validation_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Validation type used for software statement.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"none", "jwks", "jwks_uri", "script"}

					return validateEnum(v, enums)
				},
			},
			"key_sign_with_same_key_but_diff_alg": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies if signing to be done with same key but apply different algorithms.",
			},
			"key_algs_allowed_for_generation": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of algorithm allowed to be used for key generation. Example: 'RS256', 'RS512', 'ES384', 'PS256'",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"RS256", "RS512", "ES384", "PS256"}

						return validateEnum(v, enums)
					},
				},
			},
			"static_kid": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Specifies static Kid",
			},
			"static_decryption_kid": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Specifies static decryption Kid",
			},

			"ox_eleven_verify_signature_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL for the oxEleven Verify Signature Endpoint. Example: https://server.example.com/oxeleven/rest/oxeleven/verifySignature",
				ValidateDiagFunc: validateURL,
			},
			"ox_eleven_delete_key_endpoint": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "URL for the oxEleven Delete Key Endpoint. Example: https://server.example.com/oxeleven/rest/oxeleven/deleteKey",
			},
			"ox_eleven_generate_key_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL for the oxEleven Generate Key Endpoint. Example: https://server.example.com/oxeleven/rest/oxeleven/generateKey",
				ValidateDiagFunc: validateURL,
			},
			"ox_eleven_sign_endpoint": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL for the oxEleven Sign Endpoint. Example: https://server.example.com/oxeleven/rest/oxeleven/sign",
				ValidateDiagFunc: validateURL,
			},
			"ox_eleven_test_mode_token": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "oxEleven Test Mode Token.",
			},
			"logging_level": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Logging level for jans-auth logger.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "false"}

					return validateEnum(v, enums)
				},
			},
			"dynamic_grant_type_default": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `List of the OAuth 2.0 Grant Type values that it's possible to set via client 
							registration API. One of 'none', 'authorization_code', 'implicit', 'password', 'client_credentials', 'refresh_token', 
							'urn:ietf:params:oauth:grant-type:uma-ticket', 'urn:openid:params:grant-type:ciba', 'urn:ietf:params:oauth:grant-type:device_code'.`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"introspection_response_scopes_backward_compatibility": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying introspection response backward compatibility mode.",
			},
			"http_logging_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Enable/Disable request/response logging filter.",
			},
			"http_logging_exclude_paths": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of base URI for which request/response logging filter should not record activity. Example: \"/auth/img\", \"/auth/stylesheet\"",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"external_logger_configuration": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Path to external log4j2 logging configuration. Example: /identity/logviewer/configure",
			},
			"update_client_access_time": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value to specify if application should update oxLastAccessTime/oxLastLogonTime attributes on client authentication.",
			},
			"device_authz_request_expires_in": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Expiration time given for device authorization requests.",
			},
			"authentication_filters_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to enable user authentication filters.",
			},
			"backchannel_token_delivery_modes_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Backchannel Token Delivery Modes Supported.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"use_highest_level_script_if_acr_script_not_found": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Enable/Disable usage of highest level script in case ACR script does not exist.",
			},
			"pairwise_calculation_salt": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Salt to calculate algorithmic pairwise IDs.",
			},
			"end_session_with_access_token": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Accept access token to call end_session endpoint.",
			},
			"client_white_list": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "White List for Client Redirection URIs.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"error_reason_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to return detailed reason of the error from AS..",
			},
			"metric_reporter_keep_data_days": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The days to keep metric reported data.",
			},
			"update_user_last_logon_time": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value to specify if application should update oxLastLogonTime attribute on user authentication.",
			},
			"disable_u2f_endpoint": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Enable/Disable U2F endpoints.",
			},
			"backchannel_authentication_endpoint": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Backchannel Authentication Endpoint. Example: https://server.example.com/oxeleven/rest/backchannel/backchannelAuthenticationEndpoint()",
			},
			"pairwise_calculation_key": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Key to calculate algorithmic pairwise IDs.",
			},
			"skip_refresh_token_during_refreshing": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to skip refreshing tokens on refreshing.",
			},
			"disable_jdk_logger": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to enable JDK Loggers.",
			},
			"metric_reporter_interval": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The interval for metric reporter in seconds.",
			},
			"use_local_cache": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to enable local in-memory cache.",
			},
			"introspection_script_backward_compatibility": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether switch off client's introspection scripts (true value) and run all scripts that exists on server.",
			},
			"authentication_protection_configuration": {
				Type:        schema.TypeList,
				Optional:    true,
				MaxItems:    1,
				Description: "Authentication Brute Force Protection Configuration.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"attempt_expiration": {
							Type:     schema.TypeInt,
							Optional: true,
						},
						"maximum_allowed_attempts_without_delay": {
							Type:     schema.TypeInt,
							Optional: true,
						},
						"delay_time": {
							Type:     schema.TypeInt,
							Optional: true,
						},
						"brute_force_protection_enabled": {
							Type:     schema.TypeBool,
							Optional: true,
						},
					},
				},
			},
			"introspection_access_token_must_have_uma_protection_scope": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Reject introspection requests if access_token in Authorization header does not have uma_protection scope.",
			},
			"introspection_skip_authorization": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies if authorization to be skipped for introspection.",
			},
			"openid_scope_backward_compatibility": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Set to false to only allow token endpoint request for openid scope with grant type equals to authorization_code, restrict access to userinfo to scope openid and only return id_token if scope contains openid.",
			},
			"device_authz_token_poll_interval": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Default interval returned to the client to process device token requests.",
			},
			"backchannel_user_code_parameter_supported": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Backchannel User Code Parameter Supported",
			},
			"check_user_presence_on_refresh_token": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Check whether user exists and is active before creating RefreshToken. Set it to true if check is needed (Default value is false - don't check.)",
			},
			"log_client_id_on_client_authentication": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value to specify if application should log the Client ID on client authentication.",
			},
			"client_reg_default_to_code_flow_with_refresh": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to add Authorization Code Flow with Refresh grant during client registration.",
			},
			"grant_types_and_response_types_autofix_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to Grant types and Response types can be auto fixed.",
			},
			"authentication_filters": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of authentication filters.",
				Elem:        resourceAuthenticationFilter(),
			},
			"client_black_list": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Black List for Client Redirection URIs.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"log_client_name_on_client_authentication": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value to specify if application should log the Client Name on client authentication.",
			},
			"backchannel_authentication_response_interval": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Backchannel Authentication Response Interval.",
			},
			"ciba_grant_life_extra_time_sec": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Specifies the CIBA Grant life extra time in seconds.",
			},
			"session_id_unauthenticated_unused_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The lifetime for unused unauthenticated session states.",
			},
			"stat_timer_interval_in_seconds": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Statistical data capture time interval.",
			},
			"stat_authorization_scope": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Scope required for Statistical Authorization.",
			},
			"allow_spontaneous_scopes": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies whether to allow spontaneous scopes.",
			},
			"spontaneous_scope_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The lifetime of spontaneous scope in seconds.",
			},
			"session_id_request_parameter_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to enable session_id HTTP request parameter.",
			},
			"enabled_components": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of auth components enabled Example: HEALTH_CHECK, USERINFO, CLIENTINFO, ID_GENERATION, REGISTRATION, INTROSPECTION, etc..",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"person_custom_object_class_list": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "LDAP custom object class list for dynamic person enrolment. One of 'gluuCustomPerson', 'gluuPerson'.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"stat_web_service_interval_limit_in_seconds": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Statistical data capture time interval limit.",
			},
			"img_location": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The location for image files.",
			},
			"software_statement_validation_claim_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Validation claim name for software statement.",
			},
			"jms_broker_uri_set": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "JMS Broker URI Set.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"js_location": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The location for JavaScript files.",
			},
			"css_location": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The location for CSS files.",
			},
			"backchannel_authentication_request_signing_alg_values_supported": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Backchannel Authentication Request Signing Alg Values Supported.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"backchannel_client_id": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Backchannel Client Id.",
			},
			"enabled_oauth_audit_logging": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "enabled OAuth Audit Logging.",
			},
			"jms_user_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "JMS UserName.",
			},
			"jms_password": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "JMS Password.",
			},
			"cookie_domain": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Sets cookie domain for all cookies created by OP.",
			},
		},
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
	}

}

func resourceAppConfigurationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	appConfig, err := c.GetAppConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, appConfig); err != nil {
		return diag.FromErr(err)
	}

	d.SetId("jans_app_configuration")

	return diags
}

func resourceAppConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var appConfig jans.AppConfiguration
	if err := fromSchemaResource(d, &appConfig); err != nil {
		return diag.FromErr(err)
	}

	if _, err := c.UpdateAppConfiguration(ctx, &appConfig); err != nil {
		return diag.FromErr(err)
	}

	return resourceAppConfigurationRead(ctx, d, meta)
}
