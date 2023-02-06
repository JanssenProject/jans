/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import io.jans.agama.model.EngineConfig;
import io.jans.as.model.common.*;
import io.jans.as.model.error.ErrorHandlingMethod;
import io.jans.as.model.jwk.KeySelectionStrategy;
import io.jans.as.model.ssa.SsaConfiguration;
import io.jans.as.model.ssa.SsaValidationConfig;
import io.jans.doc.annotation.DocProperty;

import java.util.*;

/**
 * Represents the configuration JSON file.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version March 15, 2022
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration implements Configuration {

    public static final int DEFAULT_SESSION_ID_LIFETIME = 86400;
    public static final KeySelectionStrategy DEFAULT_KEY_SELECTION_STRATEGY = KeySelectionStrategy.OLDER;
    public static final String DEFAULT_STAT_SCOPE = "jans_stat";

    @DocProperty(description = "URL using the https scheme that OP asserts as Issuer identifier")
    private String issuer;

    @DocProperty(description = "The base URL for endpoints")
    private String baseEndpoint;

    @DocProperty(description = "The authorization endpoint URL")
    private String authorizationEndpoint;

    @DocProperty(description = "The token endpoint URL")
    private String tokenEndpoint;

    @DocProperty(description = "The URL for the access_token or refresh_token revocation endpoint")
    private String tokenRevocationEndpoint;

    @DocProperty(description = "The User Info endpoint URL")
    private String userInfoEndpoint;

    @DocProperty(description = "The Client Info endpoint URL")
    private String clientInfoEndpoint;

    @DocProperty(description = "URL for an OP IFrame that supports cross-origin communications for session state information with the RP Client using the HTML5 postMessage API")
    private String checkSessionIFrame;

    @DocProperty(description = "URL at the OP to which an RP can perform a redirect to request that the end user be logged out at the OP")
    private String endSessionEndpoint;

    @DocProperty(description = "URL of the OP's JSON Web Key Set (JWK) document. This contains the signing key(s) the RP uses to validate signatures from the OP")
    private String jwksUri;

    @DocProperty(description = "Registration endpoint URL")
    private String registrationEndpoint;

    @DocProperty(description = "Discovery endpoint URL")
    private String openIdDiscoveryEndpoint;

    @DocProperty(description = "URL for the Open ID Connect Configuration Endpoint")
    private String openIdConfigurationEndpoint;

    @DocProperty(description = "ID Generation endpoint URL")
    private String idGenerationEndpoint;

    @DocProperty(description = "Introspection endpoint URL")
    private String introspectionEndpoint;

    @DocProperty(description = "URL for Pushed Authorisation Request (PAR) Endpoint")
    private String parEndpoint;

    @DocProperty(description = "Boolean value to indicate of Pushed Authorisation Request(PAR)is required", defaultValue = "false")
    private Boolean requirePar = false;

    @DocProperty(description = "URL for the Device Authorization")
    private String deviceAuthzEndpoint;

    @DocProperty(description = "URL for Mutual TLS (mTLS) Client Authentication and Certificate-Bound Access Tokens (MTLS) Endpoint")
    private String mtlsAuthorizationEndpoint;

    @DocProperty(description = "URL for Mutual TLS (mTLS) Authorization token Endpoint")
    private String mtlsTokenEndpoint;

    @DocProperty(description = "URL for Mutual TLS (mTLS) Authorization token revocation endpoint")
    private String mtlsTokenRevocationEndpoint;

    @DocProperty(description = "Mutual TLS (mTLS) user info endpoint URL")
    private String mtlsUserInfoEndpoint;

    @DocProperty(description = "URL for Mutual TLS (mTLS) Client Info endpoint")
    private String mtlsClientInfoEndpoint;

    @DocProperty(description = "URL for Mutual TLS (mTLS) IFrame that supports cross-origin communications for session state information with the RP Client using the HTML5 postMessage API")
    private String mtlsCheckSessionIFrame;

    @DocProperty(description = "URL for Mutual TLS (mTLS) to which an RP can perform a redirect to request that the end user be logged out at the OP")
    private String mtlsEndSessionEndpoint;

    @DocProperty(description = "URL for Mutual TLS (mTLS) of the OP's JSON Web Key Set (JWK) document")
    private String mtlsJwksUri;

    @DocProperty(description = "Mutual TLS (mTLS) registration endpoint URL")
    private String mtlsRegistrationEndpoint;

    @DocProperty(description = "Mutual TLS (mTLS) ID generation endpoint URL")
    private String mtlsIdGenerationEndpoint;

    @DocProperty(description = "Mutual TLS (mTLS) introspection endpoint URL")
    private String mtlsIntrospectionEndpoint;

    @DocProperty(description = "Mutual TLS (mTLS) Pushed Authorization Requests(PAR) endpoint URL")
    private String mtlsParEndpoint;

    @DocProperty(description = "Mutual TLS (mTLS) device authorization endpoint URL")
    private String mtlsDeviceAuthzEndpoint;

    @DocProperty(description = "Boolean value true encrypts request object", defaultValue = "false")
    private Boolean requireRequestObjectEncryption = false;

    @DocProperty(description = "Boolean value true check for Proof Key for Code Exchange (PKCE)", defaultValue = "false")
    private Boolean requirePkce = false;

    @DocProperty(description = "Boolean value true allow all value for revoke endpoint", defaultValue = "false")
    private Boolean allowAllValueForRevokeEndpoint = false;

    @DocProperty(description = "Sector Identifier cache lifetime in minutes", defaultValue = "1440")
    private int sectorIdentifierCacheLifetimeInMinutes = 1440;

    @DocProperty(description = "UMA Configuration endpoint URL")
    private String umaConfigurationEndpoint;

    @DocProperty(description = "Issue RPT as JWT or as random string", defaultValue = "false")
    private Boolean umaRptAsJwt = false;

    @DocProperty(description = "UMA RPT lifetime")
    private int umaRptLifetime;

    @DocProperty(description = "UMA ticket lifetime")
    private int umaTicketLifetime;

    @DocProperty(description = "UMA PCT lifetime")
    private int umaPctLifetime;

    @DocProperty(description = "UMA Resource lifetime")
    private int umaResourceLifetime;

    @DocProperty(description = "Add UMA scopes automatically if it is not registered yet")
    private Boolean umaAddScopesAutomatically;

    @DocProperty(description = "Validate claim_token as id_token assuming it is issued by local id", defaultValue = "false")
    private Boolean umaValidateClaimToken = false;

    @DocProperty(description = "Specify whether to grant access to resources if there is no any policies associated with scopes", defaultValue = "false")
    private Boolean umaGrantAccessIfNoPolicies = false;

    @DocProperty(description = "Restrict access to resource by associated client", defaultValue = "false")
    private Boolean umaRestrictResourceToAssociatedClient = false;

    @DocProperty(description = "Statistical data capture time interval")
    private int statTimerIntervalInSeconds;

    @DocProperty(description = "Scope required for Statistical Authorization")
    private String statAuthorizationScope;

    @DocProperty(description = "Specifies whether to allow spontaneous scopes")
    private Boolean allowSpontaneousScopes;

    @DocProperty(description = "The lifetime of spontaneous scope in seconds")
    private int spontaneousScopeLifetime;

    @DocProperty(description = "Specifies which LDAP attribute is used for the subject identifier claim")
    private String openidSubAttribute;

    @DocProperty(description = "Specifies whether public subject identifier is allowed per client", defaultValue = "false")
    private Boolean publicSubjectIdentifierPerClientEnabled = false;

    @DocProperty(description = "A list of the subject identifiers supported per client")
    private List<String> subjectIdentifiersPerClientSupported;

    @DocProperty(description = "This list details which OAuth 2.0 response_type values are supported by this OP.", defaultValue = "By default, every combination of code, token and id_token is supported.")
    private Set<Set<ResponseType>> responseTypesSupported;

    @DocProperty(description = "This list details which OAuth 2.0 response modes are supported by this OP")
    private Set<ResponseMode> responseModesSupported;

    @DocProperty(description = "This list details which OAuth 2.0 grant types are supported by this OP")
    private Set<GrantType> grantTypesSupported;

    @DocProperty(description = "This list details which Subject Identifier types that the OP supports. Valid types include pairwise and public.")
    private List<String> subjectTypesSupported;

    @DocProperty(description = "The default subject type used for dynamic client registration")
    private String defaultSubjectType;

    @DocProperty(description = "List of authorization signing algorithms supported by this OP")
    private List<String> authorizationSigningAlgValuesSupported;

    @DocProperty(description = "List of authorization encryption algorithms supported by this OP")
    private List<String> authorizationEncryptionAlgValuesSupported;

    @DocProperty(description = "A list of the authorization encryption algorithms supported")
    private List<String> authorizationEncryptionEncValuesSupported;

    @DocProperty(description = "This JSON Array lists which JWS signing algorithms (alg values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT")
    private List<String> userInfoSigningAlgValuesSupported;

    @DocProperty(description = "This JSON Array lists which JWS encryption algorithms (alg values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT")
    private List<String> userInfoEncryptionAlgValuesSupported;

    @DocProperty(description = "This JSON Array lists which JWS encryption algorithms (enc values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT")
    private List<String> userInfoEncryptionEncValuesSupported;

    @DocProperty(description = "A list of the JWS signing algorithms (alg values) supported by the OP for the ID Token to encode the Claims in a JWT")
    private List<String> idTokenSigningAlgValuesSupported;

    @DocProperty(description = "A list of the JWE encryption algorithms (alg values) supported by the OP for the ID Token to encode the Claims in a JWT")
    private List<String> idTokenEncryptionAlgValuesSupported;

    @DocProperty(description = "A list of the JWE encryption algorithms (enc values) supported by the OP for the ID Token to encode the Claims in a JWT")
    private List<String> idTokenEncryptionEncValuesSupported;

    @DocProperty(description = "A list of the JWS signing algorithms (alg values) supported by the OP for the access token to encode the Claims in a JWT")
    private List<String> accessTokenSigningAlgValuesSupported;

    @DocProperty(description = "Boolean value true indicates that signed request object is mandatory", defaultValue = "false")
    private Boolean forceSignedRequestObject = false;

    @DocProperty(description = "A list of the JWS signing algorithms (alg values) supported by the OP for Request Objects")
    private List<String> requestObjectSigningAlgValuesSupported;

    @DocProperty(description = "A list of the JWE encryption algorithms (alg values) supported by the OP for Request Objects")
    private List<String> requestObjectEncryptionAlgValuesSupported;

    @DocProperty(description = "A list of the JWE encryption algorithms (enc values) supported by the OP for Request Objects")
    private List<String> requestObjectEncryptionEncValuesSupported;

    @DocProperty(description = "A list of Client Authentication methods supported by this Token Endpoint")
    private List<String> tokenEndpointAuthMethodsSupported;

    @DocProperty(description = "A list of the JWS signing algorithms (alg values) supported by the Token Endpoint for the signature on the JWT used to authenticate the Client at the Token Endpoint for the private_key_jwt and client_secret_jwt authentication methods")
    private List<String> tokenEndpointAuthSigningAlgValuesSupported;

    @DocProperty(description = "This list details the custom attributes allowed for dynamic registration")
    private List<String> dynamicRegistrationCustomAttributes;

    @DocProperty(description = "This map provides default custom attributes with values for dynamic registration")
    private JsonNode dynamicRegistrationDefaultCustomAttributes;

    @DocProperty(description = "A list of the display parameter values that the OpenID Provider supports")
    private List<String> displayValuesSupported;

    @DocProperty(description = "A list of the Claim Types that the OpenID Provider supports")
    private List<String> claimTypesSupported;

    @DocProperty(description = "A list of algorithms that will be used in JWKS endpoint")
    private List<String> jwksAlgorithmsSupported;

    @DocProperty(description = "URL of a page containing human-readable information that developers might want or need to know when using the OpenID Provider")
    private String serviceDocumentation;

    @DocProperty(description = "This list details the languages and scripts supported for values in the claims being returned")
    private List<String> claimsLocalesSupported;

    @DocProperty(description = "Array containing a list of the JWT Confirmation Method member names supported by the OP for Token Binding of ID Tokens. The presence of this parameter indicates that the OpenID Provider supports Token Binding of ID Tokens. If omitted, the default is that the OpenID Provider does not support Token Binding of ID Tokens")
    private List<String> idTokenTokenBindingCnfValuesSupported;

    @DocProperty(description = "This list details the languages and scripts supported for the user interface")
    private List<String> uiLocalesSupported;

    @DocProperty(description = "Specifies whether the OP supports use of the claims parameter")
    private Boolean claimsParameterSupported;

    @DocProperty(description = "Boolean value specifying whether the OP supports use of the request parameter")
    private Boolean requestParameterSupported;

    @DocProperty(description = "Boolean value specifying whether the OP supports use of the request_uri parameter")
    private Boolean requestUriParameterSupported;

    @DocProperty(description = "Boolean value specifying whether the OP supports use of the request_uri hash verification")
    private Boolean requestUriHashVerificationEnabled;

    @DocProperty(description = "Boolean value specifying whether the OP requires any request_uri values used to be pre-registered using the request_uris registration parameter")
    private Boolean requireRequestUriRegistration;

    @DocProperty(description = "Block list for requestUri that can come to Authorization Endpoint (e.g. localhost)")
    private List<String> requestUriBlockList;

    @DocProperty(description = "URL that the OpenID Provider provides to the person registering the Client to read about the OP's requirements on how the Relying Party can use the data provided by the OP")
    private String opPolicyUri;

    @DocProperty(description = "URL that the OpenID Provider provides to the person registering the Client to read about OpenID Provider's terms of service")
    private String opTosUri;

    @DocProperty(description = "The lifetime of the Authorization Code")
    private int authorizationCodeLifetime;

    @DocProperty(description = "The lifetime of the Refresh Token")
    private int refreshTokenLifetime;

    @DocProperty(description = "The lifetime of the ID Token")
    private int idTokenLifetime;

    @DocProperty(description = "Boolean value specifying whether idToken filters claims based on accessToken")
    private Boolean idTokenFilterClaimsBasedOnAccessToken;

    @DocProperty(description = "The lifetime of the short lived Access Token")
    private int accessTokenLifetime;

    @DocProperty(description = "Time interval for the Clean Service in seconds")
    private int cleanServiceInterval;

    @DocProperty(description = "Clean service chunk size which is used during clean up", defaultValue = "100")
    private int cleanServiceBatchChunkSize = 100;

    @DocProperty(description = "Boolean value specifying whether to regenerate keys")
    private Boolean keyRegenerationEnabled;

    @DocProperty(description = "The interval for key regeneration in hours")
    private int keyRegenerationInterval;

    @DocProperty(description = "The default signature algorithm to sign ID Tokens")
    private String defaultSignatureAlgorithm;

    @DocProperty(description = "OpenID Connect Version")
    private String jansOpenIdConnectVersion;

    @DocProperty(description = "URL for the Inum generator Service")
    private String jansId;

    @DocProperty(description = "Expiration time in seconds for clients created with dynamic registration, 0 or -1 means never expire", defaultValue = "-1")
    private int dynamicRegistrationExpirationTime = -1;

    @DocProperty(description = "Boolean value specifying whether to persist client authorizations")
    private Boolean dynamicRegistrationPersistClientAuthorizations;

    @DocProperty(description = "Boolean value specifying whether a client is trusted and no authorization is required")
    private Boolean trustedClientEnabled;

    @DocProperty(description = "Choose whether to skip authorization if a client has an OpenId scope and a pairwise ID", defaultValue = "false")
    private Boolean skipAuthorizationForOpenIdScopeAndPairwiseId = false;

    @DocProperty(description = "Boolean value specifying whether to enable scopes parameter in dynamic registration")
    private Boolean dynamicRegistrationScopesParamEnabled;

    @DocProperty(description = "Boolean value specifying whether to enable Password Grant Type during Dynamic Registration", defaultValue = "false")
    private Boolean dynamicRegistrationPasswordGrantTypeEnabled = false;

    @DocProperty(description = "List of grant scopes for dynamic registration")
    private List<String> dynamicRegistrationAllowedPasswordGrantScopes;

    @DocProperty(description = "LDAP custom object class for dynamic registration")
    private String dynamicRegistrationCustomObjectClass;

    @DocProperty(description = "This list details LDAP custom object classes for dynamic person enrollment")
    private List<String> personCustomObjectClassList;

    @DocProperty(description = "Specifies whether to persist id_token into LDAP (otherwise saves into cache)", defaultValue = "false")
    private Boolean persistIdTokenInLdap = false;

    @DocProperty(description = "Specifies whether to persist refresh_token into LDAP (otherwise saves into cache)", defaultValue = "true")
    private Boolean persistRefreshTokenInLdap = true;

    @DocProperty(description = "Allows post-logout redirect without validation for the End Session endpoint (still AS validates it against clientWhiteList url pattern property)", defaultValue = "false")
    private Boolean allowPostLogoutRedirectWithoutValidation = false;

    @DocProperty(description = "Boolean value to specify whether to invalidate session_id and consent_session_id cookies right after successful or unsuccessful authorization", defaultValue = "false")
    private Boolean invalidateSessionCookiesAfterAuthorizationFlow = false;

    @DocProperty(description = "Boolean value specifying whether a client_secret is returned on client GET or PUT. Set to true by default which means to return secret", defaultValue = "false")
    private Boolean returnClientSecretOnRead = false;

    @DocProperty(description = "Boolean value specifying whether reject JWT requested or validated with algorithm None. Default value is true", defaultValue = "true")
    private Boolean rejectJwtWithNoneAlg = true;

    @DocProperty(description = "Boolean value specifying whether expiration notificator is enabled (used to identify expiration for persistence that support TTL, like Couchbase)", defaultValue = "false")
    private Boolean expirationNotificatorEnabled = false;

    @DocProperty(description = "Boolean value specifying whether to use nested Jwt during encryption", defaultValue = "true")
    private Boolean useNestedJwtDuringEncryption = true;

    @DocProperty(description = "The expiration notificator maximum size limit")
    private int expirationNotificatorMapSizeLimit = 100000;

    @DocProperty(description = "The expiration notificator interval in second")
    private int expirationNotificatorIntervalInSeconds = 600;

    @DocProperty(description = "Enable/Disable redirect uris validation using regular expression", defaultValue = "false")
    private Boolean redirectUrisRegexEnabled = false;

    @DocProperty(description = "Enable/Disable usage of highest level script in case ACR script does not exist", defaultValue = "true")
    private Boolean useHighestLevelScriptIfAcrScriptNotFound = true;

    @DocProperty(description = "Boolean value specifying whether to enable user authentication filters")
    private Boolean authenticationFiltersEnabled;

    @DocProperty(description = "Boolean value specifying whether to enable client authentication filters")
    private Boolean clientAuthenticationFiltersEnabled;

    @DocProperty(description = "Boolean value specifying whether to add Authorization Code Flow with Refresh grant during client registratio")
    private Boolean clientRegDefaultToCodeFlowWithRefresh;

    @DocProperty(description = "Boolean value specifying whether to Grant types and Response types can be auto fixed")
    private Boolean grantTypesAndResponseTypesAutofixEnabled;

    @DocProperty(description = "This list details filters for user authentication")
    private List<AuthenticationFilter> authenticationFilters;

    @DocProperty(description = "This list details filters for client authentication")
    private List<ClientAuthenticationFilter> clientAuthenticationFilters;

    @DocProperty(description = "This list specifies the CORS configuration filters")
    private List<CorsConfigurationFilter> corsConfigurationFilters;

    @DocProperty(description = "The lifetime for unused session states")
    private int sessionIdUnusedLifetime;

    @DocProperty(description = "The lifetime for unused unauthenticated session states")
    private int sessionIdUnauthenticatedUnusedLifetime = 120; // 120 seconds

    @DocProperty(description = "Boolean value specifying whether to persist session ID on prompt none")
    private Boolean sessionIdPersistOnPromptNone;

    @DocProperty(description = "Boolean value specifying whether to enable session_id HTTP request parameter", defaultValue = "false")
    private Boolean sessionIdRequestParameterEnabled = false; // #1195

    @DocProperty(description = "Boolean value specifying whether change session_id on authentication. Default value is true", defaultValue = "true")
    private Boolean changeSessionIdOnAuthentication = true;

    @DocProperty(description = "Boolean value specifying whether to persist session_id in cache", defaultValue = "false")
    private Boolean sessionIdPersistInCache = false;

    @DocProperty(description = "Boolean value specifying whether to include sessionId in response", defaultValue = "false")
    private Boolean includeSidInResponse = false;

    @DocProperty(description = "Boolean value specifying whether to disable prompt=login", defaultValue = "false")
    private Boolean disablePromptLogin = false;


    /**
     * SessionId will be expired after sessionIdLifetime seconds
     */
    @DocProperty(description = "The lifetime of session id in seconds. If 0 or -1 then expiration is not set. session_id cookie expires when browser session ends")
    private Integer sessionIdLifetime = DEFAULT_SESSION_ID_LIFETIME;

    @DocProperty(description = "Dedicated property to control lifetime of the server side OP session object in seconds. Overrides sessionIdLifetime. By default value is 0, so object lifetime equals sessionIdLifetime (which sets both cookie and object expiration). It can be useful if goal is to keep different values for client cookie and server object")
    private Integer serverSessionIdLifetime = sessionIdLifetime; // by default same as sessionIdLifetime

    @DocProperty(description = "Authorization Scope for active session")
    private String activeSessionAuthorizationScope;

    @DocProperty(description = "The interval for configuration update in seconds")
    private int configurationUpdateInterval;

    @DocProperty(description = "Choose if client can update Grant Type values")
    private Boolean enableClientGrantTypeUpdate;

    @DocProperty(description = "This list details which OAuth 2.0 grant types can be set up with the client registration API")
    private Set<GrantType> dynamicGrantTypeDefault;

    @DocProperty(description = "The location for CSS files")
    private String cssLocation;

    @DocProperty(description = "The location for JavaScript files")
    private String jsLocation;

    @DocProperty(description = "The location for image files")
    private String imgLocation;

    @DocProperty(description = "The interval for metric reporter in seconds")
    private int metricReporterInterval;

    @DocProperty(description = "The days to keep metric reported data")
    private int metricReporterKeepDataDays;

    @DocProperty(description = "the pairwise ID type")
    private String pairwiseIdType; // persistent, algorithmic

    @DocProperty(description = "Key to calculate algorithmic pairwise IDs")
    private String pairwiseCalculationKey;

    @DocProperty(description = "Salt to calculate algorithmic pairwise IDs")
    private String pairwiseCalculationSalt;

    @DocProperty(description = "When true, clients with the same Sector ID also share the same Subject ID", defaultValue = "false")
    private Boolean shareSubjectIdBetweenClientsWithSameSectorId = false;

    @DocProperty(description = "Web Key Storage Type")
    private WebKeyStorage webKeysStorage;

    @DocProperty(description = "DN of certificate issuer")
    private String dnName;

    @DocProperty(description = "The Key Store File (JKS)")
    // Jans Auth KeyStore
    private String keyStoreFile;

    @DocProperty(description = "The Key Store password")
    private String keyStoreSecret;

    @DocProperty(description = "Key Selection Strategy : OLDER, NEWER, FIRST", defaultValue = "OLDER")
    private KeySelectionStrategy keySelectionStrategy = DEFAULT_KEY_SELECTION_STRATEGY;

    @DocProperty(description = "List of algorithm allowed to be used for key generation")
    private List<String> keyAlgsAllowedForGeneration = new ArrayList<>();

    @DocProperty(description = "Specifies if signing to be done with same key but apply different algorithms")
    private Boolean keySignWithSameKeyButDiffAlg; // https://github.com/JanssenProject/jans-auth-server/issues/95

    @DocProperty(description = "Specifies static Kid")
    private String staticKid;

    @DocProperty(description = "Specifies static decryption Kid")
    private String staticDecryptionKid;



    //oxEleven
    @DocProperty(description = "oxEleven Test Mode Token")
    private String jansElevenTestModeToken;

    @DocProperty(description = "oxEleven Generate Key endpoint URL")
    private String jansElevenGenerateKeyEndpoint;

    @DocProperty(description = "oxEleven Sign endpoint UR")
    private String jansElevenSignEndpoint;

    @DocProperty(description = "oxEleven Verify Signature endpoint URL")
    private String jansElevenVerifySignatureEndpoint;

    @DocProperty(description = "oxEleven Delete Key endpoint URL")
    private String jansElevenDeleteKeyEndpoint;

    @DocProperty(description = "If True, rejects introspection requests if access_token does not have the uma_protection scope in its authorization header", defaultValue = "false")
    private Boolean introspectionAccessTokenMustHaveUmaProtectionScope = false;

    @DocProperty(description = "Specifies if authorization to be skipped for introspection")
    private Boolean introspectionSkipAuthorization;

    @DocProperty(description = "Choose whether to accept access tokens to call end_session endpoint")
    private Boolean endSessionWithAccessToken;

    @DocProperty(description = "Sets cookie domain for all cookies created by OP")
    private String cookieDomain;

    @DocProperty(description = "enable OAuth Audit Logging")
    private Boolean enabledOAuthAuditLogging;

    @DocProperty(description = "JMS Broker URI Set")
    private Set<String> jmsBrokerURISet;

    @DocProperty(description = "JMS UserName")
    private String jmsUserName;

    @DocProperty(description = "JMS Password")
    private String jmsPassword;

    @DocProperty(description = "This list specifies which external URIs can be called by AS (if empty any URI can be called)")
    private List<String> externalUriWhiteList;

    @DocProperty(description = "This list specifies which client redirection URIs are white-listed")
    private List<String> clientWhiteList;

    @DocProperty(description = "This list specified which client redirection URIs are black-listed")
    private List<String> clientBlackList;

    @DocProperty(description = "Choose whether to include claims in ID tokens")
    private Boolean legacyIdTokenClaims;

    @DocProperty(description = "Choose whether to enable the custom response header parameter to return custom headers with the authorization response")
    private Boolean customHeadersWithAuthorizationResponse;

    @DocProperty(description = "Choose whether to support front channel session logout")
    private Boolean frontChannelLogoutSessionSupported;

    @DocProperty(description = "Specify the logging level for oxAuth loggers")
    private String loggingLevel;

    @DocProperty(description = "Logging layout used for Jans Authorization Server loggers")
    private String loggingLayout;

    @DocProperty(description = "Choose if application should update oxLastLogonTime attribute upon user authentication")
    private Boolean updateUserLastLogonTime;

    @DocProperty(description = "Choose if application should update oxLastAccessTime/oxLastLogonTime attributes upon client authentication")
    private Boolean updateClientAccessTime;

    @DocProperty(description = "Choose if application should log the Client ID on client authentication")
    private Boolean logClientIdOnClientAuthentication;

    @DocProperty(description = "Choose if application should log the Client Name on client authentication")
    private Boolean logClientNameOnClientAuthentication;

    @DocProperty(description = "Choose whether to disable JDK loggers", defaultValue = "true")
    private Boolean disableJdkLogger = true;

    @DocProperty(description = "This list details the allowed custom parameters for authorization requests")
    private Set<AuthorizationRequestCustomParameter> authorizationRequestCustomAllowedParameters;

    @DocProperty(description = "Set to false to only allow token endpoint request for openid scope with grant type equals to authorization_code, restrict access to userinfo to scope openid and only return id_token if scope contains openid", defaultValue = "false")
    private Boolean openidScopeBackwardCompatibility = false;

    @DocProperty(description = "Choose whether to disable U2F endpoints", defaultValue = "false")
    private Boolean disableU2fEndpoint = false;

    // Token Exchange
    @DocProperty(description = "", defaultValue = "false")
    private Boolean rotateDeviceSecret = false;

    @DocProperty(description = "", defaultValue = "false")
    private Boolean returnDeviceSecretFromAuthzEndpoint = false;

    // DCR
    @DocProperty(description = "Boolean value enables DCR signature validation. Default is false", defaultValue = "false")
    private Boolean dcrSignatureValidationEnabled = false;

    @DocProperty(description = "Specifies shared secret for Dynamic Client Registration")
    private String dcrSignatureValidationSharedSecret;

    @DocProperty(description = "Specifies claim name inside software statement. Value of claim should point to JWKS URI")
    private String dcrSignatureValidationSoftwareStatementJwksURIClaim;

    @DocProperty(description = "Specifies claim name inside software statement. Value of claim should point to inlined JWKS")
    private String dcrSignatureValidationSoftwareStatementJwksClaim;

    @DocProperty(description = "Specifies JWKS for all DCR's validations")
    private String dcrSignatureValidationJwks;

    @DocProperty(description = "Specifies JWKS URI for all DCR's validations")
    private String dcrSignatureValidationJwksUri;

    @DocProperty(description = "Boolean value indicating if DCR authorization to be performed using client credentials", defaultValue = "false")
    private Boolean dcrAuthorizationWithClientCredentials = false;

    @DocProperty(description = "Boolean value indicating if DCR authorization allowed with MTLS", defaultValue = "false")
    private Boolean dcrAuthorizationWithMTLS = false;

    @DocProperty(description = "List of DCR issuers")
    private List<String> dcrIssuers = new ArrayList<>();

    @DocProperty(description = "Cache in local memory cache attributes, scopes, clients and organization entry with expiration 60 seconds", defaultValue = "false")
    private Boolean useLocalCache = false;

    @DocProperty(description = "Boolean value specifying whether to turn on FAPI compatibility mode. If true AS behaves in more strict mode", defaultValue = "false")
    private Boolean fapiCompatibility = false;

    @DocProperty(description = "Boolean value specifying whether force id_token_hint parameter presence", defaultValue = "false")
    private Boolean forceIdTokenHintPrecense = false;

    @DocProperty(description = "default value false. If true and id_token is not found in db, request is rejected", defaultValue = "false")
    private Boolean rejectEndSessionIfIdTokenExpired = false;

    @DocProperty(description = "default value false. If true, sid check will be skipped", defaultValue = "false")
    private Boolean allowEndSessionWithUnmatchedSid = false;

    @DocProperty(description = "Boolean value specifying whether force offline_access scope to enable refresh_token grant type. Default value is true", defaultValue = "true")
    private Boolean forceOfflineAccessScopeToEnableRefreshToken = true;

    @DocProperty(description = "Boolean value specifying whether to return detailed reason of the error from AS. Default value is false", defaultValue = "false")
    private Boolean errorReasonEnabled = false;

    @DocProperty(description = "Boolean value specifying whether to remove Refresh Tokens on logout. Default value is true", defaultValue = "true")
    private Boolean removeRefreshTokensForClientOnLogout = true;

    @DocProperty(description = "Boolean value specifying whether to skip refreshing tokens on refreshing", defaultValue = "false")
    private Boolean skipRefreshTokenDuringRefreshing = false;

    @DocProperty(description = "Boolean value specifying whether to extend refresh tokens on rotation", defaultValue = "false")
    private Boolean refreshTokenExtendLifetimeOnRotation = false;

    @DocProperty(description = "Check whether user exists and is active before creating RefreshToken. Set it to true if check is needed(Default value is false - don't check.", defaultValue = "false")
    private Boolean checkUserPresenceOnRefreshToken = false;

    @DocProperty(description = "Boolean value specifying whether to turn on Consent Gathering Script backward compatibility mode. If true AS will pick up script with higher level globally. If false (default) AS will pick up script based on client configuration", defaultValue = "false")
    private Boolean consentGatheringScriptBackwardCompatibility = false; // means ignore client configuration (as defined in 4.2) and determine it globally (as in 4.1 and earlier)

    @DocProperty(description = "Boolean value specifying whether switch off client's introspection scripts (true value) and run all scripts that exists on server. Default value is false", defaultValue = "false")
    private Boolean introspectionScriptBackwardCompatibility = false; // means ignore client configuration (as defined in 4.2) and determine it globally (as in 4.1 and earlier)

    @DocProperty(description = "Boolean value specifying introspection response backward compatibility mode", defaultValue = "false")
    private Boolean introspectionResponseScopesBackwardCompatibility = false;

    @DocProperty(description = "Validation type used for software statement")
    private String softwareStatementValidationType = SoftwareStatementValidationType.DEFAULT.getValue();

    @DocProperty(description = "Validation claim name for software statement")
    private String softwareStatementValidationClaimName;

    @DocProperty(description = "Authentication Brute Force Protection Configuration")
    private AuthenticationProtectionConfiguration authenticationProtectionConfiguration;

    @DocProperty(description = "A list of possible error handling methods")
    private ErrorHandlingMethod errorHandlingMethod = ErrorHandlingMethod.INTERNAL;

    @DocProperty(description = "Boolean value specifying whether to disable authentication when max_age=0", defaultValue = "false")
    private Boolean disableAuthnForMaxAgeZero;

    @DocProperty(description = "Boolean value specifying whether to keep authenticator attributes on ACR change", defaultValue = "false")
    private Boolean keepAuthenticatorAttributesOnAcrChange = false;

    @DocProperty(description = "Expiration time given for device authorization requests")
    private int deviceAuthzRequestExpiresIn;

    @DocProperty(description = "Default interval returned to the client to process device token requests")
    private int deviceAuthzTokenPollInterval;

    @DocProperty(description = "Response type used to process device authz requests")
    private String deviceAuthzResponseTypeToProcessAuthz;
    // CIBA
    @DocProperty(description = "Backchannel Client Id")
    private String backchannelClientId;

    @DocProperty(description = "Backchannel Redirect Uri")
    private String backchannelRedirectUri;

    @DocProperty(description = "Backchannel Authentication Endpoint")
    private String backchannelAuthenticationEndpoint;

    @DocProperty(description = "Backchannel Device Registration Endpoint")
    private String backchannelDeviceRegistrationEndpoint;

    @DocProperty(description = "Backchannel Token Delivery Modes Supported")
    private List<String> backchannelTokenDeliveryModesSupported;

    @DocProperty(description = "Backchannel Authentication Request Signing Alg Values Supported")
    private List<String> backchannelAuthenticationRequestSigningAlgValuesSupported;

    @DocProperty(description = "Backchannel User Code Parameter Supported")
    private Boolean backchannelUserCodeParameterSupported;

    @DocProperty(description = "Backchannel Binding Message Pattern")
    private String backchannelBindingMessagePattern;

    @DocProperty(description = "Backchannel Authentication Response Expires In")
    private int backchannelAuthenticationResponseExpiresIn;

    @DocProperty(description = "Backchannel Authentication Response Interval")
    private int backchannelAuthenticationResponseInterval;

    @DocProperty(description = "Backchannel Login Hint Claims")
    private List<String> backchannelLoginHintClaims;

    @DocProperty(description = "CIBA End User Notification Config")
    private CIBAEndUserNotificationConfig cibaEndUserNotificationConfig;

    @DocProperty(description = "Specifies the allowable elapsed time in seconds backchannel request processor executes")
    private int backchannelRequestsProcessorJobIntervalSec;

    @DocProperty(description = "Each backchannel request processor iteration fetches chunk of data to be processed")
    private int backchannelRequestsProcessorJobChunkSize;

    @DocProperty(description = "Specifies the CIBA Grant life extra time in seconds")
    private int cibaGrantLifeExtraTimeSec;

    @DocProperty(description = "Specifies the CIBA token expiration time in seconds")
    private int cibaMaxExpirationTimeAllowedSec;

    // DPoP
    @DocProperty(description = "Demonstration of Proof-of-Possession (DPoP) authorization signing algorithms supported")
    private List<String> dpopSigningAlgValuesSupported;

    @DocProperty(description = "Demonstration of Proof-of-Possession (DPoP) timeout", defaultValue = "5")
    private int dpopTimeframe = 5;

    @DocProperty(description = "Demonstration of Proof-of-Possession (DPoP) cache time", defaultValue = "3600")
    private int dpopJtiCacheTime = 3600;

    @DocProperty(description = "Specifies if a token without implicit grant types is allowed")
    private Boolean allowIdTokenWithoutImplicitGrantType;

    @DocProperty(description = "Lifetime of discovery cache", defaultValue = "60")
    private int discoveryCacheLifetimeInMinutes = 60;

    @DocProperty(description = "List of configuration response claim allowed to be displayed in discovery endpoint")
    private List<String> discoveryAllowedKeys;

    @DocProperty(description = "List of configuration response claims which must not be displayed in discovery endpoint response")
    private List<String> discoveryDenyKeys;

    @DocProperty(description = "List of enabled feature flags")
    private List<String> featureFlags;

    @DocProperty(description = "Enable/disable request/response logging filter")
    private Boolean httpLoggingEnabled; // Used in ServletLoggingFilter to enable http request/response logging.

    @DocProperty(description = "This list details the base URIs for which the request/response logging filter will not record activity")
    private Set<String> httpLoggingExcludePaths; // Used in ServletLoggingFilter to exclude some paths from logger. Paths example: ["/jans-auth/img", "/jans-auth/stylesheet"]

    @DocProperty(description = "The path to the external log4j2 logging configuration")
    private String externalLoggerConfiguration; // Path to external log4j2 configuration file. This property might be configured from oxTrust: /identity/logviewer/configure

    @DocProperty(description = "Engine Config which offers an alternative way to build authentication flows in Janssen server")
    private EngineConfig agamaConfiguration;

    @DocProperty(description = "DCR SSA Validation configurations used to perform validation of SSA or DCR")
    private List<SsaValidationConfig> dcrSsaValidationConfigs;

    @DocProperty(description = "SSA Configuration")
    private SsaConfiguration ssaConfiguration;

    @DocProperty(description = "Enable/Disable block authorizations that originate from Webview (Mobile apps).", defaultValue = "false")
    private Boolean blockWebviewAuthorizationEnabled = false;

    @DocProperty(description = "List of key value date formatters, e.g. 'userinfo: 'yyyy-MM-dd', etc.")
    private Map<String, String> dateFormatterPatterns = new HashMap<>();

    public Map<String, String> getDateFormatterPatterns() {
        return dateFormatterPatterns;
    }

    public void setDateFormatterPatterns(Map<String, String> dateFormatterPatterns) {
        this.dateFormatterPatterns = dateFormatterPatterns;
    }

    public List<SsaValidationConfig> getDcrSsaValidationConfigs() {
        if (dcrSsaValidationConfigs == null) dcrSsaValidationConfigs = new ArrayList<>();
        return dcrSsaValidationConfigs;
    }

    public Boolean getRequireRequestObjectEncryption() {
        if (requireRequestObjectEncryption == null) requireRequestObjectEncryption = false;
        return requireRequestObjectEncryption;
    }

    public void setRequireRequestObjectEncryption(Boolean requireRequestObjectEncryption) {
        this.requireRequestObjectEncryption = requireRequestObjectEncryption;
    }

    public Boolean getAllowAllValueForRevokeEndpoint() {
        if (allowAllValueForRevokeEndpoint == null) allowAllValueForRevokeEndpoint = false;
        return allowAllValueForRevokeEndpoint;
    }

    public void setAllowAllValueForRevokeEndpoint(Boolean allowAllValueForRevokeEndpoint) {
        this.allowAllValueForRevokeEndpoint = allowAllValueForRevokeEndpoint;
    }

    public Boolean getReturnDeviceSecretFromAuthzEndpoint() {
        return returnDeviceSecretFromAuthzEndpoint;
    }

    public void setReturnDeviceSecretFromAuthzEndpoint(Boolean returnDeviceSecretFromAuthzEndpoint) {
        this.returnDeviceSecretFromAuthzEndpoint = returnDeviceSecretFromAuthzEndpoint;
    }

    public Boolean getRotateDeviceSecret() {
        if (rotateDeviceSecret == null) rotateDeviceSecret = false;
        return rotateDeviceSecret;
    }

    public void setRotateDeviceSecret(Boolean rotateDeviceSecret) {
        this.rotateDeviceSecret = rotateDeviceSecret;
    }

    public Boolean getRequirePkce() {
        if (requirePkce == null) requirePkce = false;
        return requirePkce;
    }

    public void setRequirePkce(Boolean requirePkce) {
        this.requirePkce = requirePkce;
    }

    public Boolean getAllowIdTokenWithoutImplicitGrantType() {
        if (allowIdTokenWithoutImplicitGrantType == null) allowIdTokenWithoutImplicitGrantType = false;
        return allowIdTokenWithoutImplicitGrantType;
    }

    public void setAllowIdTokenWithoutImplicitGrantType(Boolean allowIdTokenWithoutImplicitGrantType) {
        this.allowIdTokenWithoutImplicitGrantType = allowIdTokenWithoutImplicitGrantType;
    }

    public List<String> getDiscoveryDenyKeys() {
        if (discoveryDenyKeys == null) discoveryDenyKeys = new ArrayList<>();
        return discoveryDenyKeys;
    }

    public void setDiscoveryDenyKeys(List<String> discoveryDenyKeys) {
        this.discoveryDenyKeys = discoveryDenyKeys;
    }

    public List<String> getDiscoveryAllowedKeys() {
        if (discoveryAllowedKeys == null) discoveryAllowedKeys = new ArrayList<>();
        return discoveryAllowedKeys;
    }

    public void setDiscoveryAllowedKeys(List<String> discoveryAllowedKeys) {
        this.discoveryAllowedKeys = discoveryAllowedKeys;
    }

    public Boolean getCheckUserPresenceOnRefreshToken() {
        if (checkUserPresenceOnRefreshToken == null) checkUserPresenceOnRefreshToken = false;
        return checkUserPresenceOnRefreshToken;
    }

    public void setCheckUserPresenceOnRefreshToken(Boolean checkUserPresenceOnRefreshToken) {
        this.checkUserPresenceOnRefreshToken = checkUserPresenceOnRefreshToken;
    }

    public boolean isFeatureEnabled(FeatureFlagType flagType) {
        final Set<FeatureFlagType> flags = FeatureFlagType.from(this);
        if (flags.isEmpty())
            return true;

        return flags.contains(flagType);
    }

    public List<String> getFeatureFlags() {
        if (featureFlags == null) featureFlags = new ArrayList<>();
        return featureFlags;
    }

    public void setFeatureFlags(List<String> featureFlags) {
        this.featureFlags = featureFlags;
    }

    public Boolean isUseNestedJwtDuringEncryption() {
        if (useNestedJwtDuringEncryption == null) useNestedJwtDuringEncryption = true;
        return useNestedJwtDuringEncryption;
    }

    public void setUseNestedJwtDuringEncryption(Boolean useNestedJwtDuringEncryption) {
        this.useNestedJwtDuringEncryption = useNestedJwtDuringEncryption;
    }

    public KeySelectionStrategy getKeySelectionStrategy() {
        if (keySelectionStrategy == null) keySelectionStrategy = DEFAULT_KEY_SELECTION_STRATEGY;
        return keySelectionStrategy;
    }

    public void setKeySelectionStrategy(KeySelectionStrategy keySelectionStrategy) {
        this.keySelectionStrategy = keySelectionStrategy;
    }

    public Boolean getKeySignWithSameKeyButDiffAlg() {
        if (keySignWithSameKeyButDiffAlg == null) keySignWithSameKeyButDiffAlg = false;
        return keySignWithSameKeyButDiffAlg;
    }

    public void setKeySignWithSameKeyButDiffAlg(Boolean keySignWithSameKeyButDiffAlg) {
        this.keySignWithSameKeyButDiffAlg = keySignWithSameKeyButDiffAlg;
    }

    public String getStaticKid() {
        return staticKid;
    }

    public void setStaticKid(String staticKid) {
        this.staticKid = staticKid;
    }

    public String getStaticDecryptionKid() {
        return staticDecryptionKid;
    }

    public void setStaticDecryptionKid(String staticDecryptionKid) {
        this.staticDecryptionKid = staticDecryptionKid;
    }

    public List<String> getKeyAlgsAllowedForGeneration() {
        if (keyAlgsAllowedForGeneration == null) keyAlgsAllowedForGeneration = new ArrayList<>();
        return keyAlgsAllowedForGeneration;
    }

    public void setKeyAlgsAllowedForGeneration(List<String> keyAlgsAllowedForGeneration) {
        this.keyAlgsAllowedForGeneration = keyAlgsAllowedForGeneration;
    }

    public int getDiscoveryCacheLifetimeInMinutes() {
        return discoveryCacheLifetimeInMinutes;
    }

    public void setDiscoveryCacheLifetimeInMinutes(int discoveryCacheLifetimeInMinutes) {
        this.discoveryCacheLifetimeInMinutes = discoveryCacheLifetimeInMinutes;
    }

    public String getSoftwareStatementValidationType() {
        if (softwareStatementValidationType == null) {
            softwareStatementValidationType = SoftwareStatementValidationType.DEFAULT.getValue();
            return softwareStatementValidationType;
        }
        return softwareStatementValidationType;
    }

    public String getSoftwareStatementValidationClaimName() {
        return softwareStatementValidationClaimName;
    }

    public void setSoftwareStatementValidationType(String softwareStatementValidationType) {
        this.softwareStatementValidationType = softwareStatementValidationType;
    }

    public void setSoftwareStatementValidationClaimName(String softwareStatementValidationClaimName) {
        this.softwareStatementValidationClaimName = softwareStatementValidationClaimName;
    }

    public Boolean getSkipRefreshTokenDuringRefreshing() {
        if (skipRefreshTokenDuringRefreshing == null) skipRefreshTokenDuringRefreshing = false;
        return skipRefreshTokenDuringRefreshing;
    }

    public void setSkipRefreshTokenDuringRefreshing(Boolean skipRefreshTokenDuringRefreshing) {
        this.skipRefreshTokenDuringRefreshing = skipRefreshTokenDuringRefreshing;
    }

    public Boolean getRefreshTokenExtendLifetimeOnRotation() {
        if (refreshTokenExtendLifetimeOnRotation == null) refreshTokenExtendLifetimeOnRotation = false;
        return refreshTokenExtendLifetimeOnRotation;
    }

    public void setRefreshTokenExtendLifetimeOnRotation(Boolean refreshTokenExtendLifetimeOnRotation) {
        this.refreshTokenExtendLifetimeOnRotation = refreshTokenExtendLifetimeOnRotation;
    }

    public int getSectorIdentifierCacheLifetimeInMinutes() {
        return sectorIdentifierCacheLifetimeInMinutes;
    }

    public void setSectorIdentifierCacheLifetimeInMinutes(int sectorIdentifierCacheLifetimeInMinutes) {
        this.sectorIdentifierCacheLifetimeInMinutes = sectorIdentifierCacheLifetimeInMinutes;
    }

    public Boolean getExpirationNotificatorEnabled() {
        if (expirationNotificatorEnabled == null) expirationNotificatorEnabled = false;
        return expirationNotificatorEnabled;
    }

    public void setExpirationNotificatorEnabled(Boolean expirationNotificatorEnabled) {
        this.expirationNotificatorEnabled = expirationNotificatorEnabled;
    }

    public int getExpirationNotificatorMapSizeLimit() {
        if (expirationNotificatorMapSizeLimit == 0) expirationNotificatorMapSizeLimit = 100000;
        return expirationNotificatorMapSizeLimit;
    }

    public void setExpirationNotificatorMapSizeLimit(int expirationNotificatorMapSizeLimit) {
        this.expirationNotificatorMapSizeLimit = expirationNotificatorMapSizeLimit;
    }

    public int getExpirationNotificatorIntervalInSeconds() {
        return expirationNotificatorIntervalInSeconds;
    }

    public void setExpirationNotificatorIntervalInSeconds(int expirationNotificatorIntervalInSeconds) {
        this.expirationNotificatorIntervalInSeconds = expirationNotificatorIntervalInSeconds;
    }

    public Boolean getRejectJwtWithNoneAlg() {
        if (rejectJwtWithNoneAlg == null) rejectJwtWithNoneAlg = true;
        return rejectJwtWithNoneAlg;
    }

    public void setRejectJwtWithNoneAlg(Boolean rejectJwtWithNoneAlg) {
        this.rejectJwtWithNoneAlg = rejectJwtWithNoneAlg;
    }

    public Boolean getIntrospectionScriptBackwardCompatibility() {
        if (introspectionScriptBackwardCompatibility == null) introspectionScriptBackwardCompatibility = false;
        return introspectionScriptBackwardCompatibility;
    }

    public void setIntrospectionScriptBackwardCompatibility(Boolean introspectionScriptBackwardCompatibility) {
        this.introspectionScriptBackwardCompatibility = introspectionScriptBackwardCompatibility;
    }

    public Boolean getIntrospectionResponseScopesBackwardCompatibility() {
        if (introspectionResponseScopesBackwardCompatibility == null)
            introspectionResponseScopesBackwardCompatibility = false;
        return introspectionScriptBackwardCompatibility;
    }

    public void setIntrospectionResponseScopesBackwardCompatibility(Boolean introspectionResponseScopesBackwardCompatibility) {
        this.introspectionResponseScopesBackwardCompatibility = introspectionResponseScopesBackwardCompatibility;
    }

    public Boolean getConsentGatheringScriptBackwardCompatibility() {
        if (consentGatheringScriptBackwardCompatibility == null) consentGatheringScriptBackwardCompatibility = false;
        return consentGatheringScriptBackwardCompatibility;
    }

    public void setConsentGatheringScriptBackwardCompatibility(Boolean consentGatheringScriptBackwardCompatibility) {
        this.consentGatheringScriptBackwardCompatibility = consentGatheringScriptBackwardCompatibility;
    }

    public Boolean getErrorReasonEnabled() {
        if (errorReasonEnabled == null) errorReasonEnabled = false;
        return errorReasonEnabled;
    }

    public void setErrorReasonEnabled(Boolean errorReasonEnabled) {
        this.errorReasonEnabled = errorReasonEnabled;
    }

    public Boolean getForceOfflineAccessScopeToEnableRefreshToken() {
        if (forceOfflineAccessScopeToEnableRefreshToken == null) forceOfflineAccessScopeToEnableRefreshToken = true;
        return forceOfflineAccessScopeToEnableRefreshToken;
    }

    public void setForceOfflineAccessScopeToEnableRefreshToken(Boolean forceOfflineAccessScopeToEnableRefreshToken) {
        this.forceOfflineAccessScopeToEnableRefreshToken = forceOfflineAccessScopeToEnableRefreshToken;
    }

    public Boolean getDisablePromptLogin() {
        if (disablePromptLogin == null) disablePromptLogin = false;
        return disablePromptLogin;
    }

    public void setDisablePromptLogin(Boolean disablePromptLogin) {
        this.disablePromptLogin = disablePromptLogin;
    }

    public Boolean getIncludeSidInResponse() {
        if (includeSidInResponse == null) includeSidInResponse = false;
        return includeSidInResponse;
    }

    public void setIncludeSidInResponse(Boolean includeSidInResponse) {
        this.includeSidInResponse = includeSidInResponse;
    }

    public Boolean getSessionIdPersistInCache() {
        if (sessionIdPersistInCache == null) sessionIdPersistInCache = false;
        return sessionIdPersistInCache;
    }

    public void setSessionIdPersistInCache(Boolean sessionIdPersistInCache) {
        this.sessionIdPersistInCache = sessionIdPersistInCache;
    }

    public Boolean getChangeSessionIdOnAuthentication() {
        if (changeSessionIdOnAuthentication == null) changeSessionIdOnAuthentication = true;
        return changeSessionIdOnAuthentication;
    }

    public void setChangeSessionIdOnAuthentication(Boolean changeSessionIdOnAuthentication) {
        this.changeSessionIdOnAuthentication = changeSessionIdOnAuthentication;
    }

    public Boolean getReturnClientSecretOnRead() {
        if (returnClientSecretOnRead == null) returnClientSecretOnRead = false;
        return returnClientSecretOnRead;
    }

    public void setReturnClientSecretOnRead(Boolean returnClientSecretOnRead) {
        this.returnClientSecretOnRead = returnClientSecretOnRead;
    }

    public boolean isFapi() {
        return Boolean.TRUE.equals(getFapiCompatibility());
    }

    public Boolean getFapiCompatibility() {
        if (fapiCompatibility == null) fapiCompatibility = false;
        return fapiCompatibility;
    }

    public void setFapiCompatibility(Boolean fapiCompatibility) {
        this.fapiCompatibility = fapiCompatibility;
    }

    public Boolean getDcrAuthorizationWithClientCredentials() {
        if (dcrAuthorizationWithClientCredentials == null) dcrAuthorizationWithClientCredentials = false;
        return dcrAuthorizationWithClientCredentials;
    }

    public void setDcrAuthorizationWithClientCredentials(Boolean dcrAuthorizationWithClientCredentials) {
        this.dcrAuthorizationWithClientCredentials = dcrAuthorizationWithClientCredentials;
    }

    public String getDcrSignatureValidationSharedSecret() {
        return dcrSignatureValidationSharedSecret;
    }

    public void setDcrSignatureValidationSharedSecret(String dcrSignatureValidationSharedSecret) {
        this.dcrSignatureValidationSharedSecret = dcrSignatureValidationSharedSecret;
    }

    public Boolean getDcrSignatureValidationEnabled() {
        if (dcrSignatureValidationEnabled == null) dcrSignatureValidationEnabled = false;
        return dcrSignatureValidationEnabled;
    }

    public void setDcrSignatureValidationEnabled(Boolean dcrSignatureValidationEnabled) {
        this.dcrSignatureValidationEnabled = dcrSignatureValidationEnabled;
    }

    public String getDcrSignatureValidationSoftwareStatementJwksURIClaim() {
        return dcrSignatureValidationSoftwareStatementJwksURIClaim;
    }

    public void setDcrSignatureValidationSoftwareStatementJwksURIClaim(String dcrSignatureValidationSoftwareStatementJwksURIClaim) {
        this.dcrSignatureValidationSoftwareStatementJwksURIClaim = dcrSignatureValidationSoftwareStatementJwksURIClaim;
    }

    public String getDcrSignatureValidationSoftwareStatementJwksClaim() {
        return dcrSignatureValidationSoftwareStatementJwksClaim;
    }

    public void setDcrSignatureValidationSoftwareStatementJwksClaim(String dcrSignatureValidationSoftwareStatementJwksClaim) {
        this.dcrSignatureValidationSoftwareStatementJwksClaim = dcrSignatureValidationSoftwareStatementJwksClaim;
    }

    public String getDcrSignatureValidationJwks() {
        return dcrSignatureValidationJwks;
    }

    public void setDcrSignatureValidationJwks(String dcrSignatureValidationJwks) {
        this.dcrSignatureValidationJwks = dcrSignatureValidationJwks;
    }

    public String getDcrSignatureValidationJwksUri() {
        return dcrSignatureValidationJwksUri;
    }

    public void setDcrSignatureValidationJwksUri(String dcrSignatureValidationJwksUri) {
        this.dcrSignatureValidationJwksUri = dcrSignatureValidationJwksUri;
    }

    public Boolean getDcrAuthorizationWithMTLS() {
        if (dcrAuthorizationWithMTLS == null) dcrAuthorizationWithMTLS = false;
        return dcrAuthorizationWithMTLS;
    }

    public void setDcrAuthorizationWithMTLS(Boolean dcrAuthorizationWithMTLS) {
        this.dcrAuthorizationWithMTLS = dcrAuthorizationWithMTLS;
    }

    public List<String> getDcrIssuers() {
        if (dcrIssuers == null) dcrIssuers = new ArrayList<>();
        return dcrIssuers;
    }

    public void setDcrIssuers(List<String> dcrIssuers) {
        this.dcrIssuers = dcrIssuers;
    }

    public Boolean getForceIdTokenHintPrecense() {
        if (forceIdTokenHintPrecense == null) forceIdTokenHintPrecense = false;
        return forceIdTokenHintPrecense;
    }

    public void setForceIdTokenHintPrecense(Boolean forceIdTokenHintPrecense) {
        this.forceIdTokenHintPrecense = forceIdTokenHintPrecense;
    }

    public Boolean getRejectEndSessionIfIdTokenExpired() {
        return rejectEndSessionIfIdTokenExpired;
    }

    public void setRejectEndSessionIfIdTokenExpired(Boolean rejectEndSessionIfIdTokenExpired) {
        this.rejectEndSessionIfIdTokenExpired = rejectEndSessionIfIdTokenExpired;
    }

    public Boolean getAllowEndSessionWithUnmatchedSid() {
        return allowEndSessionWithUnmatchedSid;
    }

    public void setAllowEndSessionWithUnmatchedSid(Boolean allowEndSessionWithUnmatchedSid) {
        this.allowEndSessionWithUnmatchedSid = allowEndSessionWithUnmatchedSid;
    }

    public Boolean getRemoveRefreshTokensForClientOnLogout() {
        if (removeRefreshTokensForClientOnLogout == null) removeRefreshTokensForClientOnLogout = true;
        return removeRefreshTokensForClientOnLogout;
    }

    public void setRemoveRefreshTokensForClientOnLogout(Boolean removeRefreshTokensForClientOnLogout) {
        this.removeRefreshTokensForClientOnLogout = removeRefreshTokensForClientOnLogout;
    }

    public Boolean getDisableJdkLogger() {
        return disableJdkLogger;
    }

    public void setDisableJdkLogger(Boolean disableJdkLogger) {
        this.disableJdkLogger = disableJdkLogger;
    }

    public Boolean getFrontChannelLogoutSessionSupported() {
        return frontChannelLogoutSessionSupported;
    }

    public void setFrontChannelLogoutSessionSupported(
            Boolean frontChannelLogoutSessionSupported) {
        this.frontChannelLogoutSessionSupported = frontChannelLogoutSessionSupported;
    }

    public Boolean getIntrospectionAccessTokenMustHaveUmaProtectionScope() {
        return introspectionAccessTokenMustHaveUmaProtectionScope;
    }

    public void setIntrospectionAccessTokenMustHaveUmaProtectionScope(Boolean introspectionAccessTokenMustHaveUmaProtectionScope) {
        this.introspectionAccessTokenMustHaveUmaProtectionScope = introspectionAccessTokenMustHaveUmaProtectionScope;
    }

    public Boolean getIntrospectionSkipAuthorization() {
        if (introspectionSkipAuthorization == null) introspectionSkipAuthorization = false;
        return introspectionSkipAuthorization;
    }

    public void setIntrospectionSkipAuthorization(Boolean introspectionSkipAuthorization) {
        this.introspectionSkipAuthorization = introspectionSkipAuthorization;
    }

    public Boolean getUmaRptAsJwt() {
        return umaRptAsJwt;
    }

    public void setUmaRptAsJwt(Boolean umaRptAsJwt) {
        this.umaRptAsJwt = umaRptAsJwt;
    }

    public Boolean getUmaAddScopesAutomatically() {
        return umaAddScopesAutomatically;
    }

    public void setUmaAddScopesAutomatically(Boolean umaAddScopesAutomatically) {
        this.umaAddScopesAutomatically = umaAddScopesAutomatically;
    }

    public Boolean getUmaValidateClaimToken() {
        return umaValidateClaimToken;
    }

    public void setUmaValidateClaimToken(Boolean umaValidateClaimToken) {
        this.umaValidateClaimToken = umaValidateClaimToken;
    }

    public Boolean getUmaGrantAccessIfNoPolicies() {
        return umaGrantAccessIfNoPolicies;
    }

    public void setUmaGrantAccessIfNoPolicies(Boolean umaGrantAccessIfNoPolicies) {
        this.umaGrantAccessIfNoPolicies = umaGrantAccessIfNoPolicies;
    }

    public Boolean getUmaRestrictResourceToAssociatedClient() {
        return umaRestrictResourceToAssociatedClient;
    }

    public void setUmaRestrictResourceToAssociatedClient(Boolean umaRestrictResourceToAssociatedClient) {
        this.umaRestrictResourceToAssociatedClient = umaRestrictResourceToAssociatedClient;
    }

    /**
     * Returns the issuer identifier.
     *
     * @return The issuer identifier.
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Sets the issuer identifier.
     *
     * @param issuer The issuer identifier.
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * Returns the base URI of the endpoints.
     *
     * @return The base URI of endpoints.
     */
    public String getBaseEndpoint() {
        return baseEndpoint;
    }

    /**
     * Sets the base URI of the endpoints.
     *
     * @param baseEndpoint The base URI of the endpoints.
     */
    public void setBaseEndpoint(String baseEndpoint) {
        this.baseEndpoint = baseEndpoint;
    }

    /**
     * Returns the URL of the Authentication and Authorization endpoint.
     *
     * @return The URL of the Authentication and Authorization endpoint.
     */
    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    /**
     * Sets the URL of the Authentication and Authorization endpoint.
     *
     * @param authorizationEndpoint The URL of the Authentication and Authorization endpoint.
     */
    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    /**
     * Returns the URL of the Token endpoint.
     *
     * @return The URL of the Token endpoint.
     */
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    /**
     * Sets the URL of the Token endpoint.
     *
     * @param tokenEndpoint The URL of the Token endpoint.
     */
    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    /**
     * Returns the URL of the Token Revocation endpoint.
     *
     * @return The URL of the Token Revocation endpoint.
     */
    public String getTokenRevocationEndpoint() {
        return tokenRevocationEndpoint;
    }

    /**
     * Sets the URL of the Token Revocation endpoint.
     *
     * @param tokenRevocationEndpoint The URL of the Token Revocation endpoint.
     */
    public void setTokenRevocationEndpoint(String tokenRevocationEndpoint) {
        this.tokenRevocationEndpoint = tokenRevocationEndpoint;
    }

    /**
     * Returns the URL of the User Info endpoint.
     *
     * @return The URL of the User Info endpoint.
     */
    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    /**
     * Sets the URL for the User Info endpoint.
     *
     * @param userInfoEndpoint The URL for the User Info endpoint.
     */
    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    /**
     * Returns the URL od the Client Info endpoint.
     *
     * @return The URL of the Client Info endpoint.
     */
    public String getClientInfoEndpoint() {
        return clientInfoEndpoint;
    }

    /**
     * Sets the URL for the Client Info endpoint.
     *
     * @param clientInfoEndpoint The URL for the Client Info endpoint.
     */
    public void setClientInfoEndpoint(String clientInfoEndpoint) {
        this.clientInfoEndpoint = clientInfoEndpoint;
    }

    /**
     * Returns the URL of an OP endpoint that provides a page to support cross-origin
     * communications for session state information with the RP client.
     *
     * @return The Check Session iFrame URL.
     */
    public String getCheckSessionIFrame() {
        return checkSessionIFrame;
    }

    /**
     * Sets the  URL of an OP endpoint that provides a page to support cross-origin
     * communications for session state information with the RP client.
     *
     * @param checkSessionIFrame The Check Session iFrame URL.
     */
    public void setCheckSessionIFrame(String checkSessionIFrame) {
        this.checkSessionIFrame = checkSessionIFrame;
    }

    /**
     * Returns the URL of the End Session endpoint.
     *
     * @return The URL of the End Session endpoint.
     */
    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    /**
     * Sets the URL of the End Session endpoint.
     *
     * @param endSessionEndpoint The URL of the End Session endpoint.
     */
    public void setEndSessionEndpoint(String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }

    /**
     * Returns the URL of the OP's JSON Web Key Set (JWK) document that contains the Server's signing key(s)
     * that are used for signing responses to the Client.
     * The JWK Set may also contain the Server's encryption key(s) that are used by the Client to encrypt
     * requests to the Server.
     *
     * @return The URL of the OP's JSON Web Key Set (JWK) document.
     */
    public String getJwksUri() {
        return jwksUri;
    }

    /**
     * Sets the URL of the OP's JSON Web Key Set (JWK) document that contains the Server's signing key(s)
     * that are used for signing responses to the Client.
     * The JWK Set may also contain the Server's encryption key(s) that are used by the Client to encrypt
     * requests to the Server.
     *
     * @param jwksUri The URL of the OP's JSON Web Key Set (JWK) document.
     */
    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    /**
     * Returns the URL of the Dynamic Client Registration endpoint.
     *
     * @return The URL of the Dynamic Client Registration endpoint.
     */
    public String getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    /**
     * Sets the URL of the Dynamic Client Registration endpoint.
     *
     * @param registrationEndpoint The URL of the Dynamic Client Registration endpoint.
     */
    public void setRegistrationEndpoint(String registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }

    public String getOpenIdDiscoveryEndpoint() {
        return openIdDiscoveryEndpoint;
    }

    public void setOpenIdDiscoveryEndpoint(String openIdDiscoveryEndpoint) {
        this.openIdDiscoveryEndpoint = openIdDiscoveryEndpoint;
    }

    public String getUmaConfigurationEndpoint() {
        return umaConfigurationEndpoint;
    }

    public void setUmaConfigurationEndpoint(String umaConfigurationEndpoint) {
        this.umaConfigurationEndpoint = umaConfigurationEndpoint;
    }

    public String getOpenidSubAttribute() {
        return openidSubAttribute;
    }

    public void setOpenidSubAttribute(String openidSubAttribute) {
        this.openidSubAttribute = openidSubAttribute;
    }

    public Boolean getPublicSubjectIdentifierPerClientEnabled() {
        if (publicSubjectIdentifierPerClientEnabled == null) {
            publicSubjectIdentifierPerClientEnabled = false;
        }

        return publicSubjectIdentifierPerClientEnabled;
    }

    public void setPublicSubjectIdentifierPerClientEnabled(Boolean publicSubjectIdentifierPerClientEnabled) {
        this.publicSubjectIdentifierPerClientEnabled = publicSubjectIdentifierPerClientEnabled;
    }

    public List<String> getSubjectIdentifiersPerClientSupported() {
        if (subjectIdentifiersPerClientSupported == null) {
            subjectIdentifiersPerClientSupported = new ArrayList<>();
        }

        return subjectIdentifiersPerClientSupported;
    }

    public void setSubjectIdentifiersPerClientSupported(List<String> subjectIdentifiersPerClientSupported) {
        this.subjectIdentifiersPerClientSupported = subjectIdentifiersPerClientSupported;
    }

    public String getIdGenerationEndpoint() {
        return idGenerationEndpoint;
    }

    public void setIdGenerationEndpoint(String idGenerationEndpoint) {
        this.idGenerationEndpoint = idGenerationEndpoint;
    }

    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
    }

    public String getParEndpoint() {
        return parEndpoint;
    }

    public void setParEndpoint(String parEndpoint) {
        this.parEndpoint = parEndpoint;
    }

    public Boolean getRequirePar() {
        if (requirePar == null) requirePar = false;
        return requirePar;
    }

    public void setRequirePar(Boolean requirePar) {
        this.requirePar = requirePar;
    }

    public String getOpenIdConfigurationEndpoint() {
        return openIdConfigurationEndpoint;
    }

    public void setOpenIdConfigurationEndpoint(String openIdConfigurationEndpoint) {
        this.openIdConfigurationEndpoint = openIdConfigurationEndpoint;
    }

    public Set<Set<ResponseType>> getResponseTypesSupported() {
        return responseTypesSupported;
    }

    public Set<ResponseType> getAllResponseTypesSupported() {
        Set<ResponseType> types = new HashSet<>();
        if (responseTypesSupported != null) {
            for (Set<ResponseType> set : responseTypesSupported) {
                types.addAll(set);
            }
        }
        return types;
    }

    public void setResponseTypesSupported(Set<Set<ResponseType>> responseTypesSupported) {
        this.responseTypesSupported = responseTypesSupported;
    }

    public Set<ResponseMode> getResponseModesSupported() {
        return responseModesSupported;
    }

    public void setResponseModesSupported(Set<ResponseMode> responseModesSupported) {
        this.responseModesSupported = responseModesSupported;
    }

    public Set<GrantType> getGrantTypesSupported() {
        return grantTypesSupported;
    }

    public void setGrantTypesSupported(Set<GrantType> grantTypesSupported) {
        this.grantTypesSupported = grantTypesSupported;
    }

    public List<String> getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    public void setSubjectTypesSupported(List<String> subjectTypesSupported) {
        this.subjectTypesSupported = subjectTypesSupported;
    }

    public String getDefaultSubjectType() {
        return defaultSubjectType;
    }

    public void setDefaultSubjectType(String defaultSubjectType) {
        this.defaultSubjectType = defaultSubjectType;
    }

    public List<String> getAuthorizationSigningAlgValuesSupported() {
        return authorizationSigningAlgValuesSupported;
    }

    public void setAuthorizationSigningAlgValuesSupported(List<String> authorizationSigningAlgValuesSupported) {
        this.authorizationSigningAlgValuesSupported = authorizationSigningAlgValuesSupported;
    }

    public List<String> getAuthorizationEncryptionAlgValuesSupported() {
        return authorizationEncryptionAlgValuesSupported;
    }

    public void setAuthorizationEncryptionAlgValuesSupported(List<String> authorizationEncryptionAlgValuesSupported) {
        this.authorizationEncryptionAlgValuesSupported = authorizationEncryptionAlgValuesSupported;
    }

    public List<String> getAuthorizationEncryptionEncValuesSupported() {
        return authorizationEncryptionEncValuesSupported;
    }

    public void setAuthorizationEncryptionEncValuesSupported(List<String> authorizationEncryptionEncValuesSupported) {
        this.authorizationEncryptionEncValuesSupported = authorizationEncryptionEncValuesSupported;
    }

    public List<String> getUserInfoSigningAlgValuesSupported() {
        return userInfoSigningAlgValuesSupported;
    }

    public void setUserInfoSigningAlgValuesSupported(List<String> userInfoSigningAlgValuesSupported) {
        this.userInfoSigningAlgValuesSupported = userInfoSigningAlgValuesSupported;
    }

    public List<String> getUserInfoEncryptionAlgValuesSupported() {
        return userInfoEncryptionAlgValuesSupported;
    }

    public void setUserInfoEncryptionAlgValuesSupported(List<String> userInfoEncryptionAlgValuesSupported) {
        this.userInfoEncryptionAlgValuesSupported = userInfoEncryptionAlgValuesSupported;
    }

    public int getStatTimerIntervalInSeconds() {
        return statTimerIntervalInSeconds;
    }

    public void setStatTimerIntervalInSeconds(int statTimerIntervalInSeconds) {
        this.statTimerIntervalInSeconds = statTimerIntervalInSeconds;
    }

    public String getStatAuthorizationScope() {
        if (statAuthorizationScope == null) statAuthorizationScope = DEFAULT_STAT_SCOPE;
        return statAuthorizationScope;
    }

    public void setStatAuthorizationScope(String statAuthorizationScope) {
        this.statAuthorizationScope = statAuthorizationScope;
    }

    public List<String> getUserInfoEncryptionEncValuesSupported() {
        return userInfoEncryptionEncValuesSupported;
    }

    public void setUserInfoEncryptionEncValuesSupported(List<String> userInfoEncryptionEncValuesSupported) {
        this.userInfoEncryptionEncValuesSupported = userInfoEncryptionEncValuesSupported;
    }

    public List<String> getIdTokenSigningAlgValuesSupported() {
        return idTokenSigningAlgValuesSupported;
    }

    public void setIdTokenSigningAlgValuesSupported(List<String> idTokenSigningAlgValuesSupported) {
        this.idTokenSigningAlgValuesSupported = idTokenSigningAlgValuesSupported;
    }

    public List<String> getIdTokenEncryptionAlgValuesSupported() {
        return idTokenEncryptionAlgValuesSupported;
    }

    public void setIdTokenEncryptionAlgValuesSupported(List<String> idTokenEncryptionAlgValuesSupported) {
        this.idTokenEncryptionAlgValuesSupported = idTokenEncryptionAlgValuesSupported;
    }

    public List<String> getIdTokenEncryptionEncValuesSupported() {
        return idTokenEncryptionEncValuesSupported;
    }

    public void setIdTokenEncryptionEncValuesSupported(List<String> idTokenEncryptionEncValuesSupported) {
        this.idTokenEncryptionEncValuesSupported = idTokenEncryptionEncValuesSupported;
    }

    public List<String> getAccessTokenSigningAlgValuesSupported() {
        return accessTokenSigningAlgValuesSupported;
    }

    public void setAccessTokenSigningAlgValuesSupported(List<String> accessTokenSigningAlgValuesSupported) {
        this.accessTokenSigningAlgValuesSupported = accessTokenSigningAlgValuesSupported;
    }

    public Boolean getForceSignedRequestObject() {
        if (forceSignedRequestObject == null) {
            return false;
        }

        return forceSignedRequestObject;
    }

    public void setForceSignedRequestObject(Boolean forceSignedRequestObject) {
        this.forceSignedRequestObject = forceSignedRequestObject;
    }

    public List<String> getRequestObjectSigningAlgValuesSupported() {
        return requestObjectSigningAlgValuesSupported;
    }

    public void setRequestObjectSigningAlgValuesSupported(List<String> requestObjectSigningAlgValuesSupported) {
        this.requestObjectSigningAlgValuesSupported = requestObjectSigningAlgValuesSupported;
    }

    public List<String> getRequestObjectEncryptionAlgValuesSupported() {
        return requestObjectEncryptionAlgValuesSupported;
    }

    public void setRequestObjectEncryptionAlgValuesSupported(List<String> requestObjectEncryptionAlgValuesSupported) {
        this.requestObjectEncryptionAlgValuesSupported = requestObjectEncryptionAlgValuesSupported;
    }

    public List<String> getRequestObjectEncryptionEncValuesSupported() {
        return requestObjectEncryptionEncValuesSupported;
    }

    public void setRequestObjectEncryptionEncValuesSupported(List<String> requestObjectEncryptionEncValuesSupported) {
        this.requestObjectEncryptionEncValuesSupported = requestObjectEncryptionEncValuesSupported;
    }

    public List<String> getTokenEndpointAuthMethodsSupported() {
        return tokenEndpointAuthMethodsSupported;
    }

    public void setTokenEndpointAuthMethodsSupported(List<String> tokenEndpointAuthMethodsSupported) {
        this.tokenEndpointAuthMethodsSupported = tokenEndpointAuthMethodsSupported;
    }

    public List<String> getTokenEndpointAuthSigningAlgValuesSupported() {
        return tokenEndpointAuthSigningAlgValuesSupported;
    }

    public void setTokenEndpointAuthSigningAlgValuesSupported(List<String> tokenEndpointAuthSigningAlgValuesSupported) {
        this.tokenEndpointAuthSigningAlgValuesSupported = tokenEndpointAuthSigningAlgValuesSupported;
    }

    public JsonNode getDynamicRegistrationDefaultCustomAttributes() {
        return dynamicRegistrationDefaultCustomAttributes;
    }

    public void setDynamicRegistrationDefaultCustomAttributes(JsonNode dynamicRegistrationDefaultCustomAttributes) {
        this.dynamicRegistrationDefaultCustomAttributes = dynamicRegistrationDefaultCustomAttributes;
    }

    public List<String> getDynamicRegistrationCustomAttributes() {
        return dynamicRegistrationCustomAttributes;
    }

    public void setDynamicRegistrationCustomAttributes(List<String> dynamicRegistrationCustomAttributes) {
        this.dynamicRegistrationCustomAttributes = dynamicRegistrationCustomAttributes;
    }

    public List<String> getDisplayValuesSupported() {
        return displayValuesSupported;
    }

    public void setDisplayValuesSupported(List<String> displayValuesSupported) {
        this.displayValuesSupported = displayValuesSupported;
    }

    public List<String> getClaimTypesSupported() {
        return claimTypesSupported;
    }

    public void setClaimTypesSupported(List<String> claimTypesSupported) {
        this.claimTypesSupported = claimTypesSupported;
    }

    public List<String> getJwksAlgorithmsSupported() {
        return jwksAlgorithmsSupported;
    }

    public void setJwksAlgorithmsSupported(List<String> jwksAlgorithmsSupported) {
        this.jwksAlgorithmsSupported = jwksAlgorithmsSupported;
    }

    public String getServiceDocumentation() {
        return serviceDocumentation;
    }

    public void setServiceDocumentation(String serviceDocumentation) {
        this.serviceDocumentation = serviceDocumentation;
    }

    public List<String> getClaimsLocalesSupported() {
        return claimsLocalesSupported;
    }

    public void setClaimsLocalesSupported(List<String> claimsLocalesSupported) {
        this.claimsLocalesSupported = claimsLocalesSupported;
    }

    public List<String> getIdTokenTokenBindingCnfValuesSupported() {
        if (idTokenTokenBindingCnfValuesSupported == null) {
            idTokenTokenBindingCnfValuesSupported = new ArrayList<>();
        }
        return idTokenTokenBindingCnfValuesSupported;
    }

    public void setIdTokenTokenBindingCnfValuesSupported(List<String> idTokenTokenBindingCnfValuesSupported) {
        this.idTokenTokenBindingCnfValuesSupported = idTokenTokenBindingCnfValuesSupported;
    }

    public List<String> getUiLocalesSupported() {
        return uiLocalesSupported;
    }

    public void setUiLocalesSupported(List<String> uiLocalesSupported) {
        this.uiLocalesSupported = uiLocalesSupported;
    }

    public Boolean getClaimsParameterSupported() {
        return claimsParameterSupported;
    }

    public void setClaimsParameterSupported(Boolean claimsParameterSupported) {
        this.claimsParameterSupported = claimsParameterSupported;
    }

    public Boolean getRequestParameterSupported() {
        return requestParameterSupported;
    }

    public void setRequestParameterSupported(Boolean requestParameterSupported) {
        this.requestParameterSupported = requestParameterSupported;
    }

    public Boolean getRequestUriParameterSupported() {
        return requestUriParameterSupported;
    }

    public void setRequestUriParameterSupported(Boolean requestUriParameterSupported) {
        this.requestUriParameterSupported = requestUriParameterSupported;
    }

    public Boolean getRequireRequestUriRegistration() {
        return requireRequestUriRegistration;
    }

    public void setRequireRequestUriRegistration(Boolean requireRequestUriRegistration) {
        this.requireRequestUriRegistration = requireRequestUriRegistration;
    }

    public List<String> getRequestUriBlockList() {
        if (requestUriBlockList == null) requestUriBlockList = Lists.newArrayList();
        return requestUriBlockList;
    }

    public void setRequestUriBlockList(List<String> requestUriBlockList) {
        this.requestUriBlockList = requestUriBlockList;
    }

    public String getOpPolicyUri() {
        return opPolicyUri;
    }

    public void setOpPolicyUri(String opPolicyUri) {
        this.opPolicyUri = opPolicyUri;
    }

    public String getOpTosUri() {
        return opTosUri;
    }

    public void setOpTosUri(String opTosUri) {
        this.opTosUri = opTosUri;
    }

    public int getAuthorizationCodeLifetime() {
        return authorizationCodeLifetime;
    }

    public void setAuthorizationCodeLifetime(int authorizationCodeLifetime) {
        this.authorizationCodeLifetime = authorizationCodeLifetime;
    }

    public int getRefreshTokenLifetime() {
        return refreshTokenLifetime;
    }

    public void setRefreshTokenLifetime(int refreshTokenLifetime) {
        this.refreshTokenLifetime = refreshTokenLifetime;
    }

    public int getIdTokenLifetime() {
        return idTokenLifetime;
    }

    public void setIdTokenLifetime(int idTokenLifetime) {
        this.idTokenLifetime = idTokenLifetime;
    }

    public int getAccessTokenLifetime() {
        return accessTokenLifetime;
    }

    public void setAccessTokenLifetime(int accessTokenLifetime) {
        this.accessTokenLifetime = accessTokenLifetime;
    }

    public int getUmaRptLifetime() {
        return umaRptLifetime;
    }

    public void setUmaRptLifetime(int umaRptLifetime) {
        this.umaRptLifetime = umaRptLifetime;
    }

    public int getUmaTicketLifetime() {
        return umaTicketLifetime;
    }

    public void setUmaTicketLifetime(int umaTicketLifetime) {
        this.umaTicketLifetime = umaTicketLifetime;
    }

    public int getUmaResourceLifetime() {
        return umaResourceLifetime;
    }

    public void setUmaResourceLifetime(int umaResourceLifetime) {
        this.umaResourceLifetime = umaResourceLifetime;
    }

    public int getUmaPctLifetime() {
        return umaPctLifetime;
    }

    public void setUmaPctLifetime(int umaPctLifetime) {
        this.umaPctLifetime = umaPctLifetime;
    }

    public Boolean getAllowSpontaneousScopes() {
        if (allowSpontaneousScopes == null) allowSpontaneousScopes = false;
        return allowSpontaneousScopes;
    }

    public void setAllowSpontaneousScopes(Boolean allowSpontaneousScopes) {
        this.allowSpontaneousScopes = allowSpontaneousScopes;
    }

    public int getSpontaneousScopeLifetime() {
        return spontaneousScopeLifetime;
    }

    public void setSpontaneousScopeLifetime(int spontaneousScopeLifetime) {
        this.spontaneousScopeLifetime = spontaneousScopeLifetime;
    }

    public int getCleanServiceInterval() {
        return cleanServiceInterval;
    }

    public void setCleanServiceInterval(int cleanServiceInterval) {
        this.cleanServiceInterval = cleanServiceInterval;
    }

    public int getCleanServiceBatchChunkSize() {
        return cleanServiceBatchChunkSize;
    }

    public void setCleanServiceBatchChunkSize(int cleanServiceBatchChunkSize) {
        this.cleanServiceBatchChunkSize = cleanServiceBatchChunkSize;
    }

    public Boolean getKeyRegenerationEnabled() {
        return keyRegenerationEnabled;
    }

    public void setKeyRegenerationEnabled(Boolean keyRegenerationEnabled) {
        this.keyRegenerationEnabled = keyRegenerationEnabled;
    }

    public int getKeyRegenerationInterval() {
        return keyRegenerationInterval;
    }

    public void setKeyRegenerationInterval(int keyRegenerationInterval) {
        this.keyRegenerationInterval = keyRegenerationInterval;
    }

    public String getDefaultSignatureAlgorithm() {
        return defaultSignatureAlgorithm;
    }

    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        this.defaultSignatureAlgorithm = defaultSignatureAlgorithm;
    }

    public String getJansOpenIdConnectVersion() {
        return jansOpenIdConnectVersion;
    }

    public void setJansOpenIdConnectVersion(String jansOpenIdConnectVersion) {
        this.jansOpenIdConnectVersion = jansOpenIdConnectVersion;
    }

    public String getJansId() {
        return jansId;
    }

    public void setJansId(String jansId) {
        this.jansId = jansId;
    }

    public int getDynamicRegistrationExpirationTime() {
        return dynamicRegistrationExpirationTime;
    }

    public void setDynamicRegistrationExpirationTime(int dynamicRegistrationExpirationTime) {
        this.dynamicRegistrationExpirationTime = dynamicRegistrationExpirationTime;
    }

    public Boolean getDynamicRegistrationPersistClientAuthorizations() {
        return dynamicRegistrationPersistClientAuthorizations;
    }

    public void setDynamicRegistrationPersistClientAuthorizations(Boolean dynamicRegistrationPersistClientAuthorizations) {
        this.dynamicRegistrationPersistClientAuthorizations = dynamicRegistrationPersistClientAuthorizations;
    }

    public Boolean getTrustedClientEnabled() {
        return trustedClientEnabled;
    }

    public void setTrustedClientEnabled(Boolean trustedClientEnabled) {
        this.trustedClientEnabled = trustedClientEnabled;
    }

    public Boolean getSkipAuthorizationForOpenIdScopeAndPairwiseId() {
        return skipAuthorizationForOpenIdScopeAndPairwiseId;
    }

    public void setSkipAuthorizationForOpenIdScopeAndPairwiseId(Boolean skipAuthorizationForOpenIdScopeAndPairwiseId) {
        this.skipAuthorizationForOpenIdScopeAndPairwiseId = skipAuthorizationForOpenIdScopeAndPairwiseId;
    }

    public Boolean getDynamicRegistrationScopesParamEnabled() {
        return dynamicRegistrationScopesParamEnabled;
    }

    public void setDynamicRegistrationScopesParamEnabled(Boolean dynamicRegistrationScopesParamEnabled) {
        this.dynamicRegistrationScopesParamEnabled = dynamicRegistrationScopesParamEnabled;
    }

    public Boolean getPersistIdTokenInLdap() {
        return persistIdTokenInLdap;
    }

    public void setPersistIdTokenInLdap(Boolean persistIdTokenInLdap) {
        this.persistIdTokenInLdap = persistIdTokenInLdap;
    }

    public Boolean getPersistRefreshTokenInLdap() {
        return persistRefreshTokenInLdap;
    }

    public void setPersistRefreshTokenInLdap(Boolean persistRefreshTokenInLdap) {
        this.persistRefreshTokenInLdap = persistRefreshTokenInLdap;
    }

    public Boolean getAllowPostLogoutRedirectWithoutValidation() {
        if (allowPostLogoutRedirectWithoutValidation == null) allowPostLogoutRedirectWithoutValidation = false;
        return allowPostLogoutRedirectWithoutValidation;
    }

    public void setAllowPostLogoutRedirectWithoutValidation(Boolean allowPostLogoutRedirectWithoutValidation) {
        this.allowPostLogoutRedirectWithoutValidation = allowPostLogoutRedirectWithoutValidation;
    }

    public Boolean getInvalidateSessionCookiesAfterAuthorizationFlow() {
        if (invalidateSessionCookiesAfterAuthorizationFlow == null) {
            invalidateSessionCookiesAfterAuthorizationFlow = false;
        }
        return invalidateSessionCookiesAfterAuthorizationFlow;
    }

    public void setInvalidateSessionCookiesAfterAuthorizationFlow(Boolean invalidateSessionCookiesAfterAuthorizationFlow) {
        this.invalidateSessionCookiesAfterAuthorizationFlow = invalidateSessionCookiesAfterAuthorizationFlow;
    }

    public String getDynamicRegistrationCustomObjectClass() {
        return dynamicRegistrationCustomObjectClass;
    }

    public void setDynamicRegistrationCustomObjectClass(String dynamicRegistrationCustomObjectClass) {
        this.dynamicRegistrationCustomObjectClass = dynamicRegistrationCustomObjectClass;
    }

    public List<String> getPersonCustomObjectClassList() {
        return personCustomObjectClassList;
    }

    public void setPersonCustomObjectClassList(List<String> personCustomObjectClassList) {
        this.personCustomObjectClassList = personCustomObjectClassList;
    }

    public Boolean getAuthenticationFiltersEnabled() {
        return authenticationFiltersEnabled;
    }

    public void setAuthenticationFiltersEnabled(Boolean authenticationFiltersEnabled) {
        this.authenticationFiltersEnabled = authenticationFiltersEnabled;
    }

    public Boolean getClientAuthenticationFiltersEnabled() {
        return clientAuthenticationFiltersEnabled;
    }

    public void setClientAuthenticationFiltersEnabled(Boolean clientAuthenticationFiltersEnabled) {
        this.clientAuthenticationFiltersEnabled = clientAuthenticationFiltersEnabled;
    }

    public List<AuthenticationFilter> getAuthenticationFilters() {
        if (authenticationFilters == null) {
            authenticationFilters = new ArrayList<>();
        }

        return authenticationFilters;
    }

    public List<ClientAuthenticationFilter> getClientAuthenticationFilters() {
        if (clientAuthenticationFilters == null) {
            clientAuthenticationFilters = new ArrayList<>();
        }

        return clientAuthenticationFilters;
    }


    public List<CorsConfigurationFilter> getCorsConfigurationFilters() {
        if (corsConfigurationFilters == null) {
            corsConfigurationFilters = new ArrayList<>();
        }

        return corsConfigurationFilters;
    }

    public int getSessionIdUnusedLifetime() {
        return sessionIdUnusedLifetime;
    }

    public void setSessionIdUnusedLifetime(int sessionIdUnusedLifetime) {
        this.sessionIdUnusedLifetime = sessionIdUnusedLifetime;
    }

    public int getSessionIdUnauthenticatedUnusedLifetime() {
        return sessionIdUnauthenticatedUnusedLifetime;
    }

    public void setSessionIdUnauthenticatedUnusedLifetime(int sessionIdUnauthenticatedUnusedLifetime) {
        this.sessionIdUnauthenticatedUnusedLifetime = sessionIdUnauthenticatedUnusedLifetime;
    }

    public Boolean getSessionIdPersistOnPromptNone() {
        return sessionIdPersistOnPromptNone;
    }

    public void setSessionIdPersistOnPromptNone(Boolean sessionIdPersistOnPromptNone) {
        this.sessionIdPersistOnPromptNone = sessionIdPersistOnPromptNone;
    }

    public Boolean getSessionIdRequestParameterEnabled() {
        if (sessionIdRequestParameterEnabled == null) {
            sessionIdRequestParameterEnabled = false;
        }
        return sessionIdRequestParameterEnabled;
    }

    public void setSessionIdRequestParameterEnabled(Boolean sessionIdRequestParameterEnabled) {
        this.sessionIdRequestParameterEnabled = sessionIdRequestParameterEnabled;
    }

    public int getConfigurationUpdateInterval() {
        return configurationUpdateInterval;
    }

    public void setConfigurationUpdateInterval(int configurationUpdateInterval) {
        this.configurationUpdateInterval = configurationUpdateInterval;
    }

    public String getJsLocation() {
        return jsLocation;
    }

    public void setJsLocation(String jsLocation) {
        this.jsLocation = jsLocation;
    }

    public String getCssLocation() {
        return cssLocation;
    }

    public void setCssLocation(String cssLocation) {
        this.cssLocation = cssLocation;
    }

    public String getImgLocation() {
        return imgLocation;
    }

    public void setImgLocation(String imgLocation) {
        this.imgLocation = imgLocation;
    }

    public int getMetricReporterInterval() {
        return metricReporterInterval;
    }

    public void setMetricReporterInterval(int metricReporterInterval) {
        this.metricReporterInterval = metricReporterInterval;
    }

    public int getMetricReporterKeepDataDays() {
        return metricReporterKeepDataDays;
    }

    public void setMetricReporterKeepDataDays(int metricReporterKeepDataDays) {
        this.metricReporterKeepDataDays = metricReporterKeepDataDays;
    }

    public String getPairwiseIdType() {
        return pairwiseIdType;
    }

    public void setPairwiseIdType(String pairwiseIdType) {
        this.pairwiseIdType = pairwiseIdType;
    }

    public String getPairwiseCalculationKey() {
        return pairwiseCalculationKey;
    }

    public void setPairwiseCalculationKey(String pairwiseCalculationKey) {
        this.pairwiseCalculationKey = pairwiseCalculationKey;
    }

    public String getPairwiseCalculationSalt() {
        return pairwiseCalculationSalt;
    }

    public void setPairwiseCalculationSalt(String pairwiseCalculationSalt) {
        this.pairwiseCalculationSalt = pairwiseCalculationSalt;
    }

    public Boolean isShareSubjectIdBetweenClientsWithSameSectorId() {
        return shareSubjectIdBetweenClientsWithSameSectorId;
    }

    public void setShareSubjectIdBetweenClientsWithSameSectorId(Boolean shareSubjectIdBetweenClientsWithSameSectorId) {
        this.shareSubjectIdBetweenClientsWithSameSectorId = shareSubjectIdBetweenClientsWithSameSectorId;
    }

    public WebKeyStorage getWebKeysStorage() {
        return webKeysStorage;
    }

    public void setWebKeysStorage(WebKeyStorage webKeysStorage) {
        this.webKeysStorage = webKeysStorage;
    }

    public String getDnName() {
        return dnName;
    }

    public void setDnName(String dnName) {
        this.dnName = dnName;
    }

    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    public String getKeyStoreSecret() {
        return keyStoreSecret;
    }

    public void setKeyStoreSecret(String keyStoreSecret) {
        this.keyStoreSecret = keyStoreSecret;
    }

    public String getJansElevenTestModeToken() {
        return jansElevenTestModeToken;
    }

    public void setJansElevenTestModeToken(String jansElevenTestModeToken) {
        this.jansElevenTestModeToken = jansElevenTestModeToken;
    }

    public String getJansElevenGenerateKeyEndpoint() {
        return jansElevenGenerateKeyEndpoint;
    }

    public void setJansElevenGenerateKeyEndpoint(String jansElevenGenerateKeyEndpoint) {
        this.jansElevenGenerateKeyEndpoint = jansElevenGenerateKeyEndpoint;
    }

    public String getJansElevenSignEndpoint() {
        return jansElevenSignEndpoint;
    }

    public void setJansElevenSignEndpoint(String jansElevenSignEndpoint) {
        this.jansElevenSignEndpoint = jansElevenSignEndpoint;
    }

    public String getJansElevenVerifySignatureEndpoint() {
        return jansElevenVerifySignatureEndpoint;
    }

    public void setJansElevenVerifySignatureEndpoint(String jansElevenVerifySignatureEndpoint) {
        this.jansElevenVerifySignatureEndpoint = jansElevenVerifySignatureEndpoint;
    }

    public String getJansElevenDeleteKeyEndpoint() {
        return jansElevenDeleteKeyEndpoint;
    }

    public void setJansElevenDeleteKeyEndpoint(String jansElevenDeleteKeyEndpoint) {
        this.jansElevenDeleteKeyEndpoint = jansElevenDeleteKeyEndpoint;
    }

    public Boolean getEndSessionWithAccessToken() {
        return endSessionWithAccessToken;
    }

    public void setEndSessionWithAccessToken(Boolean endSessionWithAccessToken) {
        this.endSessionWithAccessToken = endSessionWithAccessToken;
    }

    public String getCookieDomain() {
        return cookieDomain;
    }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    public Boolean getEnabledOAuthAuditLogging() {
        return enabledOAuthAuditLogging;
    }

    public void setEnabledOAuthAuditLogging(Boolean enabledOAuthAuditLogging) {
        this.enabledOAuthAuditLogging = enabledOAuthAuditLogging;
    }

    public Set<String> getJmsBrokerURISet() {
        return jmsBrokerURISet;
    }

    public void setJmsBrokerURISet(Set<String> jmsBrokerURISet) {
        this.jmsBrokerURISet = jmsBrokerURISet;
    }

    public String getJmsUserName() {
        return jmsUserName;
    }

    public void setJmsUserName(String jmsUserName) {
        this.jmsUserName = jmsUserName;
    }

    public String getJmsPassword() {
        return jmsPassword;
    }

    public void setJmsPassword(String jmsPassword) {
        this.jmsPassword = jmsPassword;
    }

    public List<String> getExternalUriWhiteList() {
        if (externalUriWhiteList == null) externalUriWhiteList = new ArrayList<>();
        return externalUriWhiteList;
    }

    public void setExternalUriWhiteList(List<String> externalUriWhiteList) {
        this.externalUriWhiteList = externalUriWhiteList;
    }

    public List<String> getClientWhiteList() {
        return clientWhiteList;
    }

    public void setClientWhiteList(List<String> clientWhiteList) {
        this.clientWhiteList = clientWhiteList;
    }

    public List<String> getClientBlackList() {
        return clientBlackList;
    }

    public void setClientBlackList(List<String> clientBlackList) {
        this.clientBlackList = clientBlackList;
    }

    public Boolean getLegacyIdTokenClaims() {
        return legacyIdTokenClaims;
    }

    public void setLegacyIdTokenClaims(Boolean legacyIdTokenClaims) {
        this.legacyIdTokenClaims = legacyIdTokenClaims;
    }

    public Boolean getCustomHeadersWithAuthorizationResponse() {
        if (customHeadersWithAuthorizationResponse == null) {
            return false;
        }

        return customHeadersWithAuthorizationResponse;
    }

    public void setCustomHeadersWithAuthorizationResponse(Boolean customHeadersWithAuthorizationResponse) {
        this.customHeadersWithAuthorizationResponse = customHeadersWithAuthorizationResponse;
    }

    public Boolean getUpdateUserLastLogonTime() {
        return updateUserLastLogonTime != null && updateUserLastLogonTime;
    }

    public void setUpdateUserLastLogonTime(Boolean updateUserLastLogonTime) {
        this.updateUserLastLogonTime = updateUserLastLogonTime;
    }

    public Boolean getUpdateClientAccessTime() {
        return updateClientAccessTime != null && updateClientAccessTime;
    }

    public void setUpdateClientAccessTime(Boolean updateClientAccessTime) {
        this.updateClientAccessTime = updateClientAccessTime;
    }

    public Boolean getHttpLoggingEnabled() {
        return httpLoggingEnabled;
    }

    public void setHttpLoggingEnabled(Boolean httpLoggingEnabled) {
        this.httpLoggingEnabled = httpLoggingEnabled;
    }

    public Set<String> getHttpLoggingExcludePaths() {
        return httpLoggingExcludePaths;
    }

    public void setHttpLoggingExcludePaths(Set<String> httpLoggingExcludePaths) {
        this.httpLoggingExcludePaths = httpLoggingExcludePaths;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public String getLoggingLayout() {
        return loggingLayout;
    }

    public void setLoggingLayout(String loggingLayout) {
        this.loggingLayout = loggingLayout;
    }

    public Boolean getEnableClientGrantTypeUpdate() {
        return enableClientGrantTypeUpdate;
    }

    public void setEnableClientGrantTypeUpdate(Boolean enableClientGrantTypeUpdate) {
        this.enableClientGrantTypeUpdate = enableClientGrantTypeUpdate;
    }

    public Set<GrantType> getDynamicGrantTypeDefault() {
        return dynamicGrantTypeDefault;
    }

    public void setDynamicGrantTypeDefault(Set<GrantType> dynamicGrantTypeDefault) {
        this.dynamicGrantTypeDefault = dynamicGrantTypeDefault;
    }

    /**
     * @return session_id lifetime. If null or value is zero or less then session_id lifetime is not set and will expire when browser session ends.
     */
    public Integer getSessionIdLifetime() {
        return sessionIdLifetime;
    }

    public void setSessionIdLifetime(Integer sessionIdLifetime) {
        this.sessionIdLifetime = sessionIdLifetime;
    }

    public String getActiveSessionAuthorizationScope() {
        return activeSessionAuthorizationScope;
    }

    public void setActiveSessionAuthorizationScope(String activeSessionAuthorizationScope) {
        this.activeSessionAuthorizationScope = activeSessionAuthorizationScope;
    }

    public Integer getServerSessionIdLifetime() {
        return serverSessionIdLifetime;
    }

    public void setServerSessionIdLifetime(Integer serverSessionIdLifetime) {
        this.serverSessionIdLifetime = serverSessionIdLifetime;
    }

    public Boolean getLogClientIdOnClientAuthentication() {
        return logClientIdOnClientAuthentication;
    }

    public void setLogClientIdOnClientAuthentication(Boolean logClientIdOnClientAuthentication) {
        this.logClientIdOnClientAuthentication = logClientIdOnClientAuthentication;
    }

    public Boolean getLogClientNameOnClientAuthentication() {
        return logClientNameOnClientAuthentication;
    }

    public void setLogClientNameOnClientAuthentication(Boolean logClientNameOnClientAuthentication) {
        this.logClientNameOnClientAuthentication = logClientNameOnClientAuthentication;
    }

    public String getExternalLoggerConfiguration() {
        return externalLoggerConfiguration;
    }

    public void setExternalLoggerConfiguration(String externalLoggerConfiguration) {
        this.externalLoggerConfiguration = externalLoggerConfiguration;
    }

    public Set<AuthorizationRequestCustomParameter> getAuthorizationRequestCustomAllowedParameters() {
        return authorizationRequestCustomAllowedParameters;
    }

    public void setAuthorizationRequestCustomAllowedParameters(Set<AuthorizationRequestCustomParameter> authorizationRequestCustomAllowedParameters) {
        this.authorizationRequestCustomAllowedParameters = authorizationRequestCustomAllowedParameters;
    }

    public Boolean getOpenidScopeBackwardCompatibility() {
        return openidScopeBackwardCompatibility;
    }

    public void setOpenidScopeBackwardCompatibility(Boolean openidScopeBackwardCompatibility) {
        this.openidScopeBackwardCompatibility = openidScopeBackwardCompatibility;
    }

    public Boolean getDisableU2fEndpoint() {
        return disableU2fEndpoint;
    }

    public void setDisableU2fEndpoint(Boolean disableU2fEndpoint) {
        this.disableU2fEndpoint = disableU2fEndpoint;
    }

    public AuthenticationProtectionConfiguration getAuthenticationProtectionConfiguration() {
        return authenticationProtectionConfiguration;
    }

    public void setAuthenticationProtectionConfiguration(AuthenticationProtectionConfiguration authenticationProtectionConfiguration) {
        this.authenticationProtectionConfiguration = authenticationProtectionConfiguration;
    }

    public ErrorHandlingMethod getErrorHandlingMethod() {
        return errorHandlingMethod;
    }

    public void setErrorHandlingMethod(ErrorHandlingMethod errorHandlingMethod) {
        this.errorHandlingMethod = errorHandlingMethod;
    }

    public Boolean getUseLocalCache() {
        return useLocalCache;
    }

    public void setUseLocalCache(Boolean useLocalCache) {
        this.useLocalCache = useLocalCache;
    }

    public Boolean getKeepAuthenticatorAttributesOnAcrChange() {
        return keepAuthenticatorAttributesOnAcrChange;
    }

    public void setKeepAuthenticatorAttributesOnAcrChange(Boolean keepAuthenticatorAttributesOnAcrChange) {
        this.keepAuthenticatorAttributesOnAcrChange = keepAuthenticatorAttributesOnAcrChange;
    }

    public Boolean getDisableAuthnForMaxAgeZero() {
        return disableAuthnForMaxAgeZero;
    }

    public void setDisableAuthnForMaxAgeZero(Boolean disableAuthnForMaxAgeZero) {
        this.disableAuthnForMaxAgeZero = disableAuthnForMaxAgeZero;
    }

    public String getBackchannelClientId() {
        return backchannelClientId;
    }

    public void setBackchannelClientId(String backchannelClientId) {
        this.backchannelClientId = backchannelClientId;
    }

    public String getBackchannelRedirectUri() {
        return backchannelRedirectUri;
    }

    public void setBackchannelRedirectUri(String backchannelRedirectUri) {
        this.backchannelRedirectUri = backchannelRedirectUri;
    }

    public String getBackchannelAuthenticationEndpoint() {
        return backchannelAuthenticationEndpoint;
    }

    public void setBackchannelAuthenticationEndpoint(String backchannelAuthenticationEndpoint) {
        this.backchannelAuthenticationEndpoint = backchannelAuthenticationEndpoint;
    }

    public String getBackchannelDeviceRegistrationEndpoint() {
        return backchannelDeviceRegistrationEndpoint;
    }

    public void setBackchannelDeviceRegistrationEndpoint(String backchannelDeviceRegistrationEndpoint) {
        this.backchannelDeviceRegistrationEndpoint = backchannelDeviceRegistrationEndpoint;
    }

    public List<String> getBackchannelTokenDeliveryModesSupported() {
        if (backchannelTokenDeliveryModesSupported == null)
            backchannelTokenDeliveryModesSupported = Lists.newArrayList();
        return backchannelTokenDeliveryModesSupported;
    }

    public void setBackchannelTokenDeliveryModesSupported(List<String> backchannelTokenDeliveryModesSupported) {
        this.backchannelTokenDeliveryModesSupported = backchannelTokenDeliveryModesSupported;
    }

    public List<String> getBackchannelAuthenticationRequestSigningAlgValuesSupported() {
        if (backchannelAuthenticationRequestSigningAlgValuesSupported == null)
            backchannelAuthenticationRequestSigningAlgValuesSupported = Lists.newArrayList();
        return backchannelAuthenticationRequestSigningAlgValuesSupported;
    }

    public void setBackchannelAuthenticationRequestSigningAlgValuesSupported(List<String> backchannelAuthenticationRequestSigningAlgValuesSupported) {
        this.backchannelAuthenticationRequestSigningAlgValuesSupported = backchannelAuthenticationRequestSigningAlgValuesSupported;
    }

    public Boolean getBackchannelUserCodeParameterSupported() {
        return backchannelUserCodeParameterSupported;
    }

    public void setBackchannelUserCodeParameterSupported(Boolean backchannelUserCodeParameterSupported) {
        this.backchannelUserCodeParameterSupported = backchannelUserCodeParameterSupported;
    }

    public String getBackchannelBindingMessagePattern() {
        return backchannelBindingMessagePattern;
    }

    public void setBackchannelBindingMessagePattern(String backchannelBindingMessagePattern) {
        this.backchannelBindingMessagePattern = backchannelBindingMessagePattern;
    }

    /**
     * Returns a number with a positive integer value indicating the expiration time
     * of the "auth_req_id" in seconds since the authentication request was received.
     *
     * @return Default expires_in value.
     */
    public int getBackchannelAuthenticationResponseExpiresIn() {
        return backchannelAuthenticationResponseExpiresIn;
    }

    public void setBackchannelAuthenticationResponseExpiresIn(int backchannelAuthenticationResponseExpiresIn) {
        this.backchannelAuthenticationResponseExpiresIn = backchannelAuthenticationResponseExpiresIn;
    }

    /**
     * Returns a number with a positive integer value indicating the minimum amount
     * of time in seconds that the Client must wait between polling requests to the
     * token endpoint.
     * This parameter will only be present if the Client is registered to use the
     * Poll or Ping modes.
     *
     * @return Interval value.
     */
    public int getBackchannelAuthenticationResponseInterval() {
        return backchannelAuthenticationResponseInterval;
    }

    public void setBackchannelAuthenticationResponseInterval(int backchannelAuthenticationResponseInterval) {
        this.backchannelAuthenticationResponseInterval = backchannelAuthenticationResponseInterval;
    }

    public List<String> getBackchannelLoginHintClaims() {
        return backchannelLoginHintClaims;
    }

    public void setBackchannelLoginHintClaims(List<String> backchannelLoginHintClaims) {
        this.backchannelLoginHintClaims = backchannelLoginHintClaims;
    }

    public CIBAEndUserNotificationConfig getCibaEndUserNotificationConfig() {
        return cibaEndUserNotificationConfig;
    }

    public void setCibaEndUserNotificationConfig(CIBAEndUserNotificationConfig cibaEndUserNotificationConfig) {
        this.cibaEndUserNotificationConfig = cibaEndUserNotificationConfig;
    }

    public List<String> getDynamicRegistrationAllowedPasswordGrantScopes() {
        if (dynamicRegistrationAllowedPasswordGrantScopes == null)
            dynamicRegistrationAllowedPasswordGrantScopes = Lists.newArrayList();
        return dynamicRegistrationAllowedPasswordGrantScopes;
    }

    public void setDynamicRegistrationAllowedPasswordGrantScopes(List<String> dynamicRegistrationAllowedPasswordGrantScopes) {
        this.dynamicRegistrationAllowedPasswordGrantScopes = dynamicRegistrationAllowedPasswordGrantScopes;
    }

    /**
     * Returns a flag to determinate if Jans Auth supports password grant type for
     * dynamic client registration.
     *
     * @return Boolean, true if it supports, false if it doesn't support.
     */
    public Boolean getDynamicRegistrationPasswordGrantTypeEnabled() {
        return dynamicRegistrationPasswordGrantTypeEnabled;
    }

    /**
     * This method sets the flag that define if Jans Auth supports or not password
     * grant type for dynamic client registration.
     *
     * @param dynamicRegistrationPasswordGrantTypeEnabled Boolean value for
     *                                                    the flag.
     */
    public void setDynamicRegistrationPasswordGrantTypeEnabled(Boolean dynamicRegistrationPasswordGrantTypeEnabled) {
        this.dynamicRegistrationPasswordGrantTypeEnabled = dynamicRegistrationPasswordGrantTypeEnabled;
    }

    public int getBackchannelRequestsProcessorJobIntervalSec() {
        return backchannelRequestsProcessorJobIntervalSec;
    }

    public void setBackchannelRequestsProcessorJobIntervalSec(int backchannelRequestsProcessorJobIntervalSec) {
        this.backchannelRequestsProcessorJobIntervalSec = backchannelRequestsProcessorJobIntervalSec;
    }

    public int getCibaGrantLifeExtraTimeSec() {
        return cibaGrantLifeExtraTimeSec;
    }

    public void setCibaGrantLifeExtraTimeSec(int cibaGrantLifeExtraTimeSec) {
        this.cibaGrantLifeExtraTimeSec = cibaGrantLifeExtraTimeSec;
    }

    public int getCibaMaxExpirationTimeAllowedSec() {
        return cibaMaxExpirationTimeAllowedSec;
    }

    public void setCibaMaxExpirationTimeAllowedSec(int cibaMaxExpirationTimeAllowedSec) {
        this.cibaMaxExpirationTimeAllowedSec = cibaMaxExpirationTimeAllowedSec;
    }

    public int getBackchannelRequestsProcessorJobChunkSize() {
        return backchannelRequestsProcessorJobChunkSize;
    }

    public void setBackchannelRequestsProcessorJobChunkSize(int backchannelRequestsProcessorJobChunkSize) {
        this.backchannelRequestsProcessorJobChunkSize = backchannelRequestsProcessorJobChunkSize;
    }

    public Boolean getClientRegDefaultToCodeFlowWithRefresh() {
        if (clientRegDefaultToCodeFlowWithRefresh == null) clientRegDefaultToCodeFlowWithRefresh = false;
        return clientRegDefaultToCodeFlowWithRefresh;
    }

    public void setClientRegDefaultToCodeFlowWithRefresh(Boolean clientRegDefaultToCodeFlowWithRefresh) {
        this.clientRegDefaultToCodeFlowWithRefresh = clientRegDefaultToCodeFlowWithRefresh;
    }

    public Boolean getGrantTypesAndResponseTypesAutofixEnabled() {
        if (grantTypesAndResponseTypesAutofixEnabled == null) grantTypesAndResponseTypesAutofixEnabled = false;
        return grantTypesAndResponseTypesAutofixEnabled;
    }

    public void setGrantTypesAndResponseTypesAutofixEnabled(Boolean grantTypesAndResponseTypesAutofixEnabled) {
        this.grantTypesAndResponseTypesAutofixEnabled = grantTypesAndResponseTypesAutofixEnabled;
    }

    public String getDeviceAuthzEndpoint() {
        return deviceAuthzEndpoint;
    }

    public void setDeviceAuthzEndpoint(String deviceAuthzEndpoint) {
        this.deviceAuthzEndpoint = deviceAuthzEndpoint;
    }

    public int getDeviceAuthzRequestExpiresIn() {
        return deviceAuthzRequestExpiresIn;
    }

    public void setDeviceAuthzRequestExpiresIn(int deviceAuthzRequestExpiresIn) {
        this.deviceAuthzRequestExpiresIn = deviceAuthzRequestExpiresIn;
    }

    public int getDeviceAuthzTokenPollInterval() {
        return deviceAuthzTokenPollInterval;
    }

    public void setDeviceAuthzTokenPollInterval(int deviceAuthzTokenPollInterval) {
        this.deviceAuthzTokenPollInterval = deviceAuthzTokenPollInterval;
    }

    public String getDeviceAuthzResponseTypeToProcessAuthz() {
        return deviceAuthzResponseTypeToProcessAuthz;
    }

    public void setDeviceAuthzResponseTypeToProcessAuthz(String deviceAuthzResponseTypeToProcessAuthz) {
        this.deviceAuthzResponseTypeToProcessAuthz = deviceAuthzResponseTypeToProcessAuthz;
    }

    public Boolean getRequestUriHashVerificationEnabled() {
        return requestUriHashVerificationEnabled != null && requestUriHashVerificationEnabled;
    }

    public void setRequestUriHashVerificationEnabled(Boolean requestUriHashVerificationEnabled) {
        this.requestUriHashVerificationEnabled = requestUriHashVerificationEnabled;
    }

    public Boolean getIdTokenFilterClaimsBasedOnAccessToken() {
        return idTokenFilterClaimsBasedOnAccessToken != null && idTokenFilterClaimsBasedOnAccessToken;
    }

    public void setIdTokenFilterClaimsBasedOnAccessToken(Boolean idTokenFilterClaimsBasedOnAccessToken) {
        this.idTokenFilterClaimsBasedOnAccessToken = idTokenFilterClaimsBasedOnAccessToken;
    }

    public String getMtlsAuthorizationEndpoint() {
        return mtlsAuthorizationEndpoint;
    }

    public void setMtlsAuthorizationEndpoint(String mtlsAuthorizationEndpoint) {
        this.mtlsAuthorizationEndpoint = mtlsAuthorizationEndpoint;
    }

    public String getMtlsTokenEndpoint() {
        return mtlsTokenEndpoint;
    }

    public void setMtlsTokenEndpoint(String mtlsTokenEndpoint) {
        this.mtlsTokenEndpoint = mtlsTokenEndpoint;
    }

    public String getMtlsTokenRevocationEndpoint() {
        return mtlsTokenRevocationEndpoint;
    }

    public void setMtlsTokenRevocationEndpoint(String mtlsTokenRevocationEndpoint) {
        this.mtlsTokenRevocationEndpoint = mtlsTokenRevocationEndpoint;
    }

    public String getMtlsUserInfoEndpoint() {
        return mtlsUserInfoEndpoint;
    }

    public void setMtlsUserInfoEndpoint(String mtlsUserInfoEndpoint) {
        this.mtlsUserInfoEndpoint = mtlsUserInfoEndpoint;
    }

    public String getMtlsClientInfoEndpoint() {
        return mtlsClientInfoEndpoint;
    }

    public void setMtlsClientInfoEndpoint(String mtlsClientInfoEndpoint) {
        this.mtlsClientInfoEndpoint = mtlsClientInfoEndpoint;
    }

    public String getMtlsCheckSessionIFrame() {
        return mtlsCheckSessionIFrame;
    }

    public void setMtlsCheckSessionIFrame(String mtlsCheckSessionIFrame) {
        this.mtlsCheckSessionIFrame = mtlsCheckSessionIFrame;
    }

    public String getMtlsEndSessionEndpoint() {
        return mtlsEndSessionEndpoint;
    }

    public void setMtlsEndSessionEndpoint(String mtlsEndSessionEndpoint) {
        this.mtlsEndSessionEndpoint = mtlsEndSessionEndpoint;
    }

    public String getMtlsJwksUri() {
        return mtlsJwksUri;
    }

    public void setMtlsJwksUri(String mtlsJwksUri) {
        this.mtlsJwksUri = mtlsJwksUri;
    }

    public String getMtlsRegistrationEndpoint() {
        return mtlsRegistrationEndpoint;
    }

    public void setMtlsRegistrationEndpoint(String mtlsRegistrationEndpoint) {
        this.mtlsRegistrationEndpoint = mtlsRegistrationEndpoint;
    }

    public String getMtlsIdGenerationEndpoint() {
        return mtlsIdGenerationEndpoint;
    }

    public void setMtlsIdGenerationEndpoint(String mtlsIdGenerationEndpoint) {
        this.mtlsIdGenerationEndpoint = mtlsIdGenerationEndpoint;
    }

    public String getMtlsIntrospectionEndpoint() {
        return mtlsIntrospectionEndpoint;
    }

    public void setMtlsIntrospectionEndpoint(String mtlsIntrospectionEndpoint) {
        this.mtlsIntrospectionEndpoint = mtlsIntrospectionEndpoint;
    }

    public String getMtlsParEndpoint() {
        return mtlsParEndpoint;
    }

    public void setMtlsParEndpoint(String mtlsParEndpoint) {
        this.mtlsParEndpoint = mtlsParEndpoint;
    }

    public String getMtlsDeviceAuthzEndpoint() {
        return mtlsDeviceAuthzEndpoint;
    }

    public void setMtlsDeviceAuthzEndpoint(String mtlsDeviceAuthzEndpoint) {
        this.mtlsDeviceAuthzEndpoint = mtlsDeviceAuthzEndpoint;
    }

    public List<String> getDpopSigningAlgValuesSupported() {
        if (dpopSigningAlgValuesSupported == null) dpopSigningAlgValuesSupported = new ArrayList<>();
        return dpopSigningAlgValuesSupported;
    }

    public void setDpopSigningAlgValuesSupported(List<String> dpopSigningAlgValuesSupported) {
        this.dpopSigningAlgValuesSupported = dpopSigningAlgValuesSupported;
    }

    public int getDpopTimeframe() {
        return dpopTimeframe;
    }

    public void setDpopTimeframe(int dpopTimeframe) {
        this.dpopTimeframe = dpopTimeframe;
    }

    public int getDpopJtiCacheTime() {
        return dpopJtiCacheTime;
    }

    public void setDpopJtiCacheTime(int dpopJtiCacheTime) {
        this.dpopJtiCacheTime = dpopJtiCacheTime;
    }

    public Boolean getRedirectUrisRegexEnabled() {
        return redirectUrisRegexEnabled != null && redirectUrisRegexEnabled;
    }

    public void setRedirectUrisRegexEnabled(Boolean redirectUrisRegexEnabled) {
        this.redirectUrisRegexEnabled = redirectUrisRegexEnabled;
    }

    public Boolean getUseHighestLevelScriptIfAcrScriptNotFound() {
        return useHighestLevelScriptIfAcrScriptNotFound != null && useHighestLevelScriptIfAcrScriptNotFound;
    }

    public void setUseHighestLevelScriptIfAcrScriptNotFound(Boolean useHighestLevelScriptIfAcrScriptNotFound) {
        this.useHighestLevelScriptIfAcrScriptNotFound = useHighestLevelScriptIfAcrScriptNotFound;
    }

    public EngineConfig getAgamaConfiguration() {
        return agamaConfiguration;
    }

    public void setAgamaConfiguration(EngineConfig agamaConfiguration) {
        this.agamaConfiguration = agamaConfiguration;
    }

    public SsaConfiguration getSsaConfiguration() {
        return ssaConfiguration;
    }

    public void setSsaConfiguration(SsaConfiguration ssaConfiguration) {
        this.ssaConfiguration = ssaConfiguration;
    }

    public Boolean getBlockWebviewAuthorizationEnabled() {
        return blockWebviewAuthorizationEnabled;
    }

    public void setBlockWebviewAuthorizationEnabled(Boolean blockWebviewAuthorizationEnabled) {
        this.blockWebviewAuthorizationEnabled = blockWebviewAuthorizationEnabled;
    }
}
