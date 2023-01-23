---
tags:
  - administration
  - reference
  - database
---
|DN |
|-|
|o=jans|
|ou=people,o=jans|
|ou=groups,o=jans|
|ou=attributes,o=jans|
|ou=scopes,o=jans|
|ou=clients,o=jans|
|ou=stat,o=jans|
|ou=par,o=jans|
|ou=sessions,o=jans|
|ou=tokens,o=jans|
|ou=authorizations,o=jans|
|ou=scripts,o=jans|
|ou=resetPasswordRequests,o=jans|
|ou=uma,o=jans|
|ou=resources,ou=uma,o=jans|
|ou=pct,ou=uma,o=jans|
|ou=push,o=jans|
|ou=application,ou=push,o=jans|
|ou=device,ou=push,o=jans|
|ou=u2f,o=jans|
|ou=registration_requests,ou=u2f,o=jans|
|ou=authentication_requests,ou=u2f,o=jans|
|ou=registered_devices,ou=u2f,o=jans|
|ou=metric,o=jans|
|ou=sector_identifiers,o=jans|
|ou=ciba,o=jans|
|ou=trustRelationships,o=jans|
|ou=ssa,o=jans|

## ObjectClasses

| ObjectClasses|
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

### ObjectClass: agmFlow

| Field             |Description |
| -| -|
| dn            | |
| objectClass       | |
| agFlowQname       | |
| agFlowMeta        | |
| jansScr           | |
| jansEnabled       | |
| jansScrError      | |
| agFlowTrans       | |
| jansRevision      | |
| jansCustomMessage | |

### agmFlowRun


| Field             | Description |
| -| -|
| jansId           | |
| objectClass       | |
| dn                | |
| agFlowSt          | |
| agFlowEncCont     | |
| jansCustomMessage | |
| exp               | |

### jansAdminConfDyn

| Field        |Description |
|  -| -|
| inum         |       |
| objectClass  | |
| dn           | |
| c            | |
| ou           | |
| description  | |
| displayName  | |
| jansConfDyn  | |
| o            | |
| jansRevision | |

### jansAppConf

| Field                 | |
| -| -|
| objectClass           | |
| ou                    | |
| description           | |
| displayName           | |
| jansHostname          | |
| jansLastUpd           | |
| jansManager           | |
| jansOrgProfileMgt     | |
| jansScimEnabled       | |
| jansEmail             | |
| jansSmtpConf          | |
| jansSslExpiry         | |
| jansStatus            | |
| jansUrl               | |
| o                     | |
| jansAuthMode          | |
| jansDbAuth            | |
| jansLogViewerConfig   | |
| jansLogConfigLocation | |
| jansCacheConf         | |
| jansDocStoreConf      | |
| jansSoftVer           | |
| userPassword          | |
| jansConfDyn           | |
| jansConfErrors        | |
| jansConfStatic        | |
| jansConfWebKeys       | |
| jansWebKeysSettings   | |
| jansConfApp           | |
| jansRevision          | |


###  jansAttr       

| Field                 | Description|Datatype|Usage|
| -| -| -|-|
| inum                  | | | | 
| objectClass           | | | | 
| dn                    | | | | 
| description           | | | | 
| displayName           | |||
| jansAttrEditTyp       | |||
| jansAttrName          | |||
| jansAttrOrigin        | |||
| jansAttrSystemEditTyp | |||
| jansAttrTyp           | |||
| jansClaimName         | |||
| jansAttrUsgTyp        | |||
| jansAttrViewTyp       | |||
| jansSAML1URI          | |||
| jansSAML2URI          | |||
| jansStatus            | |||
| jansMultivaluedAttr   | |||
| jansHideOnDiscovery   | |||
| jansNameIdTyp         | |||
| jansScimCustomAttr    | |||
| jansSourceAttr        | |||
| seeAlso               | |||
| urn                   | |||
| jansRegExp            | |||
| jansTooltip           | |||
| jansValidation        | |||

            
### jansCache                  

| Field                 |  |
| -| -| 
| objectClass | |
| dn          | |
| uuid        | |
| iat         | |
| exp         | |
| del         | |
| dat         | |

### jansCibaReq             
| Field                 | |
| -| -| 
| objectClass  | |
| dn           | |
| authReqId    | |
| clnId        | |
| usrId        | |
| creationDate | |
| exp          | |
| jansStatus   | |
   
### jansClnt                   
| Field                 | |
| -| -|
|inum| |
| objectClass                             | |
| dn                                      | |
| o                                       | |
| jansGrp                                 | |
| displayName                             | |
| displayNameLocalized                    | |
| description                             | |
| jansAppTyp                              | |
| jansClntIdIssuedAt                      | |
| jansClntSecret                          | |
| jansClntSecretExpAt                     | |
| exp                                     | |
| del                                     | |
| jansClntURI                             | |
| jansClntURILocalized                    | |
| jansContact                             | |
| jansDefAcrValues                        | |
| jansDefMaxAge                           | |
| jansGrantTyp                            | |
| jansIdTknEncRespAlg                     | |
| jansIdTknEncRespEnc                     | |
| jansIdTknSignedRespAlg                  | |
| jansInitiateLoginURI                    | |
| jansJwksURI                             | |
| jansJwks                                | |
| jansLogoURI                             | |
| jansLogoURILocalized                    | |
| jansPolicyURI                           | |
| jansPolicyURILocalized||
| jansPostLogoutRedirectURI               | |
| jansRedirectURI                         | |
| jansRegistrationAccessTkn               | |
| jansReqObjSigAlg                        | |
| jansReqObjEncAlg                        | |
| jansReqObjEncEnc                        | |
| jansReqURI                              | |
| jansRespTyp                             | |
| jansScope                               | |
| jansClaim                               | |
| jansSectorIdentifierURI                 | |
| jansSignedRespAlg                       | |
| jansSubjectTyp                          | |
| jansTknEndpointAuthMethod               | |
| jansTknEndpointAuthSigAlg               | |
| jansTosURI                              | |
| jansTosURILocalized                     | |
| jansTrustedClnt                         | |
| jansUsrInfEncRespAlg                    | |
| jansUsrInfEncRespEnc                    | |
| jansExtraConf                           | |
| jansClaimRedirectURI                    | |
| jansLastAccessTime                      | |
| jansLastLogonTime                       | |
| jansPersistClntAuthzs                   | |
| jansInclClaimsInIdTkn                   | |
| jansRefreshTknLife                      | |
| jansDisabled                            | |
| jansLogoutURI                           | |
| jansLogoutSessRequired                  | |
| jansdId                                 | |
| jansAuthorizedOrigins                   | |
| tknBndCnf                               | |
| jansAccessTknAsJwt                      | |
| jansAccessTknSigAlg                     | |
| jansAccessTknLife                       | |
| jansSoftId                              | |
| jansSoftVer                             | |
| jansSoftStatement                       | |
| jansRptAsJwt                            | |
| jansAttrs                               | |
| jansBackchannelTknDeliveryMode          | |
| jansBackchannelClntNotificationEndpoint | |
| jansBackchannelAuthnReqSigAlg           | |
| jansBackchannelUsrCodeParameter         | |

### jansClntAuthz              
| Field                 | |
| -| -| 
| jansId      | |
| objectClass | |
| dn          | |
| jansClntId  | |
| jansUsrId   | |
| exp         | |
| del         | |
| jansScope   | |

### jansCustomScr              
| Field                 ||
| -| -| 
| inum               | |
| objectClass        | |
| dn                 | |
| displayName        | |
| description        | |
| jansScr            | |
| jansScrTyp         | |
| jansProgLng        | |
| jansModuleProperty | |
| jansConfProperty   | |
| jansLevel          | |
| jansRevision       | |
| jansEnabled        | |
| jansScrError       | |
| jansAlias          | |

### jansDeviceRegistration     
| Field                 | |
| -| -| 
| jansId   | |
| objectClass                | |
| dn                         | |
| displayName                | |
| description                | |
| jansDeviceKeyHandle        | |
| jansDeviceHashCode         | |
| jansApp                    | |
| jansDeviceRegistrationConf | |
| jansDeviceNotificationConf | |
| jansNickName               | |
| jansDeviceData             | |
| jansCounter                | |
| jansStatus                 | |
| del                        | |
| exp                        | |
| personInum                 | |
| creationDate               | |
| jansLastAccessTime         | |
| jansMetaLastMod            | |
| jansMetaLocation           | |
| jansMetaVer                | |

### jansDocument               
| Field                 |  |
| -| -| 
| inum               | |
| objectClass        | |
| dn                 | |
| ou                 | |
| displayName        | |
| description        | |
| document           | |
| creationDate       | |
| jansModuleProperty | |
| jansLevel          | |
| jansRevision       | |
| jansEnabled        | |
| jansAlias          | |

### jansFido2AuthnEntry        
| Field                 | |
| -| -| 
| jansId          | |
| objectClass       | |
| dn                | |
| creationDate      | |
| jansSessStateId   | |
| jansCodeChallenge | |
| personInum        | |
| jansAuthData      | |
| jansStatus        | |

### jansFido2RegistrationEntry 
| Field                 | |
| -| -| 
| jansId                                          | |
| objectClass                | |
| dn                         | |
| creationDate               | |
| displayName                | |
| jansSessStateId            | |
| jansCodeChallenge          | |
| jansCodeChallengeHash      | |
| jansPublicKeyId            | |
| personInum                 | |
| jansRegistrationData       | |
| jansDeviceNotificationConf | |
| jansCounter                | |
| jansStatus                 | |

### jansGrant                  
| Field                 | |
| -| -| 
| grtId       | |
| objectClass | |
| dn          | |
| iat         | |

### jansGrp                    
| Field                 |  |
| -| -|
| inum             | |
| objectClass      | |
| dn               | |
| c                | |
| description      | |
| displayName      | |
| jansStatus       | |
| member           | |
| o                | |
| owner            | |
| seeAlso          | |
| jansMetaCreated  | |
| jansMetaLastMod  | |
| jansMetaLocation | |
| jansMetaVer      | |

### jansInumMap                
| Field                 | |
| -| -| 
| inum                     | |
| objectClass              | |
| dn                       | |
| jansStatus               | |
| jansPrimaryKeyAttrName   | |
| jansPrimaryKeyValue      | |
| jansSecondaryKeyAttrName | |
| jansSecondaryKeyValue    | |
| jansTertiaryKeyAttrName  | |
| jansTertiaryKeyValue     | |

### jansMetric                 
| Field                 | |
| -| -| 
| uniqueIdentifier | |
| objectClass      | |
| dn               | |
| jansStartDate    | |
| jansEndDate      | |
| jansAppTyp       | |
| jansMetricTyp    | |
| creationDate     | |
| del              | |
| exp              | |
| jansData         | |
| jansHost         | |

### jansOrganization           
| Field                 | |
| -| -| 
| objectClass           | |
| dn                    | |
| c                     | |
| county                | |
| description           | |
| displayName           | |
| jansCustomMessage     | |
| jansFaviconImage      | |
| jansLogoImage         | |
| jansManager           | |
| jansManagerGrp        | |
| jansOrgShortName      | |
| jansThemeColor        | |
| inum                  | |
| l                     | |
| mail                  | |
| memberOf              | |
| o                     | |
| jansCreationTimestamp | |
| jansRegistrationConf  | |
| postalCode            | |
| st                    | |
| street                | |
| telephoneNumber       | |
| title                 | |
| uid                   | |
| jansLogoPath          | |
| jansStatus            | |
| jansFaviconPath       | |

### jansPairwiseIdentifier     
| Field                 | |
| -| -| 
| jansId               | |
| objectClass          | |
| dn                   | |
| jansSectorIdentifier | |
| jansClntId           | |
| jansUsrId            | |

### jansPar                    
| Field                 | |
| -| -| 
| jansId      | |
| objectClass | |
| dn          | |
| jansAttrs   | |
| exp         | |
| del         | |

### jansPassResetReq           
| Field                 | |
| -| -| 
| jansGuid     | |
| objectClass  | |
| dn           | |
| creationDate | |
| personInum   | |

### jansPerson                 
| Field                 | |
| -| -| 
| inum| |
| objectClass                          | |
| jansAssociatedClnt                   ||
| c                                    | |
| displayName                          | |
| givenName                            | |
| jansManagedOrganizations             | |
| jansOptOuts                          | |
| jansStatus                           | |
| inum                                 | |
| mail                                 | |
| memberOf                             | |
| o                                    | |
| jansPersistentJWT                    | |
| jansCreationTimestamp                | |
| jansExtUid                           | |
| jansOTPCache                         | |
| jansLastLogonTime                    | |
| jansActive                           | |
| jansAddres                           | |
| jansEmail                            | |
| jansEntitlements                     | |
| jansExtId                            | |
| jansImsValue                         | |
| jansMetaCreated                      | |
| jansMetaLastMod                      | |
| jansMetaLocation                     | |
| jansMetaVer                          | |
| jansNameFormatted                    | |
| jansPhoneValue                       | |
| jansPhotos                           | |
| jansProfileURL                       | |
| jansRole                             | |
| jansTitle                            | |
| jansUsrTyp                           | |
| jansHonorificPrefix                  | |
| jansHonorificSuffix                  | |
| jans509Certificate                   | |
| jansPassExpDate                      | |
| persistentId                         | |
| middleName                           | |
| nickname                             | |
| jansPrefUsrName                      | |
| profile                              | |
| picture                              | |
| website                              | |
| emailVerified                        | |
| gender                               | |
| birthdate                            | |
| zoneinfo                             | |
| locale                               | |
| phoneNumberVerified                  | |
| address                              | |
| updatedAt                            | |
| preferredLanguage                    | |
| role                                 | |
| secretAnswer                         | |
| secretQuestion                       | |
| seeAlso                              | |
| sn                                   | |
| cn                                   | |
| transientId                          | |
| uid                                  | |
| userPassword                         | |
| st                                   | |
| street                               | |
| l                                    | |
| jansCountInvalidLogin                | |
| jansEnrollmentCode                   | |
| jansIMAPData                         | |
| jansPPID                             | |
| jansGuid                             | |
| jansPreferredMethod                  | |
| userCertificate                      | |
| jansOTPDevices                       | |
| jansMobileDevices                    | |
| jansTrustedDevices                   | |
| jansStrongAuthPolicy                 | |
| jansUnlinkedExternalUids             | |
| jansBackchannelDeviceRegistrationTkn | |
| jansBackchannelUsrCode               | |
| telephoneNumber                      | |
| mobile                               | |
| carLicense                           | |
| facsimileTelephoneNumber             | |
| departmentNumber                     | |
| employeeType                         | |
| manager                              | |
| postOfficeBox                        | |
| employeeNumber                       | |
| preferredDeliveryMethod              | |
| roomNumber                           | |
| secretary                            | |
| homePostalAddress                    | |
| postalCode                           | |
| description                          | |
| title                                | |
| jansAdminUIRole                      | |

### jansPushApp                
| Field                 | |
| -| -| 
| jansId          | |
| objectClass     | |
| dn              | |
| displayName     | |
| jansName        | |
| jansPushAppConf | |

### jansPushDevice             
| Field                 | |
| -| -|
| jansId             | |
| objectClass        | |
| dn                 | |
| jansUsrId          | |
| jansPushApp        | |
| jansPushDeviceConf | |
| jansTyp            | |

### jansRp                     
| Field                 | |
| -| -| 
| jansId      | |
| objectClass | |
| dn          | |
| dat         | |

### jansScope                  
| Field                 | |
| -| -|
| inum               | |
| jansId             | |
| objectClass        | |
| dn                 | |
| jansDefScope       | |
| description        | |
| displayName        | |
| jansScopeTyp       | |
| creatorId          | |
| creatorTyp         | |
| creatorAttrs       | |
| creationDate       | |
| jansClaim          | |
| jansScrDn          | |
| jansGrpClaims      | |
| jansIconUrl        | |
| jansUmaPolicyScrDn | |
| jansAttrs          | |
| exp                | |
| del                | |

### jansScr                    
| Field                 |  |
| -| -|
| inum        | |
| objectClass | |
| dn          | |
| inum        | |
| jansScrTyp  | |

### jansSectorIdentifier       
| Field                 | |
| -| -| 
| jansId          | |
| objectClass     | |
| dn              | |
| jansId          | |
| description     | |
| jansRedirectURI | |
| jansClntId      | |

### jansSessId                 
| Field                 | |
| -| -| 
| jansId                   | |
| objectClass              | |
| dn                       | |
| sid                      | |
| creationDate             | |
| exp                      | |
| del                      | |
| jansLastAccessTime       | |
| jansUsrDN                | |
| authnTime                | |
| jansState                | |
| jansSessState            | |
| jansPermissionGranted    | |
| jansAsJwt                | |
| jansJwt                  | |
| jansPermissionGrantedMap | |
| jansInvolvedClnts        | |
| deviceSecret             | |
| jansSessAttr             | |

### jansSsa                    
| Field                 | |
| -| -| 
| inum         | |
| objectClass  | |
| dn           | |
| o            | |
| jansAttrs    | |
| description  | |
| exp          | |
| del          | |
| jansState    | |
| creatorId    | |
| creatorTyp   | |
| creationDate | |

### jansStatEntry              
| Field                 | |
| -| -| 
| jansId      | |
| objectClass | |
| dn          | |
| dat         | |
| jansData    | |
| attr        | |

### jansToken                  
| Field                 ||
| -| -| 
| tknCde      | |
| objectClass | |
| dn          | |
| authnTime   | |
| authzCode   | |
| iat         | |
| exp         | |
| del         | |
| grtId       | |
| grtTyp      | |
| jwtReq      | |
| nnc         | |
| scp         | |
| tknTyp      | |
| usrId       | |
| jansUsrDN   | |
| clnId       | |
| acr         | |
| uuid        | |
| chlng       | |
| chlngMth    | |
| clms        | |
| ssnId       | |
| attr        | |
| tknBndCnf   | |
| dpop        | |

### jansU2fReq                 
| Field                 | |
| -| -|
| jansId          | |
| objectClass     | |
| dn              | |
| jansReqId       | |
| jansReq         | |
| jansSessStateId | |
| del             | |
| exp             | |
| personInum      | |
| creationDate    | |

### jansUmaPCT                 
| Field                 ||
|  -| -|
| objectClass     | |
| dn              | |
| clnId           | |
| iat             | |
| exp             | |
| del             | |
| tknCde          | |
| ssnId           | |
| jansClaimValues | |
| dpop            | |
| authnTime       | |
| authzCode       | |
| grtId           | |
| grtTyp          | |
| jwtReq          | |
| nnc             | |
| scp             | |
| tknTyp          | |
| usrId           | |
| jansUsrDN       | |
| acr             | |
| uuid            | |
| chlng           | |
| chlngMth        | |
| clms            | |
| attr            | |
| tknBndCnf       | |

### jansUmaRPT                 
| Field                 |  |
|  -| -|
| objectClass       | |
| dn                | |
| authnTime         | |
| clnId             | |
| iat               | |
| exp               | |
| del               | |
| tknCde            | |
| usrId             | |
| ssnId             | |
| jansUmaPermission | |
| uuid              | |
| dpop              | |
| authzCode         | |
| grtId             | |
| grtTyp            | |
| jwtReq            | |
| nnc               | |
| scp               | |
| tknTyp            | |
| jansUsrDN         | |
| acr               | |
| chlng             | |
| chlngMth          | |
| clms              | |
| attr              | |
| tknBndCnf         |     |

### jansUmaResource            
| Field                 | Description|
| -| -|
| objectClass         | |
| dn                  | |
| displayName         | |
| inum                | |
| owner               | |
| jansAssociatedClnt  | |
| jansUmaScope        | |
| jansFaviconImage    | |
| jansGrp             | |
| jansId              | |
| jansResource        | |
| jansRevision        | |
| jansTyp             | |
| jansScopeExpression | |
| iat                 | |
| exp                 | |
| del                 | |
| description         | |

### jansUmaResourcePermission  
| Field                 | |
| -|-|
| objectClass       | |
| dn                | |
| exp               | |
| del               | |
| jansUmaScope      | |
| jansConfCode      | |
| jansResourceSetId | |
| jansAttrs         | |
| jansTicket        | |
| jansStatus        | |


