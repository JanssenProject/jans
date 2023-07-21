---
tags:
  - administration
  - database
  - MySQL
  - Indexes
---

# MySQL Indexes


### adsPrjDeployment
| Table            | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ---------------- | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| adsPrjDeployment | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| adsPrjDeployment | 0          | doc_id   | 1            | doc_id      |      |         |               |

### agmFlow
| Table   | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ------- | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| agmFlow | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| agmFlow | 0          | doc_id   | 1            | doc_id      |      |         |               |

### agmFlowRun
| Table      | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ---------- | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| agmFlowRun | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| agmFlowRun | 0          | doc_id   | 1            | doc_id      |      |         |               |

### jansAppConf
| Table       | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ----------- | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansAppConf | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| jansAppConf | 0          | doc_id   | 1            | doc_id      |      |         |               |

### jansAttr
| Table    | Non_unique | Key_name                | Seq_in_index | Column_name    | Null | Comment | Index_comment |
| -------- | ---------- | ----------------------- | ------------ | -------------- | ---- | ------- | ------------- |
| jansAttr | 0          | PRIMARY                 | 1            | doc_id         |      |         |               |
| jansAttr | 0          | doc_id                  | 1            | doc_id         |      |         |               |
| jansAttr | 1          | jansAttr_description    | 1            | description    | YES  |         |               |
| jansAttr | 1          | jansAttr_displayName    | 1            | displayName    | YES  |         |               |
| jansAttr | 1          | jansAttr_jansAttrName   | 1            | jansAttrName   | YES  |         |               |
| jansAttr | 1          | jansAttr_jansAttrOrigin | 1            | jansAttrOrigin | YES  |         |               |
| jansAttr | 1          | jansAttr_inum           | 1            | inum           | YES  |         |               |

### jansCache
| Table     | Non_unique | Key_name             | Seq_in_index | Column_name | Null | Comment | Index_comment |
| --------- | ---------- | -------------------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansCache | 0          | PRIMARY              | 1            | doc_id      |      |         |               |
| jansCache | 0          | doc_id               | 1            | doc_id      |      |         |               |
| jansCache | 1          | jansCache_CustomIdx1 | 1            | del         | YES  |         |               |
| jansCache | 1          | jansCache_CustomIdx1 | 2            | exp         | YES  |         |               |

### jansCibaReq
| Table       | Non_unique | Key_name               | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ----------- | ---------- | ---------------------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansCibaReq | 0          | PRIMARY                | 1            | doc_id      |      |         |               |
| jansCibaReq | 0          | doc_id                 | 1            | doc_id      |      |         |               |
| jansCibaReq | 1          | jansCibaReq_CustomIdx1 | 1            | jansStatus  | YES  |         |               |
| jansCibaReq | 1          | jansCibaReq_CustomIdx1 | 2            | exp         | YES  |         |               |

### jansClnt
| Table    | Non_unique | Key_name                           | Seq_in_index | Column_name               | Null | Comment | Index_comment |
| -------- | ---------- | ---------------------------------- | ------------ | ------------------------- | ---- | ------- | ------------- |
| jansClnt | 0          | PRIMARY                            | 1            | doc_id                    |      |         |               |
| jansClnt | 0          | doc_id                             | 1            | doc_id                    |      |         |               |
| jansClnt | 1          | jansClnt_displayName               | 1            | displayName               | YES  |         |               |
| jansClnt | 1          | jansClnt_description               | 1            | description               | YES  |         |               |
| jansClnt | 1          | jansClnt_inum                      | 1            | inum                      | YES  |         |               |
| jansClnt | 1          | jansClnt_jansClntSecretExpAt       | 1            | jansClntSecretExpAt       | YES  |         |               |
| jansClnt | 1          | jansClnt_jansRegistrationAccessTkn | 1            | jansRegistrationAccessTkn | YES  |         |               |
| jansClnt | 1          | jansClnt_CustomIdx1                | 1            | del                       | YES  |         |               |
| jansClnt | 1          | jansClnt_CustomIdx1                | 2            | exp                       | YES  |         |               |

### jansClntAuthz
| Table         | Non_unique | Key_name                 | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ------------- | ---------- | ------------------------ | ------------ | ----------- | ---- | ------- | ------------- |
| jansClntAuthz | 0          | PRIMARY                  | 1            | doc_id      |      |         |               |
| jansClntAuthz | 0          | doc_id                   | 1            | doc_id      |      |         |               |
| jansClntAuthz | 1          | jansClntAuthz_jansUsrId  | 1            | jansUsrId   | YES  |         |               |
| jansClntAuthz | 1          | jansClntAuthz_CustomIdx1 | 1            | del         | YES  |         |               |
| jansClntAuthz | 1          | jansClntAuthz_CustomIdx1 | 2            | exp         | YES  |         |               |
| jansClntAuthz | 1          | jansClntId_json_1        | 1            | None        | YES  |         |               |
| jansClntAuthz | 1          | jansClntId_json_2        | 1            | None        | YES  |         |               |
| jansClntAuthz | 1          | jansClntId_json_3        | 1            | None        | YES  |         |               |
| jansClntAuthz | 1          | jansClntId_json_4        | 1            | None        | YES  |         |               |

### jansCustomScr
| Table         | Non_unique | Key_name                 | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ------------- | ---------- | ------------------------ | ------------ | ----------- | ---- | ------- | ------------- |
| jansCustomScr | 0          | PRIMARY                  | 1            | doc_id      |      |         |               |
| jansCustomScr | 0          | doc_id                   | 1            | doc_id      |      |         |               |
| jansCustomScr | 1          | jansCustomScr_inum       | 1            | inum        | YES  |         |               |
| jansCustomScr | 1          | jansCustomScr_jansScrTyp | 1            | jansScrTyp  | YES  |         |               |

### jansDeviceRegistration
| Table                  | Non_unique | Key_name                                   | Seq_in_index | Column_name         | Null | Comment | Index_comment |
| ---------------------- | ---------- | ------------------------------------------ | ------------ | ------------------- | ---- | ------- | ------------- |
| jansDeviceRegistration | 0          | PRIMARY                                    | 1            | doc_id              |      |         |               |
| jansDeviceRegistration | 0          | doc_id                                     | 1            | doc_id              |      |         |               |
| jansDeviceRegistration | 1          | jansDeviceRegistration_jansDeviceKeyHandle | 1            | jansDeviceKeyHandle | YES  |         |               |
| jansDeviceRegistration | 1          | jansDeviceRegistration_jansDeviceHashCode  | 1            | jansDeviceHashCode  | YES  |         |               |
| jansDeviceRegistration | 1          | jansDeviceRegistration_jansApp             | 1            | jansApp             | YES  |         |               |
| jansDeviceRegistration | 1          | jansDeviceRegistration_jansStatus          | 1            | jansStatus          | YES  |         |               |
| jansDeviceRegistration | 1          | jansDeviceRegistration_personInum          | 1            | personInum          | YES  |         |               |
| jansDeviceRegistration | 1          | jansDeviceRegistration_creationDate        | 1            | creationDate        | YES  |         |               |
| jansDeviceRegistration | 1          | jansDeviceRegistration_CustomIdx1          | 1            | del                 | YES  |         |               |
| jansDeviceRegistration | 1          | jansDeviceRegistration_CustomIdx1          | 2            | exp                 | YES  |         |               |

### jansDocument
| Table        | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ------------ | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansDocument | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| jansDocument | 0          | doc_id   | 1            | doc_id      |      |         |               |

### jansFido2AuthnEntry
| Table               | Non_unique | Key_name                                  | Seq_in_index | Column_name           | Null | Comment | Index_comment |
| ------------------- | ---------- | ----------------------------------------- | ------------ | --------------------- | ---- | ------- | ------------- |
| jansFido2AuthnEntry | 0          | PRIMARY                                   | 1            | doc_id                |      |         |               |
| jansFido2AuthnEntry | 0          | doc_id                                    | 1            | doc_id                |      |         |               |
| jansFido2AuthnEntry | 1          | jansFido2AuthnEntry_creationDate          | 1            | creationDate          | YES  |         |               |
| jansFido2AuthnEntry | 1          | jansFido2AuthnEntry_jansApp               | 1            | jansApp               | YES  |         |               |
| jansFido2AuthnEntry | 1          | jansFido2AuthnEntry_jansCodeChallenge     | 1            | jansCodeChallenge     | YES  |         |               |
| jansFido2AuthnEntry | 1          | jansFido2AuthnEntry_jansCodeChallengeHash | 1            | jansCodeChallengeHash | YES  |         |               |
| jansFido2AuthnEntry | 1          | jansFido2AuthnEntry_personInum            | 1            | personInum            | YES  |         |               |
| jansFido2AuthnEntry | 1          | jansFido2AuthnEntry_jansStatus            | 1            | jansStatus            | YES  |         |               |
| jansFido2AuthnEntry | 1          | jansFido2AuthnEntry_CustomIdx1            | 1            | del                   | YES  |         |               |
| jansFido2AuthnEntry | 1          | jansFido2AuthnEntry_CustomIdx1            | 2            | exp                   | YES  |         |               |

### jansFido2RegistrationEntry
| Table                      | Non_unique | Key_name                                         | Seq_in_index | Column_name           | Null | Comment | Index_comment |
| -------------------------- | ---------- | ------------------------------------------------ | ------------ | --------------------- | ---- | ------- | ------------- |
| jansFido2RegistrationEntry | 0          | PRIMARY                                          | 1            | doc_id                |      |         |               |
| jansFido2RegistrationEntry | 0          | doc_id                                           | 1            | doc_id                |      |         |               |
| jansFido2RegistrationEntry | 1          | jansFido2RegistrationEntry_creationDate          | 1            | creationDate          | YES  |         |               |
| jansFido2RegistrationEntry | 1          | jansFido2RegistrationEntry_jansApp               | 1            | jansApp               | YES  |         |               |
| jansFido2RegistrationEntry | 1          | jansFido2RegistrationEntry_jansCodeChallenge     | 1            | jansCodeChallenge     | YES  |         |               |
| jansFido2RegistrationEntry | 1          | jansFido2RegistrationEntry_jansCodeChallengeHash | 1            | jansCodeChallengeHash | YES  |         |               |
| jansFido2RegistrationEntry | 1          | jansFido2RegistrationEntry_jansPublicKeyId       | 1            | jansPublicKeyId       | YES  |         |               |
| jansFido2RegistrationEntry | 1          | jansFido2RegistrationEntry_jansPublicKeyIdHash   | 1            | jansPublicKeyIdHash   | YES  |         |               |
| jansFido2RegistrationEntry | 1          | jansFido2RegistrationEntry_personInum            | 1            | personInum            | YES  |         |               |
| jansFido2RegistrationEntry | 1          | jansFido2RegistrationEntry_jansStatus            | 1            | jansStatus            | YES  |         |               |
| jansFido2RegistrationEntry | 1          | jansFido2RegistrationEntry_CustomIdx1            | 1            | del                   | YES  |         |               |
| jansFido2RegistrationEntry | 1          | jansFido2RegistrationEntry_CustomIdx1            | 2            | exp                   | YES  |         |               |

### jansGrant
| Table     | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| --------- | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansGrant | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| jansGrant | 0          | doc_id   | 1            | doc_id      |      |         |               |

### jansGrp
| Table   | Non_unique | Key_name            | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ------- | ---------- | ------------------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansGrp | 0          | PRIMARY             | 1            | doc_id      |      |         |               |
| jansGrp | 0          | doc_id              | 1            | doc_id      |      |         |               |
| jansGrp | 1          | jansGrp_description | 1            | description | YES  |         |               |
| jansGrp | 1          | jansGrp_displayName | 1            | displayName | YES  |         |               |
| jansGrp | 1          | jansGrp_inum        | 1            | inum        | YES  |         |               |

### jansInumMap
| Table       | Non_unique | Key_name               | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ----------- | ---------- | ---------------------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansInumMap | 0          | PRIMARY                | 1            | doc_id      |      |         |               |
| jansInumMap | 0          | doc_id                 | 1            | doc_id      |      |         |               |
| jansInumMap | 1          | jansInumMap_jansStatus | 1            | jansStatus  | YES  |         |               |
| jansInumMap | 1          | jansInumMap_inum       | 1            | inum        | YES  |         |               |

### jansMetric
| Table      | Non_unique | Key_name                 | Seq_in_index | Column_name   | Null | Comment | Index_comment |
| ---------- | ---------- | ------------------------ | ------------ | ------------- | ---- | ------- | ------------- |
| jansMetric | 0          | PRIMARY                  | 1            | doc_id        |      |         |               |
| jansMetric | 0          | doc_id                   | 1            | doc_id        |      |         |               |
| jansMetric | 1          | jansMetric_jansStartDate | 1            | jansStartDate | YES  |         |               |
| jansMetric | 1          | jansMetric_jansEndDate   | 1            | jansEndDate   | YES  |         |               |
| jansMetric | 1          | jansMetric_jansAppTyp    | 1            | jansAppTyp    | YES  |         |               |
| jansMetric | 1          | jansMetric_jansMetricTyp | 1            | jansMetricTyp | YES  |         |               |
| jansMetric | 1          | jansMetric_CustomIdx1    | 1            | del           | YES  |         |               |
| jansMetric | 1          | jansMetric_CustomIdx1    | 2            | exp           | YES  |         |               |

### jansOrganization
| Table            | Non_unique | Key_name             | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ---------------- | ---------- | -------------------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansOrganization | 0          | PRIMARY              | 1            | doc_id      |      |         |               |
| jansOrganization | 0          | doc_id               | 1            | doc_id      |      |         |               |
| jansOrganization | 1          | jansOrganization_uid | 1            | uid         | YES  |         |               |

### jansPairwiseIdentifier
| Table                  | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ---------------------- | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansPairwiseIdentifier | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| jansPairwiseIdentifier | 0          | doc_id   | 1            | doc_id      |      |         |               |

### jansPar
| Table   | Non_unique | Key_name           | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ------- | ---------- | ------------------ | ------------ | ----------- | ---- | ------- | ------------- |
| jansPar | 0          | PRIMARY            | 1            | doc_id      |      |         |               |
| jansPar | 0          | doc_id             | 1            | doc_id      |      |         |               |
| jansPar | 1          | jansPar_jansId     | 1            | jansId      | YES  |         |               |
| jansPar | 1          | jansPar_CustomIdx1 | 1            | del         | YES  |         |               |
| jansPar | 1          | jansPar_CustomIdx1 | 2            | exp         | YES  |         |               |

### jansPassResetReq
| Table            | Non_unique | Key_name                      | Seq_in_index | Column_name  | Null | Comment | Index_comment |
| ---------------- | ---------- | ----------------------------- | ------------ | ------------ | ---- | ------- | ------------- |
| jansPassResetReq | 0          | PRIMARY                       | 1            | doc_id       |      |         |               |
| jansPassResetReq | 0          | doc_id                        | 1            | doc_id       |      |         |               |
| jansPassResetReq | 1          | jansPassResetReq_creationDate | 1            | creationDate | YES  |         |               |

### jansPerson
| Table      | Non_unique | Key_name               | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ---------- | ---------- | ---------------------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansPerson | 0          | PRIMARY                | 1            | doc_id      |      |         |               |
| jansPerson | 0          | doc_id                 | 1            | doc_id      |      |         |               |
| jansPerson | 1          | jansPerson_displayName | 1            | displayName | YES  |         |               |
| jansPerson | 1          | jansPerson_givenName   | 1            | givenName   | YES  |         |               |
| jansPerson | 1          | jansPerson_inum        | 1            | inum        | YES  |         |               |
| jansPerson | 1          | jansPerson_mail        | 1            | mail        | YES  |         |               |
| jansPerson | 1          | jansPerson_sn          | 1            | sn          | YES  |         |               |
| jansPerson | 1          | jansPerson_uid         | 1            | uid         | YES  |         |               |
| jansPerson | 1          | jansExtUid_json_1      | 1            | None        | YES  |         |               |
| jansPerson | 1          | jansExtUid_json_2      | 1            | None        | YES  |         |               |
| jansPerson | 1          | jansExtUid_json_3      | 1            | None        | YES  |         |               |
| jansPerson | 1          | jansExtUid_json_4      | 1            | None        | YES  |         |               |
| jansPerson | 1          | jansPerson_CustomIdx1  | 1            | None        | YES  |         |               |
| jansPerson | 1          | jansPerson_CustomIdx2  | 1            | None        | YES  |         |               |

### jansPushApp
| Table       | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ----------- | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansPushApp | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| jansPushApp | 0          | doc_id   | 1            | doc_id      |      |         |               |

### jansPushDevice
| Table          | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| -------------- | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansPushDevice | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| jansPushDevice | 0          | doc_id   | 1            | doc_id      |      |         |               |

### jansRp
| Table  | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ------ | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansRp | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| jansRp | 0          | doc_id   | 1            | doc_id      |      |         |               |

### jansScope
| Table     | Non_unique | Key_name              | Seq_in_index | Column_name | Null | Comment | Index_comment |
| --------- | ---------- | --------------------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansScope | 0          | PRIMARY               | 1            | doc_id      |      |         |               |
| jansScope | 0          | doc_id                | 1            | doc_id      |      |         |               |
| jansScope | 1          | jansScope_description | 1            | description | YES  |         |               |
| jansScope | 1          | jansScope_displayName | 1            | displayName | YES  |         |               |
| jansScope | 1          | jansScope_jansId      | 1            | jansId      | YES  |         |               |
| jansScope | 1          | jansScope_CustomIdx1  | 1            | del         | YES  |         |               |
| jansScope | 1          | jansScope_CustomIdx1  | 2            | exp         | YES  |         |               |

### jansScr
| Table   | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ------- | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansScr | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| jansScr | 0          | doc_id   | 1            | doc_id      |      |         |               |

### jansSectorIdentifier
| Table                | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| -------------------- | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansSectorIdentifier | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| jansSectorIdentifier | 0          | doc_id   | 1            | doc_id      |      |         |               |

### jansSessId
| Table      | Non_unique | Key_name                | Seq_in_index | Column_name  | Null | Comment | Index_comment |
| ---------- | ---------- | ----------------------- | ------------ | ------------ | ---- | ------- | ------------- |
| jansSessId | 0          | PRIMARY                 | 1            | doc_id       |      |         |               |
| jansSessId | 0          | doc_id                  | 1            | doc_id       |      |         |               |
| jansSessId | 1          | jansSessId_sid          | 1            | sid          | YES  |         |               |
| jansSessId | 1          | jansSessId_jansUsrDN    | 1            | jansUsrDN    | YES  |         |               |
| jansSessId | 1          | jansSessId_deviceSecret | 1            | deviceSecret | YES  |         |               |
| jansSessId | 1          | jansSessId_CustomIdx1   | 1            | del          | YES  |         |               |
| jansSessId | 1          | jansSessId_CustomIdx1   | 2            | exp          | YES  |         |               |

### jansSsa
| Table   | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ------- | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansSsa | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| jansSsa | 0          | doc_id   | 1            | doc_id      |      |         |               |

### jansStatEntry
| Table         | Non_unique | Key_name             | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ------------- | ---------- | -------------------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansStatEntry | 0          | PRIMARY              | 1            | doc_id      |      |         |               |
| jansStatEntry | 0          | doc_id               | 1            | doc_id      |      |         |               |
| jansStatEntry | 1          | jansStatEntry_jansId | 1            | jansId      | YES  |         |               |

### jansToken
| Table     | Non_unique | Key_name             | Seq_in_index | Column_name | Null | Comment | Index_comment |
| --------- | ---------- | -------------------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansToken | 0          | PRIMARY              | 1            | doc_id      |      |         |               |
| jansToken | 0          | doc_id               | 1            | doc_id      |      |         |               |
| jansToken | 1          | jansToken_authzCode  | 1            | authzCode   | YES  |         |               |
| jansToken | 1          | jansToken_grtId      | 1            | grtId       | YES  |         |               |
| jansToken | 1          | jansToken_tknCde     | 1            | tknCde      | YES  |         |               |
| jansToken | 1          | jansToken_ssnId      | 1            | ssnId       | YES  |         |               |
| jansToken | 1          | jansToken_CustomIdx1 | 1            | del         | YES  |         |               |
| jansToken | 1          | jansToken_CustomIdx1 | 2            | exp         | YES  |         |               |

### jansU2fReq
| Table      | Non_unique | Key_name                | Seq_in_index | Column_name  | Null | Comment | Index_comment |
| ---------- | ---------- | ----------------------- | ------------ | ------------ | ---- | ------- | ------------- |
| jansU2fReq | 0          | PRIMARY                 | 1            | doc_id       |      |         |               |
| jansU2fReq | 0          | doc_id                  | 1            | doc_id       |      |         |               |
| jansU2fReq | 1          | jansU2fReq_creationDate | 1            | creationDate | YES  |         |               |
| jansU2fReq | 1          | jansU2fReq_CustomIdx1   | 1            | del          | YES  |         |               |
| jansU2fReq | 1          | jansU2fReq_CustomIdx1   | 2            | exp          | YES  |         |               |

### jansUmaPCT
| Table      | Non_unique | Key_name              | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ---------- | ---------- | --------------------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansUmaPCT | 0          | PRIMARY               | 1            | doc_id      |      |         |               |
| jansUmaPCT | 0          | doc_id                | 1            | doc_id      |      |         |               |
| jansUmaPCT | 1          | jansUmaPCT_tknCde     | 1            | tknCde      | YES  |         |               |
| jansUmaPCT | 1          | jansUmaPCT_CustomIdx1 | 1            | del         | YES  |         |               |
| jansUmaPCT | 1          | jansUmaPCT_CustomIdx1 | 2            | exp         | YES  |         |               |

### jansUmaRPT
| Table      | Non_unique | Key_name | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ---------- | ---------- | -------- | ------------ | ----------- | ---- | ------- | ------------- |
| jansUmaRPT | 0          | PRIMARY  | 1            | doc_id      |      |         |               |
| jansUmaRPT | 0          | doc_id   | 1            | doc_id      |      |         |               |

### jansUmaResource
| Table           | Non_unique | Key_name                     | Seq_in_index | Column_name  | Null | Comment | Index_comment |
| --------------- | ---------- | ---------------------------- | ------------ | ------------ | ---- | ------- | ------------- |
| jansUmaResource | 0          | PRIMARY                      | 1            | doc_id       |      |         |               |
| jansUmaResource | 0          | doc_id                       | 1            | doc_id       |      |         |               |
| jansUmaResource | 1          | jansUmaResource_displayName  | 1            | displayName  | YES  |         |               |
| jansUmaResource | 1          | jansUmaResource_jansUmaScope | 1            | jansUmaScope | YES  |         |               |
| jansUmaResource | 1          | jansUmaResource_jansId       | 1            | jansId       | YES  |         |               |
| jansUmaResource | 1          | jansUmaResource_CustomIdx1   | 1            | del          | YES  |         |               |
| jansUmaResource | 1          | jansUmaResource_CustomIdx1   | 2            | exp          | YES  |         |               |

### jansUmaResourcePermission
| Table                     | Non_unique | Key_name                             | Seq_in_index | Column_name | Null | Comment | Index_comment |
| ------------------------- | ---------- | ------------------------------------ | ------------ | ----------- | ---- | ------- | ------------- |
| jansUmaResourcePermission | 0          | PRIMARY                              | 1            | doc_id      |      |         |               |
| jansUmaResourcePermission | 0          | doc_id                               | 1            | doc_id      |      |         |               |
| jansUmaResourcePermission | 1          | jansUmaResourcePermission_jansTicket | 1            | jansTicket  | YES  |         |               |
