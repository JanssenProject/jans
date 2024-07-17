//
//  DCResponse.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 11.10.2023.
//

import Foundation

struct DCResponse: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case clientId = "client_id"
        case clientSecret = "client_secret"
        case clientName = "client_name"
        case authorizationChallengeEndpoint = "authorization_challenge_endpoint"
        case endSessionEndpoint = "end_session_endpoint"
    }
    
    var clientId: String?
    var clientSecret: String?
    var clientName: String?
    var authorizationChallengeEndpoint: String?
    var endSessionEndpoint: String?
}
