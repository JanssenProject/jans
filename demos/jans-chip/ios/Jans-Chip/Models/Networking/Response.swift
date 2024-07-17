//
//  Response.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 16.07.2024.
//

import Foundation

public struct Response: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case attestationObject
        case clientDataJSON
    }
    
    var attestationObject: String
    var clientDataJSON: String
}
