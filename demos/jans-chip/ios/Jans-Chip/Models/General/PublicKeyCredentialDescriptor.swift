//
//  PublicKeyCredentialDescriptor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.07.2024.
//

import Foundation

struct PublicKeyCredentialDescriptor: Codable {
    var id: Data
    var type: String
    var transports: [String]
    
    init(id: Data, type: String, transports: [String]) {
        self.id = id
        self.type = type
        self.transports = transports
    }
    
    init() {
        self.id = Data()
        self.type = ""
        self.transports = []
    }
}
