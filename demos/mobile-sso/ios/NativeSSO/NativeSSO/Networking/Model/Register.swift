//
//  Register.swift
//  NativeSSO
//
//  Created by Nazar Yavornytskyy on 11/21/22.
//

import Foundation

struct Register: Codable {
    let registration_client_uri: String
    let client_id: String
    let client_secret: String
}
