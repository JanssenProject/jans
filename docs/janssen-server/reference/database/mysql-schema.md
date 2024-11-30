---
tags:
  - administration
  - database
  - MySQL
  - Indexes
---

# MySQL Schema

## Tables
| Table names                |
| -------------------------- |
| adsPrjDeployment           |
| agmFlow                    |
| agmFlowRun                 |
| jansAppConf                |
| jansAttr                   |
| jansCache                  |
| jansCibaReq                |
| jansClnt                   |
| jansClntAuthz              |
| jansCustomScr              |
| jansDeviceRegistration     |
| jansDocument               |
| jansFido2AuthnEntry        |
| jansFido2RegistrationEntry |
| jansGrant                  |
| jansGrp                    |
| jansInumMap                |
| jansMetric                 |
| jansOrganization           |
| jansPairwiseIdentifier     |
| jansPar                    |
| jansPassResetReq           |
| jansPerson                 |
| jansPushApp                |
| jansPushDevice             |
| jansRp                     |
| jansScope                  |
| jansScr                    |
| jansSectorIdentifier       |
| jansSessId                 |
| jansSsa                    |
| jansStatEntry              |
| jansToken                  |
| jansU2fReq                 |
| jansUmaPCT                 |
| jansUmaRPT                 |
| jansUmaResource            |
| jansUmaResourcePermission  |

### adsPrjDeployment
| Field             | Type         | Null | Key | Default | Comment                                   |
| ----------------- | ------------ | ---- | --- | ------- | ----------------------------------------- |
| doc_id            | varchar(64)  | NO   | PRI | None    |                                           |
| objectClass       | varchar(48)  | YES  |     | None    |                                           |
| dn                | varchar(128) | YES  |     | None    |                                           |
| jansId            | varchar(128) | YES  |     | None    | Identifier                                |
| jansStartDate     | datetime(3)  | YES  |     | None    | Start date                                |
| jansActive        | smallint     | YES  |     | None    |                                           |
| jansEndDate       | datetime(3)  | YES  |     | None    | End date                                  |
| adsPrjAssets      | longtext     | YES  |     | None    | Assets of an ADS project                  |
| adsPrjDeplDetails | text         | YES  |     | None    | Misc details associated to an ADS project |

### agmFlow
| Field             | Type         | Null | Key | Default | Comment                                                                  |
| ----------------- | ------------ | ---- | --- | ------- | ------------------------------------------------------------------------ |
| doc_id            | varchar(64)  | NO   | PRI | None    |                                                                          |
| objectClass       | varchar(48)  | YES  |     | None    |                                                                          |
| dn                | varchar(128) | YES  |     | None    |                                                                          |
| agFlowQname       | varchar(64)  | YES  |     | None    | Full name of an agama flow                                               |
| agFlowMeta        | text         | YES  |     | None    | Metadata of an agama flow                                                |
| jansScr           | text         | YES  |     | None    | Attr that contains script (python, java script)                          |
| jansEnabled       | smallint     | YES  |     | None    | Status of the entry, used by many objectclasses                          |
| jansScrError      | text         | YES  |     | None    | Attr that contains first error which application get during it execution |
| agFlowTrans       | text         | YES  |     | None    | Transpiled code of an agama flow                                         |
| jansRevision      | int          | YES  |     | None    | Revision                                                                 |
| jansCustomMessage | varchar(128) | YES  |     | None    | exclude custom welcome message                                           |

### agmFlowRun
| Field             | Type         | Null | Key | Default | Comment                                                  |
| ----------------- | ------------ | ---- | --- | ------- | -------------------------------------------------------- |
| doc_id            | varchar(64)  | NO   | PRI | None    |                                                          |
| objectClass       | varchar(48)  | YES  |     | None    |                                                          |
| dn                | varchar(128) | YES  |     | None    |                                                          |
| jansId            | varchar(128) | YES  |     | None    | Identifier                                               |
| agFlowSt          | text         | YES  |     | None    | Details of a running agama flow instance                 |
| agFlowEncCont     | mediumtext   | YES  |     | None    | Continuation associated to a running agama flow instance |
| jansCustomMessage | varchar(128) | YES  |     | None    | exclude custom welcome message                           |
| exp               | datetime(3)  | YES  |     | None    | jans Exp                                                 |

### jansAppConf
| Field                 | Type         | Null | Key | Default | Comment                                                                      |
| --------------------- | ------------ | ---- | --- | ------- | ---------------------------------------------------------------------------- |
| doc_id                | varchar(64)  | NO   | PRI | None    |                                                                              |
| objectClass           | varchar(48)  | YES  |     | None    |                                                                              |
| dn                    | varchar(128) | YES  |     | None    |                                                                              |
| c                     | varchar(2)   | YES  |     | None    |                                                                              |
| ou                    | varchar(64)  | YES  |     | None    |                                                                              |
| description           | varchar(768) | YES  |     | None    |                                                                              |
| displayName           | varchar(128) | YES  |     | None    | preferred name of a person to be used when displaying entries                |
| jansHostname          | varchar(64)  | YES  |     | None    | The hostname of the Jans Server instance                                     |
| jansLastUpd           | datetime(3)  | YES  |     | None    | Monitors last time the server was able to connect to  the monitoring system. |
| jansManager           | varchar(64)  | YES  |     | None    | Used to specify if a person has the manager role                             |
| jansOrgProfileMgt     | smallint     | YES  |     | None    | enable or disable profile management feature in exclude                      |
| jansScimEnabled       | smallint     | YES  |     | None    | exclude SCIM feature - enabled or disabled                                   |
| jansEmail             | json         | YES  |     | None    |                                                                              |
| jansSmtpConf          | json         | YES  |     | None    | SMTP configuration                                                           |
| jansSslExpiry         | varchar(64)  | YES  |     | None    | SAML Trust Relationship configuration                                        |
| jansStatus            | varchar(16)  | YES  |     | None    | Status of the entry, used by many objectclasses                              |
| jansUrl               | varchar(64)  | YES  |     | None    | Jans instance URL                                                            |
| inum                  | varchar(64)  | YES  |     | None    | XRI i-number                                                                 |
| o                     | varchar(64)  | YES  |     | None    |                                                                              |
| jansAuthMode          | varchar(64)  | YES  |     | None    |                                                                              |
| jansDbAuth            | json         | YES  |     | None    | Custom IDP authentication configuration                                      |
| jansLogViewerConfig   | varchar(64)  | YES  |     | None    | Log viewer configuration                                                     |
| jansLogConfigLocation | varchar(64)  | YES  |     | None    | Path to external log4j2.xml                                                  |
| jansCacheConf         | text         | YES  |     | None    | Cache configuration                                                          |
| jansDocStoreConf      | text         | YES  |     | None    | jansDocStoreConf                                                             |
| jansSoftVer           | varchar(64)  | YES  |     | None    |                                                                              |
| userPassword          | varchar(256) | YES  |     | None    |                                                                              |
| jansConfDyn           | text         | YES  |     | None    | jans Dyn Conf                                                                |
| jansConfErrors        | text         | YES  |     | None    | jans Errors Conf                                                             |
| jansConfStatic        | text         | YES  |     | None    | jans Static Conf                                                             |
| jansConfWebKeys       | text         | YES  |     | None    | jans Web Keys Conf                                                           |
| jansWebKeysSettings   | varchar(64)  | YES  |     | None    | jans Web Keys Conf                                                           |
| jansConfApp           | text         | YES  |     | None    | jans App Conf                                                                |
| jansRevision          | int          | YES  |     | None    | Revision                                                                     |

### jansAttr
| Field                 | Type         | Null | Key | Default | Comment                                                                                                                      |
| --------------------- | ------------ | ---- | --- | ------- | ---------------------------------------------------------------------------------------------------------------------------- |
| doc_id                | varchar(64)  | NO   | PRI | None    |                                                                                                                              |
| objectClass           | varchar(48)  | YES  |     | None    |                                                                                                                              |
| dn                    | varchar(128) | YES  |     | None    |                                                                                                                              |
| description           | varchar(768) | YES  | MUL | None    |                                                                                                                              |
| displayName           | varchar(128) | YES  | MUL | None    | preferred name of a person to be used when displaying entries                                                                |
| jansAttrEditTyp       | json         | YES  |     | None    | Specify in exclude who can update an attribute, admin or user                                                                |
| jansAttrName          | varchar(64)  | YES  | MUL | None    | Specify an identifier for an attribute. May be multi-value  where an attribute has two names, like givenName and first-name. |
| jansAttrOrigin        | varchar(64)  | YES  | MUL | None    | Specify the person objectclass associated with the attribute,  used for display purposes in exclude.                         |
| jansAttrSystemEditTyp | varchar(64)  | YES  |     | None    | TODO - still required?                                                                                                       |
| jansAttrTyp           | varchar(64)  | YES  |     | None    | Data type of attribute. Values can be string, photo, numeric, date                                                           |
| jansClaimName         | varchar(64)  | YES  |     | None    | Used by jans in conjunction with jansttributeName to map claims to attributes in datastore.                                       |
| jansAttrUsgTyp        | varchar(64)  | YES  |     | None    | TODO - Usg? Value can be OpenID                                                                                              |
| jansAttrViewTyp       | json         | YES  |     | None    | Specify in exclude who can view an attribute, admin or user                                                                  |
| jansSAML1URI          | varchar(64)  | YES  |     | None    | SAML 1 uri of attribute                                                                                                      |
| jansSAML2URI          | varchar(64)  | YES  |     | None    | SAML 2 uri of attribute                                                                                                      |
| jansStatus            | varchar(16)  | YES  |     | None    | Status of the entry, used by many objectclasses                                                                              |
| inum                  | varchar(64)  | YES  | MUL | None    | XRI i-number                                                                                                                 |
| jansMultivaluedAttr   | smallint     | YES  |     | None    |                                                                                                                              |
| jansHideOnDiscovery   | smallint     | YES  |     | None    |                                                                                                                              |
| jansNameIdTyp         | varchar(64)  | YES  |     | None    | NameId Typ                                                                                                                   |
| jansScimCustomAttr    | smallint     | YES  |     | None    |                                                                                                                              |
| jansSourceAttr        | varchar(64)  | YES  |     | None    | Source Attr for this Attr                                                                                                    |
| seeAlso               | varchar(64)  | YES  |     | None    |                                                                                                                              |
| urn                   | varchar(128) | YES  |     | None    |                                                                                                                              |
| jansRegExp            | varchar(64)  | YES  |     | None    | Regular expression used to validate attribute data                                                                           |
| jansTooltip           | varchar(64)  | YES  |     | None    | Custom tooltip to be shown on the UI                                                                                         |
| jansValidation        | tinytext     | YES  |     | None    | This data has information about attribute Validation                                                                         |

### jansCache
| Field       | Type         | Null | Key | Default | Comment           |
| ----------- | ------------ | ---- | --- | ------- | ----------------- |
| doc_id      | varchar(64)  | NO   | PRI | None    |                   |
| objectClass | varchar(48)  | YES  |     | None    |                   |
| dn          | varchar(128) | YES  |     | None    |                   |
| uuid        | varchar(64)  | YES  |     | None    | Unique identifier |
| iat         | datetime(3)  | YES  |     | None    | jans Creation     |
| exp         | datetime(3)  | YES  |     | None    | jans Exp          |
| del         | smallint     | YES  | MUL | None    | del               |
| dat         | text         | YES  |     | None    | OX data           |

### jansCibaReq
| Field        | Type         | Null | Key | Default | Comment                                         |
| ------------ | ------------ | ---- | --- | ------- | ----------------------------------------------- |
| doc_id       | varchar(64)  | NO   | PRI | None    |                                                 |
| objectClass  | varchar(48)  | YES  |     | None    |                                                 |
| dn           | varchar(128) | YES  |     | None    |                                                 |
| authReqId    | varchar(64)  | YES  |     | None    | Authn request id                                |
| clnId        | varchar(64)  | YES  |     | None    | jans Clnt id                                    |
| usrId        | varchar(64)  | YES  |     | None    | jans user id                                    |
| creationDate | datetime(3)  | YES  |     | None    | Creation Date used for password reset requests  |
| exp          | datetime(3)  | YES  |     | None    | jans Exp                                        |
| jansStatus   | varchar(16)  | YES  | MUL | None    | Status of the entry, used by many objectclasses |

### jansClnt
| Field                                   | Type         | Null | Key | Default | Comment                                                       |
| --------------------------------------- | ------------ | ---- | --- | ------- | ------------------------------------------------------------- |
| doc_id                                  | varchar(64)  | NO   | PRI | None    |                                                               |
| objectClass                             | varchar(48)  | YES  |     | None    |                                                               |
| dn                                      | varchar(128) | YES  |     | None    |                                                               |
| o                                       | varchar(64)  | YES  |     | None    |                                                               |
| jansGrp                                 | varchar(64)  | YES  |     | None    | Group                                                         |
| displayName                             | varchar(128) | YES  | MUL | None    | preferred name of a person to be used when displaying entries |
| displayNameLocalized                    | json         | YES  |     | None    | jans Display Name Localized                                   |
| description                             | varchar(768) | YES  | MUL | None    |                                                               |
| inum                                    | varchar(64)  | YES  | MUL | None    | XRI i-number                                                  |
| jansAppTyp                              | varchar(64)  | YES  |     | None    | jans App Typ                                                  |
| jansClntIdIssuedAt                      | datetime(3)  | YES  |     | None    | jans Clnt Issued At                                           |
| jansClntSecret                          | varchar(64)  | YES  |     | None    | jans Clnt Secret                                              |
| jansClntSecretExpAt                     | datetime(3)  | YES  | MUL | None    | Date client expires                                           |
| exp                                     | datetime(3)  | YES  |     | None    | jans Exp                                                      |
| del                                     | smallint     | YES  | MUL | None    | del                                                           |
| jansClntURI                             | tinytext     | YES  |     | None    | jans Clnt URI                                                 |
| jansClntURILocalized                    | json         | YES  |     | None    | jans Clnt URI localized                                       |
| jansContact                             | json         | YES  |     | None    | jans Contact                                                  |
| jansDefAcrValues                        | json         | YES  |     | None    | jans Def Acr Values                                           |
| jansDefMaxAge                           | int          | YES  |     | None    | jans Def Max Age                                              |
| jansGrantTyp                            | json         | YES  |     | None    | jans Grant Typ                                                |
| jansIdTknEncRespAlg                     | varchar(64)  | YES  |     | None    | jans ID Tkn Enc Resp Alg                                      |
| jansIdTknEncRespEnc                     | varchar(64)  | YES  |     | None    | jans ID Tkn Enc Resp Enc                                      |
| jansIdTknSignedRespAlg                  | varchar(64)  | YES  |     | None    | jans ID Tkn Signed Resp Alg                                   |
| jansInitiateLoginURI                    | tinytext     | YES  |     | None    | jans Initiate Login URI                                       |
| jansJwksURI                             | tinytext     | YES  |     | None    | jans JWKs URI                                                 |
| jansJwks                                | text         | YES  |     | None    | jans JWKs                                                     |
| jansLogoURI                             | tinytext     | YES  |     | None    | jans Logo URI                                                 |
| jansLogoURILocalized                    | json         | YES  |     | None    | jans Logo URI localized                                       |
| jansPolicyURI                           | tinytext     | YES  |     | None    | jans Policy URI                                               |
| jansPolicyURILocalized                  | json         | YES  |     | None    | jans Policy URI localized                                     |
| jansPostLogoutRedirectURI               | json         | YES  |     | None    | jans Post Logout Redirect URI                                 |
| jansRedirectURI                         | json         | YES  |     | None    | jans Redirect URI                                             |
| jansRegistrationAccessTkn               | varchar(64)  | YES  | MUL | None    | jans Registration Access Tkn                                  |
| jansReqObjSigAlg                        | varchar(64)  | YES  |     | None    | jans Req Obj Sig Alg                                          |
| jansReqObjEncAlg                        | varchar(64)  | YES  |     | None    | jans Req Obj Enc Alg                                          |
| jansReqObjEncEnc                        | varchar(64)  | YES  |     | None    | jans Req Obj Enc Enc                                          |
| jansReqURI                              | json         | YES  |     | None    | jans Req URI                                                  |
| jansRespTyp                             | json         | YES  |     | None    | jans Resp Typ                                                 |
| jansScope                               | json         | YES  |     | None    | jans Attr Scope                                               |
| jansClaim                               | json         | YES  |     | None    | jans Attr Claim                                               |
| jansSectorIdentifierURI                 | tinytext     | YES  |     | None    | jans Sector Identifier URI                                    |
| jansSignedRespAlg                       | varchar(64)  | YES  |     | None    | jans Signed Resp Alg                                          |
| jansSubjectTyp                          | varchar(64)  | YES  |     | None    | jans Subject Typ                                              |
| jansTknEndpointAuthMethod               | varchar(64)  | YES  |     | None    | jans Tkn Endpoint Auth Method                                 |
| jansTknEndpointAuthSigAlg               | varchar(64)  | YES  |     | None    | jans Tkn Endpoint Auth Sig Alg                                |
| jansTosURI                              | tinytext     | YES  |     | None    | jans TOS URI                                                  |
| jansTosURILocalized                     | json         | YES  |     | None    | jans Tos URI localized                                        |
| jansTrustedClnt                         | smallint     | YES  |     | None    | jans Trusted Clnt                                             |
| jansUsrInfEncRespAlg                    | varchar(64)  | YES  |     | None    | jans Usr Inf Enc Resp Alg                                     |
| jansUsrInfEncRespEnc                    | varchar(64)  | YES  |     | None    | jans Usr Inf Enc Resp Enc                                     |
| jansExtraConf                           | varchar(64)  | YES  |     | None    | jans additional configuration                                 |
| jansClaimRedirectURI                    | json         | YES  |     | None    | Claim Redirect URI                                            |
| jansLastAccessTime                      | datetime(3)  | YES  |     | None    | Last access time                                              |
| jansLastLogonTime                       | datetime(3)  | YES  |     | None    | Last logon time                                               |
| jansPersistClntAuthzs                   | smallint     | YES  |     | None    | jans Persist Clnt Authzs                                      |
| jansInclClaimsInIdTkn                   | smallint     | YES  |     | None    | jans Incl Claims In Id Tkn                                    |
| jansRefreshTknLife                      | int          | YES  |     | None    | Life of refresh token                                         |
| jansDisabled                            | smallint     | YES  |     | None    | Status of client                                              |
| jansLogoutURI                           | json         | YES  |     | None    | jans Policy URI                                               |
| jansLogoutSessRequired                  | smallint     | YES  |     | None    | jans Policy URI                                               |
| jansdId                                 | varchar(64)  | YES  |     | None    | jansd Id                                                      |
| jansAuthorizedOrigins                   | json         | YES  |     | None    | jans Authorized Origins                                       |
| tknBndCnf                               | tinytext     | YES  |     | None    | jansauth - Tkn Binding Id Hash                                |
| jansAccessTknAsJwt                      | smallint     | YES  |     | None    | jansauth - indicator whether to return access token as JWT    |
| jansAccessTknSigAlg                     | varchar(64)  | YES  |     | None    | jansauth - access token signing algorithm                     |
| jansAccessTknLife                       | int          | YES  |     | None    | Life of access token                                          |
| jansSoftId                              | varchar(64)  | YES  |     | None    | Soft Identifier                                               |
| jansSoftVer                             | varchar(64)  | YES  |     | None    |                                                               |
| jansSoftStatement                       | text         | YES  |     | None    | Soft Statement                                                |
| jansRptAsJwt                            | smallint     | YES  |     | None    | jansRptAsJwt                                                  |
| jansAttrs                               | text         | YES  |     | None    | Attrs                                                         |
| jansBackchannelTknDeliveryMode          | varchar(64)  | YES  |     | None    | jans Backchannel Tkn Delivery Mode                            |
| jansBackchannelClntNotificationEndpoint | varchar(64)  | YES  |     | None    | jans Backchannel Clnt Notification Endpoint                   |
| jansBackchannelAuthnReqSigAlg           | varchar(64)  | YES  |     | None    | jans Backchannel Authn Req Sig Alg                            |
| jansBackchannelUsrCodeParameter         | smallint     | YES  |     | None    | jans Backchannel Usr Code Parameter                           |

### jansClntAuthz
| Field       | Type         | Null | Key | Default | Comment         |
| ----------- | ------------ | ---- | --- | ------- | --------------- |
| doc_id      | varchar(100) | NO   | PRI | None    |                 |
| objectClass | varchar(48)  | YES  |     | None    |                 |
| dn          | varchar(128) | YES  |     | None    |                 |
| jansId      | varchar(128) | YES  |     | None    | Identifier      |
| jansClntId  | json         | YES  |     | None    | jans Clnt id    |
| jansUsrId   | varchar(64)  | YES  | MUL | None    | jans user id    |
| exp         | datetime(3)  | YES  |     | None    | jans Exp        |
| del         | smallint     | YES  | MUL | None    | del             |
| jansScope   | json         | YES  |     | None    | jans Attr Scope |

### jansCustomScr
| Field              | Type         | Null | Key | Default | Comment                                                                  |
| ------------------ | ------------ | ---- | --- | ------- | ------------------------------------------------------------------------ |
| doc_id             | varchar(64)  | NO   | PRI | None    |                                                                          |
| objectClass        | varchar(48)  | YES  |     | None    |                                                                          |
| dn                 | varchar(128) | YES  |     | None    |                                                                          |
| inum               | varchar(64)  | YES  | MUL | None    | XRI i-number                                                             |
| displayName        | varchar(128) | YES  |     | None    | preferred name of a person to be used when displaying entries            |
| description        | varchar(768) | YES  |     | None    |                                                                          |
| jansScr            | text         | YES  |     | None    | Attr that contains script (python, java script)                          |
| jansScrTyp         | varchar(64)  | YES  | MUL | None    | Attr that contains script type (e.g. python, java script)                |
| jansProgLng        | varchar(64)  | YES  |     | None    | programming language                                                     |
| jansModuleProperty | json         | YES  |     | None    | Module property                                                          |
| jansConfProperty   | json         | YES  |     | None    | Conf property                                                            |
| jansLevel          | int          | YES  |     | None    | Level                                                                    |
| jansRevision       | int          | YES  |     | None    | Revision                                                                 |
| jansEnabled        | smallint     | YES  |     | None    | Status of the entry, used by many objectclasses                          |
| jansScrError       | text         | YES  |     | None    | Attr that contains first error which application get during it execution |
| jansAlias          | json         | YES  |     | None    | jansAlias                                                                |

### jansDeviceRegistration
| Field                      | Type         | Null | Key | Default | Comment                                                       |
| -------------------------- | ------------ | ---- | --- | ------- | ------------------------------------------------------------- |
| doc_id                     | varchar(64)  | NO   | PRI | None    |                                                               |
| objectClass                | varchar(48)  | YES  |     | None    |                                                               |
| dn                         | varchar(128) | YES  |     | None    |                                                               |
| jansId                     | varchar(128) | YES  |     | None    | Identifier                                                    |
| displayName                | varchar(128) | YES  |     | None    | preferred name of a person to be used when displaying entries |
| description                | varchar(768) | YES  |     | None    |                                                               |
| jansDeviceKeyHandle        | varchar(128) | YES  | MUL | None    | jansDeviceKeyHandle                                           |
| jansDeviceHashCode         | int          | YES  | MUL | None    | jansDeviceHashCode                                            |
| jansApp                    | varchar(96)  | YES  | MUL | None    | jansApp                                                       |
| jansDeviceRegistrationConf | text         | YES  |     | None    | jansDeviceRegistrationConf                                    |
| jansDeviceNotificationConf | varchar(64)  | YES  |     | None    | Extended push notification configuration                      |
| jansNickName               | varchar(64)  | YES  |     | None    | jansNickName                                                  |
| jansDeviceData             | tinytext     | YES  |     | None    | jansDeviceData                                                |
| jansCounter                | int          | YES  |     | None    | jansCounter                                                   |
| jansStatus                 | varchar(16)  | YES  | MUL | None    | Status of the entry, used by many objectclasses               |
| del                        | smallint     | YES  | MUL | None    | del                                                           |
| exp                        | datetime(3)  | YES  |     | None    | jans Exp                                                      |
| personInum                 | varchar(64)  | YES  | MUL | None    | Inum of a person                                              |
| creationDate               | datetime(3)  | YES  | MUL | None    | Creation Date used for password reset requests                |
| jansLastAccessTime         | datetime(3)  | YES  |     | None    | Last access time                                              |
| jansMetaLastMod            | varchar(64)  | YES  |     | None    |                                                               |
| jansMetaLocation           | tinytext     | YES  |     | None    |                                                               |
| jansMetaVer                | varchar(64)  | YES  |     | None    |                                                               |

### jansDocument
| Field              | Type         | Null | Key | Default | Comment                                                       |
| ------------------ | ------------ | ---- | --- | ------- | ------------------------------------------------------------- |
| doc_id             | varchar(64)  | NO   | PRI | None    |                                                               |
| objectClass        | varchar(48)  | YES  |     | None    |                                                               |
| dn                 | varchar(128) | YES  |     | None    |                                                               |
| inum               | varchar(64)  | YES  |     | None    | XRI i-number                                                  |
| ou                 | varchar(64)  | YES  |     | None    |                                                               |
| displayName        | varchar(128) | YES  |     | None    | preferred name of a person to be used when displaying entries |
| description        | varchar(768) | YES  |     | None    |                                                               |
| document           | varchar(64)  | YES  |     | None    | Save Document in DB                                           |
| creationDate       | datetime(3)  | YES  |     | None    | Creation Date used for password reset requests                |
| jansModuleProperty | json         | YES  |     | None    | Module property                                               |
| jansLevel          | int          | YES  |     | None    | Level                                                         |
| jansRevision       | int          | YES  |     | None    | Revision                                                      |
| jansEnabled        | smallint     | YES  |     | None    | Status of the entry, used by many objectclasses               |
| jansAlias          | json         | YES  |     | None    | jansAlias                                                     |

### jansFido2AuthnEntry
| Field                 | Type         | Null | Key | Default | Comment                                         |
| --------------------- | ------------ | ---- | --- | ------- | ----------------------------------------------- |
| doc_id                | varchar(64)  | NO   | PRI | None    |                                                 |
| objectClass           | varchar(48)  | YES  |     | None    |                                                 |
| dn                    | varchar(128) | YES  |     | None    |                                                 |
| jansId                | varchar(128) | YES  |     | None    | Identifier                                      |
| creationDate          | datetime(3)  | YES  | MUL | None    | Creation Date used for password reset requests  |
| jansApp               | varchar(96)  | YES  | MUL | None    | jansApp                                         |
| jansSessStateId       | varchar(64)  | YES  |     | None    | jansSessStateId                                 |
| jansCodeChallenge     | varchar(64)  | YES  | MUL | None    | OX PKCE code challenge                          |
| jansCodeChallengeHash | int          | YES  | MUL | None    | OX code challenge hash                          |
| personInum            | varchar(64)  | YES  | MUL | None    | Inum of a person                                |
| jansAuthData          | text         | YES  |     | None    | jansAuthData                                    |
| jansStatus            | varchar(16)  | YES  | MUL | None    | Status of the entry, used by many objectclasses |
| exp                   | datetime(3)  | YES  |     | None    | jans Exp                                        |
| del                   | smallint     | YES  | MUL | None    | del                                             |

### jansFido2RegistrationEntry
| Field                      | Type         | Null | Key | Default | Comment                                                       |
| -------------------------- | ------------ | ---- | --- | ------- | ------------------------------------------------------------- |
| doc_id                     | varchar(64)  | NO   | PRI | None    |                                                               |
| objectClass                | varchar(48)  | YES  |     | None    |                                                               |
| dn                         | varchar(128) | YES  |     | None    |                                                               |
| jansId                     | varchar(128) | YES  |     | None    | Identifier                                                    |
| creationDate               | datetime(3)  | YES  | MUL | None    | Creation Date used for password reset requests                |
| displayName                | varchar(128) | YES  |     | None    | preferred name of a person to be used when displaying entries |
| jansApp                    | varchar(96)  | YES  | MUL | None    | jansApp                                                       |
| jansSessStateId            | varchar(64)  | YES  |     | None    | jansSessStateId                                               |
| jansCodeChallenge          | varchar(64)  | YES  | MUL | None    | OX PKCE code challenge                                        |
| jansCodeChallengeHash      | int          | YES  | MUL | None    | OX code challenge hash                                        |
| jansPublicKeyId            | varchar(96)  | YES  | MUL | None    | jansPublicKeyId                                               |
| jansPublicKeyIdHash        | int          | YES  | MUL | None    | jansPublicKeyIdHash                                           |
| personInum                 | varchar(64)  | YES  | MUL | None    | Inum of a person                                              |
| jansRegistrationData       | text         | YES  |     | None    | jansRegistrationData                                          |
| jansDeviceData             | tinytext     | YES  |     | None    | jansDeviceData                                                |
| jansDeviceNotificationConf | varchar(64)  | YES  |     | None    | Extended push notification configuration                      |
| jansCounter                | int          | YES  |     | None    | jansCounter                                                   |
| jansStatus                 | varchar(16)  | YES  | MUL | None    | Status of the entry, used by many objectclasses               |
| exp                        | datetime(3)  | YES  |     | None    | jans Exp                                                      |
| del                        | smallint     | YES  | MUL | None    | del                                                           |

### jansGrant
| Field       | Type         | Null | Key | Default | Comment       |
| ----------- | ------------ | ---- | --- | ------- | ------------- |
| doc_id      | varchar(64)  | NO   | PRI | None    |               |
| objectClass | varchar(48)  | YES  |     | None    |               |
| dn          | varchar(128) | YES  |     | None    |               |
| grtId       | varchar(64)  | YES  |     | None    | jans grant id |
| iat         | datetime(3)  | YES  |     | None    | jans Creation |

### jansGrp
| Field            | Type         | Null | Key | Default | Comment                                                       |
| ---------------- | ------------ | ---- | --- | ------- | ------------------------------------------------------------- |
| doc_id           | varchar(64)  | NO   | PRI | None    |                                                               |
| objectClass      | varchar(48)  | YES  |     | None    |                                                               |
| dn               | varchar(128) | YES  |     | None    |                                                               |
| c                | varchar(2)   | YES  |     | None    |                                                               |
| description      | varchar(768) | YES  | MUL | None    |                                                               |
| displayName      | varchar(128) | YES  | MUL | None    | preferred name of a person to be used when displaying entries |
| jansStatus       | varchar(16)  | YES  |     | None    | Status of the entry, used by many objectclasses               |
| inum             | varchar(64)  | YES  | MUL | None    | XRI i-number                                                  |
| member           | json         | YES  |     | None    |                                                               |
| o                | varchar(64)  | YES  |     | None    |                                                               |
| owner            | varchar(64)  | YES  |     | None    |                                                               |
| seeAlso          | varchar(64)  | YES  |     | None    |                                                               |
| jansMetaCreated  | varchar(64)  | YES  |     | None    |                                                               |
| jansMetaLastMod  | varchar(64)  | YES  |     | None    |                                                               |
| jansMetaLocation | tinytext     | YES  |     | None    |                                                               |
| jansMetaVer      | varchar(64)  | YES  |     | None    |                                                               |

### jansInumMap
| Field                    | Type         | Null | Key | Default | Comment                                         |
| ------------------------ | ------------ | ---- | --- | ------- | ----------------------------------------------- |
| doc_id                   | varchar(64)  | NO   | PRI | None    |                                                 |
| objectClass              | varchar(48)  | YES  |     | None    |                                                 |
| dn                       | varchar(128) | YES  |     | None    |                                                 |
| jansStatus               | varchar(16)  | YES  | MUL | None    | Status of the entry, used by many objectclasses |
| inum                     | varchar(64)  | YES  | MUL | None    | XRI i-number                                    |
| jansPrimaryKeyAttrName   | varchar(64)  | YES  |     | None    | Primary Key Attribute Name                      |
| jansPrimaryKeyValue      | varchar(64)  | YES  |     | None    | Primary Key Value                               |
| jansSecondaryKeyAttrName | varchar(64)  | YES  |     | None    | Secondary Key Attribute Name                    |
| jansSecondaryKeyValue    | varchar(64)  | YES  |     | None    | Secondary Key Value                             |
| jansTertiaryKeyAttrName  | varchar(64)  | YES  |     | None    | Tertiary Key Attribute Name                     |
| jansTertiaryKeyValue     | varchar(64)  | YES  |     | None    | Tertiary Key Value                              |

### jansMetric
| Field            | Type         | Null | Key | Default | Comment                                        |
| ---------------- | ------------ | ---- | --- | ------- | ---------------------------------------------- |
| doc_id           | varchar(64)  | NO   | PRI | None    |                                                |
| objectClass      | varchar(48)  | YES  |     | None    |                                                |
| dn               | varchar(128) | YES  |     | None    |                                                |
| uniqueIdentifier | varchar(64)  | YES  |     | None    |                                                |
| jansStartDate    | datetime(3)  | YES  | MUL | None    | Start date                                     |
| jansEndDate      | datetime(3)  | YES  | MUL | None    | End date                                       |
| jansAppTyp       | varchar(64)  | YES  | MUL | None    | jans App Typ                                   |
| jansMetricTyp    | varchar(64)  | YES  | MUL | None    | Metric type                                    |
| creationDate     | datetime(3)  | YES  |     | None    | Creation Date used for password reset requests |
| del              | smallint     | YES  | MUL | None    | del                                            |
| exp              | datetime(3)  | YES  |     | None    | jans Exp                                       |
| jansData         | text         | YES  |     | None    | OX data                                        |
| jansHost         | varchar(64)  | YES  |     | None    | jans host                                      |

### jansOrganization
| Field                 | Type         | Null | Key | Default | Comment                                                                                             |
| --------------------- | ------------ | ---- | --- | ------- | --------------------------------------------------------------------------------------------------- |
| doc_id                | varchar(64)  | NO   | PRI | None    |                                                                                                     |
| objectClass           | varchar(48)  | YES  |     | None    |                                                                                                     |
| dn                    | varchar(128) | YES  |     | None    |                                                                                                     |
| c                     | varchar(2)   | YES  |     | None    |                                                                                                     |
| description           | varchar(768) | YES  |     | None    |                                                                                                     |
| displayName           | varchar(128) | YES  |     | None    | preferred name of a person to be used when displaying entries                                       |
| jansCustomMessage     | varchar(128) | YES  |     | None    | exclude custom welcome message                                                                      |
| jansFaviconImage      | varchar(64)  | YES  |     | None    | TODO - Stores URL of favicon                                                                        |
| jansLogoImage         | varchar(64)  | YES  |     | None    | Logo used by exclude for default look and feel.                                                     |
| jansManager           | varchar(64)  | YES  |     | None    | Used to specify if a person has the manager role                                                    |
| jansManagerGrp        | tinytext     | YES  |     | None    | Used in organizatoin entry to specifies the dn of the group that  has admin priviledges in exclude. |
| jansOrgShortName      | varchar(64)  | YES  |     | None    | Short description, as few letters as possible, no spaces.                                           |
| jansThemeColor        | varchar(64)  | YES  |     | None    | exclude login page configuration                                                                    |
| inum                  | varchar(64)  | YES  |     | None    | XRI i-number                                                                                        |
| l                     | varchar(64)  | YES  |     | None    |                                                                                                     |
| mail                  | varchar(96)  | YES  |     | None    |                                                                                                     |
| memberOf              | json         | YES  |     | None    |                                                                                                     |
| o                     | varchar(64)  | YES  |     | None    |                                                                                                     |
| jansCreationTimestamp | datetime(3)  | YES  |     | None    | Registration time                                                                                   |
| jansRegistrationConf  | varchar(64)  | YES  |     | None    | Registration Conf                                                                                   |
| postalCode            | varchar(16)  | YES  |     | None    |                                                                                                     |
| st                    | varchar(64)  | YES  |     | None    |                                                                                                     |
| street                | tinytext     | YES  |     | None    |                                                                                                     |
| telephoneNumber       | varchar(20)  | YES  |     | None    |                                                                                                     |
| title                 | varchar(64)  | YES  |     | None    |                                                                                                     |
| uid                   | varchar(64)  | YES  | MUL | None    |                                                                                                     |
| jansLogoPath          | varchar(64)  | YES  |     | None    | jansLogoPath                                                                                        |
| jansStatus            | varchar(16)  | YES  |     | None    | Status of the entry, used by many objectclasses                                                     |
| jansFaviconPath       | varchar(64)  | YES  |     | None    | jansFaviconPath                                                                                     |

### jansPairwiseIdentifier
| Field                | Type         | Null | Key | Default | Comment                |
| -------------------- | ------------ | ---- | --- | ------- | ---------------------- |
| doc_id               | varchar(64)  | NO   | PRI | None    |                        |
| objectClass          | varchar(48)  | YES  |     | None    |                        |
| dn                   | varchar(128) | YES  |     | None    |                        |
| jansId               | varchar(128) | YES  |     | None    | Identifier             |
| jansSectorIdentifier | varchar(64)  | YES  |     | None    | jans Sector Identifier |
| jansClntId           | json         | YES  |     | None    | jans Clnt id           |
| jansUsrId            | varchar(64)  | YES  |     | None    | jans user id           |

### jansPar
| Field       | Type         | Null | Key | Default | Comment    |
| ----------- | ------------ | ---- | --- | ------- | ---------- |
| doc_id      | varchar(64)  | NO   | PRI | None    |            |
| objectClass | varchar(48)  | YES  |     | None    |            |
| dn          | varchar(128) | YES  |     | None    |            |
| jansId      | varchar(128) | YES  | MUL | None    | Identifier |
| jansAttrs   | text         | YES  |     | None    | Attrs      |
| exp         | datetime(3)  | YES  |     | None    | jans Exp   |
| del         | smallint     | YES  | MUL | None    | del        |

### jansPassResetReq
| Field        | Type         | Null | Key | Default | Comment                                        |
| ------------ | ------------ | ---- | --- | ------- | ---------------------------------------------- |
| doc_id       | varchar(64)  | NO   | PRI | None    |                                                |
| objectClass  | varchar(48)  | YES  |     | None    |                                                |
| dn           | varchar(128) | YES  |     | None    |                                                |
| creationDate | datetime(3)  | YES  | MUL | None    | Creation Date used for password reset requests |
| jansGuid     | varchar(64)  | YES  |     | None    | A random string to mark temporary tokens       |
| personInum   | varchar(64)  | YES  |     | None    | Inum of a person                               |

### jansPerson
| Field                                | Type         | Null | Key | Default | Comment                                                                                                  |
| ------------------------------------ | ------------ | ---- | --- | ------- | -------------------------------------------------------------------------------------------------------- |
| doc_id                               | varchar(64)  | NO   | PRI | None    |                                                                                                          |
| objectClass                          | varchar(48)  | YES  |     | None    |                                                                                                          |
| dn                                   | varchar(128) | YES  |     | None    |                                                                                                          |
| jansAssociatedClnt                   | json         | YES  |     | None    | Associate the dn of an OAuth2 client with a person or UMA Resource Set.                                  |
| c                                    | varchar(2)   | YES  |     | None    |                                                                                                          |
| displayName                          | varchar(128) | YES  | MUL | None    | preferred name of a person to be used when displaying entries                                            |
| givenName                            | varchar(128) | YES  | MUL | None    |                                                                                                          |
| jansManagedOrganizations             | varchar(64)  | YES  |     | None    | Used to track with which organizations a person is associated                                            |
| jansOptOuts                          | json         | YES  |     | None    | White pages attributes restricted by person in exclude profile management                                |
| jansStatus                           | varchar(16)  | YES  |     | None    | Status of the entry, used by many objectclasses                                                          |
| inum                                 | varchar(64)  | YES  | MUL | None    | XRI i-number                                                                                             |
| mail                                 | varchar(96)  | YES  | MUL | None    |                                                                                                          |
| memberOf                             | json         | YES  |     | None    |                                                                                                          |
| o                                    | varchar(64)  | YES  |     | None    |                                                                                                          |
| jansPersistentJWT                    | json         | YES  |     | None    | jans Persistent JWT                                                                                      |
| jansCreationTimestamp                | datetime(3)  | YES  |     | None    | Registration time                                                                                        |
| jansExtUid                           | json         | YES  |     | None    |                                                                                                          |
| jansOTPCache                         | json         | YES  |     | None    | Stores a used OTP to prevent a hacker from using it again. Complementary to jansExtUid attribute         |
| jansLastLogonTime                    | datetime(3)  | YES  |     | None    | Last logon time                                                                                          |
| jansActive                           | smallint     | YES  |     | None    |                                                                                                          |
| jansAddress                          | json         | YES  |     | None    |                                                                                                          |
| jansEmail                            | json         | YES  |     | None    |                                                                                                          |
| jansEntitlements                     | json         | YES  |     | None    |                                                                                                          |
| jansExtId                            | varchar(128) | YES  |     | None    |                                                                                                          |
| jansImsValue                         | json         | YES  |     | None    |                                                                                                          |
| jansMetaCreated                      | varchar(64)  | YES  |     | None    |                                                                                                          |
| jansMetaLastMod                      | varchar(64)  | YES  |     | None    |                                                                                                          |
| jansMetaLocation                     | tinytext     | YES  |     | None    |                                                                                                          |
| jansMetaVer                          | varchar(64)  | YES  |     | None    |                                                                                                          |
| jansNameFormatted                    | tinytext     | YES  |     | None    |                                                                                                          |
| jansPhoneValue                       | json         | YES  |     | None    |                                                                                                          |
| jansPhotos                           | json         | YES  |     | None    |                                                                                                          |
| jansProfileURL                       | varchar(256) | YES  |     | None    |                                                                                                          |
| jansRole                             | json         | YES  |     | None    |                                                                                                          |
| jansTitle                            | varchar(64)  | YES  |     | None    |                                                                                                          |
| jansUsrTyp                           | varchar(64)  | YES  |     | None    |                                                                                                          |
| jansHonorificPrefix                  | varchar(64)  | YES  |     | None    |                                                                                                          |
| jansHonorificSuffix                  | varchar(64)  | YES  |     | None    |                                                                                                          |
| jans509Certificate                   | json         | YES  |     | None    |                                                                                                          |
| jansPassExpDate                      | datetime(3)  | YES  |     | None    | Pass Exp date, represented as an ISO 8601 (YYYY-MM-DD) format                                            |
| persistentId                         | varchar(64)  | YES  |     | None    | PersistentId                                                                                             |
| middleName                           | varchar(64)  | YES  |     | None    | Middle name(s)                                                                                           |
| nickname                             | varchar(64)  | YES  |     | None    | Casual name of the End-Usr                                                                               |
| jansPrefUsrName                      | varchar(64)  | YES  |     | None    | Shorthand Name                                                                                           |
| profile                              | varchar(64)  | YES  |     | None    | Profile page URL of the person                                                                           |
| picture                              | tinytext     | YES  |     | None    | Profile picture URL of the person                                                                        |
| website                              | varchar(64)  | YES  |     | None    | Web page or blog URL of the person                                                                       |
| emailVerified                        | smallint     | YES  |     | None    | True if the e-mail address of the person has been verified; otherwise false                              |
| gender                               | varchar(32)  | YES  |     | None    | Gender of the person either female or male                                                               |
| birthdate                            | datetime(3)  | YES  |     | None    | Birthday of the person, represented as an ISO 8601:2004 [ISO8601-2004] YYYY-MM-DD format                 |
| zoneinfo                             | varchar(64)  | YES  |     | None    | Time zone database representing the End-Usrs time zone. For example, Europe/Paris or America/Los_Angeles |
| locale                               | varchar(64)  | YES  |     | None    | Locale of the person, represented as a BCP47 [RFC5646] language tag                                      |
| phoneNumberVerified                  | smallint     | YES  |     | None    | True if the phone number of the person has been verified, otherwise false                                |
| address                              | tinytext     | YES  |     | None    | OpenID Connect formatted JSON object representing the address of the person                              |
| updatedAt                            | datetime(3)  | YES  |     | None    | Time the information of the person was last updated. Seconds from 1970-01-01T0:0:0Z                      |
| preferredLanguage                    | varchar(64)  | YES  |     | None    | preferred written or spoken language for a person                                                        |
| role                                 | json         | YES  |     | None    | Role                                                                                                     |
| secretAnswer                         | tinytext     | YES  |     | None    | Secret Answer                                                                                            |
| secretQuestion                       | tinytext     | YES  |     | None    | Secret Question                                                                                          |
| seeAlso                              | varchar(64)  | YES  |     | None    |                                                                                                          |
| sn                                   | varchar(128) | YES  | MUL | None    |                                                                                                          |
| cn                                   | varchar(128) | YES  |     | None    |                                                                                                          |
| transientId                          | varchar(64)  | YES  |     | None    | TransientId                                                                                              |
| uid                                  | varchar(64)  | YES  | MUL | None    |                                                                                                          |
| userPassword                         | varchar(256) | YES  |     | None    |                                                                                                          |
| st                                   | varchar(64)  | YES  |     | None    |                                                                                                          |
| street                               | tinytext     | YES  |     | None    |                                                                                                          |
| l                                    | varchar(64)  | YES  |     | None    |                                                                                                          |
| jansCountInvalidLogin                | varchar(64)  | YES  |     | None    | Invalid login attempts count                                                                             |
| jansEnrollmentCode                   | varchar(64)  | YES  |     | None    | jansEnrollmentCode                                                                                       |
| jansIMAPData                         | varchar(64)  | YES  |     | None    | This data has information about your imap connection                                                     |
| jansPPID                             | json         | YES  |     | None    | Persistent Pairwise ID for OpenID Connect                                                                |
| jansGuid                             | varchar(64)  | YES  |     | None    | A random string to mark temporary tokens                                                                 |
| jansPreferredMethod                  | varchar(64)  | YES  |     | None    | Casa - Preferred method to use for user authentication                                                   |
| userCertificate                      | blob         | YES  |     | None    |                                                                                                          |
| jansOTPDevices                       | varchar(512) | YES  |     | None    | Casa - Json representation of OTP devices. Complementary to jansExtUid attribute                         |
| jansMobileDevices                    | varchar(512) | YES  |     | None    | Casa - Json representation of mobile devices. Complementary to mobile attribute                          |
| jansTrustedDevices                   | text         | YES  |     | None    | Casa - Devices with which strong authentication may be skipped                                           |
| jansStrongAuthPolicy                 | varchar(64)  | YES  |     | None    | Casa - 2FA Enforcement Policy for User                                                                   |
| jansUnlinkedExternalUids             | varchar(64)  | YES  |     | None    | Casa - List of unlinked social accounts (ie disabled jansExtUids)                                        |
| jansBackchannelDeviceRegistrationTkn | varchar(64)  | YES  |     | None    | jans Backchannel Device Registration Tkn                                                                 |
| jansBackchannelUsrCode               | varchar(64)  | YES  |     | None    | jans Backchannel Usr Code                                                                                |
| telephoneNumber                      | varchar(20)  | YES  |     | None    |                                                                                                          |
| mobile                               | json         | YES  |     | None    |                                                                                                          |
| carLicense                           | varchar(64)  | YES  |     | None    | vehicle license or registration plate                                                                    |
| facsimileTelephoneNumber             | varchar(20)  | YES  |     | None    |                                                                                                          |
| departmentNumber                     | varchar(64)  | YES  |     | None    | identifies a department within an organization                                                           |
| employeeType                         | varchar(64)  | YES  |     | None    | type of employment for a person                                                                          |
| manager                              | tinytext     | YES  |     | None    |                                                                                                          |
| postOfficeBox                        | varchar(64)  | YES  |     | None    |                                                                                                          |
| employeeNumber                       | varchar(64)  | YES  |     | None    | numerically identifies an employee within an organization                                                |
| preferredDeliveryMethod              | varchar(50)  | YES  |     | None    |                                                                                                          |
| roomNumber                           | varchar(64)  | YES  |     | None    |                                                                                                          |
| secretary                            | tinytext     | YES  |     | None    |                                                                                                          |
| homePostalAddress                    | tinytext     | YES  |     | None    |                                                                                                          |
| postalCode                           | varchar(16)  | YES  |     | None    |                                                                                                          |
| description                          | varchar(768) | YES  |     | None    |                                                                                                          |
| title                                | varchar(64)  | YES  |     | None    |                                                                                                          |
| jansAdminUIRole                      | json         | YES  |     | None    | jansAdminUIRole                                                                                          |

### jansPushApp
| Field           | Type         | Null | Key | Default | Comment                                                       |
| --------------- | ------------ | ---- | --- | ------- | ------------------------------------------------------------- |
| doc_id          | varchar(64)  | NO   | PRI | None    |                                                               |
| objectClass     | varchar(48)  | YES  |     | None    |                                                               |
| dn              | varchar(128) | YES  |     | None    |                                                               |
| displayName     | varchar(128) | YES  |     | None    | preferred name of a person to be used when displaying entries |
| jansId          | varchar(128) | YES  |     | None    | Identifier                                                    |
| jansName        | varchar(64)  | YES  |     | None    | Name                                                          |
| jansPushAppConf | varchar(64)  | YES  |     | None    | jansPush application configuration                            |

### jansPushDevice
| Field              | Type         | Null | Key | Default | Comment                       |
| ------------------ | ------------ | ---- | --- | ------- | ----------------------------- |
| doc_id             | varchar(64)  | NO   | PRI | None    |                               |
| objectClass        | varchar(48)  | YES  |     | None    |                               |
| dn                 | varchar(128) | YES  |     | None    |                               |
| jansUsrId          | varchar(64)  | YES  |     | None    | jans user id                  |
| jansId             | varchar(128) | YES  |     | None    | Identifier                    |
| jansPushApp        | tinytext     | YES  |     | None    | jansPush application DN       |
| jansPushDeviceConf | varchar(64)  | YES  |     | None    | jansPush device configuration |
| jansTyp            | varchar(64)  | YES  |     | None    | jans type                     |

### jansRp
| Field       | Type         | Null | Key | Default | Comment    |
| ----------- | ------------ | ---- | --- | ------- | ---------- |
| doc_id      | varchar(64)  | NO   | PRI | None    |            |
| objectClass | varchar(48)  | YES  |     | None    |            |
| dn          | varchar(128) | YES  |     | None    |            |
| jansId      | varchar(128) | YES  |     | None    | Identifier |
| dat         | text         | YES  |     | None    | OX data    |

### jansScope
| Field              | Type         | Null | Key | Default | Comment                                                       |
| ------------------ | ------------ | ---- | --- | ------- | ------------------------------------------------------------- |
| doc_id             | varchar(64)  | NO   | PRI | None    |                                                               |
| objectClass        | varchar(48)  | YES  |     | None    |                                                               |
| dn                 | varchar(128) | YES  |     | None    |                                                               |
| jansDefScope       | smallint     | YES  |     | None    | Track the default scope for an custom OAuth2 Scope.           |
| description        | varchar(768) | YES  | MUL | None    |                                                               |
| displayName        | varchar(128) | YES  | MUL | None    | preferred name of a person to be used when displaying entries |
| inum               | varchar(64)  | YES  |     | None    | XRI i-number                                                  |
| jansScopeTyp       | varchar(64)  | YES  |     | None    | OX Attr Scope type                                            |
| creatorId          | varchar(64)  | YES  |     | None    | Creator Id                                                    |
| creatorTyp         | varchar(64)  | YES  |     | None    | Creator type                                                  |
| creatorAttrs       | varchar(64)  | YES  |     | None    | Creator Attrs                                                 |
| creationDate       | datetime(3)  | YES  |     | None    | Creation Date used for password reset requests                |
| jansClaim          | json         | YES  |     | None    | jans Attr Claim                                               |
| jansScrDn          | json         | YES  |     | None    | Script object DN                                              |
| jansGrpClaims      | smallint     | YES  |     | None    | jans Grp Attr Claims (true or false)                          |
| jansId             | varchar(128) | YES  | MUL | None    | Identifier                                                    |
| jansIconUrl        | varchar(64)  | YES  |     | None    | jans icon url                                                 |
| jansUmaPolicyScrDn | tinytext     | YES  |     | None    | OX policy script Dn                                           |
| jansAttrs          | text         | YES  |     | None    | Attrs                                                         |
| exp                | datetime(3)  | YES  |     | None    | jans Exp                                                      |
| del                | smallint     | YES  | MUL | None    | del                                                           |

### jansScr
| Field       | Type         | Null | Key | Default | Comment                                                   |
| ----------- | ------------ | ---- | --- | ------- | --------------------------------------------------------- |
| doc_id      | varchar(64)  | NO   | PRI | None    |                                                           |
| objectClass | varchar(48)  | YES  |     | None    |                                                           |
| dn          | varchar(128) | YES  |     | None    |                                                           |
| inum        | varchar(64)  | YES  |     | None    | XRI i-number                                              |
| jansScr     | text         | YES  |     | None    | Attr that contains script (python, java script)           |
| jansScrTyp  | varchar(64)  | YES  |     | None    | Attr that contains script type (e.g. python, java script) |

### jansSectorIdentifier
| Field           | Type         | Null | Key | Default | Comment           |
| --------------- | ------------ | ---- | --- | ------- | ----------------- |
| doc_id          | varchar(64)  | NO   | PRI | None    |                   |
| objectClass     | varchar(48)  | YES  |     | None    |                   |
| dn              | varchar(128) | YES  |     | None    |                   |
| jansId          | varchar(128) | YES  |     | None    | Identifier        |
| description     | varchar(768) | YES  |     | None    |                   |
| jansRedirectURI | json         | YES  |     | None    | jans Redirect URI |
| jansClntId      | json         | YES  |     | None    | jans Clnt id      |

### jansSessId
| Field                    | Type         | Null | Key | Default | Comment                                                                      |
| ------------------------ | ------------ | ---- | --- | ------- | ---------------------------------------------------------------------------- |
| doc_id                   | varchar(64)  | NO   | PRI | None    |                                                                              |
| objectClass              | varchar(48)  | YES  |     | None    |                                                                              |
| dn                       | varchar(128) | YES  |     | None    |                                                                              |
| jansId                   | varchar(128) | YES  |     | None    | Identifier                                                                   |
| sid                      | varchar(64)  | YES  | MUL | None    | Sess Identifier                                                              |
| creationDate             | datetime(3)  | YES  |     | None    | Creation Date used for password reset requests                               |
| exp                      | datetime(3)  | YES  |     | None    | jans Exp                                                                     |
| del                      | smallint     | YES  | MUL | None    | del                                                                          |
| jansLastAccessTime       | datetime(3)  | YES  |     | None    | Last access time                                                             |
| jansUsrDN                | varchar(128) | YES  | MUL | None    | jans Usr DN                                                                  |
| authnTime                | datetime(3)  | YES  |     | None    | jans Authn Time                                                              |
| jansState                | varchar(64)  | YES  |     | None    | jansState                                                                    |
| jansSessState            | text         | YES  |     | None    | jans Sess State                                                              |
| jansPermissionGranted    | smallint     | YES  |     | None    | jans Permission Granted                                                      |
| jansAsJwt                | smallint     | YES  |     | None    | Boolean field to indicate whether object is used as JWT                      |
| jansJwt                  | text         | YES  |     | None    | JWT representation of the object or otherwise jwt associated with the object |
| jansPermissionGrantedMap | text         | YES  |     | None    | jans Permission Granted Map                                                  |
| jansInvolvedClnts        | text         | YES  |     | None    | Involved clients                                                             |
| deviceSecret             | varchar(64)  | YES  | MUL | None    | deviceSecret                                                                 |
| jansSessAttr             | text         | YES  |     | None    | jansSessAttr                                                                 |

### jansSsa
| Field        | Type         | Null | Key | Default | Comment                                        |
| ------------ | ------------ | ---- | --- | ------- | ---------------------------------------------- |
| doc_id       | varchar(64)  | NO   | PRI | None    |                                                |
| objectClass  | varchar(48)  | YES  |     | None    |                                                |
| dn           | varchar(128) | YES  |     | None    |                                                |
| inum         | varchar(64)  | YES  |     | None    | XRI i-number                                   |
| o            | varchar(64)  | YES  |     | None    |                                                |
| jansAttrs    | text         | YES  |     | None    | Attrs                                          |
| description  | varchar(768) | YES  |     | None    |                                                |
| exp          | datetime(3)  | YES  |     | None    | jans Exp                                       |
| del          | smallint     | YES  |     | None    | del                                            |
| jansState    | varchar(64)  | YES  |     | None    | jansState                                      |
| creatorId    | varchar(64)  | YES  |     | None    | Creator Id                                     |
| creatorTyp   | varchar(64)  | YES  |     | None    | Creator type                                   |
| creationDate | datetime(3)  | YES  |     | None    | Creation Date used for password reset requests |

### jansStatEntry
| Field       | Type         | Null | Key | Default | Comment    |
| ----------- | ------------ | ---- | --- | ------- | ---------- |
| doc_id      | varchar(64)  | NO   | PRI | None    |            |
| objectClass | varchar(48)  | YES  |     | None    |            |
| dn          | varchar(128) | YES  |     | None    |            |
| jansId      | varchar(128) | YES  | MUL | None    | Identifier |
| dat         | text         | YES  |     | None    | OX data    |
| jansData    | text         | YES  |     | None    | OX data    |
| attr        | text         | YES  |     | None    | Attrs      |

### jansToken
| Field       | Type         | Null | Key | Default | Comment                        |
| ----------- | ------------ | ---- | --- | ------- | ------------------------------ |
| doc_id      | varchar(64)  | NO   | PRI | None    |                                |
| objectClass | varchar(48)  | YES  |     | None    |                                |
| dn          | varchar(128) | YES  |     | None    |                                |
| authnTime   | datetime(3)  | YES  |     | None    | jans Authn Time                |
| authzCode   | varchar(64)  | YES  | MUL | None    | jans authorization code        |
| iat         | datetime(3)  | YES  |     | None    | jans Creation                  |
| exp         | datetime(3)  | YES  |     | None    | jans Exp                       |
| del         | smallint     | YES  | MUL | None    | del                            |
| grtId       | varchar(64)  | YES  | MUL | None    | jans grant id                  |
| grtTyp      | varchar(64)  | YES  |     | None    | jans Grant Typ                 |
| jwtReq      | text         | YES  |     | None    | jans JWT Req                   |
| nnc         | text         | YES  |     | None    | jans nonce                     |
| scp         | text         | YES  |     | None    | jans Attr Scope                |
| tknCde      | varchar(80)  | YES  | MUL | None    | jans Tkn Code                  |
| tknTyp      | varchar(32)  | YES  |     | None    | jans Tkn Typ                   |
| usrId       | varchar(64)  | YES  |     | None    | jans user id                   |
| jansUsrDN   | varchar(128) | YES  |     | None    | jans Usr DN                    |
| clnId       | varchar(64)  | YES  |     | None    | jans Clnt id                   |
| acr         | varchar(48)  | YES  |     | None    |                                |
| uuid        | varchar(64)  | YES  |     | None    | Unique identifier              |
| chlng       | varchar(64)  | YES  |     | None    | OX PKCE code challenge         |
| chlngMth    | varchar(64)  | YES  |     | None    | OX PKCE code challenge method  |
| clms        | varchar(64)  | YES  |     | None    | jans Claims                    |
| ssnId       | varchar(64)  | YES  | MUL | None    | jans Sess DN                   |
| attr        | text         | YES  |     | None    | Attrs                          |
| tknBndCnf   | tinytext     | YES  |     | None    | jansauth - Tkn Binding Id Hash |
| dpop        | varchar(64)  | YES  |     | None    | DPoP Proof                     |

### jansU2fReq
| Field           | Type         | Null | Key | Default | Comment                                        |
| --------------- | ------------ | ---- | --- | ------- | ---------------------------------------------- |
| doc_id          | varchar(64)  | NO   | PRI | None    |                                                |
| objectClass     | varchar(48)  | YES  |     | None    |                                                |
| dn              | varchar(128) | YES  |     | None    |                                                |
| jansId          | varchar(128) | YES  |     | None    | Identifier                                     |
| jansReqId       | varchar(64)  | YES  |     | None    | jansReqId                                      |
| jansReq         | text         | YES  |     | None    | jansReq                                        |
| jansSessStateId | varchar(64)  | YES  |     | None    | jansSessStateId                                |
| del             | smallint     | YES  | MUL | None    | del                                            |
| exp             | datetime(3)  | YES  |     | None    | jans Exp                                       |
| personInum      | varchar(64)  | YES  |     | None    | Inum of a person                               |
| creationDate    | datetime(3)  | YES  | MUL | None    | Creation Date used for password reset requests |

### jansUmaPCT
| Field           | Type         | Null | Key | Default | Comment                        |
| --------------- | ------------ | ---- | --- | ------- | ------------------------------ |
| doc_id          | varchar(64)  | NO   | PRI | None    |                                |
| objectClass     | varchar(48)  | YES  |     | None    |                                |
| dn              | varchar(128) | YES  |     | None    |                                |
| clnId           | varchar(64)  | YES  |     | None    | jans Clnt id                   |
| iat             | datetime(3)  | YES  |     | None    | jans Creation                  |
| exp             | datetime(3)  | YES  |     | None    | jans Exp                       |
| del             | smallint     | YES  | MUL | None    | del                            |
| tknCde          | varchar(80)  | YES  | MUL | None    | jans Tkn Code                  |
| ssnId           | varchar(64)  | YES  |     | None    | jans Sess DN                   |
| jansClaimValues | varchar(64)  | YES  |     | None    | Claim Values                   |
| dpop            | varchar(64)  | YES  |     | None    | DPoP Proof                     |
| authnTime       | datetime(3)  | YES  |     | None    | jans Authn Time                |
| authzCode       | varchar(64)  | YES  |     | None    | jans authorization code        |
| grtId           | varchar(64)  | YES  |     | None    | jans grant id                  |
| grtTyp          | varchar(64)  | YES  |     | None    | jans Grant Typ                 |
| jwtReq          | text         | YES  |     | None    | jans JWT Req                   |
| nnc             | text         | YES  |     | None    | jans nonce                     |
| scp             | text         | YES  |     | None    | jans Attr Scope                |
| tknTyp          | varchar(32)  | YES  |     | None    | jans Tkn Typ                   |
| usrId           | varchar(64)  | YES  |     | None    | jans user id                   |
| jansUsrDN       | varchar(128) | YES  |     | None    | jans Usr DN                    |
| acr             | varchar(48)  | YES  |     | None    |                                |
| uuid            | varchar(64)  | YES  |     | None    | Unique identifier              |
| chlng           | varchar(64)  | YES  |     | None    | OX PKCE code challenge         |
| chlngMth        | varchar(64)  | YES  |     | None    | OX PKCE code challenge method  |
| clms            | varchar(64)  | YES  |     | None    | jans Claims                    |
| attr            | text         | YES  |     | None    | Attrs                          |
| tknBndCnf       | tinytext     | YES  |     | None    | jansauth - Tkn Binding Id Hash |

### jansUmaRPT
| Field             | Type         | Null | Key | Default | Comment                        |
| ----------------- | ------------ | ---- | --- | ------- | ------------------------------ |
| doc_id            | varchar(64)  | NO   | PRI | None    |                                |
| objectClass       | varchar(48)  | YES  |     | None    |                                |
| dn                | varchar(128) | YES  |     | None    |                                |
| authnTime         | datetime(3)  | YES  |     | None    | jans Authn Time                |
| clnId             | varchar(64)  | YES  |     | None    | jans Clnt id                   |
| iat               | datetime(3)  | YES  |     | None    | jans Creation                  |
| exp               | datetime(3)  | YES  |     | None    | jans Exp                       |
| del               | smallint     | YES  |     | None    | del                            |
| tknCde            | varchar(80)  | YES  |     | None    | jans Tkn Code                  |
| usrId             | varchar(64)  | YES  |     | None    | jans user id                   |
| ssnId             | varchar(64)  | YES  |     | None    | jans Sess DN                   |
| jansUmaPermission | json         | YES  |     | None    | jans uma permission            |
| uuid              | varchar(64)  | YES  |     | None    | Unique identifier              |
| dpop              | varchar(64)  | YES  |     | None    | DPoP Proof                     |
| authzCode         | varchar(64)  | YES  |     | None    | jans authorization code        |
| grtId             | varchar(64)  | YES  |     | None    | jans grant id                  |
| grtTyp            | varchar(64)  | YES  |     | None    | jans Grant Typ                 |
| jwtReq            | text         | YES  |     | None    | jans JWT Req                   |
| nnc               | text         | YES  |     | None    | jans nonce                     |
| scp               | text         | YES  |     | None    | jans Attr Scope                |
| tknTyp            | varchar(32)  | YES  |     | None    | jans Tkn Typ                   |
| jansUsrDN         | varchar(128) | YES  |     | None    | jans Usr DN                    |
| acr               | varchar(48)  | YES  |     | None    |                                |
| chlng             | varchar(64)  | YES  |     | None    | OX PKCE code challenge         |
| chlngMth          | varchar(64)  | YES  |     | None    | OX PKCE code challenge method  |
| clms              | varchar(64)  | YES  |     | None    | jans Claims                    |
| attr              | text         | YES  |     | None    | Attrs                          |
| tknBndCnf         | tinytext     | YES  |     | None    | jansauth - Tkn Binding Id Hash |

### jansUmaResource
| Field               | Type         | Null | Key | Default | Comment                                                                 |
| ------------------- | ------------ | ---- | --- | ------- | ----------------------------------------------------------------------- |
| doc_id              | varchar(64)  | NO   | PRI | None    |                                                                         |
| objectClass         | varchar(48)  | YES  |     | None    |                                                                         |
| dn                  | varchar(128) | YES  |     | None    |                                                                         |
| displayName         | varchar(128) | YES  | MUL | None    | preferred name of a person to be used when displaying entries           |
| inum                | varchar(64)  | YES  |     | None    | XRI i-number                                                            |
| owner               | varchar(64)  | YES  |     | None    |                                                                         |
| jansAssociatedClnt  | json         | YES  |     | None    | Associate the dn of an OAuth2 client with a person or UMA Resource Set. |
| jansUmaScope        | varchar(768) | YES  | MUL | None    | URI reference of scope descriptor                                       |
| jansFaviconImage    | varchar(64)  | YES  |     | None    | TODO - Stores URL of favicon                                            |
| jansGrp             | varchar(64)  | YES  |     | None    | Group                                                                   |
| jansId              | varchar(128) | YES  | MUL | None    | Identifier                                                              |
| jansResource        | tinytext     | YES  |     | None    | Host path                                                               |
| jansRevision        | int          | YES  |     | None    | Revision                                                                |
| jansTyp             | varchar(64)  | YES  |     | None    | jans type                                                               |
| jansScopeExpression | text         | YES  |     | None    | Scope expression                                                        |
| iat                 | datetime(3)  | YES  |     | None    | jans Creation                                                           |
| exp                 | datetime(3)  | YES  |     | None    | jans Exp                                                                |
| del                 | smallint     | YES  | MUL | None    | del                                                                     |
| description         | varchar(768) | YES  |     | None    |                                                                         |

### jansUmaResourcePermission
| Field             | Type         | Null | Key | Default | Comment                                         |
| ----------------- | ------------ | ---- | --- | ------- | ----------------------------------------------- |
| doc_id            | varchar(64)  | NO   | PRI | None    |                                                 |
| objectClass       | varchar(48)  | YES  |     | None    |                                                 |
| dn                | varchar(128) | YES  |     | None    |                                                 |
| exp               | datetime(3)  | YES  |     | None    | jans Exp                                        |
| del               | smallint     | YES  |     | None    | del                                             |
| jansUmaScope      | varchar(768) | YES  |     | None    | URI reference of scope descriptor               |
| jansConfCode      | varchar(64)  | YES  |     | None    | jans configuration code                         |
| jansResourceSetId | varchar(64)  | YES  |     | None    | jans resource set id                            |
| jansAttrs         | text         | YES  |     | None    | Attrs                                           |
| jansTicket        | varchar(64)  | YES  | MUL | None    | jans ticket                                     |
| jansStatus        | varchar(16)  | YES  |     | None    | Status of the entry, used by many objectclasses |
