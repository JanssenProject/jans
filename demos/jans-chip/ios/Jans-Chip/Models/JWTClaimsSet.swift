//
//  JWTClaimsSet.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 08.07.2024.
//

import Foundation

struct JWTClaimsSet: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case ISSUER = "iss"
        case SUBJECT = "sub"
        case AUDIENCE = "aud"
        case EXPIRATION_TIME = "exp"
        case NOT_BEFORE = "nbf"
        case ISSUED_AT = "iat"
        case JWT_ID = "jti"
    }
    
    var ISSUER: String?
    var SUBJECT: String?
    var AUDIENCE: [String]?
    var EXPIRATION_TIME: Date?
    var NOT_BEFORE: Date?
    var ISSUED_AT: Date?
    var JWT_ID: String?
}
