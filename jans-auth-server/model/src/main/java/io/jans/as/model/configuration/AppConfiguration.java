/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;

import io.jans.agama.model.EngineConfig;
import io.jans.as.model.common.*;
import io.jans.as.model.error.ErrorHandlingMethod;
import io.jans.as.model.jwk.KeySelectionStrategy;
import io.jans.as.model.ssa.SsaConfiguration;
import io.jans.doc.annotation.DocProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @DocProperty(description = "OpenID configuration endpoint URL")
    private String openIdConfigurationEndpoint;

    @DocProperty(description = "ID Generation endpoint URL")
    private String idGenerationEndpoint;

    @DocProperty(description = "Introspection endpoint URL")
    private String introspectionEndpoint;

    @DocProperty(description = "Pushed Authorization Requests(PAR) endpoint URL")
    private String parEndpoint;

    @DocProperty(description = "Boolean value specifying whether the OP requires Pushed Authorization Requests")
    private Boolean requirePar = false;

    @DocProperty(description = "Device authorization endpoint URL")
    private String deviceAuthzEndpoint;

    @DocProperty(description = "")

    private String mtlsAuthorizationEndpoint;

    @DocProperty(description = "Mutual TLS (mTLS) token endpoint URL")
    private String mtlsTokenEndpoint;

    @DocProperty(description = "Mutual TLS (mTLS) token revocation endpoint URL")
    private String mtlsTokenRevocationEndpoint;

    @DocProperty(description = "Mutual TLS (mTLS) user info endpoint URL")
    private String mtlsUserInfoEndpoint;

    @DocProperty(description = "Mutual TLS (mTLS) client info endpoint URL")
    private String mtlsClientInfoEndpoint;

    @DocProperty(description = "")
    private String mtlsCheckSessionIFrame;

    @DocProperty(description = "Mutual TLS (mTLS) end session endpoint URL")
    private String mtlsEndSessionEndpoint;

    @DocProperty(description = "")
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

    @DocProperty(description = "Experimental feature. This saves session data as a JWT")
    private Boolean sessionAsJwt = false;

    @DocProperty(description = "")
    private Boolean requireRequestObjectEncryption = false;

    @DocProperty(description = "")
    private Boolean requirePkce = false;

    @DocProperty(description = "")
    private Boolean allowAllValueForRevokeEndpoint = false;

    @DocProperty(description = "")
    private int sectorIdentifierCacheLifetimeInMinutes = 1440;

    @DocProperty(description = "UMA Configuration endpoint URL")
    private String umaConfigurationEndpoint;

    @DocProperty(description = "Issue RPT as JWT or as random string")
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

    @DocProperty(description = "")
    private Boolean umaValidateClaimToken = false;

    @DocProperty(description = "Specify whether to grant access to resources if there is no any policies associated with scopes")
    private Boolean umaGrantAccessIfNoPolicies = false;

    @DocProperty(description = "Restrict access to resource by associated client")
    private Boolean umaRestrictResourceToAssociatedClient = false;

    @DocProperty(description = "")
    private int statTimerIntervalInSeconds;

    @DocProperty(description = "")
    private String statAuthorizationScope;

    @DocProperty(description = "")
    private Boolean allowSpontaneousScopes;

    @DocProperty(description = "The lifetime of spontaneous scope in seconds")
    private int spontaneousScopeLifetime;

    @DocProperty(description = "Specifies which LDAP attribute is used for the subject identifier claim")
    private String openidSubAttribute;

    @DocProperty(description = "")
    private Boolean publicSubjectIdentifierPerClientEnabled = false;

    @DocProperty(description = "")
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

    @DocProperty(description = "")
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

    @DocProperty(description = "")
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

    @DocProperty(description = "This list details the custom attributes for dynamic registration")
    private List<String> dynamicRegistrationCustomAttributes;

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

    @DocProperty(description = "")
    private Boolean requestUriHashVerificationEnabled;

    @DocProperty(description = "Boolean value specifying whether the OP requires any request_uri values used to be pre-registered using the request_uris registration parameter")
    private Boolean requireRequestUriRegistration;

    @DocProperty(description = "")
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

    @DocProperty(description = "")
    private Boolean idTokenFilterClaimsBasedOnAccessToken;

    @DocProperty(description = "The lifetime of the short lived Access Token")
    private int accessTokenLifetime;

    @DocProperty(description = "")
    private int cleanServiceInterval;

    @DocProperty(description = "")
    private int cleanServiceBatchChunkSize = 100;

    @DocProperty(description = "")

    private Boolean keyRegenerationEnabled;

    @DocProperty(description = "")
    private int keyRegenerationInterval;

    @DocProperty(description = "")
    private String defaultSignatureAlgorithm;

    @DocProperty(description = "")
    private String oxOpenIdConnectVersion;

    @DocProperty(description = "")
    private String oxId;

    @DocProperty(description = "")
    private int dynamicRegistrationExpirationTime = -1;

    @DocProperty(description = "")
    private Boolean dynamicRegistrationPersistClientAuthorizations;

    @DocProperty(description = "")
    private Boolean trustedClientEnabled;

    @DocProperty(description = "")
    private Boolean skipAuthorizationForOpenIdScopeAndPairwiseId = false;

    @DocProperty(description = "")
    private Boolean dynamicRegistrationScopesParamEnabled;

    @DocProperty(description = "")
    private Boolean dynamicRegistrationPasswordGrantTypeEnabled = false;

    @DocProperty(description = "")
    private List<String> dynamicRegistrationAllowedPasswordGrantScopes;

    @DocProperty(description = "")
    private String dynamicRegistrationCustomObjectClass;

    @DocProperty(description = "")
    private List<String> personCustomObjectClassList;

    @DocProperty(description = "")

    private Boolean persistIdTokenInLdap = false;

    @DocProperty(description = "")
    private Boolean persistRefreshTokenInLdap = true;

    @DocProperty(description = "")
    private Boolean allowPostLogoutRedirectWithoutValidation = false;

    @DocProperty(description = "")
    private Boolean invalidateSessionCookiesAfterAuthorizationFlow = false;

    @DocProperty(description = "")
    private Boolean returnClientSecretOnRead = false;

    @DocProperty(description = "")
    private Boolean rejectJwtWithNoneAlg = true;

    @DocProperty(description = "")
    private Boolean expirationNotificatorEnabled = false;

    @DocProperty(description = "")
    private Boolean useNestedJwtDuringEncryption = true;

    @DocProperty(description = "")
    private int expirationNotificatorMapSizeLimit = 100000;

    @DocProperty(description = "")
    private int expirationNotificatorIntervalInSeconds = 600;

    @DocProperty(description = "")

    //feature flags
    private Boolean redirectUrisRegexEnabled = false;

    @DocProperty(description = "")
    private Boolean useHighestLevelScriptIfAcrScriptNotFound = true;

    @DocProperty(description = "")

    private Boolean authenticationFiltersEnabled;

    @DocProperty(description = "")
    private Boolean clientAuthenticationFiltersEnabled;

    @DocProperty(description = "")
    private Boolean clientRegDefaultToCodeFlowWithRefresh;

    @DocProperty(description = "")
    private Boolean grantTypesAndResponseTypesAutofixEnabled;

    @DocProperty(description = "")
    private List<AuthenticationFilter> authenticationFilters;

    @DocProperty(description = "")
    private List<ClientAuthenticationFilter> clientAuthenticationFilters;

    @DocProperty(description = "")
    private List<CorsConfigurationFilter> corsConfigurationFilters;

    @DocProperty(description = "")

    private int sessionIdUnusedLifetime;

    @DocProperty(description = "")
    private int sessionIdUnauthenticatedUnusedLifetime = 120;

    @DocProperty(description = "") // 120 seconds
    private Boolean sessionIdPersistOnPromptNone;

    @DocProperty(description = "")
    private Boolean sessionIdRequestParameterEnabled = false; // #1195
    private Boolean changeSessionIdOnAuthentication = true;

    @DocProperty(description = "")
    private Boolean sessionIdPersistInCache = false;

    @DocProperty(description = "")
    private Boolean includeSidInResponse = false;

    @DocProperty(description = "")
    /**
     * SessionId will be expired after sessionIdLifetime seconds
     */
    private Integer sessionIdLifetime = DEFAULT_SESSION_ID_LIFETIME;

    @DocProperty(description = "")
    private Integer serverSessionIdLifetime = sessionIdLifetime;

    @DocProperty(description = "") // by default same as sessionIdLifetime
    private String activeSessionAuthorizationScope;

    @DocProperty(description = "")
    private int configurationUpdateInterval;

    @DocProperty(description = "")

    private Boolean enableClientGrantTypeUpdate;

    @DocProperty(description = "")
    private Set<GrantType> dynamicGrantTypeDefault;

    @DocProperty(description = "")

    private String cssLocation;

    @DocProperty(description = "")
    private String jsLocation;

    @DocProperty(description = "")
    private String imgLocation;

    @DocProperty(description = "")
    private int metricReporterInterval;

    @DocProperty(description = "")
    private int metricReporterKeepDataDays;

    @DocProperty(description = "")
    private String pairwiseIdType;

    @DocProperty(description = "") // persistent, algorithmic
    private String pairwiseCalculationKey;

    @DocProperty(description = "")
    private String pairwiseCalculationSalt;

    @DocProperty(description = "")
    private Boolean shareSubjectIdBetweenClientsWithSameSectorId = false;

    @DocProperty(description = "")

    private WebKeyStorage webKeysStorage;

    @DocProperty(description = "")
    private String dnName;

    @DocProperty(description = "")
    // Jans Auth KeyStore
    private String keyStoreFile;

    @DocProperty(description = "")
    private String keyStoreSecret;

    @DocProperty(description = "")
    private KeySelectionStrategy keySelectionStrategy = DEFAULT_KEY_SELECTION_STRATEGY;

    @DocProperty(description = "")
    private List<String> keyAlgsAllowedForGeneration = new ArrayList<>();

    @DocProperty(description = "")
    private Boolean keySignWithSameKeyButDiffAlg;

    @DocProperty(description = "") // https://github.com/JanssenProject/jans-auth-server/issues/95
    private String staticKid;

    @DocProperty(description = "")
    private String staticDecryptionKid;

    @DocProperty(description = "")

    //oxEleven
    private String oxElevenTestModeToken;

    @DocProperty(description = "")
    private String oxElevenGenerateKeyEndpoint;

    @DocProperty(description = "")
    private String oxElevenSignEndpoint;

    @DocProperty(description = "")
    private String oxElevenVerifySignatureEndpoint;

    @DocProperty(description = "")
    private String oxElevenDeleteKeyEndpoint;

    @DocProperty(description = "")

    private Boolean introspectionAccessTokenMustHaveUmaProtectionScope = false;

    @DocProperty(description = "")
    private Boolean introspectionSkipAuthorization;

    @DocProperty(description = "")

    private Boolean endSessionWithAccessToken;

    @DocProperty(description = "")
    private String cookieDomain;

    @DocProperty(description = "")
    private Boolean enabledOAuthAuditLogging;

    @DocProperty(description = "")
    private Set<String> jmsBrokerURISet;

    @DocProperty(description = "")
    private String jmsUserName;

    @DocProperty(description = "")
    private String jmsPassword;

    @DocProperty(description = "")
    private List<String> clientWhiteList;

    @DocProperty(description = "")
    private List<String> clientBlackList;

    @DocProperty(description = "")
    private Boolean legacyIdTokenClaims;

    @DocProperty(description = "")
    private Boolean customHeadersWithAuthorizationResponse;

    @DocProperty(description = "")
    private Boolean frontChannelLogoutSessionSupported;

    @DocProperty(description = "")
    private String loggingLevel;

    @DocProperty(description = "")
    private String loggingLayout;

    @DocProperty(description = "")
    private Boolean updateUserLastLogonTime;

    @DocProperty(description = "")
    private Boolean updateClientAccessTime;

    @DocProperty(description = "")
    private Boolean logClientIdOnClientAuthentication;

    @DocProperty(description = "")
    private Boolean logClientNameOnClientAuthentication;

    @DocProperty(description = "")
    private Boolean disableJdkLogger = true;

    @DocProperty(description = "")
    private Set<AuthorizationRequestCustomParameter> authorizationRequestCustomAllowedParameters;

    @DocProperty(description = "")
    private Boolean openidScopeBackwardCompatibility = false;

    @DocProperty(description = "")
    private Boolean disableU2fEndpoint = false;

    @DocProperty(description = "")

    // Token Exchange
    private Boolean rotateDeviceSecret = false;

    @DocProperty(description = "")
    private Boolean returnDeviceSecretFromAuthzEndpoint = false;

    @DocProperty(description = "")

    // DCR
    private Boolean dcrSignatureValidationEnabled = false;

    @DocProperty(description = "")
    private String dcrSignatureValidationSharedSecret;

    @DocProperty(description = "")
    private String dcrSignatureValidationSoftwareStatementJwksURIClaim;

    @DocProperty(description = "")
    private String dcrSignatureValidationSoftwareStatementJwksClaim;

    @DocProperty(description = "")
    private String dcrSignatureValidationJwks;

    @DocProperty(description = "")
    private String dcrSignatureValidationJwksUri;

    @DocProperty(description = "")
    private Boolean dcrAuthorizationWithClientCredentials = false;

    @DocProperty(description = "")
    private Boolean dcrAuthorizationWithMTLS = false;

    @DocProperty(description = "")
    private List<String> dcrIssuers = new ArrayList<>();

    @DocProperty(description = "")

    private Boolean useLocalCache = false;

    @DocProperty(description = "")
    private Boolean fapiCompatibility = false;

    @DocProperty(description = "")
    private Boolean forceIdTokenHintPrecense = false;

    @DocProperty(description = "")
    private Boolean rejectEndSessionIfIdTokenExpired = false;

    @DocProperty(description = "")
    private Boolean allowEndSessionWithUnmatchedSid = false;

    @DocProperty(description = "")
    private Boolean forceOfflineAccessScopeToEnableRefreshToken = true;

    @DocProperty(description = "")
    private Boolean errorReasonEnabled = false;

    @DocProperty(description = "")
    private Boolean removeRefreshTokensForClientOnLogout = true;

    @DocProperty(description = "")
    private Boolean skipRefreshTokenDuringRefreshing = false;

    @DocProperty(description = "")
    private Boolean refreshTokenExtendLifetimeOnRotation = false;

    @DocProperty(description = "")
    private Boolean checkUserPresenceOnRefreshToken = false;

    @DocProperty(description = "")
    private Boolean consentGatheringScriptBackwardCompatibility = false;

    @DocProperty(description = "") // means ignore client configuration (as defined in 4.2) and determine it globally (as in 4.1 and earlier)
    private Boolean introspectionScriptBackwardCompatibility = false;

    @DocProperty(description = "") // means ignore client configuration (as defined in 4.2) and determine it globally (as in 4.1 and earlier)
    private Boolean introspectionResponseScopesBackwardCompatibility = false;

    @DocProperty(description = "")

    private String softwareStatementValidationType = SoftwareStatementValidationType.DEFAULT.getValue();

    @DocProperty(description = "")
    private String softwareStatementValidationClaimName;

    @DocProperty(description = "")

    private AuthenticationProtectionConfiguration authenticationProtectionConfiguration;

    @DocProperty(description = "")

    private ErrorHandlingMethod errorHandlingMethod = ErrorHandlingMethod.INTERNAL;

    @DocProperty(description = "")

    private Boolean disableAuthnForMaxAgeZero;

    @DocProperty(description = "")
    private Boolean keepAuthenticatorAttributesOnAcrChange = false;

    @DocProperty(description = "")
    private int deviceAuthzRequestExpiresIn;

    @DocProperty(description = "")
    private int deviceAuthzTokenPollInterval;

    @DocProperty(description = "")
    private String deviceAuthzResponseTypeToProcessAuthz;

    @DocProperty(description = "")

    // CIBA
    private String backchannelClientId;

    @DocProperty(description = "")
    private String backchannelRedirectUri;

    @DocProperty(description = "")
    private String backchannelAuthenticationEndpoint;

    @DocProperty(description = "")
    private String backchannelDeviceRegistrationEndpoint;

    @DocProperty(description = "")
    private List<String> backchannelTokenDeliveryModesSupported;

    @DocProperty(description = "")
    private List<String> backchannelAuthenticationRequestSigningAlgValuesSupported;

    @DocProperty(description = "")
    private Boolean backchannelUserCodeParameterSupported;

    @DocProperty(description = "")
    private String backchannelBindingMessagePattern;

    @DocProperty(description = "")
    private int backchannelAuthenticationResponseExpiresIn;

    @DocProperty(description = "")
    private int backchannelAuthenticationResponseInterval;

    @DocProperty(description = "")
    private List<String> backchannelLoginHintClaims;

    @DocProperty(description = "")
    private CIBAEndUserNotificationConfig cibaEndUserNotificationConfig;

    @DocProperty(description = "")
    private int backchannelRequestsProcessorJobIntervalSec;

    @DocProperty(description = "")
    private int backchannelRequestsProcessorJobChunkSize;

    @DocProperty(description = "")
    private int cibaGrantLifeExtraTimeSec;

    @DocProperty(description = "")
    private int cibaMaxExpirationTimeAllowedSec;

    @DocProperty(description = "")

    // DPoP
    private List<String> dpopSigningAlgValuesSupported;

    @DocProperty(description = "")
    private int dpopTimeframe = 5;

    @DocProperty(description = "")
    private int dpopJtiCacheTime = 3600;

    @DocProperty(description = "")

    private Boolean allowIdTokenWithoutImplicitGrantType;

    @DocProperty(description = "")

    private int discoveryCacheLifetimeInMinutes = 60;

    @DocProperty(description = "")
    private List<String> discoveryAllowedKeys;

    @DocProperty(description = "")
    private List<String> discoveryDenyKeys;

    @DocProperty(description = "")

    private List<String> featureFlags;

    @DocProperty(description = "")

    private Boolean httpLoggingEnabled;

    @DocProperty(description = "") // Used in ServletLoggingFilter to enable http request/response logging.
    private Set<String> httpLoggingExcludePaths;

    @DocProperty(description = "") // Used in ServletLoggingFilter to exclude some paths from logger. Paths example: ["/jans-auth/img", "/jans-auth/stylesheet"]
    private String externalLoggerConfiguration;

    @DocProperty(description = "") // Path to external log4j2 configuration file. This property might be configured from oxTrust: /identity/logviewer/configure
    
    private EngineConfig agamaConfiguration;

    @DocProperty(description = "")

    private SsaConfiguration ssaConfiguration;

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

    public Set<FeatureFlagType> getEnabledFeatureFlags() {
        return FeatureFlagType.fromValues(getFeatureFlags());
    }

    public boolean isFeatureEnabled(FeatureFlagType flagType) {
        final Set<FeatureFlagType> flags = getEnabledFeatureFlags();
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

    public Boolean getSessionAsJwt() {
        return sessionAsJwt;
    }

    public void setSessionAsJwt(Boolean sessionAsJwt) {
        this.sessionAsJwt = sessionAsJwt;
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

    public String getOxOpenIdConnectVersion() {
        return oxOpenIdConnectVersion;
    }

    public void setOxOpenIdConnectVersion(String oxOpenIdConnectVersion) {
        this.oxOpenIdConnectVersion = oxOpenIdConnectVersion;
    }

    public String getOxId() {
        return oxId;
    }

    public void setOxId(String oxId) {
        this.oxId = oxId;
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

    public String getOxElevenTestModeToken() {
        return oxElevenTestModeToken;
    }

    public void setOxElevenTestModeToken(String oxElevenTestModeToken) {
        this.oxElevenTestModeToken = oxElevenTestModeToken;
    }

    public String getOxElevenGenerateKeyEndpoint() {
        return oxElevenGenerateKeyEndpoint;
    }

    public void setOxElevenGenerateKeyEndpoint(String oxElevenGenerateKeyEndpoint) {
        this.oxElevenGenerateKeyEndpoint = oxElevenGenerateKeyEndpoint;
    }

    public String getOxElevenSignEndpoint() {
        return oxElevenSignEndpoint;
    }

    public void setOxElevenSignEndpoint(String oxElevenSignEndpoint) {
        this.oxElevenSignEndpoint = oxElevenSignEndpoint;
    }

    public String getOxElevenVerifySignatureEndpoint() {
        return oxElevenVerifySignatureEndpoint;
    }

    public void setOxElevenVerifySignatureEndpoint(String oxElevenVerifySignatureEndpoint) {
        this.oxElevenVerifySignatureEndpoint = oxElevenVerifySignatureEndpoint;
    }

    public String getOxElevenDeleteKeyEndpoint() {
        return oxElevenDeleteKeyEndpoint;
    }

    public void setOxElevenDeleteKeyEndpoint(String oxElevenDeleteKeyEndpoint) {
        this.oxElevenDeleteKeyEndpoint = oxElevenDeleteKeyEndpoint;
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
}
