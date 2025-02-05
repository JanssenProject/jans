//
//  Bootstrap.swift
//  iOSApp
//
//  Created by Arnab Dutta on 05/02/25.
//

import Foundation

struct Bootstrap: Codable {
    var json: String 
    
    init() {
        if let policyStoreUrl = Bundle.main.path(forResource: "policy-store", ofType: "json") {
            print(policyStoreUrl)
            
            self.json = """
{
"CEDARLING_APPLICATION_NAME": "My App",
               "CEDARLING_TOKEN_CONFIGS": {
                   "access_token": {
                       "entity_type_name": "Jans::Access_token",
                       "iss": "disabled",
                       "jti": "disabled",
                       "nbf": "disabled",
                       "exp": "disabled"
                   },
                   "id_token": {
                       "entity_type_name": "Jans::id_token",
                       "iss": "enabled",
                       "aud": "enabled",
                       "sub": "enabled",
                       "iat": "enabled",
                       "exp": "enabled"
                   },
                   "userinfo_token": {
                       "entity_type_name": "Jans::Userinfo_token",
                       "iss": "disabled",
                       "aud": "disabled",
                       "sub": "disabled",
                       "exp": "disabled",
                       "amr": "disabled",
                       "acr": "disabled",
                       "iat": "disabled"

                   }
               },
               "CEDARLING_AUDIT_HEALTH_INTERVAL": 0,
               "CEDARLING_AUDIT_TELEMETRY_INTERVAL": 0,
               "CEDARLING_DYNAMIC_CONFIGURATION": "disabled",
               "CEDARLING_ID_TOKEN_TRUST_MODE": "strict",
               "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
                   "HS256",
                   "RS256"
               ],
               "CEDARLING_JWT_SIG_VALIDATION": "disabled",
               "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
               "CEDARLING_LISTEN_SSE": "disabled",
               "CEDARLING_LOCAL_JWKS": null,
               "CEDARLING_LOCAL_POLICY_STORE": null,
               "CEDARLING_LOCK": "disabled",
               "CEDARLING_LOCK_MASTER_CONFIGURATION_URI": null,
               "CEDARLING_LOCK_SSA_JWT": null,
               "CEDARLING_LOG_LEVEL": "DEBUG",
               "CEDARLING_LOG_TTL": 120,
               "CEDARLING_LOG_TYPE": "memory",
               "CEDARLING_POLICY_STORE_ID": "840da5d85403f35ea76519ed1a18a33989f855bf1cf8",
               "CEDARLING_POLICY_STORE_LOCAL_FN": "\(policyStoreUrl)",
               "CEDARLING_POLICY_STORE_URI": "",
               "CEDARLING_USER_AUTHZ": "enabled",
               "CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION": "AND",
               "CEDARLING_WORKLOAD_AUTHZ": "enabled",
               "id": "67d412fb-5dd9-4f85-9bd3-7b6471d90aa3"
           }

""";
        } else {
            self.json = ""
        }
    }
}
