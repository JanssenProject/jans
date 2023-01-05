---
tags:
  - administration
  - reference
  - database
  - Indexes
---

## Indexes

### agmFlow                    

|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| agmFlow |          0 | PRIMARY  |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| agmFlow |          0 | doc_id   |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |


### agmFlowRun                 
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| agmFlowRun |          0 | PRIMARY  |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| agmFlowRun |          0 | doc_id   |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |

### jansAdminConfDyn 
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansAdminConfDyn |          0 | PRIMARY  |            1 | doc_id      | A         |           1 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansAdminConfDyn |          0 | doc_id   |            1 | doc_id      | A         |           1 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
          


### jansAppConf                
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansAppConf |          0 | PRIMARY  |            1 | doc_id      | A         |           5 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansAppConf |          0 | doc_id   |            1 | doc_id      | A         |           5 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |

### jansAttr      
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansAttr |          0 | PRIMARY                 |            1 | doc_id         | A         |          71 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansAttr |          0 | doc_id                  |            1 | doc_id         | A         |          71 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansAttr |          1 | jansAttr_description    |            1 | description    | A         |          70 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansAttr |          1 | jansAttr_displayName    |            1 | displayName    | A         |          71 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansAttr |          1 | jansAttr_jansAttrName   |            1 | jansAttrName   | A         |          71 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansAttr |          1 | jansAttr_jansAttrOrigin |            1 | jansAttrOrigin | A         |           3 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansAttr |          1 | jansAttr_inum           |            1 | inum           | A         |          71 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
             
### jansCache                  
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansCache |          0 | PRIMARY              |            1 | doc_id      | A         |           1 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansCache |          0 | doc_id               |            1 | doc_id      | A         |           1 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansCache |          1 | jansCache_CustomIdx1 |            1 | del         | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansCache |          1 | jansCache_CustomIdx1 |            2 | exp         | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |

### jansCibaReq                
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansCibaReq |          0 | PRIMARY                |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL                                                                        |
| jansCibaReq |          0 | doc_id                 |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL                                                                        |
| jansCibaReq |          1 | jansCibaReq_CustomIdx1 |            1 | jansStatus  | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                        |
| jansCibaReq |          1 | jansCibaReq_CustomIdx1 |            2 | exp         | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                        |
| jansCibaReq |          1 | clnId_json_1           |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v\') as char(128) array)              |
| jansCibaReq |          1 | clnId_json_2           |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v[0]\') as char(128) charset utf8mb4) |
| jansCibaReq |          1 | clnId_json_3           |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v[1]\') as char(128) charset utf8mb4) |
| jansCibaReq |          1 | clnId_json_4           |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v[2]\') as char(128) charset utf8mb4) |

### jansClnt 
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansClnt |          0 | PRIMARY                            |            1 | doc_id                    | A         |           3 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansClnt |          0 | doc_id                             |            1 | doc_id                    | A         |           3 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansClnt |          1 | jansClnt_displayName               |            1 | displayName               | A         |           3 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansClnt |          1 | jansClnt_description               |            1 | description               | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansClnt |          1 | jansClnt_inum                      |            1 | inum                      | A         |           3 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansClnt |          1 | jansClnt_jansClntSecretExpAt       |            1 | jansClntSecretExpAt       | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansClnt |          1 | jansClnt_jansRegistrationAccessTkn |            1 | jansRegistrationAccessTkn | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansClnt |          1 | jansClnt_CustomIdx1                |            1 | del                       | A         |           2 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansClnt |          1 | jansClnt_CustomIdx1                |            2 | exp                       | A         |           2 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
                  
### jansClntAuthz
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansClntAuthz |          0 | PRIMARY                  |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL                                                                             |
| jansClntAuthz |          0 | doc_id                   |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL                                                                             |
| jansClntAuthz |          1 | jansClntAuthz_jansUsrId  |            1 | jansUsrId   | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                             |
| jansClntAuthz |          1 | jansClntAuthz_CustomIdx1 |            1 | del         | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                             |
| jansClntAuthz |          1 | jansClntAuthz_CustomIdx1 |            2 | exp         | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                             |
| jansClntAuthz |          1 | jansClntId_json_1        |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`jansClntId`,_utf8mb4\'$.v\') as char(128) array)              |
| jansClntAuthz |          1 | jansClntId_json_2        |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`jansClntId`,_utf8mb4\'$.v[0]\') as char(128) charset utf8mb4) |
| jansClntAuthz |          1 | jansClntId_json_3        |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`jansClntId`,_utf8mb4\'$.v[1]\') as char(128) charset utf8mb4) |
| jansClntAuthz |          1 | jansClntId_json_4        |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`jansClntId`,_utf8mb4\'$.v[2]\') as char(128) charset utf8mb4) |
              
### jansCustomScr              
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansCustomScr |          0 | PRIMARY                  |            1 | doc_id      | A         |          26 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansCustomScr |          0 | doc_id                   |            1 | doc_id      | A         |          27 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansCustomScr |          1 | jansCustomScr_inum       |            1 | inum        | A         |          27 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansCustomScr |          1 | jansCustomScr_jansScrTyp |            1 | jansScrTyp  | A         |          19 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |

### jansDeviceRegistration     
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansDeviceRegistration |          0 | PRIMARY                                    |            1 | doc_id              | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansDeviceRegistration |          0 | doc_id                                     |            1 | doc_id              | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansDeviceRegistration |          1 | jansDeviceRegistration_jansDeviceKeyHandle |            1 | jansDeviceKeyHandle | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansDeviceRegistration |          1 | jansDeviceRegistration_jansDeviceHashCode  |            1 | jansDeviceHashCode  | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansDeviceRegistration |          1 | jansDeviceRegistration_jansApp             |            1 | jansApp             | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansDeviceRegistration |          1 | jansDeviceRegistration_jansStatus          |            1 | jansStatus          | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansDeviceRegistration |          1 | jansDeviceRegistration_personInum          |            1 | personInum          | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansDeviceRegistration |          1 | jansDeviceRegistration_creationDate        |            1 | creationDate        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansDeviceRegistration |          1 | jansDeviceRegistration_CustomIdx1          |            1 | del                 | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansDeviceRegistration |          1 | jansDeviceRegistration_CustomIdx1          |            2 | exp                 | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |

### jansDocument               
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansDocument |          0 | PRIMARY  |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansDocument |          0 | doc_id   |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |

### jansFido2AuthnEntry        
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansFido2AuthnEntry |          0 | PRIMARY                          |            1 | doc_id       | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansFido2AuthnEntry |          0 | doc_id                           |            1 | doc_id       | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansFido2AuthnEntry |          1 | jansFido2AuthnEntry_creationDate |            1 | creationDate | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansFido2AuthnEntry |          1 | jansFido2AuthnEntry_personInum   |            1 | personInum   | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansFido2AuthnEntry |          1 | jansFido2AuthnEntry_jansStatus   |            1 | jansStatus   | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |

### jansFido2RegistrationEntry 
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansFido2RegistrationEntry |          0 | PRIMARY                                 |            1 | doc_id       | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansFido2RegistrationEntry |          0 | doc_id                                  |            1 | doc_id       | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansFido2RegistrationEntry |          1 | jansFido2RegistrationEntry_creationDate |            1 | creationDate | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansFido2RegistrationEntry |          1 | jansFido2RegistrationEntry_personInum   |            1 | personInum   | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansFido2RegistrationEntry |          1 | jansFido2RegistrationEntry_jansStatus   |            1 | jansStatus   | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |


### jansGrant                  
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansGrant |          0 | PRIMARY  |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansGrant |          0 | doc_id   |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |

### jansGrp   
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansGrp |          0 | PRIMARY             |            1 | doc_id      | A         |           1 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansGrp |          0 | doc_id              |            1 | doc_id      | A         |           1 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansGrp |          1 | jansGrp_description |            1 | description | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansGrp |          1 | jansGrp_displayName |            1 | displayName | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansGrp |          1 | jansGrp_inum        |            1 | inum        | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |

                 
### jansInumMap       
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansInumMap |          0 | PRIMARY                |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansInumMap |          0 | doc_id                 |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansInumMap |          1 | jansInumMap_jansStatus |            1 | jansStatus  | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansInumMap |          1 | jansInumMap_inum       |            1 | inum        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
         
### jansMetric        
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansMetric |          0 | PRIMARY                  |            1 | doc_id        | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansMetric |          0 | doc_id                   |            1 | doc_id        | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansMetric |          1 | jansMetric_jansStartDate |            1 | jansStartDate | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansMetric |          1 | jansMetric_jansEndDate   |            1 | jansEndDate   | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansMetric |          1 | jansMetric_jansAppTyp    |            1 | jansAppTyp    | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansMetric |          1 | jansMetric_jansMetricTyp |            1 | jansMetricTyp | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansMetric |          1 | jansMetric_CustomIdx1    |            1 | del           | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansMetric |          1 | jansMetric_CustomIdx1    |            2 | exp           | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
         
### jansOrganization   
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansOrganization |          0 | PRIMARY              |            1 | doc_id      | A         |           1 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansOrganization |          0 | doc_id               |            1 | doc_id      | A         |           1 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansOrganization |          1 | jansOrganization_uid |            1 | uid         | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
        
### jansPairwiseIdentifier    
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansPairwiseIdentifier |          0 | PRIMARY  |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansPairwiseIdentifier |          0 | doc_id   |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
 
### jansPar            
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansPar |          0 | PRIMARY            |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansPar |          0 | doc_id             |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansPar |          1 | jansPar_jansId     |            1 | jansId      | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansPar |          1 | jansPar_CustomIdx1 |            1 | del         | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansPar |          1 | jansPar_CustomIdx1 |            2 | exp         | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
        
### jansPassResetReq     
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|      
| jansPassResetReq |          0 | PRIMARY                       |            1 | doc_id       | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansPassResetReq |          0 | doc_id                        |            1 | doc_id       | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansPassResetReq |          1 | jansPassResetReq_creationDate |            1 | creationDate | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |

### jansPerson

`show index from jansPerson;`

|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansPerson |          0 | PRIMARY                |            1 | doc_id      | A         |           1 |     NULL |   NULL |      | BTREE      |   |               | YES     | NULL|
| jansPerson |          0 | doc_id                 |            1 | doc_id      | A         |           1 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL                                                                             |
| jansPerson |          1 | jansPerson_displayName |            1 | displayName | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                             |
| jansPerson |          1 | jansPerson_givenName   |            1 | givenName   | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                             |
| jansPerson |          1 | jansPerson_inum        |            1 | inum        | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                             |
| jansPerson |          1 | jansPerson_mail        |            1 | mail        | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                             |
| jansPerson |          1 | jansPerson_sn          |            1 | sn          | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                             |
| jansPerson |          1 | jansPerson_uid         |            1 | uid         | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                             |
| jansPerson |          1 | jansExtUid_json_1      |            1 | NULL        | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`jansExtUid`,_utf8mb4\'$.v\') as char(128) array)              |
| jansPerson |          1 | jansExtUid_json_2      |            1 | NULL        | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`jansExtUid`,_utf8mb4\'$.v[0]\') as char(128) charset utf8mb4) |
| jansPerson |          1 | jansExtUid_json_3      |            1 | NULL        | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`jansExtUid`,_utf8mb4\'$.v[1]\') as char(128) charset utf8mb4) |
| jansPerson |          1 | jansExtUid_json_4      |            1 | NULL        | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`jansExtUid`,_utf8mb4\'$.v[2]\') as char(128) charset utf8mb4) |
| jansPerson |          1 | jansPerson_CustomIdx1  |            1 | NULL        | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | lower(`uid`)                                                                     |
| jansPerson |          1 | jansPerson_CustomIdx2  |            1 | NULL        | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | lower(`mail`)                                                                    |


### jansPushApp
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansPushApp |          0 | PRIMARY  |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansPushApp |          0 | doc_id   |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
                
### jansPushDevice 
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansPushDevice |          0 | PRIMARY  |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansPushDevice |          0 | doc_id   |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |

            
### jansRp        
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansRp |          0 | PRIMARY  |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansRp |          0 | doc_id   |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
                         
### jansScope     
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansScope |          0 | PRIMARY               |            1 | doc_id      | A         |          65 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansScope |          0 | doc_id                |            1 | doc_id      | A         |          65 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansScope |          1 | jansScope_description |            1 | description | A         |          65 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansScope |          1 | jansScope_displayName |            1 | displayName | A         |          65 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansScope |          1 | jansScope_jansId      |            1 | jansId      | A         |          65 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansScope |          1 | jansScope_CustomIdx1  |            1 | del         | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansScope |          1 | jansScope_CustomIdx1  |            2 | exp         | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
                          
### jansScr     
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansScr |          0 | PRIMARY  |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansScr |          0 | doc_id   |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
                            
### jansSectorIdentifier    
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansSectorIdentifier |          0 | PRIMARY  |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansSectorIdentifier |          0 | doc_id   |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
               
### jansSessId         
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansSessId |          0 | PRIMARY                 |            1 | doc_id       | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansSessId |          0 | doc_id                  |            1 | doc_id       | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansSessId |          1 | jansSessId_sid          |            1 | sid          | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansSessId |          1 | jansSessId_jansUsrDN    |            1 | jansUsrDN    | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansSessId |          1 | jansSessId_deviceSecret |            1 | deviceSecret | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansSessId |          1 | jansSessId_CustomIdx1   |            1 | del          | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansSessId |          1 | jansSessId_CustomIdx1   |            2 | exp          | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |

                     
### jansSsa         
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansSsa |          0 | PRIMARY  |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansSsa |          0 | doc_id   |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
                        
### jansStatEntry         
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansStatEntry |          0 | PRIMARY              |            1 | doc_id      | A         |           1 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansStatEntry |          0 | doc_id               |            1 | doc_id      | A         |           1 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansStatEntry |          1 | jansStatEntry_jansId |            1 | jansId      | A         |           1 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
                  
### jansToken         
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansToken |          0 | PRIMARY              |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL                                                                        |
| jansToken |          0 | doc_id               |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL                                                                        |
| jansToken |          1 | jansToken_CustomIdx1 |            1 | del         | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                        |
| jansToken |          1 | jansToken_CustomIdx1 |            2 | exp         | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                        |
| jansToken |          1 | clnId_json_1         |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v\') as char(128) array)              |
| jansToken |          1 | clnId_json_2         |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v[0]\') as char(128) charset utf8mb4) |
| jansToken |          1 | clnId_json_3         |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v[1]\') as char(128) charset utf8mb4) |
| jansToken |          1 | clnId_json_4         |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v[2]\') as char(128) charset utf8mb4) |
                      
### jansU2fReq        
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansU2fReq |          0 | PRIMARY                 |            1 | doc_id       | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansU2fReq |          0 | doc_id                  |            1 | doc_id       | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansU2fReq |          1 | jansU2fReq_creationDate |            1 | creationDate | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansU2fReq |          1 | jansU2fReq_CustomIdx1   |            1 | del          | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansU2fReq |          1 | jansU2fReq_CustomIdx1   |            2 | exp          | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
                      
### jansUmaPCT         
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansUmaPCT |          0 | PRIMARY               |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL                                                                        |
| jansUmaPCT |          0 | doc_id                |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL                                                                        |
| jansUmaPCT |          1 | jansUmaPCT_tknCde     |            1 | tknCde      | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                        |
| jansUmaPCT |          1 | jansUmaPCT_CustomIdx1 |            1 | del         | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                        |
| jansUmaPCT |          1 | jansUmaPCT_CustomIdx1 |            2 | exp         | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL                                                                        |
| jansUmaPCT |          1 | clnId_json_1          |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v\') as char(128) array)              |
| jansUmaPCT |          1 | clnId_json_2          |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v[0]\') as char(128) charset utf8mb4) |
| jansUmaPCT |          1 | clnId_json_3          |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v[1]\') as char(128) charset utf8mb4) |
| jansUmaPCT |          1 | clnId_json_4          |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v[2]\') as char(128) charset utf8mb4) |
                     
### jansUmaRPT           
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansUmaRPT |          0 | PRIMARY      |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL                                                                        |
| jansUmaRPT |          0 | doc_id       |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL                                                                        |
| jansUmaRPT |          1 | clnId_json_1 |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v\') as char(128) array)              |
| jansUmaRPT |          1 | clnId_json_2 |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v[0]\') as char(128) charset utf8mb4) |
| jansUmaRPT |          1 | clnId_json_3 |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v[1]\') as char(128) charset utf8mb4) |
| jansUmaRPT |          1 | clnId_json_4 |            1 | NULL        | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | cast(json_extract(`clnId`,_utf8mb4\'$.v[2]\') as char(128) charset utf8mb4) |
                   
### jansUmaResource             
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansUmaResource |          0 | PRIMARY                      |            1 | doc_id       | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansUmaResource |          0 | doc_id                       |            1 | doc_id       | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansUmaResource |          1 | jansUmaResource_displayName  |            1 | displayName  | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansUmaResource |          1 | jansUmaResource_jansUmaScope |            1 | jansUmaScope | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansUmaResource |          1 | jansUmaResource_jansId       |            1 | jansId       | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansUmaResource |          1 | jansUmaResource_CustomIdx1   |            1 | del          | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
| jansUmaResource |          1 | jansUmaResource_CustomIdx1   |            2 | exp          | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
            
### jansUmaResourcePermission  
|Table | Non_unique | Key_name| Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment | Visible | Expression |
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| jansUmaResourcePermission |          0 | PRIMARY                              |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansUmaResourcePermission |          0 | doc_id                               |            1 | doc_id      | A         |           0 |     NULL |   NULL |      | BTREE      |         |               | YES     | NULL       |
| jansUmaResourcePermission |          1 | jansUmaResourcePermission_jansTicket |            1 | jansTicket  | A         |           0 |     NULL |   NULL | YES  | BTREE      |         |               | YES     | NULL       |
            