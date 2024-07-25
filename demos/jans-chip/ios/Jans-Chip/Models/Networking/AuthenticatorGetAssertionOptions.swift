//
//  AuthenticatorGetAssertionOptions.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 22.07.2024.
//

import Foundation

public struct AuthenticatorGetAssertionOptions: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case rpId
        case clientDataHash
        case allowCredentialDescriptorList
        case requireUserPresence
        case requireUserVerification
    }
    
    var rpId: String?
    var clientDataHash: Data?
    var allowCredentialDescriptorList: [PublicKeyCredentialDescriptor]?
    var requireUserPresence: Bool?
    var requireUserVerification: Bool?
}
