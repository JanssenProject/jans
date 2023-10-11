//
//  OPConfiguration.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 04.10.2023.
//

import Foundation
import RealmSwift

struct OPConfiguration: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case issuer
        case registrationEndpoint = "registration_endpoint"
        case tokenEndpoint = "token_endpoint"
        case userinfoEndpoint = "userinfo_endpoint"
        case authorizationChallengeEndpoint = "authorization_challenge_endpoint"
        case revocationEndpoint = "revocation_endpoint"
    }
    
    var issuer: String
    var registrationEndpoint: String
    var tokenEndpoint: String
    var userinfoEndpoint: String
    var authorizationChallengeEndpoint: String
    var revocationEndpoint: String
    
    var toString: String {
        "OPConfiguration{" +
        "issuer='" + issuer + "'\n" +
        ", registrationEndpoint='" + registrationEndpoint + "'\n" +
        ", tokenEndpoint='" + tokenEndpoint + "'\n" +
        ", userinfoEndpoint='" + userinfoEndpoint + "'\n" +
        ", authorizationChallengeEndpoint='" + authorizationChallengeEndpoint + "'\n" +
        "}"
    }
}
