//
//  DCRequest.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.10.2023.
//

import Foundation

public struct DCRequest: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case clientName = "client_name"
        case evidence
        case jwks
        case scope
        case responseTypes = "response_types"
        case grantTypes = "grant_types"
        case ssa = "software_statement"
        case applicationType = "application_type"
        case redirectUris = "redirect_uris"
    }
    
    var clientName: String
    var evidence: String?
    var jwks: String?
    var scope: String
    var responseTypes: [String]
    var grantTypes: [String]
    var ssa: String
    var applicationType: String
    var redirectUris: [String]
    
    var asParameters: [String: Any] {
        var parameters: [String: Any] = [
            "client_name": clientName,
            "scope": scope,
            "response_types": responseTypes,
            "grant_types": grantTypes,
            "software_statement": ssa,
            "application_type": applicationType,
            "redirect_uris": redirectUris
        ]
        
        if let evidence {
            parameters.updateValue(evidence, forKey: "evidence")
        }
        if let jwks {
            parameters.updateValue(jwks, forKey: "jwks")
        }
        
        return parameters
    }
//        "issuer=\(issuer)&redirect_uris=\(redirectUris)&scope=\(scope)&response_types=\(responseTypes)&post_logout_redirect_uris=\(postLogoutRedirectUris)&grant_types=\(grantTypes)&application_type=\(applicationType)&client_name=\(clientName)&token_endpoint_auth_method=\(tokenEndpointAuthMethod)&evidence=\(evidence)&jwks=\(jwks)"
//    }
}

