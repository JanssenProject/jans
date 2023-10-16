//
//  OPConfiguration.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 04.10.2023.
//

import Foundation

struct OPConfiguration: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case sno
        case issuer
        case registrationEndpoint = "registration_endpoint"
        case tokenEndpoint = "token_endpoint"
        case userinfoEndpoint = "userinfo_endpoint"
        case authorizationChallengeEndpoint = "authorization_challenge_endpoint"
        case revocationEndpoint = "revocation_endpoint"
    }
    
    var sno: String
    var issuer: String
    var registrationEndpoint: String
    var tokenEndpoint: String
    var userinfoEndpoint: String
    var authorizationChallengeEndpoint: String
    var revocationEndpoint: String
    
    var isSuccessful: Bool = false
    
    var toString: String {
        "OPConfiguration{" +
        "sno='" + sno + "'\n" +
        "issuer='" + issuer + "'\n" +
        ", registrationEndpoint='" + registrationEndpoint + "'\n" +
        ", tokenEndpoint='" + tokenEndpoint + "'\n" +
        ", userinfoEndpoint='" + userinfoEndpoint + "'\n" +
        ", authorizationChallengeEndpoint='" + authorizationChallengeEndpoint + "'\n" +
        "}"
    }
    
    var opConfigurationObject: OPConfigurationObject {
        let opConfigurationObject = OPConfigurationObject()
        opConfigurationObject.sno = sno
        opConfigurationObject.issuer = issuer
        opConfigurationObject.registrationEndpoint = registrationEndpoint
        opConfigurationObject.tokenEndpoint = tokenEndpoint
        opConfigurationObject.userinfoEndpoint = userinfoEndpoint
        opConfigurationObject.authorizationChallengeEndpoint = authorizationChallengeEndpoint
        
        return opConfigurationObject
    }
}
