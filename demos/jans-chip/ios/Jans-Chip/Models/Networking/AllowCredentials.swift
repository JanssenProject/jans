//
//  AllowCredentials.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 22.07.2024.
//

import Foundation

public struct AllowCredentials: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case id
        case type
        case transports
    }
    
    var id: String
    var type: String
    var transports: [String]
}
