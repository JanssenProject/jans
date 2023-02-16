package jans

import (
	"context"
	"fmt"
)

// AgamaConfiguration enables an alternative way to build authentication
// flows in the Janssen server.
type AgamaConfiguration struct {
	Enabled                     bool              `schema:"enabled" json:"enabled"`
	RootDir                     string            `schema:"root_dir" json:"rootDir"`
	TemplatesPath               string            `schema:"templates_path" json:"templatesPath"`
	ScriptsPath                 string            `schema:"scripts_path" json:"scriptsPath"`
	SerializerType              string            `schema:"serializer_type" json:"serializerType"`
	MaxItemsLoggedInCollections int               `schema:"max_items_logged_in_collections" json:"maxItemsLoggedInCollections"`
	PageMismatchErrorPage       string            `schema:"page_mismatch_error_page" json:"pageMismatchErrorPage"`
	InterruptionErrorPage       string            `schema:"interruption_error_page" json:"interruptionErrorPage"`
	CrashErrorPage              string            `schema:"crash_error_page" json:"crashErrorPage"`
	FinishedFlowPage            string            `schema:"finished_flow_page" json:"finishedFlowPage"`
	BridgeScriptPage            string            `schema:"bridge_script_page" json:"bridgeScriptPage"`
	DefaultResponseHeaders      map[string]string `schema:"default_response_headers" json:"defaultResponseHeaders"`
}

// CibaEndUserNotificationConfig represents the configuration properties for
// the CIBA end user notification.
type CibaEndUserNotificationConfig struct {
	ApiKey            string `schema:"api_key" json:"apiKey"`
	AuthDomain        string `schema:"auth_domain" json:"authDomain"`
	DatabaseURL       string `schema:"database_url" json:"databaseUrl"`
	ProjectId         string `schema:"project_id" json:"projectId"`
	StorageBucket     string `schema:"storage_bucket" json:"storageBucket"`
	MessagingSenderId string `schema:"messaging_sender_id" json:"messagingSenderId"`
	AppId             string `schema:"app_id" json:"appId"`
	NotificationUrl   string `schema:"notification_url" json:"notificationUrl"`
	NotificationKey   string `schema:"notification_key" json:"notificationKey"`
	PublicVapidKey    string `schema:"public_vapid_key" json:"publicVapidKey"`
}

// CustomAllowedParameter represents a custom parameter that is allowed in
// authorization requests.
type CustomAllowedParameter struct {
	ParamName        string `schema:"param_name" json:"paramName"`
	ReturnInResponse bool   `schema:"return_in_response" json:"returnInResponse"`
}

// AuthenticationFilter represents a single Janssen authentication filter.
type AuthenticationFilter struct {
	Filter                string `schema:"filter" json:"filter"`
	Bind                  bool   `schema:"bind" json:"bind"`
	BaseDn                string `schema:"base_dn" json:"baseDn"`
	BindPasswordAttribute string `schema:"bind_password_attribute" json:"bindPasswordAttribute"`
}

// AuthenticationProtectionConfiguration manages the configruation of the brute
// force protection for the authentication server.
type AuthenticationProtectionConfiguration struct {
	AttemptExpiration                  int  `schema:"attempt_expiration" json:"attemptExpiration"`
	MaximumAllowedAttemptsWithoutDelay int  `schema:"maximum_allowed_attempts_without_delay" json:"maximumAllowedAttemptsWithoutDelay"`
	DelayTime                          int  `schema:"delay_time" json:"delayTime"`
	BruteForceProtectionEnabled        bool `schema:"brute_force_protection_enabled" json:"bruteForceProtectionEnabled"`
}

// CorsConfigurationFilter represents a single Janssen CORS configuration filter.
type CorsConfigurationFilter struct {
	FilterName             string `schema:"filter_name" json:"filterName"`
	CorsEnabled            bool   `schema:"cors_enabled" json:"corsEnabled"`
	CorsAllowedOrigins     string `schema:"cors_allowed_origins" json:"corsAllowedOrigins"`
	CorsAllowedMethods     string `schema:"cors_allowed_methods" json:"corsAllowedMethods"`
	CorsAllowedHeaders     string `schema:"cors_allowed_headers" json:"corsAllowedHeaders"`
	CorsSupportCredentials bool   `schema:"cors_support_credentials" json:"corsSupportCredentials"`
	CorsLoggingEnabled     bool   `schema:"cors_logging_enabled" json:"corsLoggingEnabled"`
	CorsPreflightMaxAge    int    `schema:"cors_preflight_max_age" json:"corsPreflightMaxAge"`
	CorsRequestDecorate    bool   `schema:"cors_request_decorate" json:"corsRequestDecorate"`
}

// AppConfiguration represents the Janssen authorization server
// configuration properties
type AppConfiguration struct {
	Issuer                                                    string                                `schema:"issuer" json:"issuer"`
	BaseEndpoint                                              string                                `schema:"base_endpoint" json:"baseEndpoint"`
	AuthorizationEndpoint                                     string                                `schema:"authorization_endpoint" json:"authorizationEndpoint"`
	TokenEndpoint                                             string                                `schema:"token_endpoint" json:"tokenEndpoint"`
	TokenRevocationEndpoint                                   string                                `schema:"token_revocation_endpoint" json:"tokenRevocationEndpoint"`
	UserInfoEndpoint                                          string                                `schema:"userinfo_endpoint" json:"userInfoEndpoint"`
	ClientInfoEndpoint                                        string                                `schema:"client_info_endpoint" json:"clientInfoEndpoint"`
	CheckSessionIframe                                        string                                `schema:"check_session_iframe" json:"checkSessionIframe"`
	EndSessionEndpoint                                        string                                `schema:"end_session_endpoint" json:"endSessionEndpoint"`
	RegistrationEndpoint                                      string                                `schema:"registration_endpoint" json:"registrationEndpoint"`
	JwksUri                                                   string                                `schema:"jwks_uri" json:"jwksUri"`
	OpenIDDiscoveryEndpoint                                   string                                `schema:"openid_discovery_endpoint" json:"openIdDiscoveryEndpoint"`
	OpenIDConfigurationEndpoint                               string                                `schema:"openid_configuration_endpoint" json:"openIdConfigurationEndpoint"`
	IDGenerationEndpoint                                      string                                `schema:"id_generation_endpoint" json:"idGenerationEndpoint"`
	IntrospectionEndpoint                                     string                                `schema:"introspection_endpoint" json:"introspectionEndpoint"`
	ParEndpoint                                               string                                `schema:"par_endpoint" json:"parEndpoint"`
	RequirePar                                                bool                                  `schema:"require_par" json:"requirePar"`
	DeviceAuthzEndpoint                                       string                                `schema:"device_authz_endpoint" json:"deviceAuthzEndpoint"`
	SessionAsJwt                                              bool                                  `schema:"session_as_jwt" json:"sessionAsJwt"`
	MtlsAuthorizationEndpoint                                 string                                `schema:"mtls_authorization_endpoint" json:"mtlsAuthorizationEndpoint"`
	MtlsTokenEndpoint                                         string                                `schema:"mtls_token_endpoint" json:"mtlsTokenEndpoint"`
	MtlsTokenRevocationEndpoint                               string                                `schema:"mtls_token_revocation_endpoint" json:"mtlsTokenRevocationEndpoint"`
	MtlsUserInfoEndpoint                                      string                                `schema:"mtls_user_info_endpoint" json:"mtlsUserInfoEndpoint"`
	MtlsClientInfoEndpoint                                    string                                `schema:"mtls_client_info_endpoint" json:"mtlsClientInfoEndpoint"`
	MtlsCheckSessionIFrame                                    string                                `schema:"mtls_check_session_iframe" json:"mtlsCheckSessionIFrame"`
	MtlsEndSessionEndpoint                                    string                                `schema:"mtls_end_session_endpoint" json:"mtlsEndSessionEndpoint"`
	MtlsJwksUri                                               string                                `schema:"mtls_jwks_uri" json:"mtlsJwksUri"`
	MtlsRegistrationEndpoint                                  string                                `schema:"mtls_registration_endpoint" json:"mtlsRegistrationEndpoint"`
	MtlsIdGenerationEndpoint                                  string                                `schema:"mtls_id_generation_endpoint" json:"mtlsIdGenerationEndpoint"`
	MtlsIntrospectionEndpoint                                 string                                `schema:"mtls_introspection_endpoint" json:"mtlsIntrospectionEndpoint"`
	MtlsParEndpoint                                           string                                `schema:"mtls_par_endpoint" json:"mtlsParEndpoint"`
	MtlsDeviceAuthzEndpoint                                   string                                `schema:"mtls_device_authz_endpoint" json:"mtlsDeviceAuthzEndpoint"`
	RequireRequestObjectEncryption                            bool                                  `schema:"require_request_object_encryption" json:"requireRequestObjectEncryption"`
	RequirePkce                                               bool                                  `schema:"require_pkce" json:"requirePkce"`
	AllowAllValueForRevokeEndpoint                            bool                                  `schema:"allow_all_value_for_revoke_endpoint" json:"allowAllValueForRevokeEndpoint"`
	SectorIdentifierCacheLifetime                             int                                   `schema:"sector_identifier_cache_lifetime" json:"sectorIdentifierCacheLifetime"`
	UmaConfigurationEndpoint                                  string                                `schema:"uma_configuration_endpoint" json:"umaConfigurationEndpoint"`
	UmaRptAsJwt                                               bool                                  `schema:"uma_rpt_as_jwt" json:"umaRptAsJwt"`
	UmaRptLifetime                                            int                                   `schema:"uma_rpt_lifetime" json:"umaRptLifetime"`
	UmaTicketLifeTime                                         int                                   `schema:"uma_ticket_lifetime" json:"umaTicketLifetime"`
	UmaPctLifetime                                            int                                   `schema:"uma_pct_lifetime" json:"umaPctLifetime"`
	UmaResourceLifetime                                       int                                   `schema:"uma_resource_lifetime" json:"umaResourceLifetime"`
	UmaAddScopesAutomatically                                 bool                                  `schema:"uma_add_scopes_automatically" json:"umaAddScopesAutomatically"`
	UmaValidateClaimToken                                     bool                                  `schema:"uma_validate_claim_token" json:"umaValidateClaimToken"`
	UmaGrantAccessIfNoPolicies                                bool                                  `schema:"uma_grant_access_if_no_policies" json:"umaGrantAccessIfNoPolicies"`
	UmaRestrictResourceToAssociatedClient                     bool                                  `schema:"uma_restrict_resource_to_associated_client" json:"umaRestrictResourceToAssociatedClient"`
	StatTimerIntervalInSeconds                                int                                   `schema:"stat_timer_interval_in_seconds" json:"statTimerIntervalInSeconds"`
	StatAuthorizationScope                                    string                                `schema:"stat_authorization_scope" json:"statAuthorizationScope"`
	AllowSpontaneousScopes                                    bool                                  `schema:"allow_spontaneous_scopes" json:"allowSpontaneousScopes"`
	SpontaneousScopeLifetime                                  int                                   `schema:"spontaneous_scope_lifetime" json:"spontaneousScopeLifetime"`
	OpenIDSubAttribute                                        string                                `schema:"openid_sub_attribute" json:"openIdSubAttribute"`
	PublicSubjectIdentifierPerClientEnabled                   bool                                  `schema:"public_subject_identifier_per_client_enabled" json:"publicSubjectIdentifierPerClientEnabled"`
	SubjectIdentifiersPerClientSupported                      []string                              `schema:"subject_identifiers_per_client_supported" json:"subjectIdentifiersPerClientSupported"`
	ResponseTypesSupported                                    [][]string                            `schema:"response_types_supported" json:"responseTypesSupported"` // XXX: array of arrays
	ResponseModesSupported                                    []string                              `schema:"response_modes_supported" json:"responseModesSupported"`
	GrantTypesSupported                                       []string                              `schema:"grant_types_supported" json:"grantTypesSupported"`
	SubjectTypesSupported                                     []string                              `schema:"subject_types_supported" json:"subjectTypesSupported"`
	DefaultSubjectType                                        string                                `schema:"default_subject_type" json:"defaultSubjectType"`
	AuthorizationSigningAlgValuesSupported                    []string                              `schema:"authorization_signing_alg_values_supported" json:"authorizationSigningAlgValuesSupported"`
	AuthorizationEncryptionAlgValuesSupported                 []string                              `schema:"authorization_encryption_alg_values_supported" json:"authorizationEncryptionAlgValuesSupported"`
	AuthorizationEncryptionEncValuesSupported                 []string                              `schema:"authorization_encryption_enc_values_supported" json:"authorizationEncryptionEncValuesSupported"`
	UserInfoSigningAlgValuesSupported                         []string                              `schema:"user_info_signing_alg_values_supported" json:"userInfoSigningAlgValuesSupported"`
	UserInfoEncryptionAlgValuesSupported                      []string                              `schema:"user_info_encryption_alg_values_supported" json:"userInfoEncryptionAlgValuesSupported"`
	UserInfoEncryptionEncValuesSupported                      []string                              `schema:"user_info_encryption_enc_values_supported" json:"userInfoEncryptionEncValuesSupported"`
	IDTokenSigningAlgValuesSupported                          []string                              `schema:"id_token_signing_alg_values_supported" json:"idTokenSigningAlgValuesSupported"`
	IDTokenEncryptionAlgValuesSupported                       []string                              `schema:"id_token_encryption_alg_values_supported" json:"idTokenEncryptionAlgValuesSupported"`
	IDTokenEncryptionEncValuesSupported                       []string                              `schema:"id_token_encryption_enc_values_supported" json:"idTokenEncryptionEncValuesSupported"`
	ForceSignedRequestObject                                  bool                                  `schema:"force_signed_request_object" json:"forceSignedRequestObject"`
	RequestObjectSigningAlgValuesSupported                    []string                              `schema:"request_object_signing_alg_values_supported" json:"requestObjectSigningAlgValuesSupported"`
	RequestObjectEncryptionAlgValuesSupported                 []string                              `schema:"request_object_encryption_alg_values_supported" json:"requestObjectEncryptionAlgValuesSupported"`
	RequestObjectEncryptionEncValuesSupported                 []string                              `schema:"request_object_encryption_enc_values_supported" json:"requestObjectEncryptionEncValuesSupported"`
	TokenEndpointAuthMethodsSupported                         []string                              `schema:"token_endpoint_auth_methods_supported" json:"tokenEndpointAuthMethodsSupported"`
	TokenEndpointAuthSigningAlgValuesSupported                []string                              `schema:"token_endpoint_auth_signing_alg_values_supported" json:"tokenEndpointAuthSigningAlgValuesSupported"`
	DisplayValuesSupported                                    []string                              `schema:"display_values_supported" json:"displayValuesSupported"`
	ClaimTypesSupported                                       []string                              `schema:"claim_types_supported" json:"claimTypesSupported"`
	JwksAlgorithmsSupported                                   []string                              `schema:"jwks_algorithms_supported" json:"jwksAlgorithmsSupported"`
	ServiceDocumentation                                      string                                `schema:"service_documentation" json:"serviceDocumentation"`
	ClaimsLocalesSupported                                    []string                              `schema:"claims_locales_supported" json:"claimsLocalesSupported"`
	IDTokenTokenBindingCnfValuesSupported                     []string                              `schema:"id_token_token_binding_cnf_values_supported" json:"idTokenTokenBindingCnfValuesSupported"`
	UILocalesSupported                                        []string                              `schema:"ui_locales_supported" json:"uiLocalesSupported"`
	ClaimsParameterSupported                                  bool                                  `schema:"claims_parameter_supported" json:"claimsParameterSupported"`
	RequestParameterSupported                                 bool                                  `schema:"request_parameter_supported" json:"requestParameterSupported"`
	RequestUriParameterSupported                              bool                                  `schema:"request_uri_parameter_supported" json:"requestUriParameterSupported"`
	RequestUriHashVerificationEnabled                         bool                                  `schema:"request_uri_hash_verification_enabled" json:"requestUriHashVerificationEnabled"`
	RequireRequestUriRegistration                             bool                                  `schema:"require_request_uri_registration" json:"requireRequestUriRegistration"`
	RequestUriBlockList                                       []string                              `schema:"request_uri_block_list" json:"requestUriBlockList"`
	OpPolicyUri                                               string                                `schema:"op_policy_uri" json:"opPolicyUri"`
	OpTosUri                                                  string                                `schema:"op_tos_uri" json:"opTosUri"`
	AuthorizationCodeLifetime                                 int                                   `schema:"authorization_code_lifetime" json:"authorizationCodeLifetime"`
	RefreshTokenLifetime                                      int                                   `schema:"refresh_token_lifetime" json:"refreshTokenLifetime"`
	IDTokenLifetime                                           int                                   `schema:"id_token_lifetime" json:"idTokenLifetime"`
	IDTokenFilterClaimsBasedOnAccessToken                     bool                                  `schema:"id_token_filter_claims_based_on_access_token" json:"idTokenFilterClaimsBasedOnAccessToken"`
	AccessTokenLifetime                                       int                                   `schema:"access_token_lifetime" json:"accessTokenLifetime"`
	CleanServiceInterval                                      int                                   `schema:"clean_service_interval" json:"cleanServiceInterval"`
	CleanServiceBatchChunkSize                                int                                   `schema:"clean_service_batch_chunk_size" json:"cleanServiceBatchChunkSize"`
	KeyRegenerationEnabled                                    bool                                  `schema:"key_regeneration_enabled" json:"keyRegenerationEnabled"`
	KeyRegenerationInterval                                   int                                   `schema:"key_regeneration_interval" json:"keyRegenerationInterval"`
	DefaultSignatureAlgorithm                                 string                                `schema:"default_signature_algorithm" json:"defaultSignatureAlgorithm"`
	OxOpenIDConnectVersion                                    string                                `schema:"ox_open_id_connect_version" json:"oxOpenIdConnectVersion"`
	OxID                                                      string                                `schema:"ox_id" json:"oxId"`
	TrustedClientEnabled                                      bool                                  `schema:"trusted_client_enabled" json:"trustedClientEnabled"`
	SkipAuthorizationForOpenIDScopeAndPairwiseID              bool                                  `schema:"skip_authorization_for_open_id_scope_and_pairwise_id" json:"skipAuthorizationForOpenIDScopeAndPairwiseID"`
	DynamicRegistrationExpirationTime                         int                                   `schema:"dynamic_registration_expiration_time" json:"dynamicRegistrationExpirationTime"`
	DynamicRegistrationCustomAttributes                       []string                              `schema:"dynamic_registration_custom_attributes" json:"dynamicRegistrationCustomAttributes"`
	DynamicRegistrationPersistClientAuthorizations            bool                                  `schema:"dynamic_registration_persist_client_authorizations" json:"dynamicRegistrationPersistClientAuthorizations"`
	DynamicRegistrationAllowedPasswordGrantScopes             []string                              `schema:"dynamic_registration_allowed_password_grant_scopes" json:"dynamicRegistrationAllowedPasswordGrantScopes"`
	DynamicRegistrationCustomObjectClass                      string                                `schema:"dynamic_registration_custom_object_class" json:"dynamicRegistrationCustomObjectClass"`
	DynamicRegistrationScopesParamEnabled                     bool                                  `schema:"dynamic_registration_scopes_param_enabled" json:"dynamicRegistrationScopesParamEnabled"`
	DynamicRegistrationPasswordGrantTypeEnabled               bool                                  `schema:"dynamic_registration_password_grant_type_enabled" json:"dynamicRegistrationPasswordGrantTypeEnabled"`
	PersistIdTokenInLdap                                      bool                                  `schema:"persist_id_token_in_ldap" json:"persistIdTokenInLdap"`
	PersistRefreshTokenInLdap                                 bool                                  `schema:"persist_refresh_token_in_ldap" json:"persistRefreshTokenInLdap"`
	AllowPostLogoutRedirectWithoutValidation                  bool                                  `schema:"allow_post_logout_redirect_without_validation" json:"allowPostLogoutRedirectWithoutValidation"`
	InvalidateSessionCookiesAfterAuthorizationFlow            bool                                  `schema:"invalidate_session_cookies_after_authorization_flow" json:"invalidateSessionCookiesAfterAuthorizationFlow"`
	ReturnClientSecretOnRead                                  bool                                  `schema:"return_client_secret_on_read" json:"returnClientSecretOnRead"`
	RejectJwtWithNoneAlg                                      bool                                  `schema:"reject_jwt_with_none_alg" json:"rejectJwtWithNoneAlg"`
	ExpirationNotificatorEnabled                              bool                                  `schema:"expiration_notificator_enabled" json:"expirationNotificatorEnabled"`
	UseNestedJwtDuringEncryption                              bool                                  `schema:"use_nested_jwt_during_encryption" json:"useNestedJwtDuringEncryption"`
	ExpirationNotificatorMapSizeLimit                         int                                   `schema:"expiration_notificator_map_size_limit" json:"expirationNotificatorMapSizeLimit"`
	ExpirationNotificatorIntervalInSeconds                    int                                   `schema:"expiration_notificator_interval_in_seconds" json:"expirationNotificatorIntervalInSeconds"`
	RedirectUrisRegexEnabled                                  bool                                  `schema:"redirect_uris_regex_enabled" json:"redirectUrisRegexEnabled"`
	UseHighestLevelScriptIfAcrScriptNotFound                  bool                                  `schema:"use_highest_level_script_if_acr_script_not_found" json:"useHighestLevelScriptIfAcrScriptNotFound"`
	AuthenticationFiltersEnabled                              bool                                  `schema:"authentication_filters_enabled" json:"authenticationFiltersEnabled"`
	ClientAuthenticationFiltersEnabled                        bool                                  `schema:"client_authentication_filters_enabled" json:"clientAuthenticationFiltersEnabled"`
	ClientRegDefaultToCodeFlowWithRefresh                     bool                                  `schema:"client_reg_default_to_code_flow_with_refresh" json:"clientRegDefaultToCodeFlowWithRefresh"`
	GrantTypesAndResponseTypesAutofixEnabled                  bool                                  `schema:"grant_types_and_response_types_autofix_enabled" json:"grantTypesAndResponseTypesAutofixEnabled"`
	AuthenticationFilters                                     []AuthenticationFilter                `schema:"authentication_filters" json:"authenticationFilters"`
	ClientAuthenticationFilters                               []AuthenticationFilter                `schema:"client_authentication_filters" json:"clientAuthenticationFilters"`
	CorsConfigurationFilters                                  []CorsConfigurationFilter             `schema:"cors_configuration_filters" json:"corsConfigurationFilters"`
	SessionIdUnusedLifetime                                   int                                   `schema:"session_id_unused_lifetime" json:"sessionIdUnusedLifetime"`
	SessionIdUnauthenticatedUnusedLifetime                    int                                   `schema:"session_id_unauthenticated_unused_lifetime" json:"sessionIdUnauthenticatedUnusedLifetime"`
	SessionIdPersistOnPromptNone                              bool                                  `schema:"session_id_persist_on_prompt_none" json:"sessionIdPersistOnPromptNone"`
	SessionIdRequestParameterEnabled                          bool                                  `schema:"session_id_request_parameter_enabled" json:"sessionIdRequestParameterEnabled"`
	ChangeSessionIdOnAuthentication                           bool                                  `schema:"change_session_id_on_authentication" json:"changeSessionIdOnAuthentication"`
	SessionIdPersistInCache                                   bool                                  `schema:"session_id_persist_in_cache" json:"sessionIdPersistInCache"`
	IncludeSidInResponse                                      bool                                  `schema:"include_sid_in_response" json:"includeSidInResponse"`
	SessionIdLifetime                                         int                                   `schema:"session_id_lifetime" json:"sessionIdLifetime"`
	ServerSessionIdLifetime                                   int                                   `schema:"server_session_id_lifetime" json:"serverSessionIdLifetime"`
	ActiveSessionAuthorizationScope                           string                                `schema:"active_session_authorization_scope" json:"activeSessionAuthorizationScope"`
	ConfigurationUpdateInterval                               int                                   `schema:"configuration_update_interval" json:"configurationUpdateInterval"`
	EnableClientGrantTypeUpdate                               bool                                  `schema:"enable_client_grant_type_update" json:"enableClientGrantTypeUpdate"`
	DynamicGrantTypeDefault                                   []string                              `schema:"dynamic_grant_type_default" json:"dynamicGrantTypeDefault"`
	MetricReporterInterval                                    int                                   `schema:"metric_reporter_interval" json:"metricReporterInterval"`
	MetricReporterKeepDataDays                                int                                   `schema:"metric_reporter_keep_data_days" json:"metricReporterKeepDataDays"`
	PairwiseIdType                                            string                                `schema:"pairwise_id_type" json:"pairwiseIdType"`
	PairwiseCalculationKey                                    string                                `schema:"pairwise_calculation_key" json:"pairwiseCalculationKey"`
	PairwiseCalculationSalt                                   string                                `schema:"pairwise_calculation_salt" json:"pairwiseCalculationSalt"`
	ShareSubjectIdBetweenClientsWithSameSectorId              bool                                  `schema:"share_subject_id_between_clients_with_same_sector_id" json:"shareSubjectIdBetweenClientsWithSameSectorId"`
	WebKeysStorage                                            string                                `schema:"web_keys_storage" json:"webKeysStorage"`
	DnName                                                    string                                `schema:"dn_name" json:"dnName"`
	KeyStoreFile                                              string                                `schema:"key_store_file" json:"keyStoreFile"`
	KeyStoreSecret                                            string                                `schema:"key_store_secret" json:"keyStoreSecret"`
	KeySelectionStrategy                                      string                                `schema:"key_selection_strategy" json:"keySelectionStrategy"`
	KeySignWithSameKeyButDiffAlg                              bool                                  `schema:"key_sign_with_same_key_but_diff_alg" json:"keySignWithSameKeyButDiffAlg"`
	KeyAlgsAllowedForGeneration                               []string                              `schema:"key_algs_allowed_for_generation" json:"keyAlgsAllowedForGeneration"`
	StaticKid                                                 string                                `schema:"static_kid" json:"staticKid"`
	StaticDecryptionKid                                       string                                `schema:"static_decryption_kid" json:"staticDecryptionKid"`
	OxElevenGenerateKeyEndpoint                               string                                `schema:"ox_eleven_generate_key_endpoint" json:"oxElevenGenerateKeyEndpoint"`
	OxElevenSignEndpoint                                      string                                `schema:"ox_eleven_sign_endpoint" json:"oxElevenSignEndpoint"`
	OxElevenVerifySignatureEndpoint                           string                                `schema:"ox_eleven_verify_signature_endpoint" json:"oxElevenVerifySignatureEndpoint"`
	OxElevenDeleteKeyEndpoint                                 string                                `schema:"ox_eleven_delete_key_endpoint" json:"oxElevenDeleteKeyEndpoint"`
	OxElevenTestModeToken                                     string                                `schema:"ox_eleven_test_mode_token" json:"oxElevenTestModeToken"`
	IntrospectionAccessTokenMustHaveUmaProtectionScope        bool                                  `schema:"introspection_access_token_must_have_uma_protection_scope" json:"introspectionAccessTokenMustHaveUmaProtectionScope"`
	IntrospectionSkipAuthorization                            bool                                  `schema:"introspection_skip_authorization" json:"introspectionSkipAuthorization"`
	EndSessionWithAccessToken                                 bool                                  `schema:"end_session_with_access_token" json:"endSessionWithAccessToken"`
	ClientWhiteList                                           []string                              `schema:"client_white_list" json:"clientWhiteList"`
	ClientBlackList                                           []string                              `schema:"client_black_list" json:"clientBlackList"`
	LegacyIdTokenClaims                                       bool                                  `schema:"legacy_id_token_claims" json:"legacyIdTokenClaims"`
	CustomHeadersWithAuthorizationResponse                    bool                                  `schema:"custom_headers_with_authorization_response" json:"customHeadersWithAuthorizationResponse"`
	FrontChannelLogoutSessionSupported                        bool                                  `schema:"front_channel_logout_session_supported" json:"frontChannelLogoutSessionSupported"`
	LoggingLevel                                              string                                `schema:"logging_level" json:"loggingLevel"`
	LoggingLayout                                             string                                `schema:"logging_layout" json:"loggingLayout"`
	UpdateUserLastLogonTime                                   bool                                  `schema:"update_user_last_logon_time" json:"updateUserLastLogonTime"`
	UpdateClientAccessTime                                    bool                                  `schema:"update_client_access_time" json:"updateClientAccessTime"`
	LogClientIdOnClientAuthentication                         bool                                  `schema:"log_client_id_on_client_authentication" json:"logClientIdOnClientAuthentication"`
	LogClientNameOnClientAuthentication                       bool                                  `schema:"log_client_name_on_client_authentication" json:"logClientNameOnClientAuthentication"`
	DisableJdkLogger                                          bool                                  `schema:"disable_jdk_logger" json:"disableJdkLogger"`
	AuthorizationRequestCustomAllowedParameters               []CustomAllowedParameter              `schema:"authorization_request_custom_allowed_parameters" json:"authorizationRequestCustomAllowedParameters"`
	OpenidScopeBackwardCompatibility                          bool                                  `schema:"openid_scope_backward_compatibility" json:"openidScopeBackwardCompatibility"`
	DisableU2fEndpoint                                        bool                                  `schema:"disable_u2f_endpoint" json:"disableU2fEndpoint"`
	DcrSignatureValidationEnabled                             bool                                  `schema:"dcr_signature_validation_enabled" json:"dcrSignatureValidationEnabled"`
	DcrAuthorizationWithClientCredentials                     bool                                  `schema:"dcr_authorization_with_client_credentials" json:"dcrAuthorizationWithClientCredentials"`
	DcrAuthorizationWithMTLS                                  bool                                  `schema:"dcr_authorization_with_mtls" json:"dcrAuthorizationWithMTLS"`
	DcrSignatureValidationJwks                                string                                `schema:"dcr_signature_validation_jwks" json:"dcrSignatureValidationJwks"`
	DcrSignatureValidationSoftwareStatementJwksUriClaim       string                                `schema:"dcr_signature_validation_software_statement_jwks_uri_claim" json:"dcrSignatureValidationSoftwareStatementJwksUriClaim"`
	DcrSignatureValidationJwksUri                             string                                `schema:"dcr_signature_validation_jwks_uri" json:"dcrSignatureValidationJwksUri"`
	DcrSignatureValidationSharedSecret                        string                                `schema:"dcr_signature_validation_shared_secret" json:"dcrSignatureValidationSharedSecret"`
	DcrSignatureValidationSoftwareStatementJwksClaim          string                                `schema:"dcr_signature_validation_software_statement_jwks_claim" json:"dcrSignatureValidationSoftwareStatementJwksClaim"`
	DcrIssuers                                                []string                              `schema:"dcr_issuers" json:"dcrIssuers"`
	UseLocalCache                                             bool                                  `schema:"use_local_cache" json:"useLocalCache"`
	FapiCompatibility                                         bool                                  `schema:"fapi_compatibility" json:"fapiCompatibility"`
	ForceIdTokenHintPrecense                                  bool                                  `schema:"force_id_token_hint_precense" json:"forceIdTokenHintPrecense"`
	ForceOfflineAccessScopeToEnableRefreshToken               bool                                  `schema:"force_offline_access_scope_to_enable_refresh_token" json:"forceOfflineAccessScopeToEnableRefreshToken"`
	ErrorReasonEnabled                                        bool                                  `schema:"error_reason_enabled" json:"errorReasonEnabled"`
	RemoveRefreshTokensForClientOnLogout                      bool                                  `schema:"remove_refresh_tokens_for_client_on_logout" json:"removeRefreshTokensForClientOnLogout"`
	SkipRefreshTokenDuringRefreshing                          bool                                  `schema:"skip_refresh_token_during_refreshing" json:"skipRefreshTokenDuringRefreshing"`
	RefreshTokenExtendLifetimeOnRotation                      bool                                  `schema:"refresh_token_extend_lifetime_on_rotation" json:"refreshTokenExtendLifetimeOnRotation"`
	CheckUserPresenceOnRefreshToken                           bool                                  `schema:"check_user_presence_on_refresh_token" json:"checkUserPresenceOnRefreshToken"`
	ConsentGatheringScriptBackwardCompatibility               bool                                  `schema:"consent_gathering_script_backward_compatibility" json:"consentGatheringScriptBackwardCompatibility"`
	IntrospectionScriptBackwardCompatibility                  bool                                  `schema:"introspection_script_backward_compatibility" json:"introspectionScriptBackwardCompatibility"`
	IntrospectionResponseScopesBackwardCompatibility          bool                                  `schema:"introspection_response_scopes_backward_compatibility" json:"introspectionResponseScopesBackwardCompatibility"`
	SoftwareStatementValidationType                           string                                `schema:"software_statement_validation_type" json:"softwareStatementValidationType"`
	AuthenticationProtectionConfiguration                     AuthenticationProtectionConfiguration `schema:"authentication_protection_configuration" json:"authenticationProtectionConfiguration"`
	ErrorHandlingMethod                                       string                                `schema:"error_handling_method" json:"errorHandlingMethod"`
	KeepAuthenticatorAttributesOnAcrChange                    bool                                  `schema:"keep_authenticator_attributes_on_acr_change" json:"keepAuthenticatorAttributesOnAcrChange"`
	DeviceAuthzRequestExpiresIn                               int                                   `schema:"device_authz_request_expires_in" json:"deviceAuthzRequestExpiresIn"`
	DeviceAuthzTokenPollInterval                              int                                   `schema:"device_authz_token_poll_interval" json:"deviceAuthzTokenPollInterval"`
	DeviceAuthzResponseTypeToProcessAuthz                     string                                `schema:"device_authz_response_type_to_process_authz" json:"deviceAuthzResponseTypeToProcessAuthz"`
	BackchannelRedirectUri                                    string                                `schema:"backchannel_redirect_uri" json:"backchannelRedirectUri"`
	BackchannelAuthenticationEndpoint                         string                                `schema:"backchannel_authentication_endpoint" json:"backchannelAuthenticationEndpoint"`
	BackchannelDeviceRegistrationEndpoint                     string                                `schema:"backchannel_device_registration_endpoint" json:"backchannelDeviceRegistrationEndpoint"`
	BackchannelTokenDeliveryModesSupported                    []string                              `schema:"backchannel_token_delivery_modes_supported" json:"backchannelTokenDeliveryModesSupported"`
	BackchannelUserCodeParameterSupported                     bool                                  `schema:"backchannel_user_code_parameter_supported" json:"backchannelUserCodeParameterSupported"`
	BackchannelBindingMessagePattern                          string                                `schema:"backchannel_binding_message_pattern" json:"backchannelBindingMessagePattern"`
	BackchannelAuthenticationResponseExpiresIn                int                                   `schema:"backchannel_authentication_response_expires_in" json:"backchannelAuthenticationResponseExpiresIn"`
	BackchannelAuthenticationResponseInterval                 int                                   `schema:"backchannel_authentication_response_interval" json:"backchannelAuthenticationResponseInterval"`
	BackchannelLoginHintClaims                                []string                              `schema:"backchannel_login_hint_claims" json:"backchannelLoginHintClaims"`
	CibaEndUserNotificationConfig                             CibaEndUserNotificationConfig         `schema:"ciba_end_user_notification_config" json:"cibaEndUserNotificationConfig"`
	BackchannelRequestsProcessorJobIntervalSec                int                                   `schema:"backchannel_requests_processor_job_interval_sec" json:"backchannelRequestsProcessorJobIntervalSec"`
	BackchannelRequestsProcessorJobChunkSize                  int                                   `schema:"backchannel_requests_processor_job_chunk_size" json:"backchannelRequestsProcessorJobChunkSize"`
	CibaGrantLifeExtraTimeSec                                 int                                   `schema:"ciba_grant_life_extra_time_sec" json:"cibaGrantLifeExtraTimeSec"`
	CibaMaxExpirationTimeAllowedSec                           int                                   `schema:"ciba_max_expiration_time_allowed_sec" json:"cibaMaxExpirationTimeAllowedSec"`
	DpopSigningAlgValuesSupported                             []string                              `schema:"dpop_signing_alg_values_supported" json:"dpopSigningAlgValuesSupported"`
	DpopTimeframe                                             int                                   `schema:"dpop_timeframe" json:"dpopTimeframe"`
	DpopJtiCacheTime                                          int                                   `schema:"dpop_jti_cache_time" json:"dpopJtiCacheTime"`
	AllowIdTokenWithoutImplicitGrantType                      bool                                  `schema:"allow_id_token_without_implicit_grant_type" json:"allowIdTokenWithoutImplicitGrantType"`
	DiscoveryAllowedKeys                                      []string                              `schema:"discovery_allowed_keys" json:"discoveryAllowedKeys"`
	DiscoveryCacheLifetimeInMinutes                           int                                   `schema:"discovery_cache_lifetime_in_minutes" json:"discoveryCacheLifetimeInMinutes"`
	DiscoveryDenyKeys                                         []string                              `schema:"discovery_deny_keys" json:"discoveryDenyKeys"`
	HttpLoggingEnabled                                        bool                                  `schema:"http_logging_enabled" json:"httpLoggingEnabled"`
	HttpLoggingExcludePaths                                   []string                              `schema:"http_logging_exclude_paths" json:"httpLoggingExcludePaths"`
	ExternalLoggerConfiguration                               string                                `schema:"external_logger_configuration" json:"externalLoggerConfiguration"`
	AgamaConfiguration                                        AgamaConfiguration                    `schema:"agama_configuration" json:"agamaConfiguration"`
	EnabledComponents                                         []string                              `schema:"enabled_components" json:"enabledComponents"`
	PersonCustomObjectClassList                               []string                              `schema:"person_custom_object_class_list" json:"personCustomObjectClassList"`
	StatWebServiceIntervalLimitInSeconds                      int                                   `schema:"stat_web_service_interval_limit_in_seconds" json:"statWebServiceIntervalLimitInSeconds"`
	ImgLocation                                               string                                `schema:"img_location" json:"imgLocation"`
	SoftwareStatementValidationClaimName                      string                                `schema:"software_statement_validation_claim_name" json:"softwareStatementValidationClaimName"`
	JmsBrokerUriSet                                           []string                              `schema:"jms_broker_uri_set" json:"jmsBrokerUriSet"`
	JsLocation                                                string                                `schema:"js_location" json:"jsLocation"`
	CssLocation                                               string                                `schema:"css_location" json:"cssLocation"`
	BackchannelAuthenticationRequestSigningAlgValuesSupported []string                              `schema:"backchannel_authentication_request_signing_alg_values_supported" json:"backchannelAuthenticationRequestSigningAlgValuesSupported"`
	BackchannelClientId                                       string                                `schema:"backchannel_client_id" json:"backchannelClientId"`
	EnabledOAuthAuditLogging                                  bool                                  `schema:"enabled_oauth_audit_logging" json:"enabledOAuthAuditLogging"`
	JmsUserName                                               string                                `schema:"jms_user_name" json:"jmsUserName"`
	JmsPassword                                               string                                `schema:"jms_password" json:"jmsPassword"`
	CookieDomain                                              string                                `schema:"cookie_domain" json:"cookieDomain"`
}

// GetAppConfiguration returns all Janssen authorization server configuration
// properties.
func (c *Client) GetAppConfiguration(ctx context.Context) (*AppConfiguration, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/properties.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &AppConfiguration{}

	if err := c.get(ctx, "/jans-config-api/api/v1/jans-auth-server/config", token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	sortArrays(&ret.AuthorizationRequestCustomAllowedParameters)

	return ret, nil
}

// UpdateAuthServiceConfig uses the provided list of patch requests to update
// the Janssen authorization servcer application configuration properties.
func (c *Client) UpdateAppConfiguration(ctx context.Context, config *AppConfiguration) (*AppConfiguration, error) {

	if config == nil {
		return nil, fmt.Errorf("config is nil")
	}

	orig, err := c.GetAppConfiguration(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to get app configuration: %w", err)
	}

	patches, err := createPatches(config, orig)
	if err != nil {
		return nil, fmt.Errorf("failed to create patches: %w", err)
	}

	if len(patches) == 0 {
		return nil, fmt.Errorf("no patches provided")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/properties.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.patch(ctx, "/jans-config-api/api/v1/jans-auth-server/config", token, patches); err != nil {
		return nil, fmt.Errorf("patch request failed: %w", err)
	}

	return c.GetAppConfiguration(ctx)
}
