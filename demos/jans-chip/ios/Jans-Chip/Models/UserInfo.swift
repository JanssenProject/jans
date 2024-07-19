//
//  UserInfo.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 23.10.2023.
//

import Foundation

struct UserInfo: Codable, ErrorHandler {
    var errorMessage: String?
    var isSuccess: Bool = true
    
    var additionalInfo: [String: String] = [:]
}

struct UserAdditionalInfo: Identifiable {
    var id = UUID()
    var key: String
    var value: String
}
