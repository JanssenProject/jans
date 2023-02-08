package jans

import (
	"context"
	"fmt"
)

// OidcClientAttribute is the definition of the attributes of an OpenId Connect Client.
type OidcClientAttribute struct {
	TLSClientAuthSubjectDn                  string   `schema:"tls_client_auth_subject_dn" json:"tlsClientAuthSubjectDn,omitempty"`
	RunIntrospectionScriptBeforeJwtCreation bool     `schema:"run_introspection_script_before_jwt_creation" json:"runIntrospectionScriptBeforeJwtCreation,omitempty"`
	KeepClientAuthorizationAfterExpiration  bool     `schema:"keep_client_authorization_after_expiration" json:"keepClientAuthorizationAfterExpiration,omitempty"`
	AllowSpontaneousScopes                  bool     `schema:"allow_spontaneous_scopes" json:"allowSpontaneousScopes,omitempty"`
	SpontaneousScopes                       []string `schema:"spontaneous_scopes" json:"spontaneousScopes,omitempty"`
	SpontaneousScopeScriptDns               []string `schema:"spontaneous_scope_script_dns" json:"spontaneousScopeScriptDns,omitempty"`
	UpdateTokenScriptDns                    []string `schema:"update_token_script_dns" json:"updateTokenScriptDns,omitempty"`
	BackchannelLogoutUri                    []string `schema:"backchannel_logout_uri" json:"backchannelLogoutUri,omitempty"`
	BackchannelLogoutSessionRequired        bool     `schema:"backchannel_logout_session_required" json:"backchannelLogoutSessionRequired,omitempty"`
	AdditionalAudience                      []string `schema:"additional_audience" json:"additionalAudience,omitempty"`
	PostAuthnScripts                        []string `schema:"post_authn_scripts" json:"postAuthnScripts,omitempty"`
	ConsentGatheringScripts                 []string `schema:"consent_gathering_scripts" json:"consentGatheringScripts,omitempty"`
	IntrospectionScripts                    []string `schema:"introspection_scripts" json:"introspectionScripts,omitempty"`
	RptClaimsScripts                        []string `schema:"rpt_claims_scripts" json:"rptClaimsScripts,omitempty"`
	RopcScripts                             []string `schema:"ropc_scripts" json:"ropcScripts,omitempty"`
	ParLifetime                             int      `schema:"par_lifetime" json:"parLifetime,omitempty"`
	RequirePar                              bool     `schema:"require_par" json:"requirePar,omitempty"`
	JansAuthSignedRespAlg                   string   `schema:"jans_auth_signed_resp_alg" json:"jansAuthSignedRespAlg,omitempty"`
	JansAuthEncRespAlg                      string   `schema:"jans_auth_enc_resp_alg" json:"jansAuthEncRespAlg,omitempty"`
	JansAuthEncRespEnc                      string   `schema:"jans_auth_enc_resp_enc" json:"jansAuthEncRespEnc,omitempty"`
	JansSubAttr                             string   `schema:"jans_sub_attr" json:"jansSubAttr,omitempty"`
	RedirectUrisRegex                       string   `schema:"redirect_uris_regex" json:"redirectUrisRegex,omitempty"`
	JansAuthorizedAcr                       []string `schema:"jans_authorized_acr" json:"jansAuthorizedAcr,omitempty"`
	JansDefaultPromptLogin                  bool     `schema:"jans_default_prompt_login" json:"jansDefaultPromptLogin,omitempty"`
}

// OidcClient is the definition of an OpenId Connect Client.
type OidcClient struct {
	Dn                                         string               `schema:"dn" json:"dn,omitempty"`
	Inum                                       string               `schema:"inum" json:"inum,omitempty"`
	ClientSecret                               string               `schema:"client_secret" json:"clientSecret,omitempty"`
	FrontChannelLogoutUri                      string               `schema:"front_channel_logout_uri" json:"frontChannelLogoutUri,omitempty"`
	FrontChannelLogoutSessionRequired          bool                 `schema:"front_channel_logout_session_required" json:"frontChannelLogoutSessionRequired,omitempty"`
	RegistrationAccessToken                    string               `schema:"registration_access_token" json:"registrationAccessToken,omitempty"`
	ClientIdIssuedAt                           string               `schema:"client_id_issued_at" json:"clientIdIssuedAt,omitempty"`
	ClientSecretExpiresAt                      string               `schema:"client_secret_expires_at" json:"clientSecretExpiresAt,omitempty"`
	RedirectUris                               []string             `schema:"redirect_uris" json:"redirectUris,omitempty"`
	ClaimRedirectUris                          []string             `schema:"claim_redirect_uris" json:"claimRedirectUris,omitempty"`
	ResponseTypes                              []string             `schema:"response_types" json:"responseTypes,omitempty"`
	GrantTypes                                 []string             `schema:"grant_types" json:"grantTypes,omitempty"`
	ApplicationType                            string               `schema:"application_type" json:"applicationType,omitempty"`
	Contacts                                   []string             `schema:"contacts" json:"contacts,omitempty"`
	IdTokenTokenBindingCnf                     string               `schema:"id_token_token_binding_cnf" json:"idTokenTokenBindingCnf,omitempty"`
	ClientName                                 OptionalString       `schema:"client_name" json:"clientName,omitempty"`
	LogoUri                                    OptionalString       `schema:"logo_uri" json:"logoUri,omitempty"`
	ClientUri                                  OptionalString       `schema:"client_uri" json:"clientUri,omitempty"`
	PolicyUri                                  OptionalString       `schema:"policy_uri" json:"policyUri,omitempty"`
	TosUri                                     OptionalString       `schema:"tos_uri" json:"tosUri,omitempty"`
	JwksUri                                    string               `schema:"jwks_uri" json:"jwksUri,omitempty"`
	Jwks                                       string               `schema:"jwks" json:"jwks,omitempty"`
	SectorIdentifierUri                        string               `schema:"sector_identifier_uri" json:"sectorIdentifierUri,omitempty"`
	SubjectType                                string               `schema:"subject_type" json:"subjectType,omitempty"`
	IdTokenSignedResponseAlg                   string               `schema:"id_token_signed_response_alg" json:"idTokenSignedResponseAlg,omitempty"`
	IdTokenEncryptedResponseAlg                string               `schema:"id_token_encrypted_response_alg" json:"idTokenEncryptedResponseAlg,omitempty"`
	IdTokenEncryptedResponseEnc                string               `schema:"id_token_encrypted_response_enc" json:"idTokenEncryptedResponseEnc,omitempty"`
	UserInfoSignedResponseAlg                  string               `schema:"user_info_signed_response_alg" json:"userInfoSignedResponseAlg,omitempty"`
	UserInfoEncryptedResponseAlg               string               `schema:"user_info_encrypted_response_alg" json:"userInfoEncryptedResponseAlg,omitempty"`
	UserInfoEncryptedResponseEnc               string               `schema:"user_info_encrypted_response_enc" json:"userInfoEncryptedResponseEnc,omitempty"`
	RequestObjectSigningAlg                    string               `schema:"request_object_signing_alg" json:"requestObjectSigningAlg,omitempty"`
	RequestObjectEncryptionAlg                 string               `schema:"request_object_encryption_alg" json:"requestObjectEncryptionAlg,omitempty"`
	RequestObjectEncryptionEnc                 string               `schema:"request_object_encryption_enc" json:"requestObjectEncryptionEnc,omitempty"`
	TokenEndpointAuthMethod                    string               `schema:"token_endpoint_auth_method" json:"tokenEndpointAuthMethod,omitempty"`
	TokenEndpointAuthSigningAlg                string               `schema:"token_endpoint_auth_signing_alg" json:"tokenEndpointAuthSigningAlg,omitempty"`
	DefaultMaxAge                              int                  `schema:"default_max_age" json:"defaultMaxAge,omitempty"`
	RequireAuthTime                            bool                 `schema:"require_auth_time" json:"requireAuthTime,omitempty"`
	DefaultAcrValues                           []string             `schema:"default_acr_values" json:"defaultAcrValues,omitempty"`
	InitiateLoginUri                           string               `schema:"initiate_login_uri" json:"initiateLoginUri,omitempty"`
	PostLogoutRedirectUris                     []string             `schema:"post_logout_redirect_uris" json:"postLogoutRedirectUris,omitempty"`
	RequestUris                                []string             `schema:"request_uris" json:"requestUris,omitempty"`
	Scopes                                     []string             `schema:"scopes" json:"scopes,omitempty"`
	Claims                                     []string             `schema:"claims" json:"claims,omitempty"`
	TrustedClient                              bool                 `schema:"trusted_client" json:"trustedClient,omitempty"`
	LastAccessTime                             int                  `schema:"last_access_time" json:"lastAccessTime,omitempty"`
	LastLogonTime                              int                  `schema:"last_logon_time" json:"lastLogonTime,omitempty"`
	PersistClientAuthorizations                bool                 `schema:"persist_client_authorizations" json:"persistClientAuthorizations,omitempty"`
	IncludeClaimsInIdToken                     bool                 `schema:"include_claims_in_id_token" json:"includeClaimsInIdToken,omitempty"`
	RefreshTokenLifetime                       int                  `schema:"refresh_token_lifetime" json:"refreshTokenLifetime,omitempty"`
	AccessTokenLifetime                        int                  `schema:"access_token_lifetime" json:"accessTokenLifetime,omitempty"`
	CustomAttributes                           []CustomAttribute    `schema:"custom_attributes" json:"customAttributes,omitempty"`
	CustomObjectClasses                        []string             `schema:"custom_object_classes" json:"customObjectClasses,omitempty"`
	RptAsJwt                                   bool                 `schema:"rpt_as_jwt" json:"rptAsJwt,omitempty"`
	AccessTokenAsJwt                           bool                 `schema:"access_token_as_jwt" json:"accessTokenAsJwt,omitempty"`
	AccessTokenSigningAlg                      string               `schema:"access_token_signing_alg" json:"accessTokenSigningAlg,omitempty"`
	Disabled                                   bool                 `schema:"disabled" json:"disabled,omitempty"`
	AuthorizedOrigins                          []string             `schema:"authorized_origins" json:"authorizedOrigins,omitempty"`
	SoftwareID                                 string               `schema:"software_id" json:"softwareId,omitempty"`
	SoftwareVersion                            string               `schema:"software_version" json:"softwareVersion,omitempty"`
	SoftwareStatement                          string               `schema:"software_statement" json:"softwareStatement,omitempty"`
	Attributes                                 *OidcClientAttribute `schema:"attributes" json:"attributes,omitempty"`
	BackchannelTokenDeliveryMode               string               `schema:"backchannel_token_delivery_mode" json:"backchannelTokenDeliveryMode,omitempty"`
	BackchannelClientNotificationEndpoint      string               `schema:"backchannel_client_notification_endpoint" json:"backchannelClientNotificationEndpoint,omitempty"`
	BackchannelAuthenticationRequestSigningAlg string               `schema:"backchannel_authentication_request_signing_alg" json:"backchannelAuthenticationRequestSigningAlg,omitempty"`
	BackchannelUserCodeParameter               string               `schema:"backchannel_user_code_parameter" json:"backchannelUserCodeParameter,omitempty"`
	ExpirationDate                             string               `schema:"expiration_date" json:"expirationDate,omitempty"`
	Deletable                                  bool                 `schema:"deletable" json:"deletable,omitempty"`
	JansID                                     string               `schema:"jans_id" json:"jansId,omitempty"`
	Description                                string               `schema:"description" json:"description,omitempty"`
	AuthenticationMethod                       string               `schema:"authentication_method" json:"authenticationMethod,omitempty"`
	TokenBindingSupported                      bool                 `schema:"token_binding_supported" json:"tokenBindingSupported,omitempty"`
	BaseDn                                     string               `schema:"base_dn" json:"baseDn,omitempty"`
}

// GetOidcClients returns all currently configured OIDC clients.
func (c *Client) GetOidcClients(ctx context.Context) ([]OidcClient, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/openid/clients.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	type response struct {
		Clients []OidcClient `json:"entries"`
	}
	ret := response{}

	if err := c.get(ctx, "/jans-config-api/api/v1/openid/clients", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret.Clients, nil
}

// GetOidcClient returns the client with the given inum.
func (c *Client) GetOidcClient(ctx context.Context, inum string) (*OidcClient, error) {

	if inum == "" {
		return nil, fmt.Errorf("inum is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/openid/clients.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &OidcClient{}

	if err := c.get(ctx, "/jans-config-api/api/v1/openid/clients/"+inum, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateOidcClient creates a new OIDC client.
func (c *Client) CreateOidcClient(ctx context.Context, client *OidcClient) (*OidcClient, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/openid/clients.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &OidcClient{}

	if err := c.post(ctx, "/jans-config-api/api/v1/openid/clients", token, client, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// // UpdateOidcClient updates an already existing OIDC client.
func (c *Client) UpdateOidcClient(ctx context.Context, client *OidcClient) (*OidcClient, error) {

	if client == nil {
		return nil, fmt.Errorf("client is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/openid/clients.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	// we can either use PUT on /clients or PATCH on /clients/{inum}

	ret := &OidcClient{}

	if err := c.put(ctx, "/jans-config-api/api/v1/openid/clients", token, client, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteOidcClient deletes an already existing OIDC client.
func (c *Client) DeleteOidcClient(ctx context.Context, inum string) error {

	if inum == "" {
		return fmt.Errorf("inum is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/openid/clients.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/api/v1/openid/clients/"+inum, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
