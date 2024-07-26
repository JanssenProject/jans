//
//  Response.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 16.07.2024.
//

import Foundation

public struct Response: Codable, ErrorHandler {
    var errorMessage: String?
    var isSuccess: Bool = true
    
    private enum CodingKeys: String, CodingKey {
        case authenticatorData
        case clientDataJSON
        case signature
    }
    
    var authenticatorData: String?
    var clientDataJSON: String?
    var signature: String?
}

public struct AttestationDataResponse: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case attestationObject
        case clientDataJSON
    }
    
    var attestationObject: String
    var clientDataJSON: String
}
