//
//  AttestationOptionRequest.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 11.07.2024.
//

import Foundation

public struct AttestationOptionRequest: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case username
        case displayName
        case attestation
    }
    
    var username: String
    var displayName: String?
    var attestation: String
}
