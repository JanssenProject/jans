//
//  AuthenticatorSelection.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 11.07.2024.
//

import Foundation

struct AuthenticatorSelection: Codable {
    var authenticatorAttachment: String
    var requireResidentKey: Bool
    var userVerification: String
    var residentKey: String
}
