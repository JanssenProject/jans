//
//  TokenResponse.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 04.10.2023.
//

import Foundation

struct ChatPreveiwMessage: Codable {
    var access_token: String
    var id_token: String
    var token_type: String
}
