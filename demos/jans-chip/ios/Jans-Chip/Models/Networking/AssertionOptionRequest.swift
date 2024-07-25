//
//  AssertionOptionRequest.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 22.07.2024.
//

import Foundation

public struct AssertionOptionRequest {
    var username: String
    var userVerification: String?
    var documentDomain: String?
    var extensions: String?
    var session_id: String?
    var description: String?
}
