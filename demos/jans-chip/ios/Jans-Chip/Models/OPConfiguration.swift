//
//  OPConfiguration.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 04.10.2023.
//

import Foundation
import RealmSwift

final class OPConfiguration: Object, Codable, ObjectKeyIdentifiable, ErrorHandler {
    
    var errorMessage: String?
    var isSuccess: Bool = true
    
    override static func primaryKey() -> String? {
        "sno"
    }
    
    private enum CodingKeys: String, CodingKey {
        case sno
        case issuer
        case registrationEndpoint = "registration_endpoint"
        case tokenEndpoint = "token_endpoint"
        case userinfoEndpoint = "userinfo_endpoint"
        case authorizationChallengeEndpoint = "authorization_challenge_endpoint"
        case revocationEndpoint = "revocation_endpoint"
    }
    
    @Persisted var sno: String?
    @Persisted var issuer: String
    @Persisted var registrationEndpoint: String
    @Persisted var tokenEndpoint: String
    @Persisted var userinfoEndpoint: String
    @Persisted var authorizationChallengeEndpoint: String
    @Persisted var revocationEndpoint: String
    
    var toString: String {
        "OPConfiguration{" +
        "sno='" + (sno ?? "") + "'\n" +
        "issuer='" + issuer + "'\n" +
        ", registrationEndpoint='" + registrationEndpoint + "'\n" +
        ", tokenEndpoint='" + tokenEndpoint + "'\n" +
        ", userinfoEndpoint='" + userinfoEndpoint + "'\n" +
        ", authorizationChallengeEndpoint='" + authorizationChallengeEndpoint + "'\n" +
        "}"
    }
}
