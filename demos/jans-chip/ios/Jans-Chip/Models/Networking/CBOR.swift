//
//  CBOR.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 16.07.2024.
//

import Foundation

public struct CBOR: Codable {
    
    var authData: Data?
    var fmt: String
    var attStmt: String
}
