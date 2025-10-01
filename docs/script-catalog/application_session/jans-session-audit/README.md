## README Auditing Authentication Attempts

Name of the script in **janssen** (for example, using **/opt/jans/jans-cli/config-cli.py** or **/opt/jans/jans-cli/config-cli-tui.py**): **jans-session-audit**.  
Script type:  **Application Sessions**.  

The script generates audit report and creates record in DB. Also **jansData** field (in JSON format), that contains summary info of session audit will be generated.  

Following are the configuration properties for the *jans-session-audit.py* script (**jans-session-audit**):  

Parameters of the script:

- **metric_audit_ou_name**: Name of the audit OU. For example: **jans_auth**.  

- **metric_audit_conf_json_file_path**: configuration file. For example: **/etc/jans/conf/jans-session-audit.json**  

File should contain follow properties:  
1. **event_types**.  
    Type of Session event(s), that will be saved in the DB:  
    - **AUTHENTICATED**
    - **UNAUTHENTICATED**
    - **UPDATED**
    - **GONE**  
    .

1. **audit_data**.  
    List of properties of **Session** (**SessionId**):
    - **userDn**
    - **id**
    - **outsideSid**
    - **lastUsedAt**
    - **authenticationTime**
    - **state**
    - **expirationDate**
    - **sessionState**
    - **permissionGranted**
    - **permissionGrantedMap**
    - **deviceSecrets**  
    .

1. **audit_cust_data**.  
    List of **Extra Session Attributes** (**SessionId**):
    - **auth_external_attributes**
    - **opbs**
    - **response_type**
    - **client_id**
    - **auth_step**
    - **acr**
    - **casa_logoUrl**
    - **remote_ip**
    - **scope**
    - **acr_values**
    - **casa_faviconUrl**
    - **redirect_uri**
    - **state**
    - **casa_prefix**
    - **casa_contextPath**
    - **casa_extraCss**  
    .

1. **audit_cust_data** Can contain only one attribute:
    - **sessionAttributes**  
    .
    
    The value (**sessionAttributes**) covers list of all **Extra Session Attributes** (**SessionId**):
    - **auth_external_attributes**
    - **opbs**
    - **response_type**
    - **client_id**
    - **auth_step**
    - **acr**
    - **casa_logoUrl**
    - **remote_ip**
    - **scope**
    - **acr_values**
    - **casa_faviconUrl**
    - **redirect_uri**
    - **state**
    - **casa_prefix**
    - **casa_contextPath**
    - **casa_extraCss**  
    .    

Generated value **jansData** in JSON format will contain type of event (one of **AUTHENTICATED**, **UNAUTHENTICATED**, **UPDATED**, **GONE**) and all properties/attributes defined by **audit_data** and **audit_cust_data**.

Examples of **/etc/jans/conf/jans-session-audit.json** file: 

##

```json
{
    "event_types": [ "AUTHENTICATED", "UNAUTHENTICATED", "UPDATED", "GONE" ],
    "audit_data": [
        "userDn",
        "id",
        "outsideSid",
        "lastUsedAt",
        "authenticationTime",
        "state",
        "expirationDate",
        "sessionState",
        "permissionGranted",
        "permissionGrantedMap",
        "deviceSecrets"
    ],
    "audit_cust_data": [
        "auth_external_attributes",
        "opbs",
        "response_type",
        "client_id",
        "auth_step",
        "acr",
        "casa_logoUrl",
        "remote_ip",
        "scope",
        "acr_values",
        "casa_faviconUrl",
        "redirect_uri",
        "state",
        "casa_prefix",
        "casa_contextPath",
        "casa_extraCss"
    ]
}
```

##

```json
{
    "event_types": [ "AUTHENTICATED","GONE" ],
    "audit_data": [
        "userDn",
        "id",
        "outsideSid",
        "lastUsedAt",
        "authenticationTime",
        "state",
        "expirationDate",
        "sessionState",
        "permissionGranted",
        "permissionGrantedMap",
        "deviceSecrets"
    ],
    "audit_cust_data": [
        "sessionAttributes"
    ]
}
```

##

- **log_level**: level of log out in the file: **/opt/jans/jetty/jans-auth/logs/jans-auth_script.log**.  
    Values:
    - **DEBUG**
    - **INFO**
    - **ERROR**

.

Examples of generated **jansData** (JSON format):

##

**/etc/jans/conf/jans-session-audit.json**:

```json
{
    "event_types": [ "AUTHENTICATED","GONE" ],
    "audit_data": [
        "userDn",
        "id",
        "outsideSid",
        "lastUsedAt",
        "authenticationTime",
        "state",
        "expirationDate",
        "sessionState",
        "permissionGranted",
        "permissionGrantedMap",
        "deviceSecrets"
    ],
    "audit_cust_data": [
        "auth_external_attributes",
        "opbs",
        "response_type",
        "client_id",
        "auth_step",
        "acr",
        "casa_logoUrl",
        "remote_ip",
        "scope",
        "acr_values",
        "casa_faviconUrl",
        "redirect_uri",
        "state",
        "casa_prefix",
        "casa_contextPath",
        "casa_extraCss"
    ]
}
```

**jansData**:

```json
{
    "type": "AUTHENTICATED",
    "authenticationTime": "Sun Jul 16 10:17:48 CDT 2023",
    "deviceSecrets": "[]",
    "permissionGranted": "None",
    "sessionState": "bc85eaccff71f7145057405363f60bbc7d7d9733faed52a84ca79eaca56248c3.b8eafaa1-b08d-414c-b15d-883ca0eb363c",
    "lastUsedAt": "Sun Jul 16 10:17:48 CDT 2023",
    "outsideSid": "3f2e08a6-06c2-4444-a2dd-1368da4e22da",
    "id": "cb9a92d2-e1e7-4754-943e-354b976938bc",
    "authState": "authenticated",
    "userDn": "inum=32c32267-6f8a-4cac-88e4-643e3abcfe6c,ou=people,o=jans",
    "expirationDate": "Mon Jul 17 10:17:31 CDT 2023",
    "permissionGrantedMap": {
        "3000.79c97b57-0bf8-4d5b-8f9c-918eff96e8b8": false
    },
    "opbs": "a8e3a9fb-f9f6-42b1-a42b-795f195407b7",
    "responseType": "code",
    "clientId": "3000.79c97b57-0bf8-4d5b-8f9c-918eff96e8b8",
    "authStep": "1",
    "acr": "casa",
    "casaLogoUrl": "/casa/images/logo.png",
    "remoteIp": "192.168.64.2",
    "scope": "openid profile user_name clientinfo",
    "acrValues": "casa",
    "casaFaviconUrl": "/casa/images/favicon.ico",
    "redirectUri": "https://gluu-1.smansoft.net/casa",
    "state": "Hr9ZeBdWVBQTyAMKna-GROKaLeqnpFkIngYImTmSIdk",
    "casaPrefix": "",
    "casaContextPath": "/casa",
    "casaExtraCss": "None",
    "authExternalAttributes": [
        {
            "casa_logoUrl": "java.lang.String"
        },
        {
            "casa_faviconUrl": "java.lang.String"
        },
        {
            "casa_prefix": "java.lang.String"
        },
        {
            "casa_contextPath": "java.lang.String"
        }
    ]
}
```

##

**/etc/jans/conf/jans-session-audit.json**: 

```json
{
    "event_types": [ "AUTHENTICATED","GONE" ],
    "audit_data": [
    ],
    "audit_cust_data": [
    ]
}
```

**jansData**:

```json
{
    "type": "GONE"
}
```

##

**/etc/jans/conf/jans-session-audit.json**: 

```json
{
    "event_types": [ "AUTHENTICATED","GONE" ],
    "audit_data": [
        "userDn",
        "id",
        "state",
        "permissionGrantedMap",
        "deviceSecrets"
    ],
    "audit_cust_data": [
    ]
}
```

**jansData**:

```json
{
    "type": "AUTHENTICATED",
    "deviceSecrets": "[]",
    "id": "8103b097-964c-4e66-8dea-c0edcfdb0438",
    "authState": "authenticated",
    "userDn": "inum=32c32267-6f8a-4cac-88e4-643e3abcfe6c,ou=people,o=jans",
    "permissionGrantedMap": {
        "3000.79c97b57-0bf8-4d5b-8f9c-918eff96e8b8": false
    }
}
```

##

**/etc/jans/conf/jans-session-audit.json**: 

```json
{
    "event_types": [ "AUTHENTICATED","GONE" ],
    "audit_data": [
        "userDn",
        "id"
    ],
    "audit_cust_data": [
        "sessionAttributes"
    ]
}
```

**jansData**:

```json

{
    "type": "AUTHENTICATED",
    "id": "426d82e8-3182-4f5e-9b1a-54fa1d9fc295",
    "userDn": "inum=32c32267-6f8a-4cac-88e4-643e3abcfe6c,ou=people,o=jans",
    "opbs": "c8727cf7-f136-4555-be86-5313039f874b",
    "responseType": "code",
    "clientId": "3000.79c97b57-0bf8-4d5b-8f9c-918eff96e8b8",
    "authStep": "1",
    "acr": "casa",
    "casaLogoUrl": "/casa/images/logo.png",
    "remoteIp": "192.168.64.2",
    "scope": "openid profile user_name clientinfo",
    "acrValues": "casa",
    "casaFaviconUrl": "/casa/images/favicon.ico",
    "redirectUri": "https://gluu-1.smansoft.net/casa",
    "state": "PEIbloYE4OUy4hkaT75uzDUuiRtQO0v5P_uy0q8iBss",
    "casaPrefix": "",
    "casaContextPath": "/casa",
    "casaExtraCss": "None",
    "authExternalAttributes": [
        {
            "casa_logoUrl": "java.lang.String"
        },
        {
            "casa_faviconUrl": "java.lang.String"
        },
        {
            "casa_prefix": "java.lang.String"
        },
        {
            "casa_contextPath": "java.lang.String"
        }
    ]
}
```

##
