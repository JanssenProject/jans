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
| accessEvaluationAllowBasicClientAuthorization | Allow basic client authorization for access evaluation endpoint. | [Details](#accessevaluationallowbasicclientauthorization) |
| accessEvaluationDiscoveryCacheLifetimeInMinutes | Lifetime of access evaluation discovery cache (/.well-known/authzen-configuration). | [Details](#accessevaluationdiscoverycachelifetimeinminutes) |
| accessEvaluationScriptName | Access evaluation custom script name. | [Details](#accessevaluationscriptname) |
| accessTokenLifetime | The lifetime of the short lived Access Token | [Details](#accesstokenlifetime) |
| accessTokenSigningAlgValuesSupported | A list of the JWS signing algorithms (alg values) supported by the OP for the access token to encode the Claims in a JWT | [Details](#accesstokensigningalgvaluessupported) |
| acrMappings | The acr mappings. When AS meets key-value in map, it tries to replace 'key' with 'value' as very first thing and use that 'value' in further processing. | [Details](#acrmappings) |
| acrToAgamaConsentFlowMapping | The acr mapping to agama consent flow name. When AS meets acr it tries to match agama consent name and set it into session attributes under 'consent_flow' name. This makes it available for main Agama Consent script, so it knows which flow to invoke. | [Details](#acrtoagamaconsentflowmapping) |
| acrToConsentScriptNameMapping | The acr mapping to consent script name. When AS meets acr it tries to match consent script name and invoke it during authorization. This takes higher precedence then client consent script configuration. | [Details](#acrtoconsentscriptnamemapping) |
| activeSessionAuthorizationScope | Authorization Scope for active session | [Details](#activesessionauthorizationscope) |
| agamaConfiguration | Engine Config which offers an alternative way to build authentication flows in Janssen server | [Details](#agamaconfiguration) |
| allowAllValueForRevokeEndpoint | Boolean value true allow all value for revoke endpoint | [Details](#allowallvalueforrevokeendpoint) |
| allowBlankValuesInDiscoveryResponse | Boolean value specifying whether to allow blank values in discovery response | [Details](#allowblankvaluesindiscoveryresponse) |
| allowClientAssertionAudWithoutStrictIssuerMatch | Boolean value to indicate whether to allow client assertion 'aud' without strict server issuer match. Default value is false which means that server requires strict match. | [Details](#allowclientassertionaudwithoutstrictissuermatch) |
| allowEndSessionWithUnmatchedSid | default value false. If true, sid check will be skipped | [Details](#allowendsessionwithunmatchedsid) |
| allowIdTokenWithoutImplicitGrantType | Specifies if a token without implicit grant types is allowed | [Details](#allowidtokenwithoutimplicitgranttype) |
| allowPostLogoutRedirectWithoutValidation | Allows post-logout redirect without validation for the End Session endpoint (still AS validates it against clientWhiteList url pattern property) | [Details](#allowpostlogoutredirectwithoutvalidation) |
| allowRevokeForOtherClients | Boolean value true allows revoking of any token for any client. False value allows remove only tokens issued by client used at Revoke Endpoint | [Details](#allowrevokeforotherclients) |
| allowSpontaneousScopes | Specifies whether to allow spontaneous scopes | [Details](#allowspontaneousscopes) |
| applyXFrameOptionsHeaderIfUriContainsAny | Add X-Frame-Options header to response if any string in the list is contained by request uri. | [Details](#applyxframeoptionsheaderifuricontainsany) |
| archivedJwkLifetimeInSeconds | Archived JWK lifetime in seconds | [Details](#archivedjwklifetimeinseconds) |
| archivedJwksUri | URL of the OP's Archived JSON Web Key Set (JWK) document. This contains the signing key(s) the RP uses to validate signatures from the OP | [Details](#archivedjwksuri) |
| authenticationFilters | This list details filters for user authentication | [Details](#authenticationfilters) |
| authenticationFiltersEnabled | Boolean value specifying whether to enable user authentication filters | [Details](#authenticationfiltersenabled) |
| authenticationProtectionConfiguration | Authentication Brute Force Protection Configuration | [Details](#authenticationprotectionconfiguration) |
| authorizationChallengeDefaultAcr | Authorization Challenge Endpoint Default ACR if no value is specified in acr_values request parameter. | [Details](#authorizationchallengedefaultacr) |
| authorizationChallengeEndpoint | The authorization challenge endpoint URL | [Details](#authorizationchallengeendpoint) |
| authorizationChallengeSessionLifetimeInSeconds | Authorization challenge session lifetime in seconds | [Details](#authorizationchallengesessionlifetimeinseconds) |
| authorizationChallengeShouldGenerateSession | Boolean value specifying whether to generate session_id (AS object and cookie) during authorization at Authorization Challenge Endpoint | [Details](#authorizationchallengeshouldgeneratesession) |
| authorizationCodeLifetime | The lifetime of the Authorization Code | [Details](#authorizationcodelifetime) |
| authorizationEncryptionAlgValuesSupported | List of authorization encryption algorithms supported by this OP | [Details](#authorizationencryptionalgvaluessupported) |
| authorizationEncryptionEncValuesSupported | A list of the authorization encryption algorithms supported | [Details](#authorizationencryptionencvaluessupported) |
| authorizationEndpoint | The authorization endpoint URL | [Details](#authorizationendpoint) |
| authorizationRequestCustomAllowedParameters | This list details the allowed custom parameters for authorization requests | [Details](#authorizationrequestcustomallowedparameters) |
| authorizationResponseIssParameterSupported | Boolean value specifying whether the authorization server includes the iss parameter in authorization responses per RFC 9207. Default: false. | [Details](#authorizationresponseissparametersupported) |
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
| cimdBlockPrivateIp | Block private/internal IP ranges for CIMD (RFC 1918, loopback, link-local) | [Details](#cimdblockprivateip) |
| cimdConnectTimeoutMs | Connection timeout in milliseconds for CIMD fetch | [Details](#cimdconnecttimeoutms) |
| cimdDomainAllowlist | Allowed domains for CIMD client_id URLs | [Details](#cimddomainallowlist) |
| cimdDomainBlocklist | Blocked domains for CIMD client_id URLs | [Details](#cimddomainblocklist) |
| cimdMaxResponseSize | Maximum response size in bytes for CIMD fetch | [Details](#cimdmaxresponsesize) |
| cimdMaxTtlMinutes | Maximum TTL in minutes for persisted CIMD client metadata (upper bound, even if HTTP Cache-Control specifies longer) | [Details](#cimdmaxttlminutes) |
| cimdReadTimeoutMs | Read timeout in milliseconds for CIMD fetch | [Details](#cimdreadtimeoutms) |
| cimdSchemeAllowlist | Allowed URL schemes for CIMD client_id (default: https only) | [Details](#cimdschemeallowlist) |
| cimdTtlMinutes | Default TTL in minutes for persisted CIMD client metadata (also used as fallback when HTTP Cache-Control header is absent) | [Details](#cimdttlminutes) |
| claimsLocalesSupported | This list details the languages and scripts supported for values in the claims being returned | [Details](#claimslocalessupported) |
| claimsParameterSupported | Specifies whether the OP supports use of the claims parameter | [Details](#claimsparametersupported) |
| claimTypesSupported | A list of the Claim Types that the OpenID Provider supports | [Details](#claimtypessupported) |
| clientAuthenticationFilters | This list details filters for client authentication | [Details](#clientauthenticationfilters) |
| clientAuthenticationFiltersEnabled | Boolean value specifying whether to enable client authentication filters | [Details](#clientauthenticationfiltersenabled) |
| clientBlackList | This list specified which client redirection URIs are black-listed | [Details](#clientblacklist) |
| clientInfoEndpoint | The Client Info endpoint URL | [Details](#clientinfoendpoint) |
| clientPeriodicUpdateTimerInterval | Interval for client periodic update timer. Update timer is used to debounce frequent updates of the client to avoid performance degradation. | [Details](#clientperiodicupdatetimerinterval) |
| clientRegDefaultToCodeFlowWithRefresh | Boolean value specifying whether to add Authorization Code Flow with Refresh grant during client registratio | [Details](#clientregdefaulttocodeflowwithrefresh) |
| clientWhiteList | This list specifies which client redirection URIs are white-listed | [Details](#clientwhitelist) |
| configurationUpdateInterval | The interval for configuration update in seconds | [Details](#configurationupdateinterval) |
| connectionServiceConfiguration | Connection service Configuration | [Details](#connectionserviceconfiguration) |
| consentGatheringScriptBackwardCompatibility | Boolean value specifying whether to turn on Consent Gathering Script backward compatibility mode. If true AS will pick up script with higher level globally. If false (default) AS will pick up script based on client configuration | [Details](#consentgatheringscriptbackwardcompatibility) |
| cookieDomain | Sets cookie domain for all cookies created by OP | [Details](#cookiedomain) |
| corsConfigurationFilters | This list specifies the CORS configuration filters | [Details](#corsconfigurationfilters) |
| cssLocation | The location for CSS files | [Details](#csslocation) |
| customHeadersWithAuthorizationResponse | Choose whether to enable the custom response header parameter to return custom headers with the authorization response | [Details](#customheaderswithauthorizationresponse) |
| dateFormatterPatterns | List of key value date formatters, e.g. 'userinfo: 'yyyy-MM-dd', etc. | [Details](#dateformatterpatterns) |
| dcrAttestationEvidenceRequired | Boolean value indicating if DCR attestation evidence is required | [Details](#dcrattestationevidencerequired) |
| dcrAuthorizationWithClientCredentials | Boolean value indicating if DCR authorization to be performed using client credentials | [Details](#dcrauthorizationwithclientcredentials) |
| dcrAuthorizationWithMTLS | Boolean value indicating if DCR authorization allowed with MTLS | [Details](#dcrauthorizationwithmtls) |
| dcrForbidExpirationTimeInRequest | Boolean value specifying whether to allow to set client's expiration time in seconds during dynamic registration. | [Details](#dcrforbidexpirationtimeinrequest) |
| dcrSignatureValidationEnabled | Boolean value enables DCR signature validation. Default is false | [Details](#dcrsignaturevalidationenabled) |
| dcrSignatureValidationJwks | Specifies JWKS for all DCR's validations | [Details](#dcrsignaturevalidationjwks) |
| dcrSignatureValidationJwksUri | Specifies JWKS URI for all DCR's validations | [Details](#dcrsignaturevalidationjwksuri) |
| dcrSignatureValidationSharedSecret | Specifies shared secret for Dynamic Client Registration | [Details](#dcrsignaturevalidationsharedsecret) |
| dcrSignatureValidationSoftwareStatementJwksClaim | Specifies claim name inside software statement. Value of claim should point to inlined JWKS | [Details](#dcrsignaturevalidationsoftwarestatementjwksclaim) |
| dcrSignatureValidationSoftwareStatementJwksURIClaim | Specifies claim name inside software statement. Value of claim should point to JWKS URI | [Details](#dcrsignaturevalidationsoftwarestatementjwksuriclaim) |
| dcrSsaValidationConfigs | DCR SSA Validation configurations used to perform validation of SSA or DCR. Only needed if softwareStatementValidationType=builtin | [Details](#dcrssavalidationconfigs) |
| defaultSignatureAlgorithm | The default signature algorithm to sign ID Tokens | [Details](#defaultsignaturealgorithm) |
| defaultSubjectType | The default subject type used for dynamic client registration | [Details](#defaultsubjecttype) |
| deviceAuthzAcr | Device authz acr | [Details](#deviceauthzacr) |
| deviceAuthzEndpoint | URL for the Device Authorization | [Details](#deviceauthzendpoint) |
| deviceAuthzRequestExpiresIn | Expiration time given for device authorization requests | [Details](#deviceauthzrequestexpiresin) |
| deviceAuthzResponseTypeToProcessAuthz | Response type used to process device authz requests | [Details](#deviceauthzresponsetypetoprocessauthz) |
| deviceAuthzTokenPollInterval | Default interval returned to the client to process device token requests | [Details](#deviceauthztokenpollinterval) |
| disableAuthnForMaxAgeZero | Boolean value specifying whether to disable authentication when max_age=0 | [Details](#disableauthnformaxagezero) |
| disableExternalLoggerConfiguration | Choose whether to disable external log4j configuration override | [Details](#disableexternalloggerconfiguration) |
| disableJdkLogger | Choose whether to disable JDK loggers | [Details](#disablejdklogger) |
| disablePromptConsent | Boolean value specifying whether to disable prompt=consent | [Details](#disablepromptconsent) |
| disablePromptCreate | Disables prompt=create user registration functionality | [Details](#disablepromptcreate) |
| disablePromptLogin | Boolean value specifying whether to disable prompt=login | [Details](#disablepromptlogin) |
| disableU2fEndpoint | Choose whether to disable U2F endpoints | [Details](#disableu2fendpoint) |
| discoveryAllowedKeys | List of configuration response claim allowed to be displayed in discovery endpoint | [Details](#discoveryallowedkeys) |
| discoveryCacheLifetimeInMinutes | Lifetime of discovery cache | [Details](#discoverycachelifetimeinminutes) |
| discoveryDenyKeys | List of configuration response claims which must not be displayed in discovery endpoint response | [Details](#discoverydenykeys) |
| displayValuesSupported | A list of the display parameter values that the OpenID Provider supports | [Details](#displayvaluessupported) |
| dnName | DN of certificate issuer | [Details](#dnname) |
| dpopJktForceForAuthorizationCode | Force dpop_jkt presence and reject calls without it. | [Details](#dpopjktforceforauthorizationcode) |
| dpopJtiCacheTime | Demonstration of Proof-of-Possession (DPoP) cache time | [Details](#dpopjticachetime) |
| dpopNonceCacheTime | Demonstration of Proof-of-Possession (DPoP) nonce cache time | [Details](#dpopnoncecachetime) |
| dpopSigningAlgValuesSupported | Demonstration of Proof-of-Possession (DPoP) authorization signing algorithms supported | [Details](#dpopsigningalgvaluessupported) |
| dpopTimeframe | Demonstration of Proof-of-Possession (DPoP) timeout | [Details](#dpoptimeframe) |
| dpopUseNonce | Demonstration of Proof-of-Possession (DPoP) use nonce | [Details](#dpopusenonce) |
| dynamicRegistrationAllowedPasswordGrantScopes | List of grant scopes for dynamic registration | [Details](#dynamicregistrationallowedpasswordgrantscopes) |
| dynamicRegistrationCustomAttributes | This list details the custom attributes allowed for dynamic registration | [Details](#dynamicregistrationcustomattributes) |
| dynamicRegistrationCustomObjectClass | Custom object class for dynamic registration | [Details](#dynamicregistrationcustomobjectclass) |
| dynamicRegistrationDefaultCustomAttributes | This map provides default custom attributes with values for dynamic registration | [Details](#dynamicregistrationdefaultcustomattributes) |
| dynamicRegistrationExpirationTime | Expiration time in seconds for clients created with dynamic registration, 0 or -1 means never expire | [Details](#dynamicregistrationexpirationtime) |
| dynamicRegistrationPasswordGrantTypeEnabled | Boolean value specifying whether to enable Password Grant Type during Dynamic Registration | [Details](#dynamicregistrationpasswordgranttypeenabled) |
| dynamicRegistrationPersistClientAuthorizations | Boolean value specifying whether to persist client authorizations | [Details](#dynamicregistrationpersistclientauthorizations) |
| dynamicRegistrationScopesParamEnabled | Boolean value specifying whether to enable scopes parameter in dynamic registration | [Details](#dynamicregistrationscopesparamenabled) |
| enableClientGrantTypeUpdate | Choose if client can update Grant Type values | [Details](#enableclientgranttypeupdate) |
| enabledOAuthAuditLogging | enable OAuth Audit Logging | [Details](#enabledoauthauditlogging) |
| enableTokenMessages | Enable Publish messages on access token issue/revoke | [Details](#enabletokenmessages) |
| endSessionEndpoint | URL at the OP to which an RP can perform a redirect to request that the end user be logged out at the OP | [Details](#endsessionendpoint) |
| endSessionWithAccessToken | Choose whether to accept access tokens to call end_session endpoint | [Details](#endsessionwithaccesstoken) |
| errorHandlingMethod | A list of possible error handling methods. Possible values: remote (send error back to RP), internal (show error page) | [Details](#errorhandlingmethod) |
| errorReasonEnabled | Boolean value specifying whether to return detailed reason of the error from AS. Default value is false | [Details](#errorreasonenabled) |
| expirationNotificatorEnabled | Boolean value specifying whether expiration notificator is enabled (used to identify expiration for persistence that support TTL, like Couchbase) | [Details](#expirationnotificatorenabled) |
| expirationNotificatorIntervalInSeconds | The expiration notificator interval in second | [Details](#expirationnotificatorintervalinseconds) |
| expirationNotificatorMapSizeLimit | The expiration notificator maximum size limit | [Details](#expirationnotificatormapsizelimit) |
| externalLoggerConfiguration | The path to the external log4j2 logging configuration | [Details](#externalloggerconfiguration) |
| externalUriWhiteList | This list specifies which external URIs can be called by AS (if empty any URI can be called) | [Details](#externaluriwhitelist) |
| fapiCompatibility | Boolean value specifying whether to turn on FAPI compatibility mode. If true AS behaves in more strict mode | [Details](#fapicompatibility) |
| featureFlags | List of enabled feature flags | [Details](#featureflags) |
| forceIdTokenHintPresence | Boolean value specifying whether force id_token_hint parameter presence | [Details](#forceidtokenhintpresence) |
| forceOfflineAccessScopeToEnableRefreshToken | Boolean value specifying whether force offline_access scope to enable refresh_token grant type. Default value is true | [Details](#forceofflineaccessscopetoenablerefreshtoken) |
| forceRopcInAuthorizationEndpoint | Specifies whether to force ROPC custom script for Authorization Endpoint. | [Details](#forceropcinauthorizationendpoint) |
| forceSignedRequestObject | Boolean value true indicates that signed request object is mandatory | [Details](#forcesignedrequestobject) |
| frontChannelLogoutSessionSupported | Choose whether to support front channel session logout | [Details](#frontchannellogoutsessionsupported) |
| grantTypesAndResponseTypesAutofixEnabled | Boolean value specifying whether to Grant types and Response types can be auto fixed | [Details](#granttypesandresponsetypesautofixenabled) |
| grantTypesSupported | This list details which OAuth 2.0 grant types are supported by this OP | [Details](#granttypessupported) |
| grantTypesSupportedByDynamicRegistration | This list details which OAuth 2.0 grant types can be set up with the dynamic client registration API | [Details](#granttypessupportedbydynamicregistration) |
| httpLoggingEnabled | Enable/disable request/response logging filter | [Details](#httploggingenabled) |
| httpLoggingExcludePaths | This list details the base URIs for which the request/response logging filter will not record activity | [Details](#httploggingexcludepaths) |
| httpLoggingResponseBodyContent | Defines if Response body will be logged. Default value is false | [Details](#httploggingresponsebodycontent) |
| idGenerationEndpoint | ID Generation endpoint URL | [Details](#idgenerationendpoint) |
| idJagIssueRefreshToken | Whether to issue refresh tokens after accepting an ID-JAG (Resource AS role). Spec recommends false. | [Details](#idjagissuerefreshtoken) |
| idJagLifetime | Lifetime in seconds for ID-JAGs issued by this AS (IdP role). | [Details](#idjaglifetime) |
| idJagTrustedIdpIssuers | Trusted IdP issuers whose ID-JAGs this AS will accept (Resource AS role). Map keyed by IdP issuer URI. | [Details](#idjagtrustedidpissuers) |
| idTokenEncryptionAlgValuesSupported | A list of the JWE encryption algorithms (alg values) supported by the OP for the ID Token to encode the Claims in a JWT | [Details](#idtokenencryptionalgvaluessupported) |
| idTokenEncryptionEncValuesSupported | A list of the JWE encryption algorithms (enc values) supported by the OP for the ID Token to encode the Claims in a JWT | [Details](#idtokenencryptionencvaluessupported) |
| idTokenFilterClaimsBasedOnAccessToken | Boolean value specifying whether idToken filters claims based on accessToken | [Details](#idtokenfilterclaimsbasedonaccesstoken) |
| idTokenLifetime | The lifetime of the ID Token | [Details](#idtokenlifetime) |
| idTokenSigningAlgValuesSupported | A list of the JWS signing algorithms (alg values) supported by the OP for the ID Token to encode the Claims in a JWT | [Details](#idtokensigningalgvaluessupported) |
| idTokenTokenBindingCnfValuesSupported | Array containing a list of the JWT Confirmation Method member names supported by the OP for Token Binding of ID Tokens. The presence of this parameter indicates that the OpenID Provider supports Token Binding of ID Tokens. If omitted, the default is that the OpenID Provider does not support Token Binding of ID Tokens | [Details](#idtokentokenbindingcnfvaluessupported) |
| imgLocation | The location for image files | [Details](#imglocation) |
| includeRefreshTokenLifetimeInTokenResponse | Boolean value specifying whether to include refresh token lifetime in token response | [Details](#includerefreshtokenlifetimeintokenresponse) |
| includeRequestedClaimsInIdToken | Boolean value to indicate whether to include requested claims in id_token (specified by 'claims' parameter at Authorization Endpoint). Default value is false to put minimize claims in token (for security). | [Details](#includerequestedclaimsinidtoken) |
| includeSidInResponse | Boolean value specifying whether to include sessionId in response | [Details](#includesidinresponse) |
| introspectionAccessTokenMustHaveIntrospectionScope | If True, rejects introspection requests if access_token does not have the 'introspection' scope in its authorization header. Comparing to 'uma_protection', 'introspection' scope is not allowed for dynamic registration' | [Details](#introspectionaccesstokenmusthaveintrospectionscope) |
| introspectionAccessTokenMustHaveUmaProtectionScope | If True, rejects introspection requests if access_token does not have the uma_protection scope in its authorization header | [Details](#introspectionaccesstokenmusthaveumaprotectionscope) |
| introspectionEncryptionAlgValuesSupported | This JSON Array lists which JWS encryption algorithms (alg values) [JWA] can be used by for the Introspection endpoint to encode the claims in a JWT | [Details](#introspectionencryptionalgvaluessupported) |
| introspectionEncryptionEncValuesSupported | This JSON Array lists which JWS encryption algorithms (enc values) [JWA] can be used by for the Introspection endpoint to encode the claims in a JWT | [Details](#introspectionencryptionencvaluessupported) |
| introspectionEndpoint | Introspection endpoint URL | [Details](#introspectionendpoint) |
| introspectionResponseScopesBackwardCompatibility | Boolean value specifying introspection response backward compatibility mode | [Details](#introspectionresponsescopesbackwardcompatibility) |
| introspectionRestrictBasicAuthnToOwnTokens | If True, allow client request only own tokens. Otherwise allow to introspect all tokens. | [Details](#introspectionrestrictbasicauthntoowntokens) |
| introspectionScriptBackwardCompatibility | Boolean value specifying whether switch off client's introspection scripts (true value) and run all scripts that exists on server. Default value is false | [Details](#introspectionscriptbackwardcompatibility) |
| introspectionSigningAlgValuesSupported | This JSON Array lists which JWS signing algorithms (alg values) [JWA] can be used by for the Introspection endpoint to encode the claims in a JWT | [Details](#introspectionsigningalgvaluessupported) |
| introspectionSkipAuthorization | Specifies if authorization to be skipped for introspection | [Details](#introspectionskipauthorization) |
| invalidateSessionCookiesAfterAuthorizationFlow | Boolean value to specify whether to invalidate session_id and consent_session_id cookies right after successful or unsuccessful authorization | [Details](#invalidatesessioncookiesafterauthorizationflow) |
| issuer | URL using the https scheme that OP asserts as Issuer identifier | [Details](#issuer) |
| jansId | URL for the Inum generator Service | [Details](#jansid) |
| jansOpenIdConnectVersion | OpenID Connect Version | [Details](#jansopenidconnectversion) |
| jmsBrokerURISet | JMS Broker URI Set | [Details](#jmsbrokeruriset) |
| jmsPassword | JMS Password | [Details](#jmspassword) |
| jmsUserName | JMS UserName | [Details](#jmsusername) |
| jsLocation | The location for JavaScript files | [Details](#jslocation) |
| jwksAlgorithmsSupported | A list of algorithms that will be used in JWKS endpoint | [Details](#jwksalgorithmssupported) |
| jwksUri | URL of the OP's JSON Web Key Set (JWK) document. This contains the signing key(s) the RP uses to validate signatures from the OP | [Details](#jwksuri) |
| jwtGrantAllowUserByUidInAssertion | Boolean value to indicate whether to allow user identification by uid claim from assertion at Token Endpoint | [Details](#jwtgrantallowuserbyuidinassertion) |
| keepAuthenticatorAttributesOnAcrChange | Boolean value specifying whether to keep authenticator attributes on ACR change | [Details](#keepauthenticatorattributesonacrchange) |
| keyAlgsAllowedForGeneration | List of algorithm allowed to be used for key generation | [Details](#keyalgsallowedforgeneration) |
| keyRegenerationEnabled | Boolean value specifying whether to regenerate keys | [Details](#keyregenerationenabled) |
| keyRegenerationInterval | The interval for key regeneration in hours | [Details](#keyregenerationinterval) |
| keySelectionStrategy | Key Selection Strategy : OLDER, NEWER, FIRST | [Details](#keyselectionstrategy) |
| keySignWithSameKeyButDiffAlg | Specifies if signing to be done with same key but apply different algorithms | [Details](#keysignwithsamekeybutdiffalg) |
| keyStoreFile | The Key Store File (JKS) | [Details](#keystorefile) |
| keyStoreSecret | The Key Store password | [Details](#keystoresecret) |
| legacyIdTokenClaims | Choose whether to include claims in ID tokens | [Details](#legacyidtokenclaims) |
| lockMessageConfig | Lock message Pub configuration | [Details](#lockmessageconfig) |
| logClientIdOnClientAuthentication | Choose if application should log the Client ID on client authentication | [Details](#logclientidonclientauthentication) |
| logClientNameOnClientAuthentication | Choose if application should log the Client Name on client authentication | [Details](#logclientnameonclientauthentication) |
| loggingLayout | Logging layout used for Jans Authorization Server loggers | [Details](#logginglayout) |
| loggingLevel | Specify the logging level of loggers | [Details](#logginglevel) |
| logNotFoundEntityAsError | Boolean value specifying whether to log not_found entity exception as error or as trace. Default value is false (trace). | [Details](#lognotfoundentityaserror) |
| logoutStatusJwtLifetime | The lifetime of Logout Status JWT. If not set falls back to 1 day | [Details](#logoutstatusjwtlifetime) |
| logoutStatusJwtSigningAlgValuesSupported | This JSON Array lists which JWS signing algorithms (alg values) [JWA] can be used by for the Logout Status JWT at Authorization Endpoint to encode the claims in a JWT | [Details](#logoutstatusjwtsigningalgvaluessupported) |
| maxPerRoute | Set the maximum number of concurrent connections per route | [Details](#maxperroute) |
| maxTotal | Set the maximum number of total open connections | [Details](#maxtotal) |
| metricReporterInterval | The interval for metric reporter in seconds | [Details](#metricreporterinterval) |
| metricReporterKeepDataDays | The days to keep metric reported data | [Details](#metricreporterkeepdatadays) |
| mtlsAuthorizationChallengeEndpoint | URL for Mutual TLS (mTLS) Client Authentication and Certificate-Bound Access Tokens (MTLS) Authorization Challenge Endpoint | [Details](#mtlsauthorizationchallengeendpoint) |
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
| openidSubAttribute | Specifies which attribute is used for the subject identifier claim | [Details](#openidsubattribute) |
| opPolicyUri | URL that the OpenID Provider provides to the person registering the Client to read about the OP's requirements on how the Relying Party can use the data provided by the OP | [Details](#oppolicyuri) |
| opTosUri | URL that the OpenID Provider provides to the person registering the Client to read about OpenID Provider's terms of service | [Details](#optosuri) |
| pairwiseCalculationKey | Key to calculate algorithmic pairwise IDs | [Details](#pairwisecalculationkey) |
| pairwiseCalculationSalt | Salt to calculate algorithmic pairwise IDs | [Details](#pairwisecalculationsalt) |
| pairwiseIdType | the pairwise ID type | [Details](#pairwiseidtype) |
| parEndpoint | URL for Pushed Authorisation Request (PAR) Endpoint | [Details](#parendpoint) |
| parForbidPublicClient | Boolean value to indicate whether public client is allowed for Pushed Authorisation Request(PAR) | [Details](#parforbidpublicclient) |
| persistIdToken | Specifies whether to persist id_token (otherwise saves into cache) | [Details](#persistidtoken) |
| persistRefreshToken | Specifies whether to persist refresh_token (otherwise saves into cache) | [Details](#persistrefreshtoken) |
| personCustomObjectClassList | This list details custom object classes for dynamic person enrollment | [Details](#personcustomobjectclasslist) |
| publicSubjectIdentifierPerClientEnabled | Specifies whether public subject identifier is allowed per client | [Details](#publicsubjectidentifierperclientenabled) |
| rateLimitConfiguration | Rate Limit Configuration | [Details](#ratelimitconfiguration) |
| redirectUrisRegexEnabled | Enable/Disable redirect uris validation using regular expression | [Details](#redirecturisregexenabled) |
| refreshTokenExtendLifetimeOnRotation | Boolean value specifying whether to extend refresh tokens on rotation | [Details](#refreshtokenextendlifetimeonrotation) |
| refreshTokenLifetime | The lifetime of the Refresh Token | [Details](#refreshtokenlifetime) |
| registrationEndpoint | Registration endpoint URL | [Details](#registrationendpoint) |
| rejectEndSessionIfIdTokenExpired | default value false. If true and id_token is not found in db, request is rejected | [Details](#rejectendsessionifidtokenexpired) |
| rejectJwtWithNoneAlg | Boolean value specifying whether reject JWT requested or validated with algorithm None. Default value is true | [Details](#rejectjwtwithnonealg) |
| removeRefreshTokensForClientOnLogout | Boolean value specifying whether to remove Refresh Tokens on logout. Default value is true | [Details](#removerefreshtokensforclientonlogout) |
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
| rotateClientRegistrationAccessTokenOnUsage | Boolean value specifying whether to rotate client registration access token after each usage | [Details](#rotateclientregistrationaccesstokenonusage) |
| rotateDeviceSecret |  | [Details](#rotatedevicesecret) |
| runAllUpdateTokenScripts | Boolean value specifying whether to run all Update Token scripts | [Details](#runallupdatetokenscripts) |
| saveTokensInCache | Boolean value specifying whether to save access_token, id_token and refresh_token in cache (with cacheKey=sha256Hex(token_code)) | [Details](#savetokensincache) |
| saveTokensInCacheAndDontSaveInPersistence | Boolean value specifying whether to save access_token, id_token and refresh_token in cache and skip persistence in DB at the same time (with cacheKey=sha256Hex(token_code)) | [Details](#savetokensincacheanddontsaveinpersistence) |
| sectorIdentifierCacheLifetimeInMinutes | Sector Identifier cache lifetime in minutes | [Details](#sectoridentifiercachelifetimeinminutes) |
| serviceDocumentation | URL of a page containing human-readable information that developers might want or need to know when using the OpenID Provider | [Details](#servicedocumentation) |
| sessionAuthnTimeCheckDuringPromptLoginThresholdMs | Integer value that allows to specify session authentication time threshold in milliseconds when client is configured from prompt login (has property defaultPromptLogin=true). For high-latency environments, consider increasing this value to 2000-5000ms. | [Details](#sessionauthntimecheckduringpromptloginthresholdms) |
| sessionIdCookieLifetime | The lifetime of session_id cookie in seconds. If 0 or -1 then expiration is not set. session_id cookie expires when browser session ends | [Details](#sessionidcookielifetime) |
| sessionIdLifetime | The lifetime of session_id server object in seconds. If not set falls back to session_id cookie expiration set by 'sessionIdCookieLifetime' configuration property | [Details](#sessionidlifetime) |
| sessionIdPersistInCache | Boolean value specifying whether to persist session_id in cache | [Details](#sessionidpersistincache) |
| sessionIdPersistOnPromptNone | Boolean value specifying whether to persist session ID on prompt none | [Details](#sessionidpersistonpromptnone) |
| sessionIdRequestParameterEnabled | Boolean value specifying whether to enable session_id HTTP request parameter | [Details](#sessionidrequestparameterenabled) |
| sessionIdUnauthenticatedUnusedLifetime | The lifetime for unused unauthenticated session states | [Details](#sessionidunauthenticatedunusedlifetime) |
| sessionIdUnusedLifetime | The lifetime for unused session states | [Details](#sessionidunusedlifetime) |
| sessionIdUserClaimsInAttributes | Defines list of user claims that has to be put in session attributes | [Details](#sessioniduserclaimsinattributes) |
| shareSubjectIdBetweenClientsWithSameSectorId | When true, clients with the same Sector ID also share the same Subject ID | [Details](#sharesubjectidbetweenclientswithsamesectorid) |
| skipAuthenticationFilterOptionsMethod | Force Authentication Filtker to process OPTIONS request | [Details](#skipauthenticationfilteroptionsmethod) |
| skipAuthorizationForOpenIdScopeAndPairwiseId | Choose whether to skip authorization if a client has an OpenId scope and a pairwise ID | [Details](#skipauthorizationforopenidscopeandpairwiseid) |
| skipRefreshTokenDuringRefreshing | Boolean value specifying whether to skip refreshing tokens on refreshing | [Details](#skiprefreshtokenduringrefreshing) |
| skipSessionAuthnTimeCheckDuringPromptLogin | Boolean value true allows to skip session authentication time check when client is configured from prompt login (has property defaultPromptLogin=true) | [Details](#skipsessionauthntimecheckduringpromptlogin) |
| softwareStatementValidationClaimName | Validation claim name for software statement | [Details](#softwarestatementvalidationclaimname) |
| softwareStatementValidationType | Validation type used for software statement | [Details](#softwarestatementvalidationtype) |
| spontaneousScopeLifetime | The lifetime of spontaneous scope in seconds | [Details](#spontaneousscopelifetime) |
| ssaConfiguration | SSA Configuration | [Details](#ssaconfiguration) |
| statAuthorizationScope | Scope required for Statistical Authorization | [Details](#statauthorizationscope) |
| staticDecryptionKid | Specifies static decryption Kid | [Details](#staticdecryptionkid) |
| staticKid | Specifies static Kid | [Details](#statickid) |
| statTimerIntervalInSeconds | Statistical data capture time interval | [Details](#stattimerintervalinseconds) |
| statusListBitSize | Specifies status list bit size. (2 bits - 4 statuses, 4 bits - 16 statuses). Defaults to 2. | [Details](#statuslistbitsize) |
| statusListIndexAllocationBlockSize | Specifies how many status list indexes AS can reserve at once within pool (when status_list feature flag is enabled). Defaults to 100. | [Details](#statuslistindexallocationblocksize) |
| statusListResponseJwtLifetime | The status list response JWT lifetime (used to set exp claim in JWT). | [Details](#statuslistresponsejwtlifetime) |
| statusListResponseJwtSignatureAlgorithm | The status list signature algorithm to sign response JWT. Defaults to RS256. | [Details](#statuslistresponsejwtsignaturealgorithm) |
| subjectIdentifiersPerClientSupported | A list of the subject identifiers supported per client | [Details](#subjectidentifiersperclientsupported) |
| subjectTypesSupported | This list details which Subject Identifier types that the OP supports. Valid types include pairwise and public. | [Details](#subjecttypessupported) |
| tokenEndpoint | The token endpoint URL | [Details](#tokenendpoint) |
| tokenEndpointAuthMethodsSupported | A list of Client Authentication methods supported by this Token Endpoint | [Details](#tokenendpointauthmethodssupported) |
| tokenEndpointAuthSigningAlgValuesSupported | A list of the JWS signing algorithms (alg values) supported by the Token Endpoint for the signature on the JWT used to authenticate the Client at the Token Endpoint for the private_key_jwt and client_secret_jwt authentication methods | [Details](#tokenendpointauthsigningalgvaluessupported) |
| tokenMessagesChannel | Channel for token messages | [Details](#tokenmessageschannel) |
| tokenRevocationEndpoint | The URL for the access_token or refresh_token revocation endpoint | [Details](#tokenrevocationendpoint) |
| trustedClientEnabled | Boolean value specifying whether a client is trusted and no authorization is required | [Details](#trustedclientenabled) |
| trustedSsaIssuers | List of trusted SSA issuers with configuration (e.g. automatically granted scopes). | [Details](#trustedssaissuers) |
| txTokenEncryptionAlgValuesSupported | This JSON Array lists which JWS encryption algorithms (alg values) [JWA] can be used by for the Transaction Tokens at Token Endpoint to encode the claims in a JWT | [Details](#txtokenencryptionalgvaluessupported) |
| txTokenEncryptionEncValuesSupported | This JSON Array lists which JWS encryption algorithms (enc values) [JWA] can be used by for the Transaction Tokens at Token Endpoint to encode the claims in a JWT | [Details](#txtokenencryptionencvaluessupported) |
| txTokenLifetime | The lifetime of the Transaction Token | [Details](#txtokenlifetime) |
| txTokenSigningAlgValuesSupported | This JSON Array lists which JWS signing algorithms (alg values) [JWA] can be used by for the Transaction Tokens at Token Endpoint to encode the claims in a JWT | [Details](#txtokensigningalgvaluessupported) |
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
| uppercaseResponseKeysInAccountAccessConsent | Boolean value to indicate whether to uppercase keys returns from /open-banking/v3.1/aisp/account-access-consents endpoint | [Details](#uppercaseresponsekeysinaccountaccessconsent) |
| useHighestLevelScriptIfAcrScriptNotFound | Enable/Disable usage of highest level script in case ACR script does not exist | [Details](#usehighestlevelscriptifacrscriptnotfound) |
| useLocalCache | Cache in local memory cache attributes, scopes, clients and organization entry with expiration 60 seconds | [Details](#uselocalcache) |
| useNestedJwtDuringEncryption | Boolean value specifying whether to use nested Jwt during encryption | [Details](#usenestedjwtduringencryption) |
| useOpenidSubAttributeValueForPairwiseLocalAccountId | Use openidSubAttribute value of user as local account id for algorithmic pairwise look up | [Details](#useopenidsubattributevalueforpairwiselocalaccountid) |
| userInfoEncryptionAlgValuesSupported | This JSON Array lists which JWS encryption algorithms (alg values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT | [Details](#userinfoencryptionalgvaluessupported) |
| userInfoEncryptionEncValuesSupported | This JSON Array lists which JWS encryption algorithms (enc values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT | [Details](#userinfoencryptionencvaluessupported) |
| userInfoEndpoint | The User Info endpoint URL | [Details](#userinfoendpoint) |
| userInfoLifetime | The lifetime of the User Info | [Details](#userinfolifetime) |
| userInfoSigningAlgValuesSupported | This JSON Array lists which JWS signing algorithms (alg values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT | [Details](#userinfosigningalgvaluessupported) |
| validateAfterInactivity | Defines period of inactivity in milliseconds after which persistent connections must be re-validated prior to being leased to the consumer | [Details](#validateafterinactivity) |
| webKeysStorage | Web Key Storage Type | [Details](#webkeysstorage) |
| xframeOptionsHeaderValue | Add X-Frame-Options header to response if any string in the list is contained by request uri. | [Details](#xframeoptionsheadervalue) |


### accessEvaluationAllowBasicClientAuthorization

- Description: Allow basic client authorization for access evaluation endpoint.

- Required: No

- Default value: false


### accessEvaluationDiscoveryCacheLifetimeInMinutes

- Description: Lifetime of access evaluation discovery cache (/.well-known/authzen-configuration).

- Required: No

- Default value: 5


### accessEvaluationScriptName

- Description: Access evaluation custom script name.

- Required: No

- Default value: None


### accessTokenLifetime

- Description: The lifetime of the short lived Access Token

- Required: No

- Default value: None


### accessTokenSigningAlgValuesSupported

- Description: A list of the JWS signing algorithms (alg values) supported by the OP for the access token to encode the Claims in a JWT

- Required: No

- Default value: None


### acrMappings

- Description: The acr mappings. When AS meets key-value in map, it tries to replace 'key' with 'value' as very first thing and use that 'value' in further processing.

- Required: No

- Default value: None


### acrToAgamaConsentFlowMapping

- Description: The acr mapping to agama consent flow name. When AS meets acr it tries to match agama consent name and set it into session attributes under 'consent_flow' name. This makes it available for main Agama Consent script, so it knows which flow to invoke.

- Required: No

- Default value: None


### acrToConsentScriptNameMapping

- Description: The acr mapping to consent script name. When AS meets acr it tries to match consent script name and invoke it during authorization. This takes higher precedence then client consent script configuration.

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


### allowBlankValuesInDiscoveryResponse

- Description: Boolean value specifying whether to allow blank values in discovery response

- Required: No

- Default value: false


### allowClientAssertionAudWithoutStrictIssuerMatch

- Description: Boolean value to indicate whether to allow client assertion 'aud' without strict server issuer match. Default value is false which means that server requires strict match.

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


### allowRevokeForOtherClients

- Description: Boolean value true allows revoking of any token for any client. False value allows remove only tokens issued by client used at Revoke Endpoint

- Required: No

- Default value: false


### allowSpontaneousScopes

- Description: Specifies whether to allow spontaneous scopes

- Required: No

- Default value: None


### applyXFrameOptionsHeaderIfUriContainsAny

- Description: Add X-Frame-Options header to response if any string in the list is contained by request uri.

- Required: No

- Default value: None


### archivedJwkLifetimeInSeconds

- Description: Archived JWK lifetime in seconds

- Required: No

- Default value: None


### archivedJwksUri

- Description: URL of the OP's Archived JSON Web Key Set (JWK) document. This contains the signing key(s) the RP uses to validate signatures from the OP

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


### authorizationChallengeDefaultAcr

- Description: Authorization Challenge Endpoint Default ACR if no value is specified in acr_values request parameter.

- Required: No

- Default value: default_challenge


### authorizationChallengeEndpoint

- Description: The authorization challenge endpoint URL

- Required: No

- Default value: None


### authorizationChallengeSessionLifetimeInSeconds

- Description: Authorization challenge session lifetime in seconds

- Required: No

- Default value: None


### authorizationChallengeShouldGenerateSession

- Description: Boolean value specifying whether to generate session_id (AS object and cookie) during authorization at Authorization Challenge Endpoint

- Required: No

- Default value: false


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


### authorizationResponseIssParameterSupported

- Description: Boolean value specifying whether the authorization server includes the iss parameter in authorization responses per RFC 9207. Default: false.

- Required: No

- Default value: false


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


### cimdBlockPrivateIp

- Description: Block private/internal IP ranges for CIMD (RFC 1918, loopback, link-local)

- Required: No

- Default value: true


### cimdConnectTimeoutMs

- Description: Connection timeout in milliseconds for CIMD fetch

- Required: No

- Default value: 5000


### cimdDomainAllowlist

- Description: Allowed domains for CIMD client_id URLs

- Required: No

- Default value: None


### cimdDomainBlocklist

- Description: Blocked domains for CIMD client_id URLs

- Required: No

- Default value: None


### cimdMaxResponseSize

- Description: Maximum response size in bytes for CIMD fetch

- Required: No

- Default value: 65536


### cimdMaxTtlMinutes

- Description: Maximum TTL in minutes for persisted CIMD client metadata (upper bound, even if HTTP Cache-Control specifies longer)

- Required: No

- Default value: 1440


### cimdReadTimeoutMs

- Description: Read timeout in milliseconds for CIMD fetch

- Required: No

- Default value: 10000


### cimdSchemeAllowlist

- Description: Allowed URL schemes for CIMD client_id (default: https only)

- Required: No

- Default value: None


### cimdTtlMinutes

- Description: Default TTL in minutes for persisted CIMD client metadata (also used as fallback when HTTP Cache-Control header is absent)

- Required: No

- Default value: 60


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


### clientPeriodicUpdateTimerInterval

- Description: Interval for client periodic update timer. Update timer is used to debounce frequent updates of the client to avoid performance degradation.

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


### connectionServiceConfiguration

- Description: Connection service Configuration

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


### dateFormatterPatterns

- Description: List of key value date formatters, e.g. 'userinfo: 'yyyy-MM-dd', etc.

- Required: No

- Default value: None


### dcrAttestationEvidenceRequired

- Description: Boolean value indicating if DCR attestation evidence is required

- Required: No

- Default value: false


### dcrAuthorizationWithClientCredentials

- Description: Boolean value indicating if DCR authorization to be performed using client credentials

- Required: No

- Default value: false


### dcrAuthorizationWithMTLS

- Description: Boolean value indicating if DCR authorization allowed with MTLS

- Required: No

- Default value: false


### dcrForbidExpirationTimeInRequest

- Description: Boolean value specifying whether to allow to set client's expiration time in seconds during dynamic registration.

- Required: No

- Default value: false


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

- Description: DCR SSA Validation configurations used to perform validation of SSA or DCR. Only needed if softwareStatementValidationType=builtin

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


### deviceAuthzAcr

- Description: Device authz acr

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


### disableExternalLoggerConfiguration

- Description: Choose whether to disable external log4j configuration override

- Required: No

- Default value: true


### disableJdkLogger

- Description: Choose whether to disable JDK loggers

- Required: No

- Default value: true


### disablePromptConsent

- Description: Boolean value specifying whether to disable prompt=consent

- Required: No

- Default value: false


### disablePromptCreate

- Description: Disables prompt=create user registration functionality

- Required: No

- Default value: None


### disablePromptLogin

- Description: Boolean value specifying whether to disable prompt=login

- Required: No

- Default value: false


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


### dpopJktForceForAuthorizationCode

- Description: Force dpop_jkt presence and reject calls without it.

- Required: No

- Default value: false


### dpopJtiCacheTime

- Description: Demonstration of Proof-of-Possession (DPoP) cache time

- Required: No

- Default value: 3600


### dpopNonceCacheTime

- Description: Demonstration of Proof-of-Possession (DPoP) nonce cache time

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


### dpopUseNonce

- Description: Demonstration of Proof-of-Possession (DPoP) use nonce

- Required: No

- Default value: false


### dynamicRegistrationAllowedPasswordGrantScopes

- Description: List of grant scopes for dynamic registration

- Required: No

- Default value: None


### dynamicRegistrationCustomAttributes

- Description: This list details the custom attributes allowed for dynamic registration

- Required: No

- Default value: None


### dynamicRegistrationCustomObjectClass

- Description: Custom object class for dynamic registration

- Required: No

- Default value: None


### dynamicRegistrationDefaultCustomAttributes

- Description: This map provides default custom attributes with values for dynamic registration

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


### enableTokenMessages

- Description: Enable Publish messages on access token issue/revoke

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

- Description: A list of possible error handling methods. Possible values: remote (send error back to RP), internal (show error page)

- Required: No

- Default value: remote


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


### forceIdTokenHintPresence

- Description: Boolean value specifying whether force id_token_hint parameter presence

- Required: No

- Default value: false


### forceOfflineAccessScopeToEnableRefreshToken

- Description: Boolean value specifying whether force offline_access scope to enable refresh_token grant type. Default value is true

- Required: No

- Default value: true


### forceRopcInAuthorizationEndpoint

- Description: Specifies whether to force ROPC custom script for Authorization Endpoint.

- Required: No

- Default value: false


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


### grantTypesSupportedByDynamicRegistration

- Description: This list details which OAuth 2.0 grant types can be set up with the dynamic client registration API

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


### httpLoggingResponseBodyContent

- Description: Defines if Response body will be logged. Default value is false

- Required: No

- Default value: false


### idGenerationEndpoint

- Description: ID Generation endpoint URL

- Required: No

- Default value: None


### idJagIssueRefreshToken

- Description: Whether to issue refresh tokens after accepting an ID-JAG (Resource AS role). Spec recommends false.

- Required: No

- Default value: false


### idJagLifetime

- Description: Lifetime in seconds for ID-JAGs issued by this AS (IdP role).

- Required: No

- Default value: 300


### idJagTrustedIdpIssuers

- Description: Trusted IdP issuers whose ID-JAGs this AS will accept (Resource AS role). Map keyed by IdP issuer URI.

- Required: No

- Default value: empty


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


### includeRefreshTokenLifetimeInTokenResponse

- Description: Boolean value specifying whether to include refresh token lifetime in token response

- Required: No

- Default value: false


### includeRequestedClaimsInIdToken

- Description: Boolean value to indicate whether to include requested claims in id_token (specified by 'claims' parameter at Authorization Endpoint). Default value is false to put minimize claims in token (for security).

- Required: No

- Default value: None


### includeSidInResponse

- Description: Boolean value specifying whether to include sessionId in response

- Required: No

- Default value: false


### introspectionAccessTokenMustHaveIntrospectionScope

- Description: If True, rejects introspection requests if access_token does not have the 'introspection' scope in its authorization header. Comparing to 'uma_protection', 'introspection' scope is not allowed for dynamic registration'

- Required: No

- Default value: false


### introspectionAccessTokenMustHaveUmaProtectionScope

- Description: If True, rejects introspection requests if access_token does not have the uma_protection scope in its authorization header

- Required: No

- Default value: false


### introspectionEncryptionAlgValuesSupported

- Description: This JSON Array lists which JWS encryption algorithms (alg values) [JWA] can be used by for the Introspection endpoint to encode the claims in a JWT

- Required: No

- Default value: None


### introspectionEncryptionEncValuesSupported

- Description: This JSON Array lists which JWS encryption algorithms (enc values) [JWA] can be used by for the Introspection endpoint to encode the claims in a JWT

- Required: No

- Default value: None


### introspectionEndpoint

- Description: Introspection endpoint URL

- Required: No

- Default value: None


### introspectionResponseScopesBackwardCompatibility

- Description: Boolean value specifying introspection response backward compatibility mode

- Required: No

- Default value: false


### introspectionRestrictBasicAuthnToOwnTokens

- Description: If True, allow client request only own tokens. Otherwise allow to introspect all tokens.

- Required: No

- Default value: false


### introspectionScriptBackwardCompatibility

- Description: Boolean value specifying whether switch off client's introspection scripts (true value) and run all scripts that exists on server. Default value is false

- Required: No

- Default value: false


### introspectionSigningAlgValuesSupported

- Description: This JSON Array lists which JWS signing algorithms (alg values) [JWA] can be used by for the Introspection endpoint to encode the claims in a JWT

- Required: No

- Default value: None


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


### jwtGrantAllowUserByUidInAssertion

- Description: Boolean value to indicate whether to allow user identification by uid claim from assertion at Token Endpoint

- Required: No

- Default value: false


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


### lockMessageConfig

- Description: Lock message Pub configuration

- Required: No

- Default value: false


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

- Description: Specify the logging level of loggers

- Required: No

- Default value: None


### logNotFoundEntityAsError

- Description: Boolean value specifying whether to log not_found entity exception as error or as trace. Default value is false (trace).

- Required: No

- Default value: None


### logoutStatusJwtLifetime

- Description: The lifetime of Logout Status JWT. If not set falls back to 1 day

- Required: No

- Default value: 86400


### logoutStatusJwtSigningAlgValuesSupported

- Description: This JSON Array lists which JWS signing algorithms (alg values) [JWA] can be used by for the Logout Status JWT at Authorization Endpoint to encode the claims in a JWT

- Required: No

- Default value: None


### maxPerRoute

- Description: Set the maximum number of concurrent connections per route

- Required: No

- Default value: 50


### maxTotal

- Description: Set the maximum number of total open connections

- Required: No

- Default value: 200


### metricReporterInterval

- Description: The interval for metric reporter in seconds

- Required: No

- Default value: None


### metricReporterKeepDataDays

- Description: The days to keep metric reported data

- Required: No

- Default value: None


### mtlsAuthorizationChallengeEndpoint

- Description: URL for Mutual TLS (mTLS) Client Authentication and Certificate-Bound Access Tokens (MTLS) Authorization Challenge Endpoint

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

- Description: Specifies which attribute is used for the subject identifier claim

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


### parForbidPublicClient

- Description: Boolean value to indicate whether public client is allowed for Pushed Authorisation Request(PAR)

- Required: No

- Default value: false


### persistIdToken

- Description: Specifies whether to persist id_token (otherwise saves into cache)

- Required: No

- Default value: false


### persistRefreshToken

- Description: Specifies whether to persist refresh_token (otherwise saves into cache)

- Required: No

- Default value: true


### personCustomObjectClassList

- Description: This list details custom object classes for dynamic person enrollment

- Required: No

- Default value: None


### publicSubjectIdentifierPerClientEnabled

- Description: Specifies whether public subject identifier is allowed per client

- Required: No

- Default value: false


### rateLimitConfiguration

- Description: Rate Limit Configuration

- Required: No

- Default value: None


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

- Description: Boolean value specifying whether to remove Refresh Tokens on logout. Default value is true

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


### rotateClientRegistrationAccessTokenOnUsage

- Description: Boolean value specifying whether to rotate client registration access token after each usage

- Required: No

- Default value: false


### rotateDeviceSecret

- Description: 

- Required: No

- Default value: false


### runAllUpdateTokenScripts

- Description: Boolean value specifying whether to run all Update Token scripts

- Required: No

- Default value: false


### saveTokensInCache

- Description: Boolean value specifying whether to save access_token, id_token and refresh_token in cache (with cacheKey=sha256Hex(token_code))

- Required: No

- Default value: None


### saveTokensInCacheAndDontSaveInPersistence

- Description: Boolean value specifying whether to save access_token, id_token and refresh_token in cache and skip persistence in DB at the same time (with cacheKey=sha256Hex(token_code))

- Required: No

- Default value: None


### sectorIdentifierCacheLifetimeInMinutes

- Description: Sector Identifier cache lifetime in minutes

- Required: No

- Default value: 1440


### serviceDocumentation

- Description: URL of a page containing human-readable information that developers might want or need to know when using the OpenID Provider

- Required: No

- Default value: None


### sessionAuthnTimeCheckDuringPromptLoginThresholdMs

- Description: Integer value that allows to specify session authentication time threshold in milliseconds when client is configured from prompt login (has property defaultPromptLogin=true). For high-latency environments, consider increasing this value to 2000-5000ms.

- Required: No

- Default value: None


### sessionIdCookieLifetime

- Description: The lifetime of session_id cookie in seconds. If 0 or -1 then expiration is not set. session_id cookie expires when browser session ends

- Required: No

- Default value: 86400


### sessionIdLifetime

- Description: The lifetime of session_id server object in seconds. If not set falls back to session_id cookie expiration set by 'sessionIdCookieLifetime' configuration property

- Required: No

- Default value: 86400


### sessionIdPersistInCache

- Description: Boolean value specifying whether to persist session_id in cache

- Required: No

- Default value: false


### sessionIdPersistOnPromptNone

- Description: Boolean value specifying whether to persist session ID on prompt none

- Required: No

- Default value: false


### sessionIdRequestParameterEnabled

- Description: Boolean value specifying whether to enable session_id HTTP request parameter

- Required: No

- Default value: false


### sessionIdUnauthenticatedUnusedLifetime

- Description: The lifetime for unused unauthenticated session states

- Required: No

- Default value: 7200


### sessionIdUnusedLifetime

- Description: The lifetime for unused session states

- Required: No

- Default value: None


### sessionIdUserClaimsInAttributes

- Description: Defines list of user claims that has to be put in session attributes

- Required: No

- Default value: None


### shareSubjectIdBetweenClientsWithSameSectorId

- Description: When true, clients with the same Sector ID also share the same Subject ID

- Required: No

- Default value: false


### skipAuthenticationFilterOptionsMethod

- Description: Force Authentication Filtker to process OPTIONS request

- Required: No

- Default value: true


### skipAuthorizationForOpenIdScopeAndPairwiseId

- Description: Choose whether to skip authorization if a client has an OpenId scope and a pairwise ID

- Required: No

- Default value: false


### skipRefreshTokenDuringRefreshing

- Description: Boolean value specifying whether to skip refreshing tokens on refreshing

- Required: No

- Default value: false


### skipSessionAuthnTimeCheckDuringPromptLogin

- Description: Boolean value true allows to skip session authentication time check when client is configured from prompt login (has property defaultPromptLogin=true)

- Required: No

- Default value: None


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


### statusListBitSize

- Description: Specifies status list bit size. (2 bits - 4 statuses, 4 bits - 16 statuses). Defaults to 2.

- Required: No

- Default value: None


### statusListIndexAllocationBlockSize

- Description: Specifies how many status list indexes AS can reserve at once within pool (when status_list feature flag is enabled). Defaults to 100.

- Required: No

- Default value: None


### statusListResponseJwtLifetime

- Description: The status list response JWT lifetime (used to set exp claim in JWT).

- Required: No

- Default value: None


### statusListResponseJwtSignatureAlgorithm

- Description: The status list signature algorithm to sign response JWT. Defaults to RS256.

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


### tokenMessagesChannel

- Description: Channel for token messages

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


### trustedSsaIssuers

- Description: List of trusted SSA issuers with configuration (e.g. automatically granted scopes).

- Required: No

- Default value: None


### txTokenEncryptionAlgValuesSupported

- Description: This JSON Array lists which JWS encryption algorithms (alg values) [JWA] can be used by for the Transaction Tokens at Token Endpoint to encode the claims in a JWT

- Required: No

- Default value: None


### txTokenEncryptionEncValuesSupported

- Description: This JSON Array lists which JWS encryption algorithms (enc values) [JWA] can be used by for the Transaction Tokens at Token Endpoint to encode the claims in a JWT

- Required: No

- Default value: None


### txTokenLifetime

- Description: The lifetime of the Transaction Token

- Required: No

- Default value: None


### txTokenSigningAlgValuesSupported

- Description: This JSON Array lists which JWS signing algorithms (alg values) [JWA] can be used by for the Transaction Tokens at Token Endpoint to encode the claims in a JWT

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


### uppercaseResponseKeysInAccountAccessConsent

- Description: Boolean value to indicate whether to uppercase keys returns from /open-banking/v3.1/aisp/account-access-consents endpoint

- Required: No

- Default value: false


### useHighestLevelScriptIfAcrScriptNotFound

- Description: Enable/Disable usage of highest level script in case ACR script does not exist

- Required: No

- Default value: false


### useLocalCache

- Description: Cache in local memory cache attributes, scopes, clients and organization entry with expiration 60 seconds

- Required: No

- Default value: false


### useNestedJwtDuringEncryption

- Description: Boolean value specifying whether to use nested Jwt during encryption

- Required: No

- Default value: true


### useOpenidSubAttributeValueForPairwiseLocalAccountId

- Description: Use openidSubAttribute value of user as local account id for algorithmic pairwise look up

- Required: No

- Default value: false


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


### userInfoLifetime

- Description: The lifetime of the User Info

- Required: No

- Default value: 3600


### userInfoSigningAlgValuesSupported

- Description: This JSON Array lists which JWS signing algorithms (alg values) [JWA] can be used by for the UserInfo endpoint to encode the claims in a JWT

- Required: No

- Default value: None


### validateAfterInactivity

- Description: Defines period of inactivity in milliseconds after which persistent connections must be re-validated prior to being leased to the consumer

- Required: No

- Default value: 2000


### webKeysStorage

- Description: Web Key Storage Type

- Required: No

- Default value: None


### xframeOptionsHeaderValue

- Description: Add X-Frame-Options header to response if any string in the list is contained by request uri.

- Required: No

- Default value: SAMEORIGIN


