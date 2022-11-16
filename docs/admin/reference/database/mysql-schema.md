---
tags:
  - administration
  - reference
  - database
---

## Tables

| Table names           |
|-|
| agmFlow                    |
| agmFlowRun                 |
| jansAdminConfDyn           |
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

### agmFlow

| Field             | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id            | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass       | varchar(48)  | YES  |     | NULL    |       |
| dn                | varchar(128) | YES  |     | NULL    |       |
| agFlowQname       | varchar(64)  | YES  |     | NULL    |       |
| agFlowMeta        | text         | YES  |     | NULL    |       |
| jansScr           | text         | YES  |     | NULL    |       |
| jansEnabled       | smallint     | YES  |     | NULL    |       |
| jansScrError      | text         | YES  |     | NULL    |       |
| agFlowTrans       | text         | YES  |     | NULL    |       |
| jansRevision      | int          | YES  |     | NULL    |       |
| jansCustomMessage | varchar(128) | YES  |     | NULL    |       |

### agmFlowRun


| Field             | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id            | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass       | varchar(48)  | YES  |     | NULL    |       |
| dn                | varchar(128) | YES  |     | NULL    |       |
| jansId            | varchar(128) | YES  |     | NULL    |       |
| agFlowSt          | text         | YES  |     | NULL    |       |
| agFlowEncCont     | mediumtext   | YES  |     | NULL    |       |
| jansCustomMessage | varchar(128) | YES  |     | NULL    |       |
| exp               | datetime(3)  | YES  |     | NULL    |       |

### jansAdminConfDyn

| Field        | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id       | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass  | varchar(48)  | YES  |     | NULL    |       |
| dn           | varchar(128) | YES  |     | NULL    |       |
| c            | varchar(2)   | YES  |     | NULL    |       |
| ou           | varchar(64)  | YES  |     | NULL    |       |
| description  | varchar(768) | YES  |     | NULL    |       |
| inum         | varchar(64)  | YES  |     | NULL    |       |
| displayName  | varchar(128) | YES  |     | NULL    |       |
| jansConfDyn  | text         | YES  |     | NULL    |       |
| o            | varchar(64)  | YES  |     | NULL    |       |
| jansRevision | int          | YES  |     | NULL    |       |

### jansAppConf

| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id                | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass           | varchar(48)  | YES  |     | NULL    |       |
| dn                    | varchar(128) | YES  |     | NULL    |       |
| c                     | varchar(2)   | YES  |     | NULL    |       |
| ou                    | varchar(64)  | YES  |     | NULL    |       |
| description           | varchar(768) | YES  |     | NULL    |       |
| displayName           | varchar(128) | YES  |     | NULL    |       |
| jansHostname          | varchar(64)  | YES  |     | NULL    |       |
| jansLastUpd           | datetime(3)  | YES  |     | NULL    |       |
| jansManager           | varchar(64)  | YES  |     | NULL    |       |
| jansOrgProfileMgt     | smallint     | YES  |     | NULL    |       |
| jansScimEnabled       | smallint     | YES  |     | NULL    |       |
| jansEmail             | json         | YES  |     | NULL    |       |
| jansSmtpConf          | json         | YES  |     | NULL    |       |
| jansSslExpiry         | varchar(64)  | YES  |     | NULL    |       |
| jansStatus            | varchar(16)  | YES  |     | NULL    |       |
| jansUrl               | varchar(64)  | YES  |     | NULL    |       |
| inum                  | varchar(64)  | YES  |     | NULL    |       |
| o                     | varchar(64)  | YES  |     | NULL    |       |
| jansAuthMode          | varchar(64)  | YES  |     | NULL    |       |
| jansDbAuth            | json         | YES  |     | NULL    |       |
| jansLogViewerConfig   | varchar(64)  | YES  |     | NULL    |       |
| jansLogConfigLocation | varchar(64)  | YES  |     | NULL    |       |
| jansCacheConf         | text         | YES  |     | NULL    |       |
| jansDocStoreConf      | text         | YES  |     | NULL    |       |
| jansSoftVer           | varchar(64)  | YES  |     | NULL    |       |
| userPassword          | varchar(256) | YES  |     | NULL    |       |
| jansConfDyn           | text         | YES  |     | NULL    |       |
| jansConfErrors        | text         | YES  |     | NULL    |       |
| jansConfStatic        | text         | YES  |     | NULL    |       |
| jansConfWebKeys       | text         | YES  |     | NULL    |       |
| jansWebKeysSettings   | varchar(64)  | YES  |     | NULL    |       |
| jansConfApp           | text         | YES  |     | NULL    |       |
| jansRevision          | int          | YES  |     | NULL    |       |


###  jansAttr       

| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id                | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass           | varchar(48)  | YES  |     | NULL    |       |
| dn                    | varchar(128) | YES  |     | NULL    |       |
| description           | varchar(768) | YES  | MUL | NULL    |       |
| displayName           | varchar(128) | YES  | MUL | NULL    |       |
| jansAttrEditTyp       | json         | YES  |     | NULL    |       |
| jansAttrName          | varchar(64)  | YES  | MUL | NULL    |       |
| jansAttrOrigin        | varchar(64)  | YES  | MUL | NULL    |       |
| jansAttrSystemEditTyp | varchar(64)  | YES  |     | NULL    |       |
| jansAttrTyp           | varchar(64)  | YES  |     | NULL    |       |
| jansClaimName         | varchar(64)  | YES  |     | NULL    |       |
| jansAttrUsgTyp        | varchar(64)  | YES  |     | NULL    |       |
| jansAttrViewTyp       | json         | YES  |     | NULL    |       |
| jansSAML1URI          | varchar(64)  | YES  |     | NULL    |       |
| jansSAML2URI          | varchar(64)  | YES  |     | NULL    |       |
| jansStatus            | varchar(16)  | YES  |     | NULL    |       |
| inum                  | varchar(64)  | YES  | MUL | NULL    |       |
| jansMultivaluedAttr   | smallint     | YES  |     | NULL    |       |
| jansHideOnDiscovery   | smallint     | YES  |     | NULL    |       |
| jansNameIdTyp         | varchar(64)  | YES  |     | NULL    |       |
| jansScimCustomAttr    | smallint     | YES  |     | NULL    |       |
| jansSourceAttr        | varchar(64)  | YES  |     | NULL    |       |
| seeAlso               | varchar(64)  | YES  |     | NULL    |       |
| urn                   | varchar(128) | YES  |     | NULL    |       |
| jansRegExp            | varchar(64)  | YES  |     | NULL    |       |
| jansTooltip           | varchar(64)  | YES  |     | NULL    |       |
| jansValidation        | tinytext     | YES  |     | NULL    |       |

            
### jansCache                  

| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id      | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass | varchar(48)  | YES  |     | NULL    |       |
| dn          | varchar(128) | YES  |     | NULL    |       |
| uuid        | varchar(64)  | YES  |     | NULL    |       |
| iat         | datetime(3)  | YES  |     | NULL    |       |
| exp         | datetime(3)  | YES  |     | NULL    |       |
| del         | smallint     | YES  | MUL | NULL    |       |
| dat         | text         | YES  |     | NULL    |       |

### jansCibaReq             
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id       | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass  | varchar(48)  | YES  |     | NULL    |       |
| dn           | varchar(128) | YES  |     | NULL    |       |
| authReqId    | varchar(64)  | YES  |     | NULL    |       |
| clnId        | json         | YES  |     | NULL    |       |
| usrId        | varchar(64)  | YES  |     | NULL    |       |
| creationDate | datetime(3)  | YES  |     | NULL    |       |
| exp          | datetime(3)  | YES  |     | NULL    |       |
| jansStatus   | varchar(16)  | YES  | MUL | NULL    |       |
   
### jansClnt                   
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
|doc_id                                  | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass                             | varchar(48)  | YES  |     | NULL    |       |
| dn                                      | varchar(128) | YES  |     | NULL    |       |
| o                                       | varchar(64)  | YES  |     | NULL    |       |
| jansGrp                                 | varchar(64)  | YES  |     | NULL    |       |
| displayName                             | varchar(128) | YES  | MUL | NULL    |       |
| displayNameLocalized                    | json         | YES  |     | NULL    |       |
| description                             | varchar(768) | YES  | MUL | NULL    |       |
| inum                                    | varchar(64)  | YES  | MUL | NULL    |       |
| jansAppTyp                              | varchar(64)  | YES  |     | NULL    |       |
| jansClntIdIssuedAt                      | datetime(3)  | YES  |     | NULL    |       |
| jansClntSecret                          | varchar(64)  | YES  |     | NULL    |       |
| jansClntSecretExpAt                     | datetime(3)  | YES  | MUL | NULL    |       |
| exp                                     | datetime(3)  | YES  |     | NULL    |       |
| del                                     | smallint     | YES  | MUL | NULL    |       |
| jansClntURI                             | tinytext     | YES  |     | NULL    |       |
| jansClntURILocalized                    | json         | YES  |     | NULL    |       |
| jansContact                             | json         | YES  |     | NULL    |       |
| jansDefAcrValues                        | json         | YES  |     | NULL    |       |
| jansDefMaxAge                           | int          | YES  |     | NULL    |       |
| jansGrantTyp                            | json         | YES  |     | NULL    |       |
| jansIdTknEncRespAlg                     | varchar(64)  | YES  |     | NULL    |       |
| jansIdTknEncRespEnc                     | varchar(64)  | YES  |     | NULL    |       |
| jansIdTknSignedRespAlg                  | varchar(64)  | YES  |     | NULL    |       |
| jansInitiateLoginURI                    | tinytext     | YES  |     | NULL    |       |
| jansJwksURI                             | tinytext     | YES  |     | NULL    |       |
| jansJwks                                | text         | YES  |     | NULL    |       |
| jansLogoURI                             | tinytext     | YES  |     | NULL    |       |
| jansLogoURILocalized                    | json         | YES  |     | NULL    |       |
| jansPolicyURI                           | tinytext     | YES  |     | NULL    |       |
| jansPolicyURILocalized
| jansPostLogoutRedirectURI               | json         | YES  |     | NULL    |       |
| jansRedirectURI                         | json         | YES  |     | NULL    |       |
| jansRegistrationAccessTkn               | varchar(64)  | YES  | MUL | NULL    |       |
| jansReqObjSigAlg                        | varchar(64)  | YES  |     | NULL    |       |
| jansReqObjEncAlg                        | varchar(64)  | YES  |     | NULL    |       |
| jansReqObjEncEnc                        | varchar(64)  | YES  |     | NULL    |       |
| jansReqURI                              | json         | YES  |     | NULL    |       |
| jansRespTyp                             | json         | YES  |     | NULL    |       |
| jansScope                               | json         | YES  |     | NULL    |       |
| jansClaim                               | json         | YES  |     | NULL    |       |
| jansSectorIdentifierURI                 | tinytext     | YES  |     | NULL    |       |
| jansSignedRespAlg                       | varchar(64)  | YES  |     | NULL    |       |
| jansSubjectTyp                          | varchar(64)  | YES  |     | NULL    |       |
| jansTknEndpointAuthMethod               | varchar(64)  | YES  |     | NULL    |       |
| jansTknEndpointAuthSigAlg               | varchar(64)  | YES  |     | NULL    |       |
| jansTosURI                              | tinytext     | YES  |     | NULL    |       |
| jansTosURILocalized                     | json         | YES  |     | NULL    |       |
| jansTrustedClnt                         | smallint     | YES  |     | NULL    |       |
| jansUsrInfEncRespAlg                    | varchar(64)  | YES  |     | NULL    |       |
| jansUsrInfEncRespEnc                    | varchar(64)  | YES  |     | NULL    |       |
| jansExtraConf                           | varchar(64)  | YES  |     | NULL    |       |
| jansClaimRedirectURI                    | json         | YES  |     | NULL    |       |
| jansLastAccessTime                      | datetime(3)  | YES  |     | NULL    |       |
| jansLastLogonTime                       | datetime(3)  | YES  |     | NULL    |       |
| jansPersistClntAuthzs                   | smallint     | YES  |     | NULL    |       |
| jansInclClaimsInIdTkn                   | smallint     | YES  |     | NULL    |       |
| jansRefreshTknLife                      | int          | YES  |     | NULL    |       |
| jansDisabled                            | smallint     | YES  |     | NULL    |       |
| jansLogoutURI                           | json         | YES  |     | NULL    |       |
| jansLogoutSessRequired                  | smallint     | YES  |     | NULL    |       |
| jansdId                                 | varchar(64)  | YES  |     | NULL    |       |
| jansAuthorizedOrigins                   | json         | YES  |     | NULL    |       |
| tknBndCnf                               | tinytext     | YES  |     | NULL    |       |
| jansAccessTknAsJwt                      | smallint     | YES  |     | NULL    |       |
| jansAccessTknSigAlg                     | varchar(64)  | YES  |     | NULL    |       |
| jansAccessTknLife                       | int          | YES  |     | NULL    |       |
| jansSoftId                              | varchar(64)  | YES  |     | NULL    |       |
| jansSoftVer                             | varchar(64)  | YES  |     | NULL    |       |
| jansSoftStatement                       | text         | YES  |     | NULL    |       |
| jansRptAsJwt                            | smallint     | YES  |     | NULL    |       |
| jansAttrs                               | text         | YES  |     | NULL    |       |
| jansBackchannelTknDeliveryMode          | varchar(64)  | YES  |     | NULL    |       |
| jansBackchannelClntNotificationEndpoint | varchar(64)  | YES  |     | NULL    |       |
| jansBackchannelAuthnReqSigAlg           | varchar(64)  | YES  |     | NULL    |       |
| jansBackchannelUsrCodeParameter         | smallint     | YES  |     | NULL    |       |

### jansClntAuthz              
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id      | varchar(100) | NO   | PRI | NULL    |       |
| objectClass | varchar(48)  | YES  |     | NULL    |       |
| dn          | varchar(128) | YES  |     | NULL    |       |
| jansId      | varchar(128) | YES  |     | NULL    |       |
| jansClntId  | json         | YES  |     | NULL    |       |
| jansUsrId   | varchar(64)  | YES  | MUL | NULL    |       |
| exp         | datetime(3)  | YES  |     | NULL    |       |
| del         | smallint     | YES  | MUL | NULL    |       |
| jansScope   | json         | YES  |     | NULL    |       |

### jansCustomScr              
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id             | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass        | varchar(48)  | YES  |     | NULL    |       |
| dn                 | varchar(128) | YES  |     | NULL    |       |
| inum               | varchar(64)  | YES  | MUL | NULL    |       |
| displayName        | varchar(128) | YES  |     | NULL    |       |
| description        | varchar(768) | YES  |     | NULL    |       |
| jansScr            | text         | YES  |     | NULL    |       |
| jansScrTyp         | varchar(64)  | YES  | MUL | NULL    |       |
| jansProgLng        | varchar(64)  | YES  |     | NULL    |       |
| jansModuleProperty | json         | YES  |     | NULL    |       |
| jansConfProperty   | json         | YES  |     | NULL    |       |
| jansLevel          | int          | YES  |     | NULL    |       |
| jansRevision       | int          | YES  |     | NULL    |       |
| jansEnabled        | smallint     | YES  |     | NULL    |       |
| jansScrError       | text         | YES  |     | NULL    |       |
| jansAlias          | json         | YES  |     | NULL    |       |

### jansDeviceRegistration     
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id                     | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass                | varchar(48)  | YES  |     | NULL    |       |
| dn                         | varchar(128) | YES  |     | NULL    |       |
| jansId                     | varchar(128) | YES  |     | NULL    |       |
| displayName                | varchar(128) | YES  |     | NULL    |       |
| description                | varchar(768) | YES  |     | NULL    |       |
| jansDeviceKeyHandle        | varchar(128) | YES  | MUL | NULL    |       |
| jansDeviceHashCode         | int          | YES  | MUL | NULL    |       |
| jansApp                    | varchar(64)  | YES  | MUL | NULL    |       |
| jansDeviceRegistrationConf | text         | YES  |     | NULL    |       |
| jansDeviceNotificationConf | varchar(64)  | YES  |     | NULL    |       |
| jansNickName               | varchar(64)  | YES  |     | NULL    |       |
| jansDeviceData             | tinytext     | YES  |     | NULL    |       |
| jansCounter                | int          | YES  |     | NULL    |       |
| jansStatus                 | varchar(16)  | YES  | MUL | NULL    |       |
| del                        | smallint     | YES  | MUL | NULL    |       |
| exp                        | datetime(3)  | YES  |     | NULL    |       |
| personInum                 | varchar(64)  | YES  | MUL | NULL    |       |
| creationDate               | datetime(3)  | YES  | MUL | NULL    |       |
| jansLastAccessTime         | datetime(3)  | YES  |     | NULL    |       |
| jansMetaLastMod            | varchar(64)  | YES  |     | NULL    |       |
| jansMetaLocation           | tinytext     | YES  |     | NULL    |       |
| jansMetaVer                | varchar(64)  | YES  |     | NULL    |       |

### jansDocument               
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id             | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass        | varchar(48)  | YES  |     | NULL    |       |
| dn                 | varchar(128) | YES  |     | NULL    |       |
| inum               | varchar(64)  | YES  |     | NULL    |       |
| ou                 | varchar(64)  | YES  |     | NULL    |       |
| displayName        | varchar(128) | YES  |     | NULL    |       |
| description        | varchar(768) | YES  |     | NULL    |       |
| document           | varchar(64)  | YES  |     | NULL    |       |
| creationDate       | datetime(3)  | YES  |     | NULL    |       |
| jansModuleProperty | json         | YES  |     | NULL    |       |
| jansLevel          | int          | YES  |     | NULL    |       |
| jansRevision       | int          | YES  |     | NULL    |       |
| jansEnabled        | smallint     | YES  |     | NULL    |       |
| jansAlias          | json         | YES  |     | NULL    |       |

### jansFido2AuthnEntry        
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id            | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass       | varchar(48)  | YES  |     | NULL    |       |
| dn                | varchar(128) | YES  |     | NULL    |       |
| jansId            | varchar(128) | YES  |     | NULL    |       |
| creationDate      | datetime(3)  | YES  | MUL | NULL    |       |
| jansSessStateId   | varchar(64)  | YES  |     | NULL    |       |
| jansCodeChallenge | varchar(64)  | YES  |     | NULL    |       |
| personInum        | varchar(64)  | YES  | MUL | NULL    |       |
| jansAuthData      | text         | YES  |     | NULL    |       |
| jansStatus        | varchar(16)  | YES  | MUL | NULL    |       |

### jansFido2RegistrationEntry 
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id                     | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass                | varchar(48)  | YES  |     | NULL    |       |
| dn                         | varchar(128) | YES  |     | NULL    |       |
| jansId                     | varchar(128) | YES  |     | NULL    |       |
| creationDate               | datetime(3)  | YES  | MUL | NULL    |       |
| displayName                | varchar(128) | YES  |     | NULL    |       |
| jansSessStateId            | varchar(64)  | YES  |     | NULL    |       |
| jansCodeChallenge          | varchar(64)  | YES  |     | NULL    |       |
| jansCodeChallengeHash      | int          | YES  |     | NULL    |       |
| jansPublicKeyId            | varchar(96)  | YES  |     | NULL    |       |
| personInum                 | varchar(64)  | YES  | MUL | NULL    |       |
| jansRegistrationData       | text         | YES  |     | NULL    |       |
| jansDeviceNotificationConf | varchar(64)  | YES  |     | NULL    |       |
| jansCounter                | int          | YES  |     | NULL    |       |
| jansStatus                 | varchar(16)  | YES  | MUL | NULL    |       |

### jansGrant                  
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id      | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass | varchar(48)  | YES  |     | NULL    |       |
| dn          | varchar(128) | YES  |     | NULL    |       |
| grtId       | varchar(64)  | YES  |     | NULL    |       |
| iat         | datetime(3)  | YES  |     | NULL    |       |

### jansGrp                    
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id           | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass      | varchar(48)  | YES  |     | NULL    |       |
| dn               | varchar(128) | YES  |     | NULL    |       |
| c                | varchar(2)   | YES  |     | NULL    |       |
| description      | varchar(768) | YES  | MUL | NULL    |       |
| displayName      | varchar(128) | YES  | MUL | NULL    |       |
| jansStatus       | varchar(16)  | YES  |     | NULL    |       |
| inum             | varchar(64)  | YES  | MUL | NULL    |       |
| member           | json         | YES  |     | NULL    |       |
| o                | varchar(64)  | YES  |     | NULL    |       |
| owner            | varchar(64)  | YES  |     | NULL    |       |
| seeAlso          | varchar(64)  | YES  |     | NULL    |       |
| jansMetaCreated  | varchar(64)  | YES  |     | NULL    |       |
| jansMetaLastMod  | varchar(64)  | YES  |     | NULL    |       |
| jansMetaLocation | tinytext     | YES  |     | NULL    |       |
| jansMetaVer      | varchar(64)  | YES  |     | NULL    |       |

### jansInumMap                
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id                   | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass              | varchar(48)  | YES  |     | NULL    |       |
| dn                       | varchar(128) | YES  |     | NULL    |       |
| jansStatus               | varchar(16)  | YES  | MUL | NULL    |       |
| inum                     | varchar(64)  | YES  | MUL | NULL    |       |
| jansPrimaryKeyAttrName   | varchar(64)  | YES  |     | NULL    |       |
| jansPrimaryKeyValue      | varchar(64)  | YES  |     | NULL    |       |
| jansSecondaryKeyAttrName | varchar(64)  | YES  |     | NULL    |       |
| jansSecondaryKeyValue    | varchar(64)  | YES  |     | NULL    |       |
| jansTertiaryKeyAttrName  | varchar(64)  | YES  |     | NULL    |       |
| jansTertiaryKeyValue     | varchar(64)  | YES  |     | NULL    |       |

### jansMetric                 
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id           | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass      | varchar(48)  | YES  |     | NULL    |       |
| dn               | varchar(128) | YES  |     | NULL    |       |
| uniqueIdentifier | varchar(64)  | YES  |     | NULL    |       |
| jansStartDate    | datetime(3)  | YES  | MUL | NULL    |       |
| jansEndDate      | datetime(3)  | YES  | MUL | NULL    |       |
| jansAppTyp       | varchar(64)  | YES  | MUL | NULL    |       |
| jansMetricTyp    | varchar(64)  | YES  | MUL | NULL    |       |
| creationDate     | datetime(3)  | YES  |     | NULL    |       |
| del              | smallint     | YES  | MUL | NULL    |       |
| exp              | datetime(3)  | YES  |     | NULL    |       |
| jansData         | text         | YES  |     | NULL    |       |
| jansHost         | varchar(64)  | YES  |     | NULL    |       |

### jansOrganization           
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id                | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass           | varchar(48)  | YES  |     | NULL    |       |
| dn                    | varchar(128) | YES  |     | NULL    |       |
| c                     | varchar(2)   | YES  |     | NULL    |       |
| county                | varchar(64)  | YES  |     | NULL    |       |
| description           | varchar(768) | YES  |     | NULL    |       |
| displayName           | varchar(128) | YES  |     | NULL    |       |
| jansCustomMessage     | varchar(128) | YES  |     | NULL    |       |
| jansFaviconImage      | varchar(64)  | YES  |     | NULL    |       |
| jansLogoImage         | varchar(64)  | YES  |     | NULL    |       |
| jansManager           | varchar(64)  | YES  |     | NULL    |       |
| jansManagerGrp        | tinytext     | YES  |     | NULL    |       |
| jansOrgShortName      | varchar(64)  | YES  |     | NULL    |       |
| jansThemeColor        | varchar(64)  | YES  |     | NULL    |       |
| inum                  | varchar(64)  | YES  |     | NULL    |       |
| l                     | varchar(64)  | YES  |     | NULL    |       |
| mail                  | varchar(96)  | YES  |     | NULL    |       |
| memberOf              | json         | YES  |     | NULL    |       |
| o                     | varchar(64)  | YES  |     | NULL    |       |
| jansCreationTimestamp | datetime(3)  | YES  |     | NULL    |       |
| jansRegistrationConf  | varchar(64)  | YES  |     | NULL    |       |
| postalCode            | varchar(16)  | YES  |     | NULL    |       |
| st                    | varchar(64)  | YES  |     | NULL    |       |
| street                | tinytext     | YES  |     | NULL    |       |
| telephoneNumber       | varchar(20)  | YES  |     | NULL    |       |
| title                 | varchar(64)  | YES  |     | NULL    |       |
| uid                   | varchar(64)  | YES  | MUL | NULL    |       |
| jansLogoPath          | varchar(64)  | YES  |     | NULL    |       |
| jansStatus            | varchar(16)  | YES  |     | NULL    |       |
| jansFaviconPath       | varchar(64)  | YES  |     | NULL    |       |

### jansPairwiseIdentifier     
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id               | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass          | varchar(48)  | YES  |     | NULL    |       |
| dn                   | varchar(128) | YES  |     | NULL    |       |
| jansId               | varchar(128) | YES  |     | NULL    |       |
| jansSectorIdentifier | varchar(64)  | YES  |     | NULL    |       |
| jansClntId           | json         | YES  |     | NULL    |       |
| jansUsrId            | varchar(64)  | YES  |     | NULL    |       |

### jansPar                    
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id      | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass | varchar(48)  | YES  |     | NULL    |       |
| dn          | varchar(128) | YES  |     | NULL    |       |
| jansId      | varchar(128) | YES  | MUL | NULL    |       |
| jansAttrs   | text         | YES  |     | NULL    |       |
| exp         | datetime(3)  | YES  |     | NULL    |       |
| del         | smallint     | YES  | MUL | NULL    |       |

### jansPassResetReq           
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id       | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass  | varchar(48)  | YES  |     | NULL    |       |
| dn           | varchar(128) | YES  |     | NULL    |       |
| creationDate | datetime(3)  | YES  | MUL | NULL    |       |
| jansGuid     | varchar(64)  | YES  |     | NULL    |       |
| personInum   | varchar(64)  | YES  |     | NULL    |       |

### jansPerson                 
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id                               | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass                          | varchar(48)  | YES  |     | NULL    |       |
| dn                                   | varchar(128) | YES  |     | NULL    |       |
| jansAssociatedClnt                   | json         | YES  |     | NULL    |       |
| c                                    | varchar(2)   | YES  |     | NULL    |       |
| displayName                          | varchar(128) | YES  | MUL | NULL    |       |
| givenName                            | varchar(128) | YES  | MUL | NULL    |       |
| jansManagedOrganizations             | varchar(64)  | YES  |     | NULL    |       |
| jansOptOuts                          | json         | YES  |     | NULL    |       |
| jansStatus                           | varchar(16)  | YES  |     | NULL    |       |
| inum                                 | varchar(64)  | YES  | MUL | NULL    |       |
| mail                                 | varchar(96)  | YES  | MUL | NULL    |       |
| memberOf                             | json         | YES  |     | NULL    |       |
| o                                    | varchar(64)  | YES  |     | NULL    |       |
| jansPersistentJWT                    | varchar(64)  | YES  |     | NULL    |       |
| jansCreationTimestamp                | datetime(3)  | YES  |     | NULL    |       |
| jansExtUid                           | json         | YES  |     | NULL    |       |
| jansOTPCache                         | json         | YES  |     | NULL    |       |
| jansLastLogonTime                    | datetime(3)  | YES  |     | NULL    |       |
| jansActive                           | smallint     | YES  |     | NULL    |       |
| jansAddres                           | json         | YES  |     | NULL    |       |
| jansEmail                            | json         | YES  |     | NULL    |       |
| jansEntitlements                     | json         | YES  |     | NULL    |       |
| jansExtId                            | varchar(128) | YES  |     | NULL    |       |
| jansImsValue                         | json         | YES  |     | NULL    |       |
| jansMetaCreated                      | varchar(64)  | YES  |     | NULL    |       |
| jansMetaLastMod                      | varchar(64)  | YES  |     | NULL    |       |
| jansMetaLocation                     | tinytext     | YES  |     | NULL    |       |
| jansMetaVer                          | varchar(64)  | YES  |     | NULL    |       |
| jansNameFormatted                    | tinytext     | YES  |     | NULL    |       |
| jansPhoneValue                       | json         | YES  |     | NULL    |       |
| jansPhotos                           | json         | YES  |     | NULL    |       |
| jansProfileURL                       | varchar(256) | YES  |     | NULL    |       |
| jansRole                             | json         | YES  |     | NULL    |       |
| jansTitle                            | varchar(64)  | YES  |     | NULL    |       |
| jansUsrTyp                           | varchar(64)  | YES  |     | NULL    |       |
| jansHonorificPrefix                  | varchar(64)  | YES  |     | NULL    |       |
| jansHonorificSuffix                  | varchar(64)  | YES  |     | NULL    |       |
| jans509Certificate                   | json         | YES  |     | NULL    |       |
| jansPassExpDate                      | datetime(3)  | YES  |     | NULL    |       |
| persistentId                         | varchar(64)  | YES  |     | NULL    |       |
| middleName                           | varchar(64)  | YES  |     | NULL    |       |
| nickname                             | varchar(64)  | YES  |     | NULL    |       |
| jansPrefUsrName                      | varchar(64)  | YES  |     | NULL    |       |
| profile                              | varchar(64)  | YES  |     | NULL    |       |
| picture                              | tinytext     | YES  |     | NULL    |       |
| website                              | varchar(64)  | YES  |     | NULL    |       |
| emailVerified                        | smallint     | YES  |     | NULL    |       |
| gender                               | varchar(32)  | YES  |     | NULL    |       |
| birthdate                            | datetime(3)  | YES  |     | NULL    |       |
| zoneinfo                             | varchar(64)  | YES  |     | NULL    |       |
| locale                               | varchar(64)  | YES  |     | NULL    |       |
| phoneNumberVerified                  | smallint     | YES  |     | NULL    |       |
| address                              | tinytext     | YES  |     | NULL    |       |
| updatedAt                            | datetime(3)  | YES  |     | NULL    |       |
| preferredLanguage                    | varchar(64)  | YES  |     | NULL    |       |
| role                                 | json         | YES  |     | NULL    |       |
| secretAnswer                         | tinytext     | YES  |     | NULL    |       |
| secretQuestion                       | tinytext     | YES  |     | NULL    |       |
| seeAlso                              | varchar(64)  | YES  |     | NULL    |       |
| sn                                   | varchar(128) | YES  | MUL | NULL    |       |
| cn                                   | varchar(128) | YES  |     | NULL    |       |
| transientId                          | varchar(64)  | YES  |     | NULL    |       |
| uid                                  | varchar(64)  | YES  | MUL | NULL    |       |
| userPassword                         | varchar(256) | YES  |     | NULL    |       |
| st                                   | varchar(64)  | YES  |     | NULL    |       |
| street                               | tinytext     | YES  |     | NULL    |       |
| l                                    | varchar(64)  | YES  |     | NULL    |       |
| jansCountInvalidLogin                | varchar(64)  | YES  |     | NULL    |       |
| jansEnrollmentCode                   | varchar(64)  | YES  |     | NULL    |       |
| jansIMAPData                         | varchar(64)  | YES  |     | NULL    |       |
| jansPPID                             | json         | YES  |     | NULL    |       |
| jansGuid                             | varchar(64)  | YES  |     | NULL    |       |
| jansPreferredMethod                  | varchar(64)  | YES  |     | NULL    |       |
| userCertificate                      | blob         | YES  |     | NULL    |       |
| jansOTPDevices                       | varchar(512) | YES  |     | NULL    |       |
| jansMobileDevices                    | varchar(512) | YES  |     | NULL    |       |
| jansTrustedDevices                   | text         | YES  |     | NULL    |       |
| jansStrongAuthPolicy                 | varchar(64)  | YES  |     | NULL    |       |
| jansUnlinkedExternalUids             | varchar(64)  | YES  |     | NULL    |       |
| jansBackchannelDeviceRegistrationTkn | varchar(64)  | YES  |     | NULL    |       |
| jansBackchannelUsrCode               | varchar(64)  | YES  |     | NULL    |       |
| telephoneNumber                      | varchar(20)  | YES  |     | NULL    |       |
| mobile                               | json         | YES  |     | NULL    |       |
| carLicense                           | varchar(64)  | YES  |     | NULL    |       |
| facsimileTelephoneNumber             | varchar(20)  | YES  |     | NULL    |       |
| departmentNumber                     | varchar(64)  | YES  |     | NULL    |       |
| employeeType                         | varchar(64)  | YES  |     | NULL    |       |
| manager                              | tinytext     | YES  |     | NULL    |       |
| postOfficeBox                        | varchar(64)  | YES  |     | NULL    |       |
| employeeNumber                       | varchar(64)  | YES  |     | NULL    |       |
| preferredDeliveryMethod              | varchar(50)  | YES  |     | NULL    |       |
| roomNumber                           | varchar(64)  | YES  |     | NULL    |       |
| secretary                            | tinytext     | YES  |     | NULL    |       |
| homePostalAddress                    | tinytext     | YES  |     | NULL    |       |
| postalCode                           | varchar(16)  | YES  |     | NULL    |       |
| description                          | varchar(768) | YES  |     | NULL    |       |
| title                                | varchar(64)  | YES  |     | NULL    |       |
| jansAdminUIRole                      | json         | YES  |     | NULL    |       |

### jansPushApp                
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id          | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass     | varchar(48)  | YES  |     | NULL    |       |
| dn              | varchar(128) | YES  |     | NULL    |       |
| displayName     | varchar(128) | YES  |     | NULL    |       |
| jansId          | varchar(128) | YES  |     | NULL    |       |
| jansName        | varchar(64)  | YES  |     | NULL    |       |
| jansPushAppConf | varchar(64)  | YES  |     | NULL    |       |

### jansPushDevice             
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id             | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass        | varchar(48)  | YES  |     | NULL    |       |
| dn                 | varchar(128) | YES  |     | NULL    |       |
| jansUsrId          | varchar(64)  | YES  |     | NULL    |       |
| jansId             | varchar(128) | YES  |     | NULL    |       |
| jansPushApp        | tinytext     | YES  |     | NULL    |       |
| jansPushDeviceConf | varchar(64)  | YES  |     | NULL    |       |
| jansTyp            | varchar(64)  | YES  |     | NULL    |       |

### jansRp                     
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id      | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass | varchar(48)  | YES  |     | NULL    |       |
| dn          | varchar(128) | YES  |     | NULL    |       |
| jansId      | varchar(128) | YES  |     | NULL    |       |
| dat         | text         | YES  |     | NULL    |       |

### jansScope                  
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id             | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass        | varchar(48)  | YES  |     | NULL    |       |
| dn                 | varchar(128) | YES  |     | NULL    |       |
| jansDefScope       | smallint     | YES  |     | NULL    |       |
| description        | varchar(768) | YES  | MUL | NULL    |       |
| displayName        | varchar(128) | YES  | MUL | NULL    |       |
| inum               | varchar(64)  | YES  |     | NULL    |       |
| jansScopeTyp       | varchar(64)  | YES  |     | NULL    |       |
| creatorId          | varchar(64)  | YES  |     | NULL    |       |
| creatorTyp         | varchar(64)  | YES  |     | NULL    |       |
| creatorAttrs       | varchar(64)  | YES  |     | NULL    |       |
| creationDate       | datetime(3)  | YES  |     | NULL    |       |
| jansClaim          | json         | YES  |     | NULL    |       |
| jansScrDn          | json         | YES  |     | NULL    |       |
| jansGrpClaims      | smallint     | YES  |     | NULL    |       |
| jansId             | varchar(128) | YES  | MUL | NULL    |       |
| jansIconUrl        | varchar(64)  | YES  |     | NULL    |       |
| jansUmaPolicyScrDn | tinytext     | YES  |     | NULL    |       |
| jansAttrs          | text         | YES  |     | NULL    |       |
| exp                | datetime(3)  | YES  |     | NULL    |       |
| del                | smallint     | YES  | MUL | NULL    |       |

### jansScr                    
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id      | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass | varchar(48)  | YES  |     | NULL    |       |
| dn          | varchar(128) | YES  |     | NULL    |       |
| inum        | varchar(64)  | YES  |     | NULL    |       |
| jansScr     | text         | YES  |     | NULL    |       |
| jansScrTyp  | varchar(64)  | YES  |     | NULL    |       |

### jansSectorIdentifier       
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id          | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass     | varchar(48)  | YES  |     | NULL    |       |
| dn              | varchar(128) | YES  |     | NULL    |       |
| jansId          | varchar(128) | YES  |     | NULL    |       |
| description     | varchar(768) | YES  |     | NULL    |       |
| jansRedirectURI | json         | YES  |     | NULL    |       |
| jansClntId      | json         | YES  |     | NULL    |       |

### jansSessId                 
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id                   | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass              | varchar(48)  | YES  |     | NULL    |       |
| dn                       | varchar(128) | YES  |     | NULL    |       |
| jansId                   | varchar(128) | YES  |     | NULL    |       |
| sid                      | varchar(64)  | YES  | MUL | NULL    |       |
| creationDate             | datetime(3)  | YES  |     | NULL    |       |
| exp                      | datetime(3)  | YES  |     | NULL    |       |
| del                      | smallint     | YES  | MUL | NULL    |       |
| jansLastAccessTime       | datetime(3)  | YES  |     | NULL    |       |
| jansUsrDN                | varchar(128) | YES  | MUL | NULL    |       |
| authnTime                | datetime(3)  | YES  |     | NULL    |       |
| jansState                | varchar(64)  | YES  |     | NULL    |       |
| jansSessState            | text         | YES  |     | NULL    |       |
| jansPermissionGranted    | smallint     | YES  |     | NULL    |       |
| jansAsJwt                | smallint     | YES  |     | NULL    |       |
| jansJwt                  | text         | YES  |     | NULL    |       |
| jansPermissionGrantedMap | text         | YES  |     | NULL    |       |
| jansInvolvedClnts        | text         | YES  |     | NULL    |       |
| deviceSecret             | varchar(64)  | YES  | MUL | NULL    |       |
| jansSessAttr             | text         | YES  |     | NULL    |       |

### jansSsa                    
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id       | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass  | varchar(48)  | YES  |     | NULL    |       |
| dn           | varchar(128) | YES  |     | NULL    |       |
| inum         | varchar(64)  | YES  |     | NULL    |       |
| o            | varchar(64)  | YES  |     | NULL    |       |
| jansAttrs    | text         | YES  |     | NULL    |       |
| description  | varchar(768) | YES  |     | NULL    |       |
| exp          | datetime(3)  | YES  |     | NULL    |       |
| del          | smallint     | YES  |     | NULL    |       |
| jansState    | varchar(64)  | YES  |     | NULL    |       |
| creatorId    | varchar(64)  | YES  |     | NULL    |       |
| creatorTyp   | varchar(64)  | YES  |     | NULL    |       |
| creationDate | datetime(3)  | YES  |     | NULL    |       |

### jansStatEntry              
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id      | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass | varchar(48)  | YES  |     | NULL    |       |
| dn          | varchar(128) | YES  |     | NULL    |       |
| jansId      | varchar(128) | YES  | MUL | NULL    |       |
| dat         | text         | YES  |     | NULL    |       |
| jansData    | text         | YES  |     | NULL    |       |
| attr        | text         | YES  |     | NULL    |       |

### jansToken                  
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id      | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass | varchar(48)  | YES  |     | NULL    |       |
| dn          | varchar(128) | YES  |     | NULL    |       |
| authnTime   | datetime(3)  | YES  |     | NULL    |       |
| authzCode   | varchar(64)  | YES  |     | NULL    |       |
| iat         | datetime(3)  | YES  |     | NULL    |       |
| exp         | datetime(3)  | YES  |     | NULL    |       |
| del         | smallint     | YES  | MUL | NULL    |       |
| grtId       | varchar(64)  | YES  |     | NULL    |       |
| grtTyp      | varchar(64)  | YES  |     | NULL    |       |
| jwtReq      | text         | YES  |     | NULL    |       |
| nnc         | text         | YES  |     | NULL    |       |
| scp         | text         | YES  |     | NULL    |       |
| tknCde      | varchar(80)  | YES  |     | NULL    |       |
| tknTyp      | varchar(32)  | YES  |     | NULL    |       |
| usrId       | varchar(64)  | YES  |     | NULL    |       |
| jansUsrDN   | varchar(128) | YES  |     | NULL    |       |
| clnId       | json         | YES  |     | NULL    |       |
| acr         | varchar(48)  | YES  |     | NULL    |       |
| uuid        | varchar(64)  | YES  |     | NULL    |       |
| chlng       | varchar(64)  | YES  |     | NULL    |       |
| chlngMth    | varchar(64)  | YES  |     | NULL    |       |
| clms        | varchar(64)  | YES  |     | NULL    |       |
| ssnId       | varchar(64)  | YES  |     | NULL    |       |
| attr        | text         | YES  |     | NULL    |       |
| tknBndCnf   | tinytext     | YES  |     | NULL    |       |
| dpop        | varchar(64)  | YES  |     | NULL    |       |

### jansU2fReq                 
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id          | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass     | varchar(48)  | YES  |     | NULL    |       |
| dn              | varchar(128) | YES  |     | NULL    |       |
| jansId          | varchar(128) | YES  |     | NULL    |       |
| jansReqId       | varchar(64)  | YES  |     | NULL    |       |
| jansReq         | text         | YES  |     | NULL    |       |
| jansSessStateId | varchar(64)  | YES  |     | NULL    |       |
| del             | smallint     | YES  | MUL | NULL    |       |
| exp             | datetime(3)  | YES  |     | NULL    |       |
| personInum      | varchar(64)  | YES  |     | NULL    |       |
| creationDate    | datetime(3)  | YES  | MUL | NULL    |       |

### jansUmaPCT                 
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id          | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass     | varchar(48)  | YES  |     | NULL    |       |
| dn              | varchar(128) | YES  |     | NULL    |       |
| clnId           | json         | YES  |     | NULL    |       |
| iat             | datetime(3)  | YES  |     | NULL    |       |
| exp             | datetime(3)  | YES  |     | NULL    |       |
| del             | smallint     | YES  | MUL | NULL    |       |
| tknCde          | varchar(80)  | YES  | MUL | NULL    |       |
| ssnId           | varchar(64)  | YES  |     | NULL    |       |
| jansClaimValues | varchar(64)  | YES  |     | NULL    |       |
| dpop            | varchar(64)  | YES  |     | NULL    |       |
| authnTime       | datetime(3)  | YES  |     | NULL    |       |
| authzCode       | varchar(64)  | YES  |     | NULL    |       |
| grtId           | varchar(64)  | YES  |     | NULL    |       |
| grtTyp          | varchar(64)  | YES  |     | NULL    |       |
| jwtReq          | text         | YES  |     | NULL    |       |
| nnc             | text         | YES  |     | NULL    |       |
| scp             | text         | YES  |     | NULL    |       |
| tknTyp          | varchar(32)  | YES  |     | NULL    |       |
| usrId           | varchar(64)  | YES  |     | NULL    |       |
| jansUsrDN       | varchar(128) | YES  |     | NULL    |       |
| acr             | varchar(48)  | YES  |     | NULL    |       |
| uuid            | varchar(64)  | YES  |     | NULL    |       |
| chlng           | varchar(64)  | YES  |     | NULL    |       |
| chlngMth        | varchar(64)  | YES  |     | NULL    |       |
| clms            | varchar(64)  | YES  |     | NULL    |       |
| attr            | text         | YES  |     | NULL    |       |
| tknBndCnf       | tinytext     | YES  |     | NULL    |       |

### jansUmaRPT                 
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id            | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass       | varchar(48)  | YES  |     | NULL    |       |
| dn                | varchar(128) | YES  |     | NULL    |       |
| authnTime         | datetime(3)  | YES  |     | NULL    |       |
| clnId             | json         | YES  |     | NULL    |       |
| iat               | datetime(3)  | YES  |     | NULL    |       |
| exp               | datetime(3)  | YES  |     | NULL    |       |
| del               | smallint     | YES  |     | NULL    |       |
| tknCde            | varchar(80)  | YES  |     | NULL    |       |
| usrId             | varchar(64)  | YES  |     | NULL    |       |
| ssnId             | varchar(64)  | YES  |     | NULL    |       |
| jansUmaPermission | json         | YES  |     | NULL    |       |
| uuid              | varchar(64)  | YES  |     | NULL    |       |
| dpop              | varchar(64)  | YES  |     | NULL    |       |
| authzCode         | varchar(64)  | YES  |     | NULL    |       |
| grtId             | varchar(64)  | YES  |     | NULL    |       |
| grtTyp            | varchar(64)  | YES  |     | NULL    |       |
| jwtReq            | text         | YES  |     | NULL    |       |
| nnc               | text         | YES  |     | NULL    |       |
| scp               | text         | YES  |     | NULL    |       |
| tknTyp            | varchar(32)  | YES  |     | NULL    |       |
| jansUsrDN         | varchar(128) | YES  |     | NULL    |       |
| acr               | varchar(48)  | YES  |     | NULL    |       |
| chlng             | varchar(64)  | YES  |     | NULL    |       |
| chlngMth          | varchar(64)  | YES  |     | NULL    |       |
| clms              | varchar(64)  | YES  |     | NULL    |       |
| attr              | text         | YES  |     | NULL    |       |
| tknBndCnf         | tinytext     | YES  |     | NULL    |       |

### jansUmaResource            
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id              | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass         | varchar(48)  | YES  |     | NULL    |       |
| dn                  | varchar(128) | YES  |     | NULL    |       |
| displayName         | varchar(128) | YES  | MUL | NULL    |       |
| inum                | varchar(64)  | YES  |     | NULL    |       |
| owner               | varchar(64)  | YES  |     | NULL    |       |
| jansAssociatedClnt  | json         | YES  |     | NULL    |       |
| jansUmaScope        | varchar(768) | YES  | MUL | NULL    |       |
| jansFaviconImage    | varchar(64)  | YES  |     | NULL    |       |
| jansGrp             | varchar(64)  | YES  |     | NULL    |       |
| jansId              | varchar(128) | YES  | MUL | NULL    |       |
| jansResource        | tinytext     | YES  |     | NULL    |       |
| jansRevision        | int          | YES  |     | NULL    |       |
| jansTyp             | varchar(64)  | YES  |     | NULL    |       |
| jansScopeExpression | text         | YES  |     | NULL    |       |
| iat                 | datetime(3)  | YES  |     | NULL    |       |
| exp                 | datetime(3)  | YES  |     | NULL    |       |
| del                 | smallint     | YES  | MUL | NULL    |       |
| description         | varchar(768) | YES  |     | NULL    |       |

### jansUmaResourcePermission  
| Field                 | Type         | Null | Key | Default | Extra |
| -| -| -| -| -| -|
| doc_id            | varchar(64)  | NO   | PRI | NULL    |       |
| objectClass       | varchar(48)  | YES  |     | NULL    |       |
| dn                | varchar(128) | YES  |     | NULL    |       |
| exp               | datetime(3)  | YES  |     | NULL    |       |
| del               | smallint     | YES  |     | NULL    |       |
| jansUmaScope      | varchar(768) | YES  |     | NULL    |       |
| jansConfCode      | varchar(64)  | YES  |     | NULL    |       |
| jansResourceSetId | varchar(64)  | YES  |     | NULL    |       |
| jansAttrs         | text         | YES  |     | NULL    |       |
| jansTicket        | varchar(64)  | YES  | MUL | NULL    |       |
| jansStatus        | varchar(16)  | YES  |     | NULL    |       |


