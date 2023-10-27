//
//  DCRequest.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.10.2023.
//

import Foundation

public struct DCRequest: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case issuer
        case redirectUris = "redirect_uris"
        case scope
        case responseTypes = "response_types"
        case postLogoutRedirectUris = "post_logout_redirect_uris"
        case grantTypes = "grant_types"
        case applicationType = "application_type"
        case clientName = "client_name"
        case tokenEndpointAuthMethod = "token_endpoint_auth_method"
        case evidence
        case jwks
    }
    
    var issuer: String
    var redirectUris: [String]
    var scope: [String]
    var responseTypes: [String]
    var postLogoutRedirectUris: [String]
    var grantTypes: [String]
    var applicationType: String
    var clientName: String
    var tokenEndpointAuthMethod: String
    var evidence: String = ""
    var jwks: String = ""
    
    var asParameters: [String: Any] {
        [
            "issuer": issuer,
            "redirect_uris": redirectUris,
            "scope": scope,
            "response_types": responseTypes,
            "post_logout_redirect_uris": postLogoutRedirectUris,
            "grant_types": grantTypes,
            "application_type": applicationType,
            "client_name": clientName,
            "token_endpoint_auth_method": tokenEndpointAuthMethod,
            "evidence": evidence,
            "jwks": jwks
        ]
//        "issuer=\(issuer)&redirect_uris=\(redirectUris)&scope=\(scope)&response_types=\(responseTypes)&post_logout_redirect_uris=\(postLogoutRedirectUris)&grant_types=\(grantTypes)&application_type=\(applicationType)&client_name=\(clientName)&token_endpoint_auth_method=\(tokenEndpointAuthMethod)&evidence=\(evidence)&jwks=\(jwks)"
    }
}

