//
//  AttestationOptionResponse.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 11.07.2024.
//

import Foundation

public struct AttestationOptionResponse: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case attestation
        case authenticatorSelection
        case challenge
        case pubKeyCredParams
        case rp
        case user
    }
    
    var attestation: String?
    var authenticatorSelection: AuthenticatorSelection?
    var challenge: String?
    var pubKeyCredParams: [PubKeyCredParam]?
    var rp: RP?
    var user: User?
    
    var isSuccessful: Bool? = true
    var errorMessage: String?
}
