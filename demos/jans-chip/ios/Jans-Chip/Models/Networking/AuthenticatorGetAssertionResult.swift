//
//  AuthenticatorGetAssertionResult.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 25.07.2024.
//

import Foundation

public struct AuthenticatorGetAssertionResult: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case selectedCredentialId = "selected_credential_id"
        case authenticatorData = "authenticator_data"
        case signature
        case selectedCredentialUserHandle = "selected_credential_user_handle"
    }
    
    var selectedCredentialId: Data?
    var authenticatorData: Data?
    var signature: Data?
    var selectedCredentialUserHandle: Data?
}
