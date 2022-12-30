---
tags:
- administration
- reference
- json
- properties
---

# Janssen Auth Server Configuration Properties

| Property Name | Description |  | 
|-----|-----|-----|
| accessTokenLifetime | The lifetime of the short lived Access Token | [Details](#accesstokenlifetime) |
| accessTokenSigningAlgValuesSupported | A list of the JWS signing algorithms (alg values) supported by the OP for the access token to encode the Claims in a JWT | [Details](#accesstokensigningalgvaluessupported) |
| activeSessionAuthorizationScope | Authorization Scope for active session | [Details](#activesessionauthorizationscope) |
| agamaConfiguration | Engine Config which offers an alternative way to build authentication flows in Janssen server | [Details](#agamaconfiguration) |
| allowAllValueForRevokeEndpoint | Boolean value true allow all value for revoke endpoint | [Details](#allowallvalueforrevokeendpoint) |
| allowEndSessionWithUnmatchedSid | default value false. If true, sid check will be skipped | [Details](#allowendsessionwithunmatchedsid) |
| allowIdTokenWithoutImplicitGrantType | Specifies if a token without implicit grant types is allowed | [Details](#allowidtokenwithoutimplicitgranttype) |
| allowPostLogoutRedirectWithoutValidation | Allows post-logout redirect without validation for the End Session endpoint (still AS validates it against clientWhiteList url pattern property) | [Details](#allowpostlogoutredirectwithoutvalidation) |
| allowSpontaneousScopes | Specifies whether to allow spontaneous scopes | [Details](#allowspontaneousscopes) |
| authenticationFilters | This list details filters for user authentication | [Details](#authenticationfilters) |
| authenticationFiltersEnabled | Boolean value specifying whether to enable user authentication filters | [Details](#authenticationfiltersenabled) |
| authenticationProtectionConfiguration | Authentication Brute Force Protection Configuration | [Details](#authenticationprotectionconfiguration) |
| authorizationCodeLifetime | The lifetime of the Authorization Code | [Details](#authorizationcodelifetime) |
| authorizationEncryptionAlgValuesSupported | List of authorization encryption algorithms supported by this OP | [Details](#authorizationencryptionalgvaluessupported) |
| authorizationEncryptionEncValuesSupported | A list of the authorization encryption algorithms supported | [Details](#authorizationencryptionencvaluessupported) |
| authorizationEndpoint | The authorization endpoint URL | [Details](#authorizationendpoint) |
| authorizationRequestCustomAllowedParameters | This list details the allowed custom parameters for authorization requests | [Details](#authorizationrequestcustomallowedparameters) |
| authorizationSigningAlgValuesSupported | List of authorization signing algorithms supported by this OP | [Details](#authorizationsigningalgvaluessupported) |
| backchannelAuthenticationEndpoint | Backchannel Authentication Endpoint | [Details](#backchannelauthenticationendpoint) |
| backchannelAuthenticationRequestSigningAlgValuesSupported | Backchannel Authentication Request Signing Alg Values Supported | [Details](#backchannelauthenticationrequestsigningalgvaluessupported) |
| backchannelAuthenticationResponseExpiresIn | Backchannel Authentication Response Expires In | [Details](#backchannelauthenticationresponseexpiresin) |
| backchannelAuthenticationResponseInterval | Backchannel Authentication Response Interval | [Details](#backchannelauthenticationresponseinterval) |
| backchannelBindingMessagePattern | Backchannel Binding Message Pattern | [Details](#backchannelbindingmessagepattern) |
| backchannelClientId | Backchannel Client Id | [Details](#backchannelclientid) |
| backchannelDeviceRegistrationEndpoint | Backchannel Device Registration Endpoint | [Details](#backchanneldeviceregistrationendpoint) |
| backchannelLoginHintClaims | Backchannel Login Hint Claims | [Details](#backchannelloginhintclaims) |
| backchannelRedirectUri | Backchannel Redirect Uri | [Details](#backchannelredirecturi) |
| backchannelRequestsProcessorJobChunkSize | Each backchannel request processor iteration fetches chunk of data to be processed | [Details](#backchannelrequestsprocessorjobchunksize) |
| backchannelRequestsProcessorJobIntervalSec | Specifies the allowable elapsed time in seconds backchannel request processor executes | [Details](#backchannelrequestsprocessorjobintervalsec) |
| backchannelTokenDeliveryModesSupported | Backchannel Token Delivery Modes Supported | [Details](#backchanneltokendeliverymodessupported) |
| backchannelUserCodeParameterSupported | Backchannel User Code Parameter Supported | [Details](#backchannelusercodeparametersupported) |
| baseEndpoint | The base URL for endpoints | [Details](#baseendpoint) |
| blockWebviewAuthorizationEnabled | Enable/Disable block authorizations that originate from Webview (Mobile apps). | [Details](#blockwebviewauthorizationenabled) |
| changeSessionIdOnAuthentication | Boolean value specifying whether change session_id on authentication. Default value is true | [Details](#changesessionidonauthentication) |
| checkSessionIFrame | URL for an OP IFrame that supports cross-origin communications for session state information with the RP Client using the HTML5 postMessage API | [Details](#checksessioniframe) |
| checkUserPresenceOnRefreshToken | Check whether user exists and is active before creating RefreshToken. Set it to true if check is needed(Default value is false - don't check. | [Details](#checkuserpresenceonrefreshtoken) |
| cibaEndUserNotificationConfig | CIBA End User Notification Config | [Details](#cibaendusernotificationconfig) |
| cibaGrantLifeExtraTimeSec | Specifies the CIBA Grant life extra time in seconds | [Details](#cibagrantlifeextratimesec) |
| cibaMaxExpirationTimeAllowedSec | Specifies the CIBA token expiration time in seconds | [Details](#cibamaxexpirationtimeallowedsec) |
| claimsLocalesSupported | This list details the languages and scripts supported for values in the claims being returned | [Details](#claimslocalessupported) |
| claimsParameterSupported | Specifies whether the OP supports use of the claims parameter | [Details](#claimsparametersupported) |
| claimTypesSupported | A list of the Claim Types that the OpenID Provider supports | [Details](#claimtypessupported) |
| cleanServiceBatchChunkSize | Clean service chunk size which is used during clean up | [Details](#cleanservicebatchchunksize) |
| cleanServiceInterval | Time interval for the Clean Service in seconds | [Details](#cleanserviceinterval) |
| clientAuthenticationFilters | This list details filters for client authentication | [Details](#clientauthenticationfilters) |
| clientAuthenticationFiltersEnabled | Boolean value specifying whether to enable client authentication filters | [Details](#clientauthenticationfiltersenabled) |
| clientBlackList | This list specified which client redirection URIs are black-listed | [Details](#clientblacklist) |
| clientInfoEndpoint | The Client Info endpoint URL | [Details](#clientinfoendpoint) |
| clientRegDefaultToCodeFlowWithRefresh | Boolean value specifying whether to add Authorization Code Flow with Refresh grant during client registratio | [Details](#clientregdefaulttocodeflowwithrefresh) |
| clientWhiteList | This list specifies which client redirection URIs are white-listed | [Details](#clientwhitelist) |
| configurationUpdateInterval | The interval for configuration update in seconds | [Details](#configurationupdateinterval) |
| consentGatheringScriptBackwardCompatibility | Boolean value specifying whether to turn on Consent Gathering Script backward compatibility mode. If true AS will pick up script with higher level globally. If false (default) AS will pick up script based on client configuration | [Details](#consentgatheringscriptbackwardcompatibility) |
| cookieDomain | Sets cookie domain for all cookies created by OP | [Details](#cookiedomain) |
| corsConfigurationFilters | This list specifies the CORS configuration filters | [Details](#corsconfigurationfilters) |
| cssLocation | The location for CSS files | [Details](#csslocation) |
| customHeadersWithAuthorizationResponse | Choose whether to enable the custom response header parameter to return custom headers with the authorization response | [Details](#customheaderswithauthorizationresponse) |
| dateFormatterPattern | List of key value, e.g. 'birthdate: 'yyyy-MM-dd', etc. | [Details](#dateformatterpattern) |
| dcrAuthorizationWithClientCredentials | Boolean value indicating if DCR authorization to be performed using client credentials | [Details](#dcrauthorizationwithclientcredentials) |
| dcrAuthorizationWithMTLS | Boolean value indicating if DCR authorization allowed with MTLS | [Details](#dcrauthorizationwithmtls) |
| dcrIssuers | List of DCR issuers | [Details](#dcrissuers) |
| dcrSignatureValidationEnabled | Boolean value enables DCR signature validation. Default is false | [Details](#dcrsignaturevalidationenabled) |
| dcrSignatureValidationJwks | Specifies JWKS for all DCR's validations | [Details](#dcrsignaturevalidationjwks) |
| dcrSignatureValidationJwksUri | Specifies JWKS URI for all DCR's validations | [Details](#dcrsignaturevalidationjwksuri) |
| dcrSignatureValidationSharedSecret | Specifies shared secret for Dynamic Client Registration | [Details](#dcrsignaturevalidationsharedsecret) |
| dcrSignatureValidationSoftwareStatementJwksClaim | Specifies claim name inside software statement. Value of claim should point to inlined JWKS | [Details](#dcrsignaturevalidationsoftwarestatementjwksclaim) |
| dcrSignatureValidationSoftwareStatementJwksURIClaim | Specifies claim name inside software statement. Value of claim should point to JWKS URI | [Details](#dcrsignaturevalidationsoftwarestatementjwksuriclaim) |
| dcrSsaValidationConfigs | DCR SSA Validation configurations used to perform validation of SSA or DCR | [Details](#dcrssavalidationconfigs) |
| defaultSignatureAlgorithm | The default signature algorithm to sign ID Tokens | [Details](#defaultsignaturealgorithm) |
| defaultSubjectType | The default subject type used for dynamic client registration | [Details](#defaultsubjecttype) |
| deviceAuthzEndpoint | URL for the Device Authorization | [Details](#deviceauthzendpoint) |
| deviceAuthzRequestExpiresIn | Expiration time given for device authorization requests | [Details](#deviceauthzrequestexpiresin) |
| deviceAuthzResponseTypeToProcessAuthz | Response type used to process device authz requests | [Details](#deviceauthzresponsetypetoprocessauthz) |
| deviceAuthzTokenPollInterval | Default interval returned to the client to process device token requests | [Details](#deviceauthztokenpollinterval) |
| disableAuthnForMaxAgeZero | Boolean value specifying whether to disable authentication when max_age=0 | [Details](#disableauthnformaxagezero) |
| disableJdkLogger | Choose whether to disable JDK loggers | [Details](#disablejdklogger) |
| disableU2fEndpoint | Choose whether to disable U2F endpoints | [Details](#disableu2fendpoint) |
| discoveryAllowedKeys | List of configuration response claim allowed to be displayed in discovery endpoint | [Details](#discoveryallowedkeys) |
| discoveryCacheLifetimeInMinutes | Lifetime of discovery cache | [Details](#discoverycachelifetimeinminutes) |
| discoveryDenyKeys | List of configuration response claims which must not be displayed in discovery endpoint response | [Details](#discoverydenykeys) |
| displayValuesSupported | A list of the display parameter values that the OpenID Provider supports | [Details](#displayvaluessupported) |
| dnName | DN of certificate issuer | [Details](#dnname) |
| dpopJtiCacheTime | Demonstration of Proof-of-Possession (DPoP) cache time | [Details](#dpopjticachetime) |
| dpopSigningAlgValuesSupported | Demonstration of Proof-of-Possession (DPoP) authorization signing algorithms supported | [Details](#dpopsigningalgvaluessupported) |
| dpopTimeframe | Demonstration of Proof-of-Possession (DPoP) timeout | [Details](#dpoptimeframe) |
| dynamicGrantTypeDefault | This list details which OAuth 2.0 grant types can be set up with the client registration API | [Details](#dynamicgranttypedefault) |
| dynamicRegistrationAllowedPasswordGrantScopes | List of grant scopes for dynamic registration | [Details](#dynamicregistrationallowedpasswordgrantscopes) |
| dynamicRegistrationCustomAttributes | This list details the custom attributes for dynamic registration | [Details](#dynamicregistrationcustomattributes) |
| dynamicRegistrationCustomObjectClass | LDAP custom object class for dynamic registration | [Details](#dynamicregistrationcustomobjectclass) |
| dynamicRegistrationExpirationTime | Expiration time in seconds for clients created with dynamic registration, 0 or -1 means never expire | [Details](#dynamicregistrationexpirationtime) |
| dynamicRegistrationPasswordGrantTypeEnabled | Boolean value specifying whether to enable Password Grant Type during Dynamic Registration | [Details](#dynamicregistrationpasswordgranttypeenabled) |
| dynamicRegistrationPersistClientAuthorizations | Boolean value specifying whether to persist client authorizations | [Details](#dynamicregistrationpersistclientauthorizations) |
| dynamicRegistrationScopesParamEnabled | Boolean value specifying whether to enable scopes parameter in dynamic registration | [Details](#dynamicregistrationscopesparamenabled) |
| enableClientGrantTypeUpdate | Choose if client can update Grant Type values | [Details](#enableclientgranttypeupdate) |
| enabledOAuthAuditLogging | enable OAuth Audit Logging | [Details](#enabledoauthauditlogging) |
| endSessionEndpoint | URL at the OP to which an RP can perform a redirect to request that the end user be logged out at the OP | [Details](#endsessionendpoint) |
| endSessionWithAccessToken | Choose whether to accept access tokens to call end_session endpoint | [Details](#endsessionwithaccesstoken) |
| errorHandlingMethod | A list of possible error handling methods | [Details](#errorhandlingmethod) |
| errorReasonEnabled | Boolean value specifying whether to return detailed reason of the error from AS. Default value is false | [Details](#errorreasonenabled) |
| expirationNotificatorEnabled | Boolean value specifying whether expiration notificator is enabled (used to identify expiration for persistence that support TTL, like Couchbase) | [Details](#expirationnotificatorenabled) |
| expirationNotificatorIntervalInSeconds | The expiration notificator interval in second | [Details](#expirationnotificatorintervalinseconds) |
| expirationNotificatorMapSizeLimit | The expiration notificator maximum size limit | [Details](#expirationnotificatormapsizelimit) |
| externalLoggerConfiguration | The path to the external log4j2 logging configuration | [Details](#externalloggerconfiguration) |
| externalUriWhiteList | This list specifies which external URIs can be called by AS (if empty any URI can be called) | [Details](#externaluriwhitelist) |
| fapiCompatibility | Boolean value specifying whether to turn on FAPI compatibility mode. If true AS behaves in more strict mode | [Details](#fapicompatibility) |
| featureFlags | List of enabled feature flags | [Details](#featureflags) |
| forceIdTokenHintPrecense | Boolean value specifying whether force id_token_hint parameter presence | [Details](#forceidtokenhintprecense) |
| forceOfflineAccessScopeToEnableRefreshToken | Boolean value specifying whether force offline_access scope to enable refresh_token grant type. Default value is true | [Details](#forceofflineaccessscopetoenablerefreshtoken) |
| forceSignedRequestObject | Boolean value true indicates that signed request object is mandatory | [Details](#forcesignedrequestobject) |
| frontChannelLogoutSessionSupported | Choose whether to support front channel session logout | [Details](#frontchannellogoutsessionsupported) |
| grantTypesAndResponseTypesAutofixEnabled | Boolean value specifying whether to Grant types and Response types can be auto fixed | [Details](#granttypesandresponsetypesautofixenabled) |
| grantTypesSupported | This list details which OAuth 2.0 grant types are supported by this OP | [Details](#granttypessupported) |
| httpLoggingEnabled | Enable/disable request/response logging filter | [Details](#httploggingenabled) |
| httpLoggingExcludePaths | This list details the base URIs for which the request/response logging filter will not record activity | [Details](#httploggingexcludepaths) |
| idGenerationEndpoint | ID Generation endpoint URL | [Details](#idgenerationendpoint) |
| idTokenEncryptionAlgValuesSupported | A list of the JWE encryption algorithms (alg values) supported by the OP for the ID Token to encode the Claims in a JWT | [Details](#idtokenencryptionalgvaluessupported) |
| idTokenEncryptionEncValuesSupported | A list of the JWE encryption algorithms (enc values) supported by the OP for the ID Token to encode the Claims in a JWT | [Details](#idtokenencryptionencvaluessupported) |
| idTokenFilterClaimsBasedOnAccessToken | Boolean value specifying whether idToken filters claims based on accessToken | [Details](#idtokenfilterclaimsbasedonaccesstoken) |
| idTokenLifetime | The lifetime of the ID Token | [Details](#idtokenlifetime) |
| idTokenSigningAlgValuesSupported | A list of the JWS signing algorithms (alg values) supported by the OP for the ID Token to encode the Claims in a JWT | [Details](#idtokensigningalgvaluessupported) |
| idTokenTokenBindingCnfValuesSupported | Array containing a list of the JWT Confirmation Method member names supported by the OP for Token Binding of ID Tokens. The presence of this parameter indicates that the OpenID Provider supports Token Binding of ID Tokens. If omitted, the default is that the OpenID Provider does not support Token Binding of ID Tokens | [Details](#idtokentokenbindingcnfvaluessupported) |
| imgLocation | The location for image files | [Details](#imglocation) |
| includeSidInResponse | Boolean value specifying whether to include sessionId in response | [Details](#includesidinresponse) |
| introspectionAccessTokenMustHaveUmaProtectionScope | If True, rejects introspection requests if access_token does not have the uma_protection scope in its authorization header | [Details](#introspectionaccesstokenmusthaveumaprotectionscope) |
| introspectionEndpoint | Introspection endpoint URL | [Details](#introspectionendpoint) |
| introspectionResponseScopesBackwardCompatibility | Boolean value specifying introspection response backward compatibility mode | [Details](#introspectionresponsescopesbackwardcompatibility) |
| introspectionScriptBackwardCompatibility | Boolean value specifying whether switch off client's introspection scripts (true value) and run all scripts that exists on server. Default value is false | [Details](#introspectionscriptbackwardcompatibility) |
| introspectionSkipAuthorization | Specifies if authorization to be skipped for introspection | [Details](#introspectionskipauthorization) |
| invalidateSessionCookiesAfterAuthorizationFlow | Boolean value to specify whether to invalidate session_id and consent_session_id cookies right after successful or unsuccessful authorization | [Details](#invalidatesessioncookiesafterauthorizationflow) |
| issuer | URL using the https scheme that OP asserts as Issuer identifier | [Details](#issuer) |
| jansElevenDeleteKeyEndpoint | oxEleven Delete Key endpoint URL | [Details](#janselevendeletekeyendpoint) |
| jansElevenGenerateKeyEndpoint | oxEleven Generate Key endpoint URL | [Details](#janselevengeneratekeyendpoint) |
| jansElevenSignEndpoint | oxEleven Sign endpoint UR | [Details](#janselevensignendpoint) |
| jansElevenTestModeToken | oxEleven Test Mode Token | [Details](#janseleventestmodetoken) |
| jansElevenVerifySignatureEndpoint | oxEleven Verify Signature endpoint URL | [Details](#janselevenverifysignatureendpoint) |
| jansId | URL for the Inum generator Service | [Details](#jansid) |
| jansOpenIdConnectVersion | OpenID Connect Version | [Details](#jansopenidconnectversion) |
| jmsBrokerURISet | JMS Broker URI Set | [Details](#jmsbrokeruriset) |
| jmsPassword | JMS Password | [Details](#jmspassword) |
| jmsUserName | JMS UserName | [Details](#jmsusername) |
| jsLocation | The location for JavaScript files | [Details](#jslocation) |
| jwksAlgorithmsSupported | A list of algorithms that will be used in JWKS endpoint | [Details](#jwksalgorithmssupported) |
| jwksUri | URL of the OP's JSON Web Key Set (JWK) document. This contains the signing key(s) the RP uses to validate signatures from the OP | [Details](#jwksuri) |
| keepAuthenticatorAttributesOnAcrChange | Boolean value specifying whether to keep authenticator attributes on ACR change | [Details](#keepauthenticatorattributesonacrchange) |
| keyAlgsAllowedForGeneration | List of algorithm allowed to be used for key generation | [Details](#keyalgsallowedforgeneration) |
| keyRegenerationEnabled | Boolean value specifying whether to regenerate keys | [Details](#keyregenerationenabled) |
| keyRegenerationInterval | The interval for key regeneration in hours | [Details](#keyregenerationinterval) |
| keySelectionStrategy | Key Selection Strategy : OLDER, NEWER, FIRST | [Details](#keyselectionstrategy) |
| keySignWithSameKeyButDiffAlg | Specifies if signing to be done with same key but apply different algorithms | [Details](#keysignwithsamekeybutdiffalg) |
| keyStoreFile | The Key Store File (JKS) | [Details](#keystorefile) |
| keyStoreSecret | The Key Store password | [Details](#keystoresecret) |
| legacyIdTokenClaims | Choose whether to include claims in ID tokens | [Details](#legacyidtokenclaims) |
| logClientIdOnClientAuthentication | Choose if application should log the Client ID on client authentication | [Details](#logclientidonclientauthentication) |
| logClientNameOnClientAuthentication | Choose if application should log the Client Name on client authentication | [Details](#logclientnameonclientauthentication) |
| loggingLayout | Logging layout used for Jans Authorization Server loggers | [Details](#logginglayout) |
| loggingLevel | Specify the logging level for oxAuth loggers | [Details](#logginglevel) |
| metricReporterInterval | The interval for metric reporter in seconds | [Details](#metricreporterinterval) |
| metricReporterKeepDataDays | The days to keep metric reported data | [Details](#metricreporterkeepdatadays) |
| mtlsAuthorizationEndpoint | URL for Mutual TLS (mTLS) Client Authentication and Certificate-Bound Access Tokens (MTLS) Endpoint | [Details](#mtlsauthorizationendpoint) |
| mtlsCheckSessionIFrame | URL for Mutual TLS (mTLS) IFrame that supports cross-origin communications for session state information with the RP Client using the HTML5 postMessage API | [Details](#mtlschecksessioniframe) |
| mtlsClientInfoEndpoint | URL for Mutual TLS (mTLS) Client Info endpoint | [Details](#mtlsclientinfoendpoint) |
| mtlsDeviceAuthzEndpoint | Mutual TLS (mTLS) device authorization endpoint URL | [Details](#mtlsdeviceauthzendpoint) |
| mtlsEndSessionEndpoint | URL for Mutual TLS (mTLS) to which an RP can perform a redirect to request that the end user be logged out at the OP | [Details](#mtlsendsessionendpoint) |
| mtlsIdGenerationEndpoint | Mutual TLS (mTLS) ID generation endpoint URL | [Details](#mtlsidgenerationendpoint) |
| mtlsIntrospectionEndpoint | Mutual TLS (mTLS) introspection endpoint URL | [Details](#mtlsintrospectionendpoint) |
| mtlsJwksUri | URL for Mutual TLS (mTLS) of the OP's JSON Web Key Set (JWK) document | [Details](#mtlsjwksuri) |
| mtlsParEndpoint | Mutual TLS (mTLS) Pushed Authorization Requests(PAR) endpoint URL | [Details](#mtlsparendpoint) |
| mtlsRegistrationEndpoint | Mutual TLS (mTLS) registration endpoint URL | [Details](#mtlsregistrationendpoint) |
| mtlsTokenEndpoint | URL for Mutual TLS (mTLS) Authorization token Endpoint | [Details](#mtlstokenendpoint) |
| mtlsTokenRevocationEndpoint | URL for Mutual TLS (mTLS) Authorization token revocation endpoint | [Details](#mtlstokenrevocationendpoint) |
| mtlsUserInfoEndpoint | Mutual TLS (mTLS) user info endpoint URL | [Details](#mtlsuserinfoendpoint) |
| openIdConfigurationEndpoint | URL for the Open ID Connect Configuration Endpoint | [Details](#openidconfigurationendpoint) |
| openIdDiscoveryEndpoint | Discovery endpoint URL | [Details](#openiddiscoveryendpoint) |
| openidScopeBackwardCompatibility | Set to false to only allow token endpoint request for openid scope with grant type equals to authorization_code, restrict access to userinfo to scope openid and only return id_token if scope contains openid | [Details](#openidscopebackwardcompatibility) |
| openidSubAttribute | Specifies which LDAP attribute is used for the subject identifier claim | [Details](#openidsubattribute) |
| opPolicyUri | URL that the OpenID Provider provides to the person registering the Client to read about the OP's requirements on how the Relying Party can use the data provided by the OP | [Details](#oppolicyuri) |
| opTosUri | URL that the OpenID Provider provides to the person registering the Client to read about OpenID Provider's terms of service | [Details](#optosuri) |
| pairwiseCalculationKey | Key to calculate algorithmic pairwise IDs | [Details](#pairwisecalculationkey) |
| pairwiseCalculationSalt | Salt to calculate algorithmic pairwise IDs | [Details](#pairwisecalculationsalt) |
| pairwiseIdType | the pairwise ID type | [Details](#pairwiseidtype) |
| parEndpoint | URL for Pushed Authorisation Request (PAR) Endpoint | [Details](#parendpoint) |
| persistIdTokenInLdap | Specifies whether to persist id_token into LDAP (otherwise saves into cache) | [Details](#persistidtokeninldap) |
| persistRefreshTokenInLdap | Specifies whether to persist refresh_token into LDAP (otherwise saves into cache) | [Details](#persistrefreshtokeninldap) |
| personCustomObjectClassList | This list details LDAP custom object classes for dynamic person enrollment | [Details](#personcustomobjectclasslist) |
| publicSubjectIdentifierPerClientEnabled | Specifies whether public subject identifier is allowed per client | [Details](#publicsubjectidentifierperclientenabled) |
| redirectUrisRegexEnabled | Enable/Disable redirect uris validation using regular expression | [Details](#redirecturisregexenabled) |
| refreshTokenExtendLifetimeOnRotation | Boolean value specifying whether to extend refresh tokens on rotation | [Details](#refreshtokenextendlifetimeonrotation) |
| refreshTokenLifetime | The lifetime of the Refresh Token | [Details](#refreshtokenlifetime) |
| registrationEndpoint | Registration endpoint URL | [Details](#registrationendpoint) |
| rejectEndSessionIfIdTokenExpired | default value false. If true and id_token is not found in db, request is rejected | [Details](#rejectendsessionifidtokenexpired) |
| rejectJwtWithNoneAlg | Boolean value specifying whether reject JWT requested or validated with algorithm None. Default value is true | [Details](#rejectjwtwithnonealg) |
| removeRefreshTokensForClientOnLogout | Boolean value specifying whether to remove Refresh Tokens on logout. Default value is false | [Details](#removerefreshtokensforclientonlogout) |
| requestObjectEncryptionAlgValuesSupported | A list of the JWE encryption algorithms (alg values) supported by the OP for Request Objects | [Details](#requestobjectencryptionalgvaluessupported) |
| requestObjectEncryptionEncValuesSupported | A list of the JWE encryption algorithms (enc values) supported by the OP for Request Objects | [Details](#requestobjectencryptionencvaluessupported) |
| requestObjectSigningAlgValuesSupported | A list of the JWS signing algorithms (alg values) supported by the OP for Request Objects | [Details](#requestobjectsigningalgvaluessupported) |
| requestParameterSupported | Boolean value specifying whether the OP supports use of the request parameter | [Details](#requestparametersupported) |
| requestUriBlockList | Block list for requestUri that can come to Authorization Endpoint (e.g. localhost) | [Details](#requesturiblocklist) |
| requestUriHashVerificationEnabled | Boolean value specifying whether the OP supports use of the request_uri hash verification | [Details](#requesturihashverificationenabled) |
| requestUriParameterSupported | Boolean value specifying whether the OP supports use of the request_uri parameter | [Details](#requesturiparametersupported) |
| requirePar | Boolean value to indicate of Pushed Authorisation Request(PAR)is required | [Details](#requirepar) |
| requirePkce | Boolean value true check for Proof Key for Code Exchange (PKCE) | [Details](#requirepkce) |
| requireRequestObjectEncryption | Boolean value true encrypts request object | [Details](#requirerequestobjectencryption) |
| requireRequestUriRegistration | Boolean value specifying whether the OP requires any request_uri values used to be pre-registered using the request_uris registration parameter | [Details](#requirerequesturiregistration) |
| responseModesSupported | This list details which OAuth 2.0 response modes are supported by this OP | [Details](#responsemodessupported) |
| responseTypesSupported | This list details which OAuth 2.0 response_type values are supported by this OP. | [Details](#responsetypessupported) |
| returnClientSecretOnRead | Boolean value specifying whether a client_secret is returned on client GET or PUT. Set to true by default which means to return secret | [Details](#returnclientsecretonread) |
| returnDeviceSecretFromAuthzEndpoint |  | [Details](#returndevicesecretfromauthzendpoint) |
| rotateDeviceSecret |  | [Details](#rotatedevicesecret) |
| sectorIdentifierCacheLifetimeInMinutes | Sector Identifier cache lifetime in minutes | [Details](#sectoridentifiercachelifetimeinminutes) |
| serverSessionIdLifetime | Dedicated property to control lifetime of the server side OP session object in seconds. Overrides sessionIdLifetime. By default value is 0, so object lifetime equals sessionIdLifetime (which sets both cookie and object expiration). It can be useful if goal is to keep different values for client cookie and server object | [Details](#serversessionidlifetime) |
| serviceDocumentation | URL of a page containing human-readable information that developers might want or need to know when using the OpenID Provider | [Details](#servicedocumentation) |
| sessionIdLifetime | The lifetime of session id in seconds. If 0 or -1 then expiration is not set. session_id cookie expires when browser session ends | [Details](#sessionidlifetime) |
| sessionIdPersistInCache | Boolean value specifying whether to persist session_id in cache | [Details](#sessionidpersistincache) |
| sessionIdPersistOnPromptNone | Boolean value specifying whether to persist session ID on prompt none | [Details](#sessionidpersistonpromptnone) |
| sessionIdRequestParameterEnabled | Boolean value specifying whether to enable session_id HTTP request parameter | [Details](#sessionidrequestparameterenabled) |
| sessionIdUnauthenticatedUnusedLifetime | The lifetime for unused unauthenticated session states | [Details](#sessionidunauthenticatedunusedlifetime) |
| sessionIdUnusedLifetime | The lifetime for unused session states | [Details](#sessionidunusedlifetime) |
| shareSubjectIdBetweenClientsWithSameSectorId | When true, clients with the same Sector ID also share the same Subject ID | [Details](#sharesubjectidbetweenclientswithsamesectorid) |
| skipAuthorizationForOpenIdScopeAndPairwiseId | Choose whether to skip authorization if a client has an OpenId scope and a pairwise ID | [Details](#skipauthorizationforopenidscopeandpairwiseid) |
| skipRefreshTokenDuringRefreshing | Boolean value specifying whether to skip refreshing tokens on refreshing | [Details](#skiprefreshtokenduringrefreshing) |
| softwareStatementValidationClaimName | Validation claim name for software statement | [Details](#softwarestatementvalidationclaimname) |
| softwareStatementValidationType | Validation type used for software statement | [Details](#softwarestatementvalidationtype) |
| spontaneousScopeLifetime | The lifetime of spontaneous scope in seconds | [Details](#spontaneousscopelifetime) |
| ssaConfiguration | SSA Configuration | [Details](#ssaconfiguration) |
| statAuthorizationScope | Scope required for Statistical Authorization | [Details](#statauthorizationscope) |
| staticDecryptionKid | Specifies static decryption Kid | [Details](#staticdecryptionkid) |
| staticKid | Specifies static Kid | [Details](#statickid) |
| statTimerIntervalInSeconds | Statistical data capture time interval | [Details](#stattimerintervalinseconds) |
| subjectIdentifiersPerClientSupported | A list of the subject identifiers supported per client | [Details](#subjectidentifiersperclientsupported) |
| subjectTypesSupported | This list details which Subject Identifier types that the OP supports. Valid types include pairwise and public. | [Details](#subjecttypessupported) |
| tokenEndpoint | The token endpoint URL | [Details](#tokenendpoint) |
| tokenEndpointAuthMethodsSupported | A list of Client Authentication methods supported by this Token Endpoint | [Details](#tokenendpointauthmethodssupported) |
| tokenEndpointAuthSigningAlgValuesSupported | A list of the JWS signing algorithms (alg values) supported by the Token Endpoint for the signature on the JWT used to authenticate the Client at the Token Endpoint for the private_key_jwt and client_secret_jwt authentication methods | [Details](#tokenendpointauthsigningalgvaluessupported) |
| tokenRevocationEndpoint | The URL for the access_token or refresh_token revocation endpoint | [Details](#tokenrevocationendpoint) |
| trustedClientEnabled | Boolean value specifying whether a client is trusted and no authorization is required | [Details](#trustedclientenabled) |
| uiLocalesSupported | This list details the languages and scripts supported for the user interface | [Details](#uilocalessupported) |
| umaAddScopesAutomatically | Add UMA scopes automatically if it is not registered yet | [Details](#umaaddscopesautomatically) |
| umaConfigurationEndpoint | UMA Configuration endpoint URL | [Details](#umaconfigurationendpoint) |
| umaGrantAccessIfNoPolicies | Specify whether to grant access to resources if there is no any policies associated with scopes | [Details](#umagrantaccessifnopolicies) |
| umaPctLifetime | UMA PCT lifetime | [Details](#umapctlifetime) |
| umaResourceLifetime | UMA Resource lifetime | [Details](#umaresourcelifetime) |
| umaRestrictResourceToAssociatedClient | Restrict access to resource by associated client | [Details](#umarestrictresourcetoassociatedclient) |
| umaRptAsJwt | Issue RPT as JWT or as random string | [Details](#umarptasjwt) |
| umaRptLifetime | UMA RPT lifetime | [Details](#umarptlifetime) |
| umaTicketLifetime | UMA ticket lifetime | [Details](#umaticketlifetime) |
| umaValidateClaimToken | Validate claim_token as id_token assuming it is issued by local id | [Details](#umavalidateclaimtoken) |
| updateClientAccessTime | Choose if application should update oxLastAccessTime/oxLastLogonTime attributes upon client authentication | [Details](#updateclientaccesstime) |
| updateUserLastLogonTime | Choose if application should update oxLastLogonTime attribute upon user authentication | [Details](#updateuserlastlogontime) |
| useHighestLevelScriptIfAcrScriptNotFound | Enable/Disable usage of highest level script in case ACR script does not exist | [Details](#usehighestlevelscriptifacrscriptnotfound) |
| useLocalCache | Cache in local memory cache attributes, scopes, clients and organization entry with expiration 60 seconds | [Details](#uselocalcache) |
| useNestedJwtDuringEncryption | Boolean value specifying whether to use nested Jwt during encryption | [Details](#usenestedjwtduringencryption) |
| userInfoConfiguration | UserInfo Configuration | [Details](#userinfoconfiguration) |
| userInfoEncryptionAlgValuesSupported | This JSON Array lists which JWS encryption algorithms (alg values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT | [Details](#userinfoencryptionalgvaluessupported) |
| userInfoEncryptionEncValuesSupported | This JSON Array lists which JWS encryption algorithms (enc values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT | [Details](#userinfoencryptionencvaluessupported) |
| userInfoEndpoint | The User Info endpoint URL | [Details](#userinfoendpoint) |
| userInfoSigningAlgValuesSupported | This JSON Array lists which JWS signing algorithms (alg values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT | [Details](#userinfosigningalgvaluessupported) |
| webKeysStorage | Web Key Storage Type | [Details](#webkeysstorage) |


### accessTokenLifetime

- Description: The lifetime of the short lived Access Token

- Required: No

- Default value: None


### accessTokenSigningAlgValuesSupported

- Description: A list of the JWS signing algorithms (alg values) supported by the OP for the access token to encode the Claims in a JWT

- Required: No

- Default value: None


### activeSessionAuthorizationScope

- Description: Authorization Scope for active session

- Required: No

- Default value: None


### agamaConfiguration

- Description: Engine Config which offers an alternative way to build authentication flows in Janssen server

- Required: No

- Default value: None


### allowAllValueForRevokeEndpoint

- Description: Boolean value true allow all value for revoke endpoint

- Required: No

- Default value: false


### allowEndSessionWithUnmatchedSid

- Description: default value false. If true, sid check will be skipped

- Required: No

- Default value: false


### allowIdTokenWithoutImplicitGrantType

- Description: Specifies if a token without implicit grant types is allowed

- Required: No

- Default value: None


### allowPostLogoutRedirectWithoutValidation

- Description: Allows post-logout redirect without validation for the End Session endpoint (still AS validates it against clientWhiteList url pattern property)

- Required: No

- Default value: false


### allowSpontaneousScopes

- Description: Specifies whether to allow spontaneous scopes

- Required: No

- Default value: None


### authenticationFilters

- Description: This list details filters for user authentication

- Required: No

- Default value: None


### authenticationFiltersEnabled

- Description: Boolean value specifying whether to enable user authentication filters

- Required: No

- Default value: None


### authenticationProtectionConfiguration

- Description: Authentication Brute Force Protection Configuration

- Required: No

- Default value: None


### authorizationCodeLifetime

- Description: The lifetime of the Authorization Code

- Required: No

- Default value: None


### authorizationEncryptionAlgValuesSupported

- Description: List of authorization encryption algorithms supported by this OP

- Required: No

- Default value: None


### authorizationEncryptionEncValuesSupported

- Description: A list of the authorization encryption algorithms supported

- Required: No

- Default value: None


### authorizationEndpoint

- Description: The authorization endpoint URL

- Required: No

- Default value: None


### authorizationRequestCustomAllowedParameters

- Description: This list details the allowed custom parameters for authorization requests

- Required: No

- Default value: None


### authorizationSigningAlgValuesSupported

- Description: List of authorization signing algorithms supported by this OP

- Required: No

- Default value: None


### backchannelAuthenticationEndpoint

- Description: Backchannel Authentication Endpoint

- Required: No

- Default value: None


### backchannelAuthenticationRequestSigningAlgValuesSupported

- Description: Backchannel Authentication Request Signing Alg Values Supported

- Required: No

- Default value: None


### backchannelAuthenticationResponseExpiresIn

- Description: Backchannel Authentication Response Expires In

- Required: No

- Default value: None


### backchannelAuthenticationResponseInterval

- Description: Backchannel Authentication Response Interval

- Required: No

- Default value: None


### backchannelBindingMessagePattern

- Description: Backchannel Binding Message Pattern

- Required: No

- Default value: None


### backchannelClientId

- Description: Backchannel Client Id

- Required: No

- Default value: None


### backchannelDeviceRegistrationEndpoint

- Description: Backchannel Device Registration Endpoint

- Required: No

- Default value: None


### backchannelLoginHintClaims

- Description: Backchannel Login Hint Claims

- Required: No

- Default value: None


### backchannelRedirectUri

- Description: Backchannel Redirect Uri

- Required: No

- Default value: None


### backchannelRequestsProcessorJobChunkSize

- Description: Each backchannel request processor iteration fetches chunk of data to be processed

- Required: No

- Default value: None


### backchannelRequestsProcessorJobIntervalSec

- Description: Specifies the allowable elapsed time in seconds backchannel request processor executes

- Required: No

- Default value: None


### backchannelTokenDeliveryModesSupported

- Description: Backchannel Token Delivery Modes Supported

- Required: No

- Default value: None


### backchannelUserCodeParameterSupported

- Description: Backchannel User Code Parameter Supported

- Required: No

- Default value: None


### baseEndpoint

- Description: The base URL for endpoints

- Required: No

- Default value: None


### blockWebviewAuthorizationEnabled

- Description: Enable/Disable block authorizations that originate from Webview (Mobile apps).

- Required: No

- Default value: false


### changeSessionIdOnAuthentication

- Description: Boolean value specifying whether change session_id on authentication. Default value is true

- Required: No

- Default value: true


### checkSessionIFrame

- Description: URL for an OP IFrame that supports cross-origin communications for session state information with the RP Client using the HTML5 postMessage API

- Required: No

- Default value: None


### checkUserPresenceOnRefreshToken

- Description: Check whether user exists and is active before creating RefreshToken. Set it to true if check is needed(Default value is false - don't check.

- Required: No

- Default value: false


### cibaEndUserNotificationConfig

- Description: CIBA End User Notification Config

- Required: No

- Default value: None


### cibaGrantLifeExtraTimeSec

- Description: Specifies the CIBA Grant life extra time in seconds

- Required: No

- Default value: None


### cibaMaxExpirationTimeAllowedSec

- Description: Specifies the CIBA token expiration time in seconds

- Required: No

- Default value: None


### claimsLocalesSupported

- Description: This list details the languages and scripts supported for values in the claims being returned

- Required: No

- Default value: None


### claimsParameterSupported

- Description: Specifies whether the OP supports use of the claims parameter

- Required: No

- Default value: None


### claimTypesSupported

- Description: A list of the Claim Types that the OpenID Provider supports

- Required: No

- Default value: None


### cleanServiceBatchChunkSize

- Description: Clean service chunk size which is used during clean up

- Required: No

- Default value: 100


### cleanServiceInterval

- Description: Time interval for the Clean Service in seconds

- Required: No

- Default value: None


### clientAuthenticationFilters

- Description: This list details filters for client authentication

- Required: No

- Default value: None


### clientAuthenticationFiltersEnabled

- Description: Boolean value specifying whether to enable client authentication filters

- Required: No

- Default value: None


### clientBlackList

- Description: This list specified which client redirection URIs are black-listed

- Required: No

- Default value: None


### clientInfoEndpoint

- Description: The Client Info endpoint URL

- Required: No

- Default value: None


### clientRegDefaultToCodeFlowWithRefresh

- Description: Boolean value specifying whether to add Authorization Code Flow with Refresh grant during client registratio

- Required: No

- Default value: None


### clientWhiteList

- Description: This list specifies which client redirection URIs are white-listed

- Required: No

- Default value: None


### configurationUpdateInterval

- Description: The interval for configuration update in seconds

- Required: No

- Default value: None


### consentGatheringScriptBackwardCompatibility

- Description: Boolean value specifying whether to turn on Consent Gathering Script backward compatibility mode. If true AS will pick up script with higher level globally. If false (default) AS will pick up script based on client configuration

- Required: No

- Default value: false


### cookieDomain

- Description: Sets cookie domain for all cookies created by OP

- Required: No

- Default value: None


### corsConfigurationFilters

- Description: This list specifies the CORS configuration filters

- Required: No

- Default value: None


### cssLocation

- Description: The location for CSS files

- Required: No

- Default value: None


### customHeadersWithAuthorizationResponse

- Description: Choose whether to enable the custom response header parameter to return custom headers with the authorization response

- Required: No

- Default value: None


### dateFormatterPattern

- Description: List of key value, e.g. 'birthdate: 'yyyy-MM-dd', etc.

- Required: No

- Default value: None


### dcrAuthorizationWithClientCredentials

- Description: Boolean value indicating if DCR authorization to be performed using client credentials

- Required: No

- Default value: false


### dcrAuthorizationWithMTLS

- Description: Boolean value indicating if DCR authorization allowed with MTLS

- Required: No

- Default value: false


### dcrIssuers

- Description: List of DCR issuers

- Required: No

- Default value: None


### dcrSignatureValidationEnabled

- Description: Boolean value enables DCR signature validation. Default is false

- Required: No

- Default value: false


### dcrSignatureValidationJwks

- Description: Specifies JWKS for all DCR's validations

- Required: No

- Default value: None


### dcrSignatureValidationJwksUri

- Description: Specifies JWKS URI for all DCR's validations

- Required: No

- Default value: None


### dcrSignatureValidationSharedSecret

- Description: Specifies shared secret for Dynamic Client Registration

- Required: No

- Default value: None


### dcrSignatureValidationSoftwareStatementJwksClaim

- Description: Specifies claim name inside software statement. Value of claim should point to inlined JWKS

- Required: No

- Default value: None


### dcrSignatureValidationSoftwareStatementJwksURIClaim

- Description: Specifies claim name inside software statement. Value of claim should point to JWKS URI

- Required: No

- Default value: None


### dcrSsaValidationConfigs

- Description: DCR SSA Validation configurations used to perform validation of SSA or DCR

- Required: No

- Default value: None


### defaultSignatureAlgorithm

- Description: The default signature algorithm to sign ID Tokens

- Required: No

- Default value: None


### defaultSubjectType

- Description: The default subject type used for dynamic client registration

- Required: No

- Default value: None


### deviceAuthzEndpoint

- Description: URL for the Device Authorization

- Required: No

- Default value: None


### deviceAuthzRequestExpiresIn

- Description: Expiration time given for device authorization requests

- Required: No

- Default value: None


### deviceAuthzResponseTypeToProcessAuthz

- Description: Response type used to process device authz requests

- Required: No

- Default value: None


### deviceAuthzTokenPollInterval

- Description: Default interval returned to the client to process device token requests

- Required: No

- Default value: None


### disableAuthnForMaxAgeZero

- Description: Boolean value specifying whether to disable authentication when max_age=0

- Required: No

- Default value: false


### disableJdkLogger

- Description: Choose whether to disable JDK loggers

- Required: No

- Default value: true


### disableU2fEndpoint

- Description: Choose whether to disable U2F endpoints

- Required: No

- Default value: false


### discoveryAllowedKeys

- Description: List of configuration response claim allowed to be displayed in discovery endpoint

- Required: No

- Default value: None


### discoveryCacheLifetimeInMinutes

- Description: Lifetime of discovery cache

- Required: No

- Default value: 60


### discoveryDenyKeys

- Description: List of configuration response claims which must not be displayed in discovery endpoint response

- Required: No

- Default value: None


### displayValuesSupported

- Description: A list of the display parameter values that the OpenID Provider supports

- Required: No

- Default value: None


### dnName

- Description: DN of certificate issuer

- Required: No

- Default value: None


### dpopJtiCacheTime

- Description: Demonstration of Proof-of-Possession (DPoP) cache time

- Required: No

- Default value: 3600


### dpopSigningAlgValuesSupported

- Description: Demonstration of Proof-of-Possession (DPoP) authorization signing algorithms supported

- Required: No

- Default value: None


### dpopTimeframe

- Description: Demonstration of Proof-of-Possession (DPoP) timeout

- Required: No

- Default value: 5


### dynamicGrantTypeDefault

- Description: This list details which OAuth 2.0 grant types can be set up with the client registration API

- Required: No

- Default value: None


### dynamicRegistrationAllowedPasswordGrantScopes

- Description: List of grant scopes for dynamic registration

- Required: No

- Default value: None


### dynamicRegistrationCustomAttributes

- Description: This list details the custom attributes for dynamic registration

- Required: No

- Default value: None


### dynamicRegistrationCustomObjectClass

- Description: LDAP custom object class for dynamic registration

- Required: No

- Default value: None


### dynamicRegistrationExpirationTime

- Description: Expiration time in seconds for clients created with dynamic registration, 0 or -1 means never expire

- Required: No

- Default value: -1


### dynamicRegistrationPasswordGrantTypeEnabled

- Description: Boolean value specifying whether to enable Password Grant Type during Dynamic Registration

- Required: No

- Default value: false


### dynamicRegistrationPersistClientAuthorizations

- Description: Boolean value specifying whether to persist client authorizations

- Required: No

- Default value: None


### dynamicRegistrationScopesParamEnabled

- Description: Boolean value specifying whether to enable scopes parameter in dynamic registration

- Required: No

- Default value: None


### enableClientGrantTypeUpdate

- Description: Choose if client can update Grant Type values

- Required: No

- Default value: None


### enabledOAuthAuditLogging

- Description: enable OAuth Audit Logging

- Required: No

- Default value: None


### endSessionEndpoint

- Description: URL at the OP to which an RP can perform a redirect to request that the end user be logged out at the OP

- Required: No

- Default value: None


### endSessionWithAccessToken

- Description: Choose whether to accept access tokens to call end_session endpoint

- Required: No

- Default value: None


### errorHandlingMethod

- Description: A list of possible error handling methods

- Required: No

- Default value: None


### errorReasonEnabled

- Description: Boolean value specifying whether to return detailed reason of the error from AS. Default value is false

- Required: No

- Default value: false


### expirationNotificatorEnabled

- Description: Boolean value specifying whether expiration notificator is enabled (used to identify expiration for persistence that support TTL, like Couchbase)

- Required: No

- Default value: false


### expirationNotificatorIntervalInSeconds

- Description: The expiration notificator interval in second

- Required: No

- Default value: None


### expirationNotificatorMapSizeLimit

- Description: The expiration notificator maximum size limit

- Required: No

- Default value: None


### externalLoggerConfiguration

- Description: The path to the external log4j2 logging configuration

- Required: No

- Default value: None


### externalUriWhiteList

- Description: This list specifies which external URIs can be called by AS (if empty any URI can be called)

- Required: No

- Default value: None


### fapiCompatibility

- Description: Boolean value specifying whether to turn on FAPI compatibility mode. If true AS behaves in more strict mode

- Required: No

- Default value: false


### featureFlags

- Description: List of enabled feature flags

- Required: No

- Default value: None


### forceIdTokenHintPrecense

- Description: Boolean value specifying whether force id_token_hint parameter presence

- Required: No

- Default value: false


### forceOfflineAccessScopeToEnableRefreshToken

- Description: Boolean value specifying whether force offline_access scope to enable refresh_token grant type. Default value is true

- Required: No

- Default value: true


### forceSignedRequestObject

- Description: Boolean value true indicates that signed request object is mandatory

- Required: No

- Default value: false


### frontChannelLogoutSessionSupported

- Description: Choose whether to support front channel session logout

- Required: No

- Default value: None


### grantTypesAndResponseTypesAutofixEnabled

- Description: Boolean value specifying whether to Grant types and Response types can be auto fixed

- Required: No

- Default value: None


### grantTypesSupported

- Description: This list details which OAuth 2.0 grant types are supported by this OP

- Required: No

- Default value: None


### httpLoggingEnabled

- Description: Enable/disable request/response logging filter

- Required: No

- Default value: None


### httpLoggingExcludePaths

- Description: This list details the base URIs for which the request/response logging filter will not record activity

- Required: No

- Default value: None


### idGenerationEndpoint

- Description: ID Generation endpoint URL

- Required: No

- Default value: None


### idTokenEncryptionAlgValuesSupported

- Description: A list of the JWE encryption algorithms (alg values) supported by the OP for the ID Token to encode the Claims in a JWT

- Required: No

- Default value: None


### idTokenEncryptionEncValuesSupported

- Description: A list of the JWE encryption algorithms (enc values) supported by the OP for the ID Token to encode the Claims in a JWT

- Required: No

- Default value: None


### idTokenFilterClaimsBasedOnAccessToken

- Description: Boolean value specifying whether idToken filters claims based on accessToken

- Required: No

- Default value: None


### idTokenLifetime

- Description: The lifetime of the ID Token

- Required: No

- Default value: None


### idTokenSigningAlgValuesSupported

- Description: A list of the JWS signing algorithms (alg values) supported by the OP for the ID Token to encode the Claims in a JWT

- Required: No

- Default value: None


### idTokenTokenBindingCnfValuesSupported

- Description: Array containing a list of the JWT Confirmation Method member names supported by the OP for Token Binding of ID Tokens. The presence of this parameter indicates that the OpenID Provider supports Token Binding of ID Tokens. If omitted, the default is that the OpenID Provider does not support Token Binding of ID Tokens

- Required: No

- Default value: None


### imgLocation

- Description: The location for image files

- Required: No

- Default value: None


### includeSidInResponse

- Description: Boolean value specifying whether to include sessionId in response

- Required: No

- Default value: false


### introspectionAccessTokenMustHaveUmaProtectionScope

- Description: If True, rejects introspection requests if access_token does not have the uma_protection scope in its authorization header

- Required: No

- Default value: false


### introspectionEndpoint

- Description: Introspection endpoint URL

- Required: No

- Default value: None


### introspectionResponseScopesBackwardCompatibility

- Description: Boolean value specifying introspection response backward compatibility mode

- Required: No

- Default value: false


### introspectionScriptBackwardCompatibility

- Description: Boolean value specifying whether switch off client's introspection scripts (true value) and run all scripts that exists on server. Default value is false

- Required: No

- Default value: false


### introspectionSkipAuthorization

- Description: Specifies if authorization to be skipped for introspection

- Required: No

- Default value: None


### invalidateSessionCookiesAfterAuthorizationFlow

- Description: Boolean value to specify whether to invalidate session_id and consent_session_id cookies right after successful or unsuccessful authorization

- Required: No

- Default value: false


### issuer

- Description: URL using the https scheme that OP asserts as Issuer identifier

- Required: No

- Default value: None


### jansElevenDeleteKeyEndpoint

- Description: oxEleven Delete Key endpoint URL

- Required: No

- Default value: None


### jansElevenGenerateKeyEndpoint

- Description: oxEleven Generate Key endpoint URL

- Required: No

- Default value: None


### jansElevenSignEndpoint

- Description: oxEleven Sign endpoint UR

- Required: No

- Default value: None


### jansElevenTestModeToken

- Description: oxEleven Test Mode Token

- Required: No

- Default value: None


### jansElevenVerifySignatureEndpoint

- Description: oxEleven Verify Signature endpoint URL

- Required: No

- Default value: None


### jansId

- Description: URL for the Inum generator Service

- Required: No

- Default value: None


### jansOpenIdConnectVersion

- Description: OpenID Connect Version

- Required: No

- Default value: None


### jmsBrokerURISet

- Description: JMS Broker URI Set

- Required: No

- Default value: None


### jmsPassword

- Description: JMS Password

- Required: No

- Default value: None


### jmsUserName

- Description: JMS UserName

- Required: No

- Default value: None


### jsLocation

- Description: The location for JavaScript files

- Required: No

- Default value: None


### jwksAlgorithmsSupported

- Description: A list of algorithms that will be used in JWKS endpoint

- Required: No

- Default value: None


### jwksUri

- Description: URL of the OP's JSON Web Key Set (JWK) document. This contains the signing key(s) the RP uses to validate signatures from the OP

- Required: No

- Default value: None


### keepAuthenticatorAttributesOnAcrChange

- Description: Boolean value specifying whether to keep authenticator attributes on ACR change

- Required: No

- Default value: false


### keyAlgsAllowedForGeneration

- Description: List of algorithm allowed to be used for key generation

- Required: No

- Default value: None


### keyRegenerationEnabled

- Description: Boolean value specifying whether to regenerate keys

- Required: No

- Default value: None


### keyRegenerationInterval

- Description: The interval for key regeneration in hours

- Required: No

- Default value: None


### keySelectionStrategy

- Description: Key Selection Strategy : OLDER, NEWER, FIRST

- Required: No

- Default value: OLDER


### keySignWithSameKeyButDiffAlg

- Description: Specifies if signing to be done with same key but apply different algorithms

- Required: No

- Default value: None


### keyStoreFile

- Description: The Key Store File (JKS)

- Required: No

- Default value: None


### keyStoreSecret

- Description: The Key Store password

- Required: No

- Default value: None


### legacyIdTokenClaims

- Description: Choose whether to include claims in ID tokens

- Required: No

- Default value: None


### logClientIdOnClientAuthentication

- Description: Choose if application should log the Client ID on client authentication

- Required: No

- Default value: None


### logClientNameOnClientAuthentication

- Description: Choose if application should log the Client Name on client authentication

- Required: No

- Default value: None


### loggingLayout

- Description: Logging layout used for Jans Authorization Server loggers

- Required: No

- Default value: None


### loggingLevel

- Description: Specify the logging level for oxAuth loggers

- Required: No

- Default value: None


### metricReporterInterval

- Description: The interval for metric reporter in seconds

- Required: No

- Default value: None


### metricReporterKeepDataDays

- Description: The days to keep metric reported data

- Required: No

- Default value: None


### mtlsAuthorizationEndpoint

- Description: URL for Mutual TLS (mTLS) Client Authentication and Certificate-Bound Access Tokens (MTLS) Endpoint

- Required: No

- Default value: None


### mtlsCheckSessionIFrame

- Description: URL for Mutual TLS (mTLS) IFrame that supports cross-origin communications for session state information with the RP Client using the HTML5 postMessage API

- Required: No

- Default value: None


### mtlsClientInfoEndpoint

- Description: URL for Mutual TLS (mTLS) Client Info endpoint

- Required: No

- Default value: None


### mtlsDeviceAuthzEndpoint

- Description: Mutual TLS (mTLS) device authorization endpoint URL

- Required: No

- Default value: None


### mtlsEndSessionEndpoint

- Description: URL for Mutual TLS (mTLS) to which an RP can perform a redirect to request that the end user be logged out at the OP

- Required: No

- Default value: None


### mtlsIdGenerationEndpoint

- Description: Mutual TLS (mTLS) ID generation endpoint URL

- Required: No

- Default value: None


### mtlsIntrospectionEndpoint

- Description: Mutual TLS (mTLS) introspection endpoint URL

- Required: No

- Default value: None


### mtlsJwksUri

- Description: URL for Mutual TLS (mTLS) of the OP's JSON Web Key Set (JWK) document

- Required: No

- Default value: None


### mtlsParEndpoint

- Description: Mutual TLS (mTLS) Pushed Authorization Requests(PAR) endpoint URL

- Required: No

- Default value: None


### mtlsRegistrationEndpoint

- Description: Mutual TLS (mTLS) registration endpoint URL

- Required: No

- Default value: None


### mtlsTokenEndpoint

- Description: URL for Mutual TLS (mTLS) Authorization token Endpoint

- Required: No

- Default value: None


### mtlsTokenRevocationEndpoint

- Description: URL for Mutual TLS (mTLS) Authorization token revocation endpoint

- Required: No

- Default value: None


### mtlsUserInfoEndpoint

- Description: Mutual TLS (mTLS) user info endpoint URL

- Required: No

- Default value: None


### openIdConfigurationEndpoint

- Description: URL for the Open ID Connect Configuration Endpoint

- Required: No

- Default value: None


### openIdDiscoveryEndpoint

- Description: Discovery endpoint URL

- Required: No

- Default value: None


### openidScopeBackwardCompatibility

- Description: Set to false to only allow token endpoint request for openid scope with grant type equals to authorization_code, restrict access to userinfo to scope openid and only return id_token if scope contains openid

- Required: No

- Default value: false


### openidSubAttribute

- Description: Specifies which LDAP attribute is used for the subject identifier claim

- Required: No

- Default value: None


### opPolicyUri

- Description: URL that the OpenID Provider provides to the person registering the Client to read about the OP's requirements on how the Relying Party can use the data provided by the OP

- Required: No

- Default value: None


### opTosUri

- Description: URL that the OpenID Provider provides to the person registering the Client to read about OpenID Provider's terms of service

- Required: No

- Default value: None


### pairwiseCalculationKey

- Description: Key to calculate algorithmic pairwise IDs

- Required: No

- Default value: None


### pairwiseCalculationSalt

- Description: Salt to calculate algorithmic pairwise IDs

- Required: No

- Default value: None


### pairwiseIdType

- Description: the pairwise ID type

- Required: No

- Default value: None


### parEndpoint

- Description: URL for Pushed Authorisation Request (PAR) Endpoint

- Required: No

- Default value: None


### persistIdTokenInLdap

- Description: Specifies whether to persist id_token into LDAP (otherwise saves into cache)

- Required: No

- Default value: false


### persistRefreshTokenInLdap

- Description: Specifies whether to persist refresh_token into LDAP (otherwise saves into cache)

- Required: No

- Default value: true


### personCustomObjectClassList

- Description: This list details LDAP custom object classes for dynamic person enrollment

- Required: No

- Default value: None


### publicSubjectIdentifierPerClientEnabled

- Description: Specifies whether public subject identifier is allowed per client

- Required: No

- Default value: false


### redirectUrisRegexEnabled

- Description: Enable/Disable redirect uris validation using regular expression

- Required: No

- Default value: false


### refreshTokenExtendLifetimeOnRotation

- Description: Boolean value specifying whether to extend refresh tokens on rotation

- Required: No

- Default value: false


### refreshTokenLifetime

- Description: The lifetime of the Refresh Token

- Required: No

- Default value: None


### registrationEndpoint

- Description: Registration endpoint URL

- Required: No

- Default value: None


### rejectEndSessionIfIdTokenExpired

- Description: default value false. If true and id_token is not found in db, request is rejected

- Required: No

- Default value: false


### rejectJwtWithNoneAlg

- Description: Boolean value specifying whether reject JWT requested or validated with algorithm None. Default value is true

- Required: No

- Default value: true


### removeRefreshTokensForClientOnLogout

- Description: Boolean value specifying whether to remove Refresh Tokens on logout. Default value is false

- Required: No

- Default value: true


### requestObjectEncryptionAlgValuesSupported

- Description: A list of the JWE encryption algorithms (alg values) supported by the OP for Request Objects

- Required: No

- Default value: None


### requestObjectEncryptionEncValuesSupported

- Description: A list of the JWE encryption algorithms (enc values) supported by the OP for Request Objects

- Required: No

- Default value: None


### requestObjectSigningAlgValuesSupported

- Description: A list of the JWS signing algorithms (alg values) supported by the OP for Request Objects

- Required: No

- Default value: None


### requestParameterSupported

- Description: Boolean value specifying whether the OP supports use of the request parameter

- Required: No

- Default value: None


### requestUriBlockList

- Description: Block list for requestUri that can come to Authorization Endpoint (e.g. localhost)

- Required: No

- Default value: None


### requestUriHashVerificationEnabled

- Description: Boolean value specifying whether the OP supports use of the request_uri hash verification

- Required: No

- Default value: None


### requestUriParameterSupported

- Description: Boolean value specifying whether the OP supports use of the request_uri parameter

- Required: No

- Default value: None


### requirePar

- Description: Boolean value to indicate of Pushed Authorisation Request(PAR)is required

- Required: No

- Default value: false


### requirePkce

- Description: Boolean value true check for Proof Key for Code Exchange (PKCE)

- Required: No

- Default value: false


### requireRequestObjectEncryption

- Description: Boolean value true encrypts request object

- Required: No

- Default value: false


### requireRequestUriRegistration

- Description: Boolean value specifying whether the OP requires any request_uri values used to be pre-registered using the request_uris registration parameter

- Required: No

- Default value: None


### responseModesSupported

- Description: This list details which OAuth 2.0 response modes are supported by this OP

- Required: No

- Default value: None


### responseTypesSupported

- Description: This list details which OAuth 2.0 response_type values are supported by this OP.

- Required: No

- Default value: By default, every combination of code, token and id_token is supported.


### returnClientSecretOnRead

- Description: Boolean value specifying whether a client_secret is returned on client GET or PUT. Set to true by default which means to return secret

- Required: No

- Default value: false


### returnDeviceSecretFromAuthzEndpoint

- Description: 

- Required: No

- Default value: false


### rotateDeviceSecret

- Description: 

- Required: No

- Default value: false


### sectorIdentifierCacheLifetimeInMinutes

- Description: Sector Identifier cache lifetime in minutes

- Required: No

- Default value: 1440


### serverSessionIdLifetime

- Description: Dedicated property to control lifetime of the server side OP session object in seconds. Overrides sessionIdLifetime. By default value is 0, so object lifetime equals sessionIdLifetime (which sets both cookie and object expiration). It can be useful if goal is to keep different values for client cookie and server object

- Required: No

- Default value: None


### serviceDocumentation

- Description: URL of a page containing human-readable information that developers might want or need to know when using the OpenID Provider

- Required: No

- Default value: None


### sessionIdLifetime

- Description: The lifetime of session id in seconds. If 0 or -1 then expiration is not set. session_id cookie expires when browser session ends

- Required: No

- Default value: None


### sessionIdPersistInCache

- Description: Boolean value specifying whether to persist session_id in cache

- Required: No

- Default value: false


### sessionIdPersistOnPromptNone

- Description: Boolean value specifying whether to persist session ID on prompt none

- Required: No

- Default value: None


### sessionIdRequestParameterEnabled

- Description: Boolean value specifying whether to enable session_id HTTP request parameter

- Required: No

- Default value: false


### sessionIdUnauthenticatedUnusedLifetime

- Description: The lifetime for unused unauthenticated session states

- Required: No

- Default value: None


### sessionIdUnusedLifetime

- Description: The lifetime for unused session states

- Required: No

- Default value: None


### shareSubjectIdBetweenClientsWithSameSectorId

- Description: When true, clients with the same Sector ID also share the same Subject ID

- Required: No

- Default value: false


### skipAuthorizationForOpenIdScopeAndPairwiseId

- Description: Choose whether to skip authorization if a client has an OpenId scope and a pairwise ID

- Required: No

- Default value: false


### skipRefreshTokenDuringRefreshing

- Description: Boolean value specifying whether to skip refreshing tokens on refreshing

- Required: No

- Default value: false


### softwareStatementValidationClaimName

- Description: Validation claim name for software statement

- Required: No

- Default value: None


### softwareStatementValidationType

- Description: Validation type used for software statement

- Required: No

- Default value: None


### spontaneousScopeLifetime

- Description: The lifetime of spontaneous scope in seconds

- Required: No

- Default value: None


### ssaConfiguration

- Description: SSA Configuration

- Required: No

- Default value: None


### statAuthorizationScope

- Description: Scope required for Statistical Authorization

- Required: No

- Default value: None


### staticDecryptionKid

- Description: Specifies static decryption Kid

- Required: No

- Default value: None


### staticKid

- Description: Specifies static Kid

- Required: No

- Default value: None


### statTimerIntervalInSeconds

- Description: Statistical data capture time interval

- Required: No

- Default value: None


### subjectIdentifiersPerClientSupported

- Description: A list of the subject identifiers supported per client

- Required: No

- Default value: None


### subjectTypesSupported

- Description: This list details which Subject Identifier types that the OP supports. Valid types include pairwise and public.

- Required: No

- Default value: None


### tokenEndpoint

- Description: The token endpoint URL

- Required: No

- Default value: None


### tokenEndpointAuthMethodsSupported

- Description: A list of Client Authentication methods supported by this Token Endpoint

- Required: No

- Default value: None


### tokenEndpointAuthSigningAlgValuesSupported

- Description: A list of the JWS signing algorithms (alg values) supported by the Token Endpoint for the signature on the JWT used to authenticate the Client at the Token Endpoint for the private_key_jwt and client_secret_jwt authentication methods

- Required: No

- Default value: None


### tokenRevocationEndpoint

- Description: The URL for the access_token or refresh_token revocation endpoint

- Required: No

- Default value: None


### trustedClientEnabled

- Description: Boolean value specifying whether a client is trusted and no authorization is required

- Required: No

- Default value: None


### uiLocalesSupported

- Description: This list details the languages and scripts supported for the user interface

- Required: No

- Default value: None


### umaAddScopesAutomatically

- Description: Add UMA scopes automatically if it is not registered yet

- Required: No

- Default value: None


### umaConfigurationEndpoint

- Description: UMA Configuration endpoint URL

- Required: No

- Default value: None


### umaGrantAccessIfNoPolicies

- Description: Specify whether to grant access to resources if there is no any policies associated with scopes

- Required: No

- Default value: false


### umaPctLifetime

- Description: UMA PCT lifetime

- Required: No

- Default value: None


### umaResourceLifetime

- Description: UMA Resource lifetime

- Required: No

- Default value: None


### umaRestrictResourceToAssociatedClient

- Description: Restrict access to resource by associated client

- Required: No

- Default value: false


### umaRptAsJwt

- Description: Issue RPT as JWT or as random string

- Required: No

- Default value: false


### umaRptLifetime

- Description: UMA RPT lifetime

- Required: No

- Default value: None


### umaTicketLifetime

- Description: UMA ticket lifetime

- Required: No

- Default value: None


### umaValidateClaimToken

- Description: Validate claim_token as id_token assuming it is issued by local id

- Required: No

- Default value: false


### updateClientAccessTime

- Description: Choose if application should update oxLastAccessTime/oxLastLogonTime attributes upon client authentication

- Required: No

- Default value: None


### updateUserLastLogonTime

- Description: Choose if application should update oxLastLogonTime attribute upon user authentication

- Required: No

- Default value: None


### useHighestLevelScriptIfAcrScriptNotFound

- Description: Enable/Disable usage of highest level script in case ACR script does not exist

- Required: No

- Default value: true


### useLocalCache

- Description: Cache in local memory cache attributes, scopes, clients and organization entry with expiration 60 seconds

- Required: No

- Default value: false


### useNestedJwtDuringEncryption

- Description: Boolean value specifying whether to use nested Jwt during encryption

- Required: No

- Default value: true


### userInfoConfiguration

- Description: UserInfo Configuration

- Required: No

- Default value: None


### userInfoEncryptionAlgValuesSupported

- Description: This JSON Array lists which JWS encryption algorithms (alg values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT

- Required: No

- Default value: None


### userInfoEncryptionEncValuesSupported

- Description: This JSON Array lists which JWS encryption algorithms (enc values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT

- Required: No

- Default value: None


### userInfoEndpoint

- Description: The User Info endpoint URL

- Required: No

- Default value: None


### userInfoSigningAlgValuesSupported

- Description: This JSON Array lists which JWS signing algorithms (alg values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT

- Required: No

- Default value: None


### webKeysStorage

- Description: Web Key Storage Type

- Required: No

- Default value: None


