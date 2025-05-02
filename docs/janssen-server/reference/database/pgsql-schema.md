---
tags:
  - administration
  - database
  - PostgreSQL
  - Indexes
---

# PostgreSQL Schema

## Tables
| Table names                |
| -------------------------- |
| jansPairwiseIdentifier     |
| jansPerson                 |
| jansOrganization           |
| jansSsa                    |
| jansAppConf                |
| jansClnt                   |
| jansScope                  |
| jansUmaResource            |
| jansUmaResourcePermission  |
| jansGrant                  |
| jansToken                  |
| jansGrp                    |
| jansAttr                   |
| jansPassResetReq           |
| jansSessId                 |
| jansUmaRPT                 |
| jansPushApp                |
| jansScr                    |
| jansCustomScr              |
| jansDeviceRegistration     |
| jansU2fReq                 |
| jansMetric                 |
| jansUmaPCT                 |
| jansCache                  |
| jansFido2RegistrationEntry |
| jansPushDevice             |
| jansClntAuthz              |
| jansSectorIdentifier       |
| jansFido2AuthnEntry        |
| jansRp                     |
| jansCibaReq                |
| jansStatEntry              |
| jansPar                    |
| jansInumMap                |
| agmFlowRun                 |
| agmFlow                    |
| adsPrjDeployment           |
| jansDocument               |

### jansPairwiseIdentifier
| Field                | Type              | Character Maximum Length | Null | Default | Comment                |
| -------------------- | ----------------- | ------------------------ | ---- | ------- | ---------------------- |
| jansId               | character varying | 128                      | YES  | None    | Identifier             |
| jansSectorIdentifier | character varying | 64                       | YES  | None    | jans Sector Identifier |
| jansClntId           | jsonb             | None                     | YES  | None    | jans Clnt id           |
| jansUsrId            | character varying | 64                       | YES  | None    | jans user id           |

### jansPerson
| Field                                | Type                        | Character Maximum Length | Null | Default | Comment                                                                                                  |
| ------------------------------------ | --------------------------- | ------------------------ | ---- | ------- | -------------------------------------------------------------------------------------------------------- |
| jansAssociatedClnt                   | jsonb                       | None                     | YES  | None    | Associate the dn of an OAuth2 client with a person or UMA Resource Set.                                  |
| displayName                          | character varying           | 128                      | YES  | None    | preferred name of a person to be used when displaying entries                                            |
| jansManagedOrganizations             | character varying           | 64                       | YES  | None    | Used to track with which organizations a person is associated                                            |
| jansOptOuts                          | jsonb                       | None                     | YES  | None    | White pages attributes restricted by person in exclude profile management                                |
| jansStatus                           | character varying           | 16                       | YES  | None    | Status of the entry, used by many objectclasses                                                          |
| inum                                 | character varying           | 64                       | YES  | None    | XRI i-number                                                                                             |
| jansPersistentJWT                    | jsonb                       | None                     | YES  | None    | jans Persistent JWT                                                                                      |
| jansCreationTimestamp                | timestamp without time zone | None                     | YES  | None    | Registration time                                                                                        |
| jansOTPCache                         | jsonb                       | None                     | YES  | None    | Stores a used OTP to prevent a hacker from using it again. Complementary to jansExtUid attribute         |
| jansLastLogonTime                    | timestamp without time zone | None                     | YES  | None    | Last logon time                                                                                          |
| jansPassExpDate                      | timestamp without time zone | None                     | YES  | None    | Pass Exp date, represented as an ISO 8601 (YYYY-MM-DD) format                                            |
| persistentId                         | character varying           | 64                       | YES  | None    | PersistentId                                                                                             |
| middleName                           | character varying           | 64                       | YES  | None    | Middle name(s)                                                                                           |
| nickname                             | character varying           | 64                       | YES  | None    | Casual name of the End-Usr                                                                               |
| jansPrefUsrName                      | character varying           | 64                       | YES  | None    | Shorthand Name                                                                                           |
| profile                              | character varying           | 64                       | YES  | None    | Profile page URL of the person                                                                           |
| picture                              | text                        | None                     | YES  | None    | Profile picture URL of the person                                                                        |
| website                              | character varying           | 64                       | YES  | None    | Web page or blog URL of the person                                                                       |
| emailVerified                        | boolean                     | None                     | YES  | None    | True if the e-mail address of the person has been verified; otherwise false                              |
| gender                               | character varying           | 32                       | YES  | None    | Gender of the person either female or male                                                               |
| birthdate                            | timestamp without time zone | None                     | YES  | None    | Birthday of the person, represented as an ISO 8601:2004 [ISO8601-2004] YYYY-MM-DD format                 |
| zoneinfo                             | character varying           | 64                       | YES  | None    | Time zone database representing the End-Usrs time zone. For example, Europe/Paris or America/Los_Angeles |
| locale                               | character varying           | 64                       | YES  | None    | Locale of the person, represented as a BCP47 [RFC5646] language tag                                      |
| phoneNumberVerified                  | boolean                     | None                     | YES  | None    | True if the phone number of the person has been verified, otherwise false                                |
| address                              | text                        | None                     | YES  | None    | OpenID Connect formatted JSON object representing the address of the person                              |
| updatedAt                            | timestamp without time zone | None                     | YES  | None    | Time the information of the person was last updated. Seconds from 1970-01-01T0:0:0Z                      |
| preferredLanguage                    | character varying           | 64                       | YES  | None    | preferred written or spoken language for a person                                                        |
| role                                 | jsonb                       | None                     | YES  | None    | Role                                                                                                     |
| secretAnswer                         | text                        | None                     | YES  | None    | Secret Answer                                                                                            |
| secretQuestion                       | text                        | None                     | YES  | None    | Secret Question                                                                                          |
| transientId                          | character varying           | 64                       | YES  | None    | TransientId                                                                                              |
| jansCountInvalidLogin                | character varying           | 64                       | YES  | None    | Invalid login attempts count                                                                             |
| jansEnrollmentCode                   | character varying           | 64                       | YES  | None    | jansEnrollmentCode                                                                                       |
| jansIMAPData                         | character varying           | 64                       | YES  | None    | This data has information about your imap connection                                                     |
| jansPPID                             | jsonb                       | None                     | YES  | None    | Persistent Pairwise ID for OpenID Connect                                                                |
| jansGuid                             | character varying           | 64                       | YES  | None    | A random string to mark temporary tokens                                                                 |
| jansPreferredMethod                  | character varying           | 64                       | YES  | None    | Casa - Preferred method to use for user authentication                                                   |
| jansOTPDevices                       | character varying           | 512                      | YES  | None    | Casa - Json representation of OTP devices. Complementary to jansExtUid attribute                         |
| jansMobileDevices                    | character varying           | 512                      | YES  | None    | Casa - Json representation of mobile devices. Complementary to mobile attribute                          |
| jansTrustedDevices                   | text                        | None                     | YES  | None    | Casa - Devices with which strong authentication may be skipped                                           |
| jansStrongAuthPolicy                 | character varying           | 64                       | YES  | None    | Casa - 2FA Enforcement Policy for User                                                                   |
| jansUnlinkedExternalUids             | character varying           | 64                       | YES  | None    | Casa - List of unlinked social accounts (ie disabled jansExtUids)                                        |
| jansBackchannelDeviceRegistrationTkn | character varying           | 64                       | YES  | None    | jans Backchannel Device Registration Tkn                                                                 |
| jansBackchannelUsrCode               | character varying           | 64                       | YES  | None    | jans Backchannel Usr Code                                                                                |
| carLicense                           | character varying           | 64                       | YES  | None    | vehicle license or registration plate                                                                    |
| departmentNumber                     | character varying           | 64                       | YES  | None    | identifies a department within an organization                                                           |
| employeeType                         | character varying           | 64                       | YES  | None    | type of employment for a person                                                                          |
| employeeNumber                       | character varying           | 64                       | YES  | None    | numerically identifies an employee within an organization                                                |
| jansAdminUIRole                      | jsonb                       | None                     | YES  | None    | jansAdminUIRole                                                                                          |

### jansOrganization
| Field                 | Type                        | Character Maximum Length | Null | Default | Comment                                                                                             |
| --------------------- | --------------------------- | ------------------------ | ---- | ------- | --------------------------------------------------------------------------------------------------- |
| displayName           | character varying           | 128                      | YES  | None    | preferred name of a person to be used when displaying entries                                       |
| jansCustomMessage     | character varying           | 128                      | YES  | None    | exclude custom welcome message                                                                      |
| jansFaviconImage      | character varying           | 64                       | YES  | None    | TODO - Stores URL of favicon                                                                        |
| jansLogoImage         | character varying           | 64                       | YES  | None    | Logo used by exclude for default look and feel.                                                     |
| jansManager           | character varying           | 64                       | YES  | None    | Used to specify if a person has the manager role                                                    |
| jansManagerGrp        | text                        | None                     | YES  | None    | Used in organizatoin entry to specifies the dn of the group that  has admin priviledges in exclude. |
| jansOrgShortName      | character varying           | 64                       | YES  | None    | Short description, as few letters as possible, no spaces.                                           |
| jansThemeColor        | character varying           | 64                       | YES  | None    | exclude login page configuration                                                                    |
| inum                  | character varying           | 64                       | YES  | None    | XRI i-number                                                                                        |
| jansCreationTimestamp | timestamp without time zone | None                     | YES  | None    | Registration time                                                                                   |
| jansRegistrationConf  | character varying           | 64                       | YES  | None    | Registration Conf                                                                                   |
| jansLogoPath          | character varying           | 64                       | YES  | None    | jansLogoPath                                                                                        |
| jansStatus            | character varying           | 16                       | YES  | None    | Status of the entry, used by many objectclasses                                                     |
| jansFaviconPath       | character varying           | 64                       | YES  | None    | jansFaviconPath                                                                                     |

### jansSsa
| Field        | Type                        | Character Maximum Length | Null | Default | Comment                                        |
| ------------ | --------------------------- | ------------------------ | ---- | ------- | ---------------------------------------------- |
| inum         | character varying           | 64                       | YES  | None    | XRI i-number                                   |
| jansAttrs    | text                        | None                     | YES  | None    | Attrs                                          |
| exp          | timestamp without time zone | None                     | YES  | None    | jans Exp                                       |
| del          | boolean                     | None                     | YES  | None    | del                                            |
| jansState    | character varying           | 64                       | YES  | None    | jansState                                      |
| creatorId    | character varying           | 64                       | YES  | None    | Creator Id                                     |
| creatorTyp   | character varying           | 64                       | YES  | None    | Creator type                                   |
| creationDate | timestamp without time zone | None                     | YES  | None    | Creation Date used for password reset requests |

### jansAppConf
| Field                 | Type                        | Character Maximum Length | Null | Default | Comment                                                                      |
| --------------------- | --------------------------- | ------------------------ | ---- | ------- | ---------------------------------------------------------------------------- |
| displayName           | character varying           | 128                      | YES  | None    | preferred name of a person to be used when displaying entries                |
| jansHostname          | character varying           | 64                       | YES  | None    | The hostname of the Jans Server instance                                     |
| jansLastUpd           | timestamp without time zone | None                     | YES  | None    | Monitors last time the server was able to connect to  the monitoring system. |
| jansManager           | character varying           | 64                       | YES  | None    | Used to specify if a person has the manager role                             |
| jansOrgProfileMgt     | boolean                     | None                     | YES  | None    | enable or disable profile management feature in exclude                      |
| jansScimEnabled       | boolean                     | None                     | YES  | None    | exclude SCIM feature - enabled or disabled                                   |
| jansSmtpConf          | jsonb                       | None                     | YES  | None    | SMTP configuration                                                           |
| jansSslExpiry         | character varying           | 64                       | YES  | None    | SAML Trust Relationship configuration                                        |
| jansStatus            | character varying           | 16                       | YES  | None    | Status of the entry, used by many objectclasses                              |
| jansUrl               | character varying           | 64                       | YES  | None    | Jans instance URL                                                            |
| inum                  | character varying           | 64                       | YES  | None    | XRI i-number                                                                 |
| jansDbAuth            | jsonb                       | None                     | YES  | None    | Custom IDP authentication configuration                                      |
| jansLogViewerConfig   | character varying           | 64                       | YES  | None    | Log viewer configuration                                                     |
| jansLogConfigLocation | character varying           | 64                       | YES  | None    | Path to external log4j2.xml                                                  |
| jansCacheConf         | text                        | None                     | YES  | None    | Cache configuration                                                          |
| jansDocStoreConf      | text                        | None                     | YES  | None    | jansDocStoreConf                                                             |
| jansConfDyn           | text                        | None                     | YES  | None    | jans Dyn Conf                                                                |
| jansConfErrors        | text                        | None                     | YES  | None    | jans Errors Conf                                                             |
| jansConfStatic        | text                        | None                     | YES  | None    | jans Static Conf                                                             |
| jansConfWebKeys       | text                        | None                     | YES  | None    | jans Web Keys Conf                                                           |
| jansWebKeysSettings   | character varying           | 64                       | YES  | None    | jans Web Keys Conf                                                           |
| jansConfApp           | text                        | None                     | YES  | None    | jans App Conf                                                                |
| jansRevision          | integer                     | None                     | YES  | None    | Revision                                                                     |

### jansClnt
| Field                                   | Type                        | Character Maximum Length | Null | Default | Comment                                                       |
| --------------------------------------- | --------------------------- | ------------------------ | ---- | ------- | ------------------------------------------------------------- |
| jansGrp                                 | character varying           | 64                       | YES  | None    | Group                                                         |
| displayName                             | character varying           | 128                      | YES  | None    | preferred name of a person to be used when displaying entries |
| displayNameLocalized                    | jsonb                       | None                     | YES  | None    | jans Display Name Localized                                   |
| inum                                    | character varying           | 64                       | YES  | None    | XRI i-number                                                  |
| jansAppTyp                              | character varying           | 64                       | YES  | None    | jans App Typ                                                  |
| jansClntIdIssuedAt                      | timestamp without time zone | None                     | YES  | None    | jans Clnt Issued At                                           |
| jansClntSecret                          | character varying           | 64                       | YES  | None    | jans Clnt Secret                                              |
| jansClntSecretExpAt                     | timestamp without time zone | None                     | YES  | None    | Date client expires                                           |
| exp                                     | timestamp without time zone | None                     | YES  | None    | jans Exp                                                      |
| del                                     | boolean                     | None                     | YES  | None    | del                                                           |
| jansClntURI                             | text                        | None                     | YES  | None    | jans Clnt URI                                                 |
| jansClntURILocalized                    | jsonb                       | None                     | YES  | None    | jans Clnt URI localized                                       |
| jansContact                             | jsonb                       | None                     | YES  | None    | jans Contact                                                  |
| jansDefAcrValues                        | jsonb                       | None                     | YES  | None    | jans Def Acr Values                                           |
| jansDefMaxAge                           | integer                     | None                     | YES  | None    | jans Def Max Age                                              |
| jansGrantTyp                            | jsonb                       | None                     | YES  | None    | jans Grant Typ                                                |
| jansIdTknEncRespAlg                     | character varying           | 64                       | YES  | None    | jans ID Tkn Enc Resp Alg                                      |
| jansIdTknEncRespEnc                     | character varying           | 64                       | YES  | None    | jans ID Tkn Enc Resp Enc                                      |
| jansIdTknSignedRespAlg                  | character varying           | 64                       | YES  | None    | jans ID Tkn Signed Resp Alg                                   |
| jansInitiateLoginURI                    | text                        | None                     | YES  | None    | jans Initiate Login URI                                       |
| jansJwksURI                             | text                        | None                     | YES  | None    | jans JWKs URI                                                 |
| jansJwks                                | text                        | None                     | YES  | None    | jans JWKs                                                     |
| jansLogoURI                             | text                        | None                     | YES  | None    | jans Logo URI                                                 |
| jansLogoURILocalized                    | jsonb                       | None                     | YES  | None    | jans Logo URI localized                                       |
| jansPolicyURI                           | text                        | None                     | YES  | None    | jans Policy URI                                               |
| jansPolicyURILocalized                  | jsonb                       | None                     | YES  | None    | jans Policy URI localized                                     |
| jansPostLogoutRedirectURI               | jsonb                       | None                     | YES  | None    | jans Post Logout Redirect URI                                 |
| jansRedirectURI                         | jsonb                       | None                     | YES  | None    | jans Redirect URI                                             |
| jansRegistrationAccessTkn               | character varying           | 64                       | YES  | None    | jans Registration Access Tkn                                  |
| jansReqObjSigAlg                        | character varying           | 64                       | YES  | None    | jans Req Obj Sig Alg                                          |
| jansReqObjEncAlg                        | character varying           | 64                       | YES  | None    | jans Req Obj Enc Alg                                          |
| jansReqObjEncEnc                        | character varying           | 64                       | YES  | None    | jans Req Obj Enc Enc                                          |
| jansReqURI                              | jsonb                       | None                     | YES  | None    | jans Req URI                                                  |
| jansRespTyp                             | jsonb                       | None                     | YES  | None    | jans Resp Typ                                                 |
| jansScope                               | jsonb                       | None                     | YES  | None    | jans Attr Scope                                               |
| jansClaim                               | jsonb                       | None                     | YES  | None    | jans Attr Claim                                               |
| jansSectorIdentifierURI                 | text                        | None                     | YES  | None    | jans Sector Identifier URI                                    |
| jansSignedRespAlg                       | character varying           | 64                       | YES  | None    | jans Signed Resp Alg                                          |
| jansSubjectTyp                          | character varying           | 64                       | YES  | None    | jans Subject Typ                                              |
| jansTknEndpointAuthMethod               | character varying           | 64                       | YES  | None    | jans Tkn Endpoint Auth Method                                 |
| jansTknEndpointAuthSigAlg               | character varying           | 64                       | YES  | None    | jans Tkn Endpoint Auth Sig Alg                                |
| jansTosURI                              | text                        | None                     | YES  | None    | jans TOS URI                                                  |
| jansTosURILocalized                     | jsonb                       | None                     | YES  | None    | jans Tos URI localized                                        |
| jansTrustedClnt                         | boolean                     | None                     | YES  | None    | jans Trusted Clnt                                             |
| jansUsrInfEncRespAlg                    | character varying           | 64                       | YES  | None    | jans Usr Inf Enc Resp Alg                                     |
| jansUsrInfEncRespEnc                    | character varying           | 64                       | YES  | None    | jans Usr Inf Enc Resp Enc                                     |
| jansExtraConf                           | character varying           | 64                       | YES  | None    | jans additional configuration                                 |
| jansClaimRedirectURI                    | jsonb                       | None                     | YES  | None    | Claim Redirect URI                                            |
| jansLastAccessTime                      | timestamp without time zone | None                     | YES  | None    | Last access time                                              |
| jansLastLogonTime                       | timestamp without time zone | None                     | YES  | None    | Last logon time                                               |
| jansPersistClntAuthzs                   | boolean                     | None                     | YES  | None    | jans Persist Clnt Authzs                                      |
| jansInclClaimsInIdTkn                   | boolean                     | None                     | YES  | None    | jans Incl Claims In Id Tkn                                    |
| jansRefreshTknLife                      | integer                     | None                     | YES  | None    | Life of refresh token                                         |
| jansDisabled                            | boolean                     | None                     | YES  | None    | Status of client                                              |
| jansLogoutURI                           | jsonb                       | None                     | YES  | None    | jans Policy URI                                               |
| jansLogoutSessRequired                  | boolean                     | None                     | YES  | None    | jans Policy URI                                               |
| jansdId                                 | character varying           | 64                       | YES  | None    | jansd Id                                                      |
| jansAuthorizedOrigins                   | jsonb                       | None                     | YES  | None    | jans Authorized Origins                                       |
| tknBndCnf                               | text                        | None                     | YES  | None    | jansauth - Tkn Binding Id Hash                                |
| jansAccessTknAsJwt                      | boolean                     | None                     | YES  | None    | jansauth - indicator whether to return access token as JWT    |
| jansAccessTknSigAlg                     | character varying           | 64                       | YES  | None    | jansauth - access token signing algorithm                     |
| jansAccessTknLife                       | integer                     | None                     | YES  | None    | Life of access token                                          |
| jansSoftId                              | character varying           | 64                       | YES  | None    | Soft Identifier                                               |
| jansSoftStatement                       | text                        | None                     | YES  | None    | Soft Statement                                                |
| jansRptAsJwt                            | boolean                     | None                     | YES  | None    | jansRptAsJwt                                                  |
| jansAttrs                               | text                        | None                     | YES  | None    | Attrs                                                         |
| jansBackchannelTknDeliveryMode          | character varying           | 64                       | YES  | None    | jans Backchannel Tkn Delivery Mode                            |
| jansBackchannelClntNotificationEndpoint | character varying           | 64                       | YES  | None    | jans Backchannel Clnt Notification Endpoint                   |
| jansBackchannelAuthnReqSigAlg           | character varying           | 64                       | YES  | None    | jans Backchannel Authn Req Sig Alg                            |
| jansBackchannelUsrCodeParameter         | boolean                     | None                     | YES  | None    | jans Backchannel Usr Code Parameter                           |

### jansScope
| Field              | Type                        | Character Maximum Length | Null | Default | Comment                                                       |
| ------------------ | --------------------------- | ------------------------ | ---- | ------- | ------------------------------------------------------------- |
| jansDefScope       | boolean                     | None                     | YES  | None    | Track the default scope for an custom OAuth2 Scope.           |
| displayName        | character varying           | 128                      | YES  | None    | preferred name of a person to be used when displaying entries |
| inum               | character varying           | 64                       | YES  | None    | XRI i-number                                                  |
| jansScopeTyp       | character varying           | 64                       | YES  | None    | OX Attr Scope type                                            |
| creatorId          | character varying           | 64                       | YES  | None    | Creator Id                                                    |
| creatorTyp         | character varying           | 64                       | YES  | None    | Creator type                                                  |
| creatorAttrs       | character varying           | 64                       | YES  | None    | Creator Attrs                                                 |
| creationDate       | timestamp without time zone | None                     | YES  | None    | Creation Date used for password reset requests                |
| jansClaim          | jsonb                       | None                     | YES  | None    | jans Attr Claim                                               |
| jansScrDn          | jsonb                       | None                     | YES  | None    | Script object DN                                              |
| jansGrpClaims      | boolean                     | None                     | YES  | None    | jans Grp Attr Claims (true or false)                          |
| jansId             | character varying           | 128                      | YES  | None    | Identifier                                                    |
| jansIconUrl        | character varying           | 64                       | YES  | None    | jans icon url                                                 |
| jansUmaPolicyScrDn | text                        | None                     | YES  | None    | OX policy script Dn                                           |
| jansAttrs          | text                        | None                     | YES  | None    | Attrs                                                         |
| exp                | timestamp without time zone | None                     | YES  | None    | jans Exp                                                      |
| del                | boolean                     | None                     | YES  | None    | del                                                           |

### jansUmaResource
| Field               | Type                        | Character Maximum Length | Null | Default | Comment                                                                 |
| ------------------- | --------------------------- | ------------------------ | ---- | ------- | ----------------------------------------------------------------------- |
| displayName         | character varying           | 128                      | YES  | None    | preferred name of a person to be used when displaying entries           |
| inum                | character varying           | 64                       | YES  | None    | XRI i-number                                                            |
| jansAssociatedClnt  | jsonb                       | None                     | YES  | None    | Associate the dn of an OAuth2 client with a person or UMA Resource Set. |
| jansUmaScope        | character varying           | 768                      | YES  | None    | URI reference of scope descriptor                                       |
| jansFaviconImage    | character varying           | 64                       | YES  | None    | TODO - Stores URL of favicon                                            |
| jansGrp             | character varying           | 64                       | YES  | None    | Group                                                                   |
| jansId              | character varying           | 128                      | YES  | None    | Identifier                                                              |
| jansResource        | text                        | None                     | YES  | None    | Host path                                                               |
| jansRevision        | integer                     | None                     | YES  | None    | Revision                                                                |
| jansTyp             | character varying           | 64                       | YES  | None    | jans type                                                               |
| jansScopeExpression | text                        | None                     | YES  | None    | Scope expression                                                        |
| iat                 | timestamp without time zone | None                     | YES  | None    | jans Creation                                                           |
| exp                 | timestamp without time zone | None                     | YES  | None    | jans Exp                                                                |
| del                 | boolean                     | None                     | YES  | None    | del                                                                     |

### jansUmaResourcePermission
| Field             | Type                        | Character Maximum Length | Null | Default | Comment                                         |
| ----------------- | --------------------------- | ------------------------ | ---- | ------- | ----------------------------------------------- |
| exp               | timestamp without time zone | None                     | YES  | None    | jans Exp                                        |
| del               | boolean                     | None                     | YES  | None    | del                                             |
| jansUmaScope      | character varying           | 768                      | YES  | None    | URI reference of scope descriptor               |
| jansConfCode      | character varying           | 64                       | YES  | None    | jans configuration code                         |
| jansResourceSetId | character varying           | 64                       | YES  | None    | jans resource set id                            |
| jansAttrs         | text                        | None                     | YES  | None    | Attrs                                           |
| jansTicket        | character varying           | 64                       | YES  | None    | jans ticket                                     |
| jansStatus        | character varying           | 16                       | YES  | None    | Status of the entry, used by many objectclasses |

### jansGrant
| Field | Type                        | Character Maximum Length | Null | Default | Comment       |
| ----- | --------------------------- | ------------------------ | ---- | ------- | ------------- |
| grtId | character varying           | 64                       | YES  | None    | jans grant id |
| iat   | timestamp without time zone | None                     | YES  | None    | jans Creation |

### jansToken
| Field     | Type                        | Character Maximum Length | Null | Default | Comment                        |
| --------- | --------------------------- | ------------------------ | ---- | ------- | ------------------------------ |
| authnTime | timestamp without time zone | None                     | YES  | None    | jans Authn Time                |
| authzCode | character varying           | 64                       | YES  | None    | jans authorization code        |
| iat       | timestamp without time zone | None                     | YES  | None    | jans Creation                  |
| exp       | timestamp without time zone | None                     | YES  | None    | jans Exp                       |
| del       | boolean                     | None                     | YES  | None    | del                            |
| grtId     | character varying           | 64                       | YES  | None    | jans grant id                  |
| grtTyp    | character varying           | 64                       | YES  | None    | jans Grant Typ                 |
| jwtReq    | text                        | None                     | YES  | None    | jans JWT Req                   |
| nnc       | text                        | None                     | YES  | None    | jans nonce                     |
| scp       | text                        | None                     | YES  | None    | jans Attr Scope                |
| tknCde    | character varying           | 80                       | YES  | None    | jans Tkn Code                  |
| tknTyp    | character varying           | 32                       | YES  | None    | jans Tkn Typ                   |
| usrId     | character varying           | 64                       | YES  | None    | jans user id                   |
| jansUsrDN | character varying           | 128                      | YES  | None    | jans Usr DN                    |
| clnId     | character varying           | 64                       | YES  | None    | jans Clnt id                   |
| uuid      | character varying           | 64                       | YES  | None    | Unique identifier              |
| chlng     | character varying           | 64                       | YES  | None    | OX PKCE code challenge         |
| chlngMth  | character varying           | 64                       | YES  | None    | OX PKCE code challenge method  |
| clms      | character varying           | 64                       | YES  | None    | jans Claims                    |
| ssnId     | character varying           | 64                       | YES  | None    | jans Sess DN                   |
| attr      | text                        | None                     | YES  | None    | Attrs                          |
| tknBndCnf | text                        | None                     | YES  | None    | jansauth - Tkn Binding Id Hash |
| dpop      | character varying           | 64                       | YES  | None    | DPoP Proof                     |

### jansGrp
| Field       | Type              | Character Maximum Length | Null | Default | Comment                                                       |
| ----------- | ----------------- | ------------------------ | ---- | ------- | ------------------------------------------------------------- |
| displayName | character varying | 128                      | YES  | None    | preferred name of a person to be used when displaying entries |
| jansStatus  | character varying | 16                       | YES  | None    | Status of the entry, used by many objectclasses               |
| inum        | character varying | 64                       | YES  | None    | XRI i-number                                                  |

### jansAttr
| Field                 | Type              | Character Maximum Length | Null | Default | Comment                                                                                                                      |
| --------------------- | ----------------- | ------------------------ | ---- | ------- | ---------------------------------------------------------------------------------------------------------------------------- |
| displayName           | character varying | 128                      | YES  | None    | preferred name of a person to be used when displaying entries                                                                |
| jansAttrEditTyp       | jsonb             | None                     | YES  | None    | Specify in exclude who can update an attribute, admin or user                                                                |
| jansAttrName          | character varying | 64                       | YES  | None    | Specify an identifier for an attribute. May be multi-value  where an attribute has two names, like givenName and first-name. |
| jansAttrOrigin        | character varying | 64                       | YES  | None    | Specify the person objectclass associated with the attribute,  used for display purposes in exclude.                         |
| jansAttrSystemEditTyp | character varying | 64                       | YES  | None    | TODO - still required?                                                                                                       |
| jansAttrTyp           | character varying | 64                       | YES  | None    | Data type of attribute. Values can be string, photo, numeric, date                                                           |
| jansClaimName         | character varying | 64                       | YES  | None    | Used by jans in conjunction with jansttributeName to map claims to attributes in LDAP.                                       |
| jansAttrUsgTyp        | character varying | 64                       | YES  | None    | TODO - Usg? Value can be OpenID                                                                                              |
| jansAttrViewTyp       | jsonb             | None                     | YES  | None    | Specify in exclude who can view an attribute, admin or user                                                                  |
| jansSAML1URI          | character varying | 64                       | YES  | None    | SAML 1 uri of attribute                                                                                                      |
| jansSAML2URI          | character varying | 64                       | YES  | None    | SAML 2 uri of attribute                                                                                                      |
| jansStatus            | character varying | 16                       | YES  | None    | Status of the entry, used by many objectclasses                                                                              |
| inum                  | character varying | 64                       | YES  | None    | XRI i-number                                                                                                                 |
| jansNameIdTyp         | character varying | 64                       | YES  | None    | NameId Typ                                                                                                                   |
| jansSourceAttr        | character varying | 64                       | YES  | None    | Source Attr for this Attr                                                                                                    |
| jansRegExp            | character varying | 64                       | YES  | None    | Regular expression used to validate attribute data                                                                           |
| jansTooltip           | character varying | 64                       | YES  | None    | Custom tooltip to be shown on the UI                                                                                         |
| jansValidation        | text              | None                     | YES  | None    | This data has information about attribute Validation                                                                         |

### jansPassResetReq
| Field        | Type                        | Character Maximum Length | Null | Default | Comment                                        |
| ------------ | --------------------------- | ------------------------ | ---- | ------- | ---------------------------------------------- |
| creationDate | timestamp without time zone | None                     | YES  | None    | Creation Date used for password reset requests |
| jansGuid     | character varying           | 64                       | YES  | None    | A random string to mark temporary tokens       |
| personInum   | character varying           | 64                       | YES  | None    | Inum of a person                               |

### jansSessId
| Field                    | Type                        | Character Maximum Length | Null | Default | Comment                                                                      |
| ------------------------ | --------------------------- | ------------------------ | ---- | ------- | ---------------------------------------------------------------------------- |
| jansId                   | character varying           | 128                      | YES  | None    | Identifier                                                                   |
| sid                      | character varying           | 64                       | YES  | None    | Sess Identifier                                                              |
| creationDate             | timestamp without time zone | None                     | YES  | None    | Creation Date used for password reset requests                               |
| exp                      | timestamp without time zone | None                     | YES  | None    | jans Exp                                                                     |
| del                      | boolean                     | None                     | YES  | None    | del                                                                          |
| jansLastAccessTime       | timestamp without time zone | None                     | YES  | None    | Last access time                                                             |
| jansUsrDN                | character varying           | 128                      | YES  | None    | jans Usr DN                                                                  |
| authnTime                | timestamp without time zone | None                     | YES  | None    | jans Authn Time                                                              |
| jansState                | character varying           | 64                       | YES  | None    | jansState                                                                    |
| jansSessState            | text                        | None                     | YES  | None    | jans Sess State                                                              |
| jansPermissionGranted    | boolean                     | None                     | YES  | None    | jans Permission Granted                                                      |
| jansAsJwt                | boolean                     | None                     | YES  | None    | Boolean field to indicate whether object is used as JWT                      |
| jansJwt                  | text                        | None                     | YES  | None    | JWT representation of the object or otherwise jwt associated with the object |
| jansPermissionGrantedMap | text                        | None                     | YES  | None    | jans Permission Granted Map                                                  |
| jansInvolvedClnts        | text                        | None                     | YES  | None    | Involved clients                                                             |
| deviceSecret             | character varying           | 64                       | YES  | None    | deviceSecret                                                                 |
| jansSessAttr             | text                        | None                     | YES  | None    | jansSessAttr                                                                 |

### jansUmaRPT
| Field             | Type                        | Character Maximum Length | Null | Default | Comment                        |
| ----------------- | --------------------------- | ------------------------ | ---- | ------- | ------------------------------ |
| authnTime         | timestamp without time zone | None                     | YES  | None    | jans Authn Time                |
| clnId             | character varying           | 64                       | YES  | None    | jans Clnt id                   |
| iat               | timestamp without time zone | None                     | YES  | None    | jans Creation                  |
| exp               | timestamp without time zone | None                     | YES  | None    | jans Exp                       |
| del               | boolean                     | None                     | YES  | None    | del                            |
| tknCde            | character varying           | 80                       | YES  | None    | jans Tkn Code                  |
| usrId             | character varying           | 64                       | YES  | None    | jans user id                   |
| ssnId             | character varying           | 64                       | YES  | None    | jans Sess DN                   |
| jansUmaPermission | jsonb                       | None                     | YES  | None    | jans uma permission            |
| uuid              | character varying           | 64                       | YES  | None    | Unique identifier              |
| dpop              | character varying           | 64                       | YES  | None    | DPoP Proof                     |
| authzCode         | character varying           | 64                       | YES  | None    | jans authorization code        |
| grtId             | character varying           | 64                       | YES  | None    | jans grant id                  |
| grtTyp            | character varying           | 64                       | YES  | None    | jans Grant Typ                 |
| jwtReq            | text                        | None                     | YES  | None    | jans JWT Req                   |
| nnc               | text                        | None                     | YES  | None    | jans nonce                     |
| scp               | text                        | None                     | YES  | None    | jans Attr Scope                |
| tknTyp            | character varying           | 32                       | YES  | None    | jans Tkn Typ                   |
| jansUsrDN         | character varying           | 128                      | YES  | None    | jans Usr DN                    |
| chlng             | character varying           | 64                       | YES  | None    | OX PKCE code challenge         |
| chlngMth          | character varying           | 64                       | YES  | None    | OX PKCE code challenge method  |
| clms              | character varying           | 64                       | YES  | None    | jans Claims                    |
| attr              | text                        | None                     | YES  | None    | Attrs                          |
| tknBndCnf         | text                        | None                     | YES  | None    | jansauth - Tkn Binding Id Hash |

### jansPushApp
| Field           | Type              | Character Maximum Length | Null | Default | Comment                                                       |
| --------------- | ----------------- | ------------------------ | ---- | ------- | ------------------------------------------------------------- |
| displayName     | character varying | 128                      | YES  | None    | preferred name of a person to be used when displaying entries |
| jansId          | character varying | 128                      | YES  | None    | Identifier                                                    |
| jansName        | character varying | 64                       | YES  | None    | Name                                                          |
| jansPushAppConf | character varying | 64                       | YES  | None    | jansPush application configuration                            |

### jansScr
| Field      | Type              | Character Maximum Length | Null | Default | Comment                                                   |
| ---------- | ----------------- | ------------------------ | ---- | ------- | --------------------------------------------------------- |
| inum       | character varying | 64                       | YES  | None    | XRI i-number                                              |
| jansScr    | text              | None                     | YES  | None    | Attr that contains script (python, java script)           |
| jansScrTyp | character varying | 64                       | YES  | None    | Attr that contains script type (e.g. python, java script) |

### jansCustomScr
| Field              | Type              | Character Maximum Length | Null | Default | Comment                                                                  |
| ------------------ | ----------------- | ------------------------ | ---- | ------- | ------------------------------------------------------------------------ |
| inum               | character varying | 64                       | YES  | None    | XRI i-number                                                             |
| displayName        | character varying | 128                      | YES  | None    | preferred name of a person to be used when displaying entries            |
| jansScr            | text              | None                     | YES  | None    | Attr that contains script (python, java script)                          |
| jansScrTyp         | character varying | 64                       | YES  | None    | Attr that contains script type (e.g. python, java script)                |
| jansProgLng        | character varying | 64                       | YES  | None    | programming language                                                     |
| jansModuleProperty | jsonb             | None                     | YES  | None    | Module property                                                          |
| jansConfProperty   | jsonb             | None                     | YES  | None    | Conf property                                                            |
| jansLevel          | integer           | None                     | YES  | None    | Level                                                                    |
| jansRevision       | integer           | None                     | YES  | None    | Revision                                                                 |
| jansEnabled        | boolean           | None                     | YES  | None    | Status of the entry, used by many objectclasses                          |
| jansScrError       | text              | None                     | YES  | None    | Attr that contains first error which application get during it execution |
| jansAlias          | jsonb             | None                     | YES  | None    | jansAlias                                                                |

### jansDeviceRegistration
| Field                      | Type                        | Character Maximum Length | Null | Default | Comment                                                       |
| -------------------------- | --------------------------- | ------------------------ | ---- | ------- | ------------------------------------------------------------- |
| jansId                     | character varying           | 128                      | YES  | None    | Identifier                                                    |
| displayName                | character varying           | 128                      | YES  | None    | preferred name of a person to be used when displaying entries |
| jansDeviceKeyHandle        | character varying           | 128                      | YES  | None    | jansDeviceKeyHandle                                           |
| jansDeviceHashCode         | integer                     | None                     | YES  | None    | jansDeviceHashCode                                            |
| jansApp                    | character varying           | 96                       | YES  | None    | jansApp                                                       |
| jansDeviceRegistrationConf | text                        | None                     | YES  | None    | jansDeviceRegistrationConf                                    |
| jansDeviceNotificationConf | character varying           | 64                       | YES  | None    | Extended push notification configuration                      |
| jansNickName               | character varying           | 64                       | YES  | None    | jansNickName                                                  |
| jansDeviceData             | text                        | None                     | YES  | None    | jansDeviceData                                                |
| jansCounter                | integer                     | None                     | YES  | None    | jansCounter                                                   |
| jansStatus                 | character varying           | 16                       | YES  | None    | Status of the entry, used by many objectclasses               |
| del                        | boolean                     | None                     | YES  | None    | del                                                           |
| exp                        | timestamp without time zone | None                     | YES  | None    | jans Exp                                                      |
| personInum                 | character varying           | 64                       | YES  | None    | Inum of a person                                              |
| creationDate               | timestamp without time zone | None                     | YES  | None    | Creation Date used for password reset requests                |
| jansLastAccessTime         | timestamp without time zone | None                     | YES  | None    | Last access time                                              |

### jansU2fReq
| Field           | Type                        | Character Maximum Length | Null | Default | Comment                                        |
| --------------- | --------------------------- | ------------------------ | ---- | ------- | ---------------------------------------------- |
| jansId          | character varying           | 128                      | YES  | None    | Identifier                                     |
| jansReqId       | character varying           | 64                       | YES  | None    | jansReqId                                      |
| jansReq         | text                        | None                     | YES  | None    | jansReq                                        |
| jansSessStateId | character varying           | 64                       | YES  | None    | jansSessStateId                                |
| del             | boolean                     | None                     | YES  | None    | del                                            |
| exp             | timestamp without time zone | None                     | YES  | None    | jans Exp                                       |
| personInum      | character varying           | 64                       | YES  | None    | Inum of a person                               |
| creationDate    | timestamp without time zone | None                     | YES  | None    | Creation Date used for password reset requests |

### jansMetric
| Field         | Type                        | Character Maximum Length | Null | Default | Comment                                        |
| ------------- | --------------------------- | ------------------------ | ---- | ------- | ---------------------------------------------- |
| jansStartDate | timestamp without time zone | None                     | YES  | None    | Start date                                     |
| jansEndDate   | timestamp without time zone | None                     | YES  | None    | End date                                       |
| jansAppTyp    | character varying           | 64                       | YES  | None    | jans App Typ                                   |
| jansMetricTyp | character varying           | 64                       | YES  | None    | Metric type                                    |
| creationDate  | timestamp without time zone | None                     | YES  | None    | Creation Date used for password reset requests |
| del           | boolean                     | None                     | YES  | None    | del                                            |
| exp           | timestamp without time zone | None                     | YES  | None    | jans Exp                                       |
| jansData      | text                        | None                     | YES  | None    | OX data                                        |
| jansHost      | character varying           | 64                       | YES  | None    | jans host                                      |

### jansUmaPCT
| Field           | Type                        | Character Maximum Length | Null | Default | Comment                        |
| --------------- | --------------------------- | ------------------------ | ---- | ------- | ------------------------------ |
| clnId           | character varying           | 64                       | YES  | None    | jans Clnt id                   |
| iat             | timestamp without time zone | None                     | YES  | None    | jans Creation                  |
| exp             | timestamp without time zone | None                     | YES  | None    | jans Exp                       |
| del             | boolean                     | None                     | YES  | None    | del                            |
| tknCde          | character varying           | 80                       | YES  | None    | jans Tkn Code                  |
| ssnId           | character varying           | 64                       | YES  | None    | jans Sess DN                   |
| jansClaimValues | character varying           | 64                       | YES  | None    | Claim Values                   |
| dpop            | character varying           | 64                       | YES  | None    | DPoP Proof                     |
| authnTime       | timestamp without time zone | None                     | YES  | None    | jans Authn Time                |
| authzCode       | character varying           | 64                       | YES  | None    | jans authorization code        |
| grtId           | character varying           | 64                       | YES  | None    | jans grant id                  |
| grtTyp          | character varying           | 64                       | YES  | None    | jans Grant Typ                 |
| jwtReq          | text                        | None                     | YES  | None    | jans JWT Req                   |
| nnc             | text                        | None                     | YES  | None    | jans nonce                     |
| scp             | text                        | None                     | YES  | None    | jans Attr Scope                |
| tknTyp          | character varying           | 32                       | YES  | None    | jans Tkn Typ                   |
| usrId           | character varying           | 64                       | YES  | None    | jans user id                   |
| jansUsrDN       | character varying           | 128                      | YES  | None    | jans Usr DN                    |
| uuid            | character varying           | 64                       | YES  | None    | Unique identifier              |
| chlng           | character varying           | 64                       | YES  | None    | OX PKCE code challenge         |
| chlngMth        | character varying           | 64                       | YES  | None    | OX PKCE code challenge method  |
| clms            | character varying           | 64                       | YES  | None    | jans Claims                    |
| attr            | text                        | None                     | YES  | None    | Attrs                          |
| tknBndCnf       | text                        | None                     | YES  | None    | jansauth - Tkn Binding Id Hash |

### jansCache
| Field | Type                        | Character Maximum Length | Null | Default | Comment           |
| ----- | --------------------------- | ------------------------ | ---- | ------- | ----------------- |
| uuid  | character varying           | 64                       | YES  | None    | Unique identifier |
| iat   | timestamp without time zone | None                     | YES  | None    | jans Creation     |
| exp   | timestamp without time zone | None                     | YES  | None    | jans Exp          |
| del   | boolean                     | None                     | YES  | None    | del               |
| dat   | text                        | None                     | YES  | None    | OX data           |

### jansFido2RegistrationEntry
| Field                      | Type                        | Character Maximum Length | Null | Default | Comment                                                       |
| -------------------------- | --------------------------- | ------------------------ | ---- | ------- | ------------------------------------------------------------- |
| jansId                     | character varying           | 128                      | YES  | None    | Identifier                                                    |
| creationDate               | timestamp without time zone | None                     | YES  | None    | Creation Date used for password reset requests                |
| displayName                | character varying           | 128                      | YES  | None    | preferred name of a person to be used when displaying entries |
| jansApp                    | character varying           | 96                       | YES  | None    | jansApp                                                       |
| jansSessStateId            | character varying           | 64                       | YES  | None    | jansSessStateId                                               |
| jansCodeChallenge          | character varying           | 64                       | YES  | None    | OX PKCE code challenge                                        |
| jansCodeChallengeHash      | integer                     | None                     | YES  | None    | OX code challenge hash                                        |
| jansPublicKeyId            | character varying           | 96                       | YES  | None    | jansPublicKeyId                                               |
| jansPublicKeyIdHash        | integer                     | None                     | YES  | None    | jansPublicKeyIdHash                                           |
| personInum                 | character varying           | 64                       | YES  | None    | Inum of a person                                              |
| jansRegistrationData       | text                        | None                     | YES  | None    | jansRegistrationData                                          |
| jansDeviceData             | text                        | None                     | YES  | None    | jansDeviceData                                                |
| jansDeviceNotificationConf | character varying           | 64                       | YES  | None    | Extended push notification configuration                      |
| jansCounter                | integer                     | None                     | YES  | None    | jansCounter                                                   |
| jansStatus                 | character varying           | 16                       | YES  | None    | Status of the entry, used by many objectclasses               |
| exp                        | timestamp without time zone | None                     | YES  | None    | jans Exp                                                      |
| del                        | boolean                     | None                     | YES  | None    | del                                                           |

### jansPushDevice
| Field              | Type              | Character Maximum Length | Null | Default | Comment                       |
| ------------------ | ----------------- | ------------------------ | ---- | ------- | ----------------------------- |
| jansUsrId          | character varying | 64                       | YES  | None    | jans user id                  |
| jansId             | character varying | 128                      | YES  | None    | Identifier                    |
| jansPushApp        | text              | None                     | YES  | None    | jansPush application DN       |
| jansPushDeviceConf | character varying | 64                       | YES  | None    | jansPush device configuration |
| jansTyp            | character varying | 64                       | YES  | None    | jans type                     |

### jansClntAuthz
| Field      | Type                        | Character Maximum Length | Null | Default | Comment         |
| ---------- | --------------------------- | ------------------------ | ---- | ------- | --------------- |
| jansId     | character varying           | 128                      | YES  | None    | Identifier      |
| jansClntId | jsonb                       | None                     | YES  | None    | jans Clnt id    |
| jansUsrId  | character varying           | 64                       | YES  | None    | jans user id    |
| exp        | timestamp without time zone | None                     | YES  | None    | jans Exp        |
| del        | boolean                     | None                     | YES  | None    | del             |
| jansScope  | jsonb                       | None                     | YES  | None    | jans Attr Scope |

### jansSectorIdentifier
| Field           | Type              | Character Maximum Length | Null | Default | Comment           |
| --------------- | ----------------- | ------------------------ | ---- | ------- | ----------------- |
| jansId          | character varying | 128                      | YES  | None    | Identifier        |
| jansRedirectURI | jsonb             | None                     | YES  | None    | jans Redirect URI |
| jansClntId      | jsonb             | None                     | YES  | None    | jans Clnt id      |

### jansFido2AuthnEntry
| Field                 | Type                        | Character Maximum Length | Null | Default | Comment                                         |
| --------------------- | --------------------------- | ------------------------ | ---- | ------- | ----------------------------------------------- |
| jansId                | character varying           | 128                      | YES  | None    | Identifier                                      |
| creationDate          | timestamp without time zone | None                     | YES  | None    | Creation Date used for password reset requests  |
| jansApp               | character varying           | 96                       | YES  | None    | jansApp                                         |
| jansSessStateId       | character varying           | 64                       | YES  | None    | jansSessStateId                                 |
| jansCodeChallenge     | character varying           | 64                       | YES  | None    | OX PKCE code challenge                          |
| jansCodeChallengeHash | integer                     | None                     | YES  | None    | OX code challenge hash                          |
| personInum            | character varying           | 64                       | YES  | None    | Inum of a person                                |
| jansAuthData          | text                        | None                     | YES  | None    | jansAuthData                                    |
| jansStatus            | character varying           | 16                       | YES  | None    | Status of the entry, used by many objectclasses |
| exp                   | timestamp without time zone | None                     | YES  | None    | jans Exp                                        |
| del                   | boolean                     | None                     | YES  | None    | del                                             |

### jansRp
| Field  | Type              | Character Maximum Length | Null | Default | Comment    |
| ------ | ----------------- | ------------------------ | ---- | ------- | ---------- |
| jansId | character varying | 128                      | YES  | None    | Identifier |
| dat    | text              | None                     | YES  | None    | OX data    |

### jansCibaReq
| Field        | Type                        | Character Maximum Length | Null | Default | Comment                                         |
| ------------ | --------------------------- | ------------------------ | ---- | ------- | ----------------------------------------------- |
| authReqId    | character varying           | 64                       | YES  | None    | Authn request id                                |
| clnId        | character varying           | 64                       | YES  | None    | jans Clnt id                                    |
| usrId        | character varying           | 64                       | YES  | None    | jans user id                                    |
| creationDate | timestamp without time zone | None                     | YES  | None    | Creation Date used for password reset requests  |
| exp          | timestamp without time zone | None                     | YES  | None    | jans Exp                                        |
| jansStatus   | character varying           | 16                       | YES  | None    | Status of the entry, used by many objectclasses |

### jansStatEntry
| Field    | Type              | Character Maximum Length | Null | Default | Comment    |
| -------- | ----------------- | ------------------------ | ---- | ------- | ---------- |
| jansId   | character varying | 128                      | YES  | None    | Identifier |
| dat      | text              | None                     | YES  | None    | OX data    |
| jansData | text              | None                     | YES  | None    | OX data    |
| attr     | text              | None                     | YES  | None    | Attrs      |

### jansPar
| Field     | Type                        | Character Maximum Length | Null | Default | Comment    |
| --------- | --------------------------- | ------------------------ | ---- | ------- | ---------- |
| jansId    | character varying           | 128                      | YES  | None    | Identifier |
| jansAttrs | text                        | None                     | YES  | None    | Attrs      |
| exp       | timestamp without time zone | None                     | YES  | None    | jans Exp   |
| del       | boolean                     | None                     | YES  | None    | del        |

### jansInumMap
| Field                    | Type              | Character Maximum Length | Null | Default | Comment                                         |
| ------------------------ | ----------------- | ------------------------ | ---- | ------- | ----------------------------------------------- |
| jansStatus               | character varying | 16                       | YES  | None    | Status of the entry, used by many objectclasses |
| inum                     | character varying | 64                       | YES  | None    | XRI i-number                                    |
| jansPrimaryKeyAttrName   | character varying | 64                       | YES  | None    | Primary Key Attribute Name                      |
| jansPrimaryKeyValue      | character varying | 64                       | YES  | None    | Primary Key Value                               |
| jansSecondaryKeyAttrName | character varying | 64                       | YES  | None    | Secondary Key Attribute Name                    |
| jansSecondaryKeyValue    | character varying | 64                       | YES  | None    | Secondary Key Value                             |
| jansTertiaryKeyAttrName  | character varying | 64                       | YES  | None    | Tertiary Key Attribute Name                     |
| jansTertiaryKeyValue     | character varying | 64                       | YES  | None    | Tertiary Key Value                              |

### agmFlowRun
| Field             | Type                        | Character Maximum Length | Null | Default | Comment                                                  |
| ----------------- | --------------------------- | ------------------------ | ---- | ------- | -------------------------------------------------------- |
| jansId            | character varying           | 128                      | YES  | None    | Identifier                                               |
| agFlowSt          | text                        | None                     | YES  | None    | Details of a running agama flow instance                 |
| agFlowEncCont     | text                        | None                     | YES  | None    | Continuation associated to a running agama flow instance |
| jansCustomMessage | character varying           | 128                      | YES  | None    | exclude custom welcome message                           |
| exp               | timestamp without time zone | None                     | YES  | None    | jans Exp                                                 |

### agmFlow
| Field             | Type              | Character Maximum Length | Null | Default | Comment                                                                  |
| ----------------- | ----------------- | ------------------------ | ---- | ------- | ------------------------------------------------------------------------ |
| agFlowQname       | character varying | 64                       | YES  | None    | Full name of an agama flow                                               |
| agFlowMeta        | text              | None                     | YES  | None    | Metadata of an agama flow                                                |
| jansScr           | text              | None                     | YES  | None    | Attr that contains script (python, java script)                          |
| jansEnabled       | boolean           | None                     | YES  | None    | Status of the entry, used by many objectclasses                          |
| jansScrError      | text              | None                     | YES  | None    | Attr that contains first error which application get during it execution |
| agFlowTrans       | text              | None                     | YES  | None    | Transpiled code of an agama flow                                         |
| jansRevision      | integer           | None                     | YES  | None    | Revision                                                                 |
| jansCustomMessage | character varying | 128                      | YES  | None    | exclude custom welcome message                                           |

### adsPrjDeployment
| Field             | Type                        | Character Maximum Length | Null | Default | Comment                                   |
| ----------------- | --------------------------- | ------------------------ | ---- | ------- | ----------------------------------------- |
| jansId            | character varying           | 128                      | YES  | None    | Identifier                                |
| jansStartDate     | timestamp without time zone | None                     | YES  | None    | Start date                                |
| jansEndDate       | timestamp without time zone | None                     | YES  | None    | End date                                  |
| adsPrjAssets      | text                        | None                     | YES  | None    | Assets of an ADS project                  |
| adsPrjDeplDetails | text                        | None                     | YES  | None    | Misc details associated to an ADS project |

### jansDocument
| Field              | Type                        | Character Maximum Length | Null | Default | Comment                                                       |
| ------------------ | --------------------------- | ------------------------ | ---- | ------- | ------------------------------------------------------------- |
| inum               | character varying           | 64                       | YES  | None    | XRI i-number                                                  |
| displayName        | character varying           | 128                      | YES  | None    | preferred name of a person to be used when displaying entries |
| document           | character varying           | 64                       | YES  | None    | Save Document in DB                                           |
| creationDate       | timestamp without time zone | None                     | YES  | None    | Creation Date used for password reset requests                |
| jansModuleProperty | jsonb                       | None                     | YES  | None    | Module property                                               |
| jansLevel          | integer                     | None                     | YES  | None    | Level                                                         |
| jansRevision       | integer                     | None                     | YES  | None    | Revision                                                      |
| jansEnabled        | boolean                     | None                     | YES  | None    | Status of the entry, used by many objectclasses               |
