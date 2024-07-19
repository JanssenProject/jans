//
//  TokenResponse.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 04.10.2023.
//

import Foundation

struct TokenResponse: Codable, ErrorHandler {
    var errorMessage: String?
    var isSuccess: Bool = true
    
    private enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case idToken = "id_token"
        case tokenType = "token_type"
        case expiresIn = "expires_in"
    }
    
    var accessToken: String
    var idToken: String?
    var tokenType: String
    var expiresIn: Int?
}
