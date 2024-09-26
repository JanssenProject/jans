//
//  Token.swift
//  NativeSSO
//
//  Created by Nazar Yavornytskyy on 11/25/22.
//

import Foundation

struct Token: Codable {
    let access_token: String
    let device_token: String
    let id_token: String
    let token_type: String
    let expires_in: Int
}

