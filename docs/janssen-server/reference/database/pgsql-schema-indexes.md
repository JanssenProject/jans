---
tags:
  - administration
  - database
  - PostgreSQL
  - Indexes
---

# PostgreSQL Indexes


### jansPairwiseIdentifier
| tablename              | indexname                   | indexdef                                                                                                  |
| ---------------------- | --------------------------- | --------------------------------------------------------------------------------------------------------- |
| jansPairwiseIdentifier | jansPairwiseIdentifier_pkey | CREATE UNIQUE INDEX "jansPairwiseIdentifier_pkey" ON public."jansPairwiseIdentifier" USING btree (doc_id) |

### jansPerson
| tablename  | indexname                             | indexdef                                                                                                                                       |
| ---------- | ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| jansPerson | jansPerson_pkey                       | CREATE UNIQUE INDEX "jansPerson_pkey" ON public."jansPerson" USING btree (doc_id)                                                              |
| jansPerson | jansPerson_givenName_idx              | CREATE INDEX "jansPerson_givenName_idx" ON public."jansPerson" USING btree ("givenName")                                                       |
| jansPerson | jansPerson_mail_idx                   | CREATE INDEX "jansPerson_mail_idx" ON public."jansPerson" USING btree (mail)                                                                   |
| jansPerson | jansPerson_sn_idx                     | CREATE INDEX "jansPerson_sn_idx" ON public."jansPerson" USING btree (sn)                                                                       |
| jansPerson | jansPerson_lower_idx                  | CREATE INDEX "jansPerson_lower_idx" ON public."jansPerson" USING btree (lower((uid)::text))                                                    |
| jansPerson | jansPerson_displayName_idx            | CREATE INDEX "jansPerson_displayName_idx" ON public."jansPerson" USING btree ("displayName")                                                   |
| jansPerson | jansPerson_inum_idx                   | CREATE INDEX "jansPerson_inum_idx" ON public."jansPerson" USING btree (inum)                                                                   |
| jansPerson | jansPerson_jsonb_path_query_array_idx | CREATE INDEX "jansPerson_jsonb_path_query_array_idx" ON public."jansPerson" USING gin (jsonb_path_query_array("jansExtUid", '$[*]'::jsonpath)) |
| jansPerson | jansPerson_uid_idx                    | CREATE INDEX "jansPerson_uid_idx" ON public."jansPerson" USING btree (uid)                                                                     |
| jansPerson | jansPerson_lower_idx1                 | CREATE INDEX "jansPerson_lower_idx1" ON public."jansPerson" USING btree (lower((mail)::text))                                                  |

### jansOrganization
| tablename        | indexname                | indexdef                                                                                      |
| ---------------- | ------------------------ | --------------------------------------------------------------------------------------------- |
| jansOrganization | jansOrganization_pkey    | CREATE UNIQUE INDEX "jansOrganization_pkey" ON public."jansOrganization" USING btree (doc_id) |
| jansOrganization | jansOrganization_uid_idx | CREATE INDEX "jansOrganization_uid_idx" ON public."jansOrganization" USING btree (uid)        |

### jansSsa
| tablename | indexname    | indexdef                                                                    |
| --------- | ------------ | --------------------------------------------------------------------------- |
| jansSsa   | jansSsa_pkey | CREATE UNIQUE INDEX "jansSsa_pkey" ON public."jansSsa" USING btree (doc_id) |

### jansAppConf
| tablename   | indexname        | indexdef                                                                            |
| ----------- | ---------------- | ----------------------------------------------------------------------------------- |
| jansAppConf | jansAppConf_pkey | CREATE UNIQUE INDEX "jansAppConf_pkey" ON public."jansAppConf" USING btree (doc_id) |

### jansClnt
| tablename | indexname                              | indexdef                                                                                                             |
| --------- | -------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| jansClnt  | jansClnt_pkey                          | CREATE UNIQUE INDEX "jansClnt_pkey" ON public."jansClnt" USING btree (doc_id)                                        |
| jansClnt  | jansClnt_description_idx               | CREATE INDEX "jansClnt_description_idx" ON public."jansClnt" USING btree (description)                               |
| jansClnt  | jansClnt_jansClntSecretExpAt_idx       | CREATE INDEX "jansClnt_jansClntSecretExpAt_idx" ON public."jansClnt" USING btree ("jansClntSecretExpAt")             |
| jansClnt  | jansClnt_del_exp_idx                   | CREATE INDEX "jansClnt_del_exp_idx" ON public."jansClnt" USING btree (del, exp)                                      |
| jansClnt  | jansClnt_displayName_idx               | CREATE INDEX "jansClnt_displayName_idx" ON public."jansClnt" USING btree ("displayName")                             |
| jansClnt  | jansClnt_inum_idx                      | CREATE INDEX "jansClnt_inum_idx" ON public."jansClnt" USING btree (inum)                                             |
| jansClnt  | jansClnt_jansRegistrationAccessTkn_idx | CREATE INDEX "jansClnt_jansRegistrationAccessTkn_idx" ON public."jansClnt" USING btree ("jansRegistrationAccessTkn") |

### jansScope
| tablename | indexname                 | indexdef                                                                                   |
| --------- | ------------------------- | ------------------------------------------------------------------------------------------ |
| jansScope | jansScope_pkey            | CREATE UNIQUE INDEX "jansScope_pkey" ON public."jansScope" USING btree (doc_id)            |
| jansScope | jansScope_displayName_idx | CREATE INDEX "jansScope_displayName_idx" ON public."jansScope" USING btree ("displayName") |
| jansScope | jansScope_del_exp_idx     | CREATE INDEX "jansScope_del_exp_idx" ON public."jansScope" USING btree (del, exp)          |
| jansScope | jansScope_description_idx | CREATE INDEX "jansScope_description_idx" ON public."jansScope" USING btree (description)   |
| jansScope | jansScope_jansId_idx      | CREATE INDEX "jansScope_jansId_idx" ON public."jansScope" USING btree ("jansId")           |

### jansUmaResource
| tablename       | indexname                        | indexdef                                                                                                 |
| --------------- | -------------------------------- | -------------------------------------------------------------------------------------------------------- |
| jansUmaResource | jansUmaResource_pkey             | CREATE UNIQUE INDEX "jansUmaResource_pkey" ON public."jansUmaResource" USING btree (doc_id)              |
| jansUmaResource | jansUmaResource_jansUmaScope_idx | CREATE INDEX "jansUmaResource_jansUmaScope_idx" ON public."jansUmaResource" USING btree ("jansUmaScope") |
| jansUmaResource | jansUmaResource_del_exp_idx      | CREATE INDEX "jansUmaResource_del_exp_idx" ON public."jansUmaResource" USING btree (del, exp)            |
| jansUmaResource | jansUmaResource_displayName_idx  | CREATE INDEX "jansUmaResource_displayName_idx" ON public."jansUmaResource" USING btree ("displayName")   |
| jansUmaResource | jansUmaResource_jansId_idx       | CREATE INDEX "jansUmaResource_jansId_idx" ON public."jansUmaResource" USING btree ("jansId")             |

### jansUmaResourcePermission
| tablename                 | indexname                                | indexdef                                                                                                                 |
| ------------------------- | ---------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| jansUmaResourcePermission | jansUmaResourcePermission_pkey           | CREATE UNIQUE INDEX "jansUmaResourcePermission_pkey" ON public."jansUmaResourcePermission" USING btree (doc_id)          |
| jansUmaResourcePermission | jansUmaResourcePermission_jansTicket_idx | CREATE INDEX "jansUmaResourcePermission_jansTicket_idx" ON public."jansUmaResourcePermission" USING btree ("jansTicket") |

### jansGrant
| tablename | indexname      | indexdef                                                                        |
| --------- | -------------- | ------------------------------------------------------------------------------- |
| jansGrant | jansGrant_pkey | CREATE UNIQUE INDEX "jansGrant_pkey" ON public."jansGrant" USING btree (doc_id) |

### jansToken
| tablename | indexname               | indexdef                                                                               |
| --------- | ----------------------- | -------------------------------------------------------------------------------------- |
| jansToken | jansToken_pkey          | CREATE UNIQUE INDEX "jansToken_pkey" ON public."jansToken" USING btree (doc_id)        |
| jansToken | jansToken_authzCode_idx | CREATE INDEX "jansToken_authzCode_idx" ON public."jansToken" USING btree ("authzCode") |
| jansToken | jansToken_grtId_idx     | CREATE INDEX "jansToken_grtId_idx" ON public."jansToken" USING btree ("grtId")         |
| jansToken | jansToken_ssnId_idx     | CREATE INDEX "jansToken_ssnId_idx" ON public."jansToken" USING btree ("ssnId")         |
| jansToken | jansToken_tknCde_idx    | CREATE INDEX "jansToken_tknCde_idx" ON public."jansToken" USING btree ("tknCde")       |
| jansToken | jansToken_del_exp_idx   | CREATE INDEX "jansToken_del_exp_idx" ON public."jansToken" USING btree (del, exp)      |

### jansGrp
| tablename | indexname               | indexdef                                                                               |
| --------- | ----------------------- | -------------------------------------------------------------------------------------- |
| jansGrp   | jansGrp_pkey            | CREATE UNIQUE INDEX "jansGrp_pkey" ON public."jansGrp" USING btree (doc_id)            |
| jansGrp   | jansGrp_description_idx | CREATE INDEX "jansGrp_description_idx" ON public."jansGrp" USING btree (description)   |
| jansGrp   | jansGrp_inum_idx        | CREATE INDEX "jansGrp_inum_idx" ON public."jansGrp" USING btree (inum)                 |
| jansGrp   | jansGrp_displayName_idx | CREATE INDEX "jansGrp_displayName_idx" ON public."jansGrp" USING btree ("displayName") |

### jansAttr
| tablename | indexname                   | indexdef                                                                                       |
| --------- | --------------------------- | ---------------------------------------------------------------------------------------------- |
| jansAttr  | jansAttr_pkey               | CREATE UNIQUE INDEX "jansAttr_pkey" ON public."jansAttr" USING btree (doc_id)                  |
| jansAttr  | jansAttr_description_idx    | CREATE INDEX "jansAttr_description_idx" ON public."jansAttr" USING btree (description)         |
| jansAttr  | jansAttr_jansAttrName_idx   | CREATE INDEX "jansAttr_jansAttrName_idx" ON public."jansAttr" USING btree ("jansAttrName")     |
| jansAttr  | jansAttr_inum_idx           | CREATE INDEX "jansAttr_inum_idx" ON public."jansAttr" USING btree (inum)                       |
| jansAttr  | jansAttr_displayName_idx    | CREATE INDEX "jansAttr_displayName_idx" ON public."jansAttr" USING btree ("displayName")       |
| jansAttr  | jansAttr_jansAttrOrigin_idx | CREATE INDEX "jansAttr_jansAttrOrigin_idx" ON public."jansAttr" USING btree ("jansAttrOrigin") |

### jansPassResetReq
| tablename        | indexname                         | indexdef                                                                                                   |
| ---------------- | --------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| jansPassResetReq | jansPassResetReq_pkey             | CREATE UNIQUE INDEX "jansPassResetReq_pkey" ON public."jansPassResetReq" USING btree (doc_id)              |
| jansPassResetReq | jansPassResetReq_creationDate_idx | CREATE INDEX "jansPassResetReq_creationDate_idx" ON public."jansPassResetReq" USING btree ("creationDate") |

### jansSessId
| tablename  | indexname                   | indexdef                                                                                       |
| ---------- | --------------------------- | ---------------------------------------------------------------------------------------------- |
| jansSessId | jansSessId_pkey             | CREATE UNIQUE INDEX "jansSessId_pkey" ON public."jansSessId" USING btree (doc_id)              |
| jansSessId | jansSessId_sid_idx          | CREATE INDEX "jansSessId_sid_idx" ON public."jansSessId" USING btree (sid)                     |
| jansSessId | jansSessId_deviceSecret_idx | CREATE INDEX "jansSessId_deviceSecret_idx" ON public."jansSessId" USING btree ("deviceSecret") |
| jansSessId | jansSessId_jansUsrDN_idx    | CREATE INDEX "jansSessId_jansUsrDN_idx" ON public."jansSessId" USING btree ("jansUsrDN")       |
| jansSessId | jansSessId_del_exp_idx      | CREATE INDEX "jansSessId_del_exp_idx" ON public."jansSessId" USING btree (del, exp)            |

### jansUmaRPT
| tablename  | indexname       | indexdef                                                                          |
| ---------- | --------------- | --------------------------------------------------------------------------------- |
| jansUmaRPT | jansUmaRPT_pkey | CREATE UNIQUE INDEX "jansUmaRPT_pkey" ON public."jansUmaRPT" USING btree (doc_id) |

### jansPushApp
| tablename   | indexname        | indexdef                                                                            |
| ----------- | ---------------- | ----------------------------------------------------------------------------------- |
| jansPushApp | jansPushApp_pkey | CREATE UNIQUE INDEX "jansPushApp_pkey" ON public."jansPushApp" USING btree (doc_id) |

### jansScr
| tablename | indexname    | indexdef                                                                    |
| --------- | ------------ | --------------------------------------------------------------------------- |
| jansScr   | jansScr_pkey | CREATE UNIQUE INDEX "jansScr_pkey" ON public."jansScr" USING btree (doc_id) |

### jansCustomScr
| tablename     | indexname                    | indexdef                                                                                         |
| ------------- | ---------------------------- | ------------------------------------------------------------------------------------------------ |
| jansCustomScr | jansCustomScr_pkey           | CREATE UNIQUE INDEX "jansCustomScr_pkey" ON public."jansCustomScr" USING btree (doc_id)          |
| jansCustomScr | jansCustomScr_inum_idx       | CREATE INDEX "jansCustomScr_inum_idx" ON public."jansCustomScr" USING btree (inum)               |
| jansCustomScr | jansCustomScr_jansScrTyp_idx | CREATE INDEX "jansCustomScr_jansScrTyp_idx" ON public."jansCustomScr" USING btree ("jansScrTyp") |

### jansDeviceRegistration
| tablename              | indexname                                      | indexdef                                                                                                                             |
| ---------------------- | ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| jansDeviceRegistration | jansDeviceRegistration_pkey                    | CREATE UNIQUE INDEX "jansDeviceRegistration_pkey" ON public."jansDeviceRegistration" USING btree (doc_id)                            |
| jansDeviceRegistration | jansDeviceRegistration_jansDeviceKeyHandle_idx | CREATE INDEX "jansDeviceRegistration_jansDeviceKeyHandle_idx" ON public."jansDeviceRegistration" USING btree ("jansDeviceKeyHandle") |
| jansDeviceRegistration | jansDeviceRegistration_jansApp_idx             | CREATE INDEX "jansDeviceRegistration_jansApp_idx" ON public."jansDeviceRegistration" USING btree ("jansApp")                         |
| jansDeviceRegistration | jansDeviceRegistration_personInum_idx          | CREATE INDEX "jansDeviceRegistration_personInum_idx" ON public."jansDeviceRegistration" USING btree ("personInum")                   |
| jansDeviceRegistration | jansDeviceRegistration_del_exp_idx             | CREATE INDEX "jansDeviceRegistration_del_exp_idx" ON public."jansDeviceRegistration" USING btree (del, exp)                          |
| jansDeviceRegistration | jansDeviceRegistration_jansDeviceHashCode_idx  | CREATE INDEX "jansDeviceRegistration_jansDeviceHashCode_idx" ON public."jansDeviceRegistration" USING btree ("jansDeviceHashCode")   |
| jansDeviceRegistration | jansDeviceRegistration_jansStatus_idx          | CREATE INDEX "jansDeviceRegistration_jansStatus_idx" ON public."jansDeviceRegistration" USING btree ("jansStatus")                   |
| jansDeviceRegistration | jansDeviceRegistration_creationDate_idx        | CREATE INDEX "jansDeviceRegistration_creationDate_idx" ON public."jansDeviceRegistration" USING btree ("creationDate")               |

### jansU2fReq
| tablename  | indexname                   | indexdef                                                                                       |
| ---------- | --------------------------- | ---------------------------------------------------------------------------------------------- |
| jansU2fReq | jansU2fReq_pkey             | CREATE UNIQUE INDEX "jansU2fReq_pkey" ON public."jansU2fReq" USING btree (doc_id)              |
| jansU2fReq | jansU2fReq_del_exp_idx      | CREATE INDEX "jansU2fReq_del_exp_idx" ON public."jansU2fReq" USING btree (del, exp)            |
| jansU2fReq | jansU2fReq_creationDate_idx | CREATE INDEX "jansU2fReq_creationDate_idx" ON public."jansU2fReq" USING btree ("creationDate") |

### jansMetric
| tablename  | indexname                    | indexdef                                                                                         |
| ---------- | ---------------------------- | ------------------------------------------------------------------------------------------------ |
| jansMetric | jansMetric_pkey              | CREATE UNIQUE INDEX "jansMetric_pkey" ON public."jansMetric" USING btree (doc_id)                |
| jansMetric | jansMetric_jansEndDate_idx   | CREATE INDEX "jansMetric_jansEndDate_idx" ON public."jansMetric" USING btree ("jansEndDate")     |
| jansMetric | jansMetric_jansMetricTyp_idx | CREATE INDEX "jansMetric_jansMetricTyp_idx" ON public."jansMetric" USING btree ("jansMetricTyp") |
| jansMetric | jansMetric_jansStartDate_idx | CREATE INDEX "jansMetric_jansStartDate_idx" ON public."jansMetric" USING btree ("jansStartDate") |
| jansMetric | jansMetric_jansAppTyp_idx    | CREATE INDEX "jansMetric_jansAppTyp_idx" ON public."jansMetric" USING btree ("jansAppTyp")       |
| jansMetric | jansMetric_del_exp_idx       | CREATE INDEX "jansMetric_del_exp_idx" ON public."jansMetric" USING btree (del, exp)              |

### jansUmaPCT
| tablename  | indexname              | indexdef                                                                            |
| ---------- | ---------------------- | ----------------------------------------------------------------------------------- |
| jansUmaPCT | jansUmaPCT_pkey        | CREATE UNIQUE INDEX "jansUmaPCT_pkey" ON public."jansUmaPCT" USING btree (doc_id)   |
| jansUmaPCT | jansUmaPCT_tknCde_idx  | CREATE INDEX "jansUmaPCT_tknCde_idx" ON public."jansUmaPCT" USING btree ("tknCde")  |
| jansUmaPCT | jansUmaPCT_del_exp_idx | CREATE INDEX "jansUmaPCT_del_exp_idx" ON public."jansUmaPCT" USING btree (del, exp) |

### jansCache
| tablename | indexname             | indexdef                                                                          |
| --------- | --------------------- | --------------------------------------------------------------------------------- |
| jansCache | jansCache_pkey        | CREATE UNIQUE INDEX "jansCache_pkey" ON public."jansCache" USING btree (doc_id)   |
| jansCache | jansCache_del_exp_idx | CREATE INDEX "jansCache_del_exp_idx" ON public."jansCache" USING btree (del, exp) |

### jansFido2RegistrationEntry
| tablename                  | indexname                                            | indexdef                                                                                                                                         |
| -------------------------- | ---------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| jansFido2RegistrationEntry | jansFido2RegistrationEntry_pkey                      | CREATE UNIQUE INDEX "jansFido2RegistrationEntry_pkey" ON public."jansFido2RegistrationEntry" USING btree (doc_id)                                |
| jansFido2RegistrationEntry | jansFido2RegistrationEntry_jansApp_idx               | CREATE INDEX "jansFido2RegistrationEntry_jansApp_idx" ON public."jansFido2RegistrationEntry" USING btree ("jansApp")                             |
| jansFido2RegistrationEntry | jansFido2RegistrationEntry_jansCodeChallengeHash_idx | CREATE INDEX "jansFido2RegistrationEntry_jansCodeChallengeHash_idx" ON public."jansFido2RegistrationEntry" USING btree ("jansCodeChallengeHash") |
| jansFido2RegistrationEntry | jansFido2RegistrationEntry_jansPublicKeyIdHash_idx   | CREATE INDEX "jansFido2RegistrationEntry_jansPublicKeyIdHash_idx" ON public."jansFido2RegistrationEntry" USING btree ("jansPublicKeyIdHash")     |
| jansFido2RegistrationEntry | jansFido2RegistrationEntry_jansStatus_idx            | CREATE INDEX "jansFido2RegistrationEntry_jansStatus_idx" ON public."jansFido2RegistrationEntry" USING btree ("jansStatus")                       |
| jansFido2RegistrationEntry | jansFido2RegistrationEntry_creationDate_idx          | CREATE INDEX "jansFido2RegistrationEntry_creationDate_idx" ON public."jansFido2RegistrationEntry" USING btree ("creationDate")                   |
| jansFido2RegistrationEntry | jansFido2RegistrationEntry_jansCodeChallenge_idx     | CREATE INDEX "jansFido2RegistrationEntry_jansCodeChallenge_idx" ON public."jansFido2RegistrationEntry" USING btree ("jansCodeChallenge")         |
| jansFido2RegistrationEntry | jansFido2RegistrationEntry_jansPublicKeyId_idx       | CREATE INDEX "jansFido2RegistrationEntry_jansPublicKeyId_idx" ON public."jansFido2RegistrationEntry" USING btree ("jansPublicKeyId")             |
| jansFido2RegistrationEntry | jansFido2RegistrationEntry_personInum_idx            | CREATE INDEX "jansFido2RegistrationEntry_personInum_idx" ON public."jansFido2RegistrationEntry" USING btree ("personInum")                       |
| jansFido2RegistrationEntry | jansFido2RegistrationEntry_del_exp_idx               | CREATE INDEX "jansFido2RegistrationEntry_del_exp_idx" ON public."jansFido2RegistrationEntry" USING btree (del, exp)                              |

### jansPushDevice
| tablename      | indexname           | indexdef                                                                                  |
| -------------- | ------------------- | ----------------------------------------------------------------------------------------- |
| jansPushDevice | jansPushDevice_pkey | CREATE UNIQUE INDEX "jansPushDevice_pkey" ON public."jansPushDevice" USING btree (doc_id) |

### jansClntAuthz
| tablename     | indexname                                | indexdef                                                                                                                                             |
| ------------- | ---------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| jansClntAuthz | jansClntAuthz_pkey                       | CREATE UNIQUE INDEX "jansClntAuthz_pkey" ON public."jansClntAuthz" USING btree (doc_id)                                                              |
| jansClntAuthz | jansClntAuthz_jsonb_path_query_array_idx | CREATE INDEX "jansClntAuthz_jsonb_path_query_array_idx" ON public."jansClntAuthz" USING gin (jsonb_path_query_array("jansClntId", '$[*]'::jsonpath)) |
| jansClntAuthz | jansClntAuthz_del_exp_idx                | CREATE INDEX "jansClntAuthz_del_exp_idx" ON public."jansClntAuthz" USING btree (del, exp)                                                            |
| jansClntAuthz | jansClntAuthz_jansUsrId_idx              | CREATE INDEX "jansClntAuthz_jansUsrId_idx" ON public."jansClntAuthz" USING btree ("jansUsrId")                                                       |

### jansSectorIdentifier
| tablename            | indexname                 | indexdef                                                                                              |
| -------------------- | ------------------------- | ----------------------------------------------------------------------------------------------------- |
| jansSectorIdentifier | jansSectorIdentifier_pkey | CREATE UNIQUE INDEX "jansSectorIdentifier_pkey" ON public."jansSectorIdentifier" USING btree (doc_id) |

### jansFido2AuthnEntry
| tablename           | indexname                                     | indexdef                                                                                                                           |
| ------------------- | --------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| jansFido2AuthnEntry | jansFido2AuthnEntry_pkey                      | CREATE UNIQUE INDEX "jansFido2AuthnEntry_pkey" ON public."jansFido2AuthnEntry" USING btree (doc_id)                                |
| jansFido2AuthnEntry | jansFido2AuthnEntry_jansApp_idx               | CREATE INDEX "jansFido2AuthnEntry_jansApp_idx" ON public."jansFido2AuthnEntry" USING btree ("jansApp")                             |
| jansFido2AuthnEntry | jansFido2AuthnEntry_jansCodeChallengeHash_idx | CREATE INDEX "jansFido2AuthnEntry_jansCodeChallengeHash_idx" ON public."jansFido2AuthnEntry" USING btree ("jansCodeChallengeHash") |
| jansFido2AuthnEntry | jansFido2AuthnEntry_jansStatus_idx            | CREATE INDEX "jansFido2AuthnEntry_jansStatus_idx" ON public."jansFido2AuthnEntry" USING btree ("jansStatus")                       |
| jansFido2AuthnEntry | jansFido2AuthnEntry_creationDate_idx          | CREATE INDEX "jansFido2AuthnEntry_creationDate_idx" ON public."jansFido2AuthnEntry" USING btree ("creationDate")                   |
| jansFido2AuthnEntry | jansFido2AuthnEntry_jansCodeChallenge_idx     | CREATE INDEX "jansFido2AuthnEntry_jansCodeChallenge_idx" ON public."jansFido2AuthnEntry" USING btree ("jansCodeChallenge")         |
| jansFido2AuthnEntry | jansFido2AuthnEntry_personInum_idx            | CREATE INDEX "jansFido2AuthnEntry_personInum_idx" ON public."jansFido2AuthnEntry" USING btree ("personInum")                       |
| jansFido2AuthnEntry | jansFido2AuthnEntry_del_exp_idx               | CREATE INDEX "jansFido2AuthnEntry_del_exp_idx" ON public."jansFido2AuthnEntry" USING btree (del, exp)                              |

### jansRp
| tablename | indexname   | indexdef                                                                  |
| --------- | ----------- | ------------------------------------------------------------------------- |
| jansRp    | jansRp_pkey | CREATE UNIQUE INDEX "jansRp_pkey" ON public."jansRp" USING btree (doc_id) |

### jansCibaReq
| tablename   | indexname                      | indexdef                                                                                              |
| ----------- | ------------------------------ | ----------------------------------------------------------------------------------------------------- |
| jansCibaReq | jansCibaReq_pkey               | CREATE UNIQUE INDEX "jansCibaReq_pkey" ON public."jansCibaReq" USING btree (doc_id)                   |
| jansCibaReq | jansCibaReq_jansStatus_exp_idx | CREATE INDEX "jansCibaReq_jansStatus_exp_idx" ON public."jansCibaReq" USING btree ("jansStatus", exp) |

### jansStatEntry
| tablename     | indexname                | indexdef                                                                                 |
| ------------- | ------------------------ | ---------------------------------------------------------------------------------------- |
| jansStatEntry | jansStatEntry_pkey       | CREATE UNIQUE INDEX "jansStatEntry_pkey" ON public."jansStatEntry" USING btree (doc_id)  |
| jansStatEntry | jansStatEntry_jansId_idx | CREATE INDEX "jansStatEntry_jansId_idx" ON public."jansStatEntry" USING btree ("jansId") |

### jansPar
| tablename | indexname           | indexdef                                                                      |
| --------- | ------------------- | ----------------------------------------------------------------------------- |
| jansPar   | jansPar_pkey        | CREATE UNIQUE INDEX "jansPar_pkey" ON public."jansPar" USING btree (doc_id)   |
| jansPar   | jansPar_jansId_idx  | CREATE INDEX "jansPar_jansId_idx" ON public."jansPar" USING btree ("jansId")  |
| jansPar   | jansPar_del_exp_idx | CREATE INDEX "jansPar_del_exp_idx" ON public."jansPar" USING btree (del, exp) |

### jansInumMap
| tablename   | indexname                  | indexdef                                                                                     |
| ----------- | -------------------------- | -------------------------------------------------------------------------------------------- |
| jansInumMap | jansInumMap_pkey           | CREATE UNIQUE INDEX "jansInumMap_pkey" ON public."jansInumMap" USING btree (doc_id)          |
| jansInumMap | jansInumMap_jansStatus_idx | CREATE INDEX "jansInumMap_jansStatus_idx" ON public."jansInumMap" USING btree ("jansStatus") |
| jansInumMap | jansInumMap_inum_idx       | CREATE INDEX "jansInumMap_inum_idx" ON public."jansInumMap" USING btree (inum)               |

### agmFlowRun
| tablename  | indexname       | indexdef                                                                          |
| ---------- | --------------- | --------------------------------------------------------------------------------- |
| agmFlowRun | agmFlowRun_pkey | CREATE UNIQUE INDEX "agmFlowRun_pkey" ON public."agmFlowRun" USING btree (doc_id) |

### agmFlow
| tablename | indexname    | indexdef                                                                    |
| --------- | ------------ | --------------------------------------------------------------------------- |
| agmFlow   | agmFlow_pkey | CREATE UNIQUE INDEX "agmFlow_pkey" ON public."agmFlow" USING btree (doc_id) |

### adsPrjDeployment
| tablename        | indexname             | indexdef                                                                                      |
| ---------------- | --------------------- | --------------------------------------------------------------------------------------------- |
| adsPrjDeployment | adsPrjDeployment_pkey | CREATE UNIQUE INDEX "adsPrjDeployment_pkey" ON public."adsPrjDeployment" USING btree (doc_id) |

### jansDocument
| tablename    | indexname         | indexdef                                                                              |
| ------------ | ----------------- | ------------------------------------------------------------------------------------- |
| jansDocument | jansDocument_pkey | CREATE UNIQUE INDEX "jansDocument_pkey" ON public."jansDocument" USING btree (doc_id) |
