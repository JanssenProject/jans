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
        case name
        case nickname
        case givenName = "given_name"
        case middleName = "middle_name"
        case inum
        case familyName = "family_name"
        case jansAdminUIRole
    }
    
    var sub: String
    var name: String?
    var nickname: String?
    var givenName: String?
    var middleName: String?
    var inum: String?
    var familyName: String?
    var jansAdminUIRole: [String]?
}
