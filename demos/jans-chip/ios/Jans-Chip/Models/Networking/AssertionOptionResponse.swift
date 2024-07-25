//
//  AssertionOptionResponse.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 22.07.2024.
//

import Foundation

public struct AssertionOptionResponse: Codable, ErrorHandler {
    var errorMessage: String?
    var isSuccess: Bool = true
    
    private enum CodingKeys: String, CodingKey {
        case challenge
        case user
        case userVerification
        case rpId
        case status
        case allowCredentials
    }
    
    var challenge: String?
    var user: String?
    var userVerification: String?
    var rpId: String?
    var status: String?
    var allowCredentials: [AllowCredentials]?
    
    init(errorMessage: String? = nil, isSuccess: Bool, challenge: String? = nil, user: String? = nil, userVerification: String? = nil, rpId: String? = nil, status: String? = nil, allowCredentials: [AllowCredentials]? = nil) {
        self.errorMessage = errorMessage
        self.isSuccess = isSuccess
        self.challenge = challenge
        self.user = user
        self.userVerification = userVerification
        self.rpId = rpId
        self.status = status
        self.allowCredentials = allowCredentials
    }
}
