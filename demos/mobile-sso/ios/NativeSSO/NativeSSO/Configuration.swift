//
//  Configuration.swift
//  NativeSSO
//
//  Created by Nazar Yavornytskyy on 11/21/22.
//

import Foundation

struct Configuration {

    static let domain = "YOUR_DOMAIN"
    static let baseURL = "https://\(host)\(path)"
    static let host = "\(domain).gluu.info"
    static let path = "/jans-auth/restv1"
}
