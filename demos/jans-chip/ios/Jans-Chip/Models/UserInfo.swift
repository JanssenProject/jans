//
//  UserInfo.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 23.10.2023.
//

import Foundation

struct UserInfo: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case sub
    }
    
    var sub: String
}
