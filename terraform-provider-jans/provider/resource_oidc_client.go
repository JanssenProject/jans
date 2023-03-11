package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceOidcClient() *schema.Resource {

	return &schema.Resource{
		CreateContext: resourceOidcClientCreate,
		ReadContext:   resourceOidcClientRead,
		UpdateContext: resourceOidcClientUpdate,
		DeleteContext: resourceOidcClientDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "",
			},
			"inum": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "XRI i-number. Client Identifier to uniquely identify the client.",
			},
			"client_secret": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The client secret.  The client MAY omit the parameter if the client secret is an empty string.",
			},
			"front_channel_logout_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"front_channel_logout_session_required": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "",
			},
			"registration_access_token": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"client_id_issued_at": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"client_secret_expires_at": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"redirect_uris": {
				Type:     schema.TypeList,
				Required: true,
				Description: `Redirection URI values used by the Client. One of these registered Redirection URI values must exactly 
						match the redirect_uri parameter value used in each Authorization Request Example: [https://client.example.org/cb]`,
				Elem: &schema.Schema{
					Type:             schema.TypeString,
					ValidateDiagFunc: validateURL,
				},
			},
			"claim_redirect_uris": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `Array of The Claims Redirect URIs to which the client wishes the authorization server to direct the 
						requesting party's user agent after completing its interaction.`,
				Elem: &schema.Schema{
					Type:             schema.TypeString,
					ValidateDiagFunc: validateURL,
				},
			},
			"response_types": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `A list of the OAuth 2.0 response_type values that the Client is declaring that it will restrict itself 
						to using. If omitted, the default is that the Client will use only the code Response Type. Allowed values are code, token, id_token.`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"code", "token", "id_token"}
						return validateEnum(v, enums)
					},
				},
			},
			"grant_types": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of the OAuth 2.0 Grant Types that the Client is declaring that it will restrict itself to using.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{
							"authorization_code",
							"implicit",
							"password",
							"client_credentials",
							"refresh_token",
							"urn:ietf:params:oauth:grant-type:uma-ticket",
							"urn:openid:params:grant-type:ciba",
							"urn:ietf:params:oauth:grant-type:device_code",
						}
						return validateEnum(v, enums)
					},
				},
			},
			"application_type": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `Kind of the application. The default, if omitted, is web. The defined values are native or web. Web Clients 
						using the OAuth Implicit Grant Type must only register URLs using the HTTPS scheme as redirect_uris, they must not use 
						localhost as the hostname. Native Clients must only register redirect_uris using custom URI schemes or URLs using the
						http scheme with localhost as the hostname.`,
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"web", "native"}
					return validateEnum(v, enums)
				},
			},
			"contacts": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "e-mail addresses of people responsible for this Client.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"id_token_token_binding_cnf": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `Specifies the JWT Confirmation Method member name (e.g. tbh) that the Relying Party expects when receiving 
						Token Bound ID Tokens. The presence of this parameter indicates that the Relying Party supports Token Binding of ID 
						Tokens. If omitted, the default is that the Relying Party does not support Token Binding of ID Tokens.`,
			},
			"client_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"logo_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "URL that references a logo for the Client application.",
			},
			"client_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "URL of the home page of the Client. The value of this field must point to a valid Web page.",
			},
			"policy_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "URL that the Relying Party Client provides to the End-User to read about the how the profile data will be used.",
			},
			"tos_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms of service.",
			},
			"jwks_uri": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `URL for the Client's JSON Web Key Set (JWK) document containing key(s) that are used for signing requests to the OP. 
							The JWK Set may also contain the Client's encryption keys(s) that are used by the OP to encrypt the responses to the Client. 
							When both signing and encryption keys are made available, a use (Key Use) parameter value is required for all keys in the 
							document to indicate each key's intended usage.`,
			},
			"jwks": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `List of JSON Web Key (JWK) - A JSON object that represents a cryptographic key. The members of the object 
							represent properties of the key, including its value. Example: { "keys" : [ { "e" : "AQAB", "n" : "gmlDX_mgMcHX.." ] }`,
			},
			"sector_identifier_uri": {
				Type:             schema.TypeString,
				Optional:         true,
				Description:      "URL using the https scheme to be used in calculating Pseudonymous Identifiers by the OP.",
				ValidateDiagFunc: validateURL,
			},
			"subject_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Subject type requested for the Client ID. Valid types include pairwise and public.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"pairwise", "public"}
					return validateEnum(v, enums)
				},
			},
			"id_token_signed_response_alg": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "JWS alg algorithm (JWA) required for signing the ID Token issued to this Client.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					return validateEnum(v, signingAlgs)
				},
			},
			"id_token_encrypted_response_alg": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "JWE alg algorithm (JWA) required for encrypting the ID Token issued to this Client.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					return validateEnum(v, encryptionAlgs)
				},
			},
			"id_token_encrypted_response_enc": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "JWE enc algorithm (JWA) required for encrypting the ID Token issued to this Client.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					return validateEnum(v, encryptionEnc)
				},
			},
			"user_info_signed_response_alg": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "JWS alg algorithm (JWA) required for signing UserInfo Responses.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					return validateEnum(v, signingAlgs)
				},
			},
			"user_info_encrypted_response_alg": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "JWE alg algorithm (JWA) required for encrypting UserInfo Responses.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					return validateEnum(v, encryptionAlgs)
				},
			},
			"user_info_encrypted_response_enc": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "JWE enc algorithm (JWA) required for encrypting UserInfo Responses.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					return validateEnum(v, encryptionEnc)
				},
			},
			"request_object_signing_alg": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "JWS alg algorithm (JWA) that must be used for signing Request Objects sent to the OP.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					return validateEnum(v, signingAlgs)
				},
			},
			"request_object_encryption_alg": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "JWE alg algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects sent to the OP.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					return validateEnum(v, encryptionAlgs)
				},
			},
			"request_object_encryption_enc": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "JWE enc algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects sent to the OP.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					return validateEnum(v, encryptionEnc)
				},
			},
			"token_endpoint_auth_method": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Requested Client Authentication method for the Token Endpoint.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"client_secret_basic", "client_secret_post", "client_secret_jwt", "private_key_jwt", "tls_client_auth", "none"}
					return validateEnum(v, enums)
				},
			},
			"token_endpoint_auth_signing_alg": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `JWS alg algorithm (JWA) that must be used for signing the JWT used to authenticate the Client at the Token Endpoint 
							for the private_key_jwt and client_secret_jwt authentication methods.`,
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					return validateEnum(v, signingAlgs)
				},
			},
			"default_max_age": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Specifies the Default Maximum Authentication Age. Example: 1000000",
			},
			"require_auth_time": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether the auth_time Claim in the ID Token is required. It is required when the value is true.",
			},
			"default_acr_values": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `Array of default requested Authentication Context Class Reference values that the Authorization Server 
						must use for processing requests from the Client.`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"initiate_login_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Specifies the URI using the https scheme that the authorization server can call to initiate a login at the client.",
			},
			"post_logout_redirect_uris": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `Provide the URLs supplied by the RP to request that the user be redirected to this location after a logout has 
						been performed. Example: [https://client.example.org/logout/page1 https://client.example.org/logout/page2 https://client.example.org/logout/page3]`,
				Elem: &schema.Schema{
					Type:             schema.TypeString,
					ValidateDiagFunc: validateURL,
				},
			},
			"request_uris": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Provide a list of requests_uri values that are pre-registered by the Client for use at the Authorization Server.",
				Elem: &schema.Schema{
					Type:             schema.TypeString,
					ValidateDiagFunc: validateURL,
				},
			},
			"scopes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Provide list of scopes granted to the client (scope dn or scope id). Example: [read write dolphin]",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"claims": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Provide list of claims granted to the client.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"trusted_client": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: `Attribute which corresponds to the "Pre-Authorization" property. Default value is false.`,
			},
			"last_access_time": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Integer timestamp, measured in the number of seconds since January 1 1970 UTC, indicating last access time.",
			},
			"last_logon_time": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Integer timestamp, measured in the number of seconds since January 1 1970 UTC, indicating last login time.",
			},
			"persist_client_authorizations": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies if the client authorization details are to be persisted. Default value is true.",
			},
			"include_claims_in_id_token": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "If true then claims are included in token id, default value is false.",
			},
			"refresh_token_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Specifies the Client-specific refresh token expiration. Example: 100000000",
			},
			"access_token_lifetime": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Specifies the Client-specific access token expiration. Example: 100000000",
			},
			"custom_attributes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem:        resourceCustomAttribute(),
			},
			"custom_object_classes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"rpt_as_jwt": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies whether RPT should be return as signed JWT.",
			},
			"access_token_as_jwt": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies whether access token as signed JWT.",
			},
			"access_token_signing_alg": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `Specifies signing algorithm that has to be used during JWT signing. If it's not specified, 
						then the default OP signing algorithm will be used.`,
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					return validateEnum(v, signingAlgs)
				},
			},
			"disabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies whether client is disabled.",
			},
			"authorized_origins": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Specifies authorized JavaScript origins.",
				Elem: &schema.Schema{
					Type:             schema.TypeString,
					ValidateDiagFunc: validateURL, // TODO: will it be a valid URL?
				},
			},
			"software_id": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `Specifies a unique identifier string (UUID) assigned by the client developer or software 
							publisher used by registration endpoints to identify the client software to be dynamically 
							registered. Example: 4NRB1-0XZABZI9E6-5SM3R`,
			},
			"software_version": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `Specifies a version identifier string for the client software identified by 'software_id'. 
							The value of the 'software_version' should change on any update to the client software identified 
							by the same 'software_id'. Example: 2.1`,
			},
			"software_statement": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `Specifies a software statement containing client metadata values about the client software as 
							claims. This is a string value containing the entire signed JWT.`,
			},
			"attributes": {
				Type:        schema.TypeList,
				Optional:    true,
				MaxItems:    1,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"tls_client_auth_subject_dn": {
							Type:     schema.TypeString,
							Optional: true,
							Description: `String representation of the expected subject distinguished name of the certificate, which 
									the OAuth client will use in mutual TLS authentication.`,
						},
						"run_introspection_script_before_jwt_creation": {
							Type:     schema.TypeBool,
							Optional: true,
							Description: `boolean property which indicates whether to run introspection script and then include claims 
									from result into access_token as JWT.`,
						},
						"keep_client_authorization_after_expiration": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "boolean property which indicates whether to keep client authorization after expiration.",
						},
						"allow_spontaneous_scopes": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "boolean, whether to allow spontaneous scopes for client.",
						},
						"spontaneous_scopes": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "List of spontaneous scope regular expression.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"spontaneous_scope_script_dns": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "List of spontaneous scope scripts.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"update_token_script_dns": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "List of update token scripts.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"backchannel_logout_uri": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "List of RP URL that will cause the RP to log itself out when sent a Logout Token by the OP.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"backchannel_logout_session_required": {
							Type:     schema.TypeBool,
							Optional: true,
							Description: `Boolean value specifying whether the RP requires that a sid (session ID) Claim be included in 
									the Logout Token to identify the RP session with the OP when true. Default value is false.`,
						},
						"additional_audience": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "List of additional client audience.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"post_authn_scripts": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "List of post authentication scripts.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"consent_gathering_scripts": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "List of consent gathering scripts.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"introspection_scripts": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "List of introspection scripts.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"rpt_claims_scripts": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "List of Requesting Party Token (RPT) claims scripts.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"ropc_scripts": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "List of Resource Owner Password Credentials (ROPC) scripts.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"par_lifetime": {
							Type:        schema.TypeInt,
							Optional:    true,
							Description: "represents the lifetime of Pushed Authorisation Request (PAR).",
						},
						"require_par": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "boolean value to indicate of Pushed Authorisation Request(PAR)is required.",
						},
						"jans_auth_signed_resp_alg": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "JWS alg algorithm JWA required for signing authorization responses.",
						},
						"jans_auth_enc_resp_alg": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "JWE alg algorithm JWA required for encrypting authorization responses.",
						},
						"jans_auth_enc_resp_enc": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "JWE enc algorithm JWA required for encrypting auhtorization responses.",
						},
						"jans_sub_attr": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "custom subject identifier attribute.",
						},
						"redirect_uris_regex": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "If set, redirectUri must match to this regexp",
						},
						"jans_authorized_acr": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "List of thentication Context Class Reference (ACR) that must exist.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"jans_default_prompt_login": {
							Type:     schema.TypeBool,
							Optional: true,
							Description: `sets prompt=login to the authorization request, which causes the authorization server 
									to force the user to sign in again before it will show the authorization prompt.`,
						},
					},
				},
			},
			"backchannel_token_delivery_mode": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "specifies how backchannel token will be delivered.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"poll", "ping", "push"}
					return validateEnum(v, enums)
				},
			},
			"backchannel_client_notification_endpoint": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `Client Initiated Backchannel Authentication (CIBA) enables a Client to initiate the authentication 
							of an end-user by means of out-of-band mechanisms. Upon receipt of the notification, the Client makes a 
							request to the token endpoint to obtain the tokens.`,
				ValidateDiagFunc: validateURL,
			},
			"backchannel_authentication_request_signing_alg": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `The JWS algorithm alg value that the Client will use for signing authentication request, as described 
							in Section 7.1.1. of OAuth 2.0 [RFC6749]. When omitted, the Client will not send signed authentication requests.`,

				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					// TODO: is this intentionally a smaller list?
					enums := []string{"RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512"}
					return validateEnum(v, enums)
				},
			},
			"backchannel_user_code_parameter": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Boolean value specifying whether the Client supports the user_code parameter. If omitted, the default value is false.",
			},
			"expiration_date": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Integer timestamp, measured in the number of seconds since January 1 1970 UTC, indicating when this permission will expire.",
			},
			"deletable": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies whether client is deletable.",
			},
			"jans_id": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Attribute Scope Id.",
			},
			"description": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Description of the client.",
			},
			"authentication_method": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"token_binding_supported": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "",
			},
			"base_dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "",
			},
		},
	}
}

func resourceOidcClientCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var client jans.OidcClient
	if err := fromSchemaResource(d, &client); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new OidcClient")
	newClient, err := c.CreateOidcClient(ctx, &client)
	if err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New OidcClient created", map[string]interface{}{"inum": newClient.Inum})

	d.SetId(newClient.Inum)

	return resourceOidcClientRead(ctx, d, meta)
}

func resourceOidcClientRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	inum := d.Id()
	client, err := c.GetOidcClient(ctx, inum)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, client); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(client.Inum)

	return diags

}

func resourceOidcClientUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var client jans.OidcClient
	if err := fromSchemaResource(d, &client); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Updating OidcClient", map[string]interface{}{"inum": client.Inum})
	if _, err := c.UpdateOidcClient(ctx, &client); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "OidcClient updated", map[string]interface{}{"inum": client.Inum})

	return resourceOidcClientRead(ctx, d, meta)
}

func resourceOidcClientDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	inum := d.Id()
	tflog.Debug(ctx, "Deleting OidcClient", map[string]interface{}{"inum": inum})
	if err := c.DeleteOidcClient(ctx, inum); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "OidcClient deleted", map[string]interface{}{"inum": inum})

	return resourceOidcClientRead(ctx, d, meta)
}
