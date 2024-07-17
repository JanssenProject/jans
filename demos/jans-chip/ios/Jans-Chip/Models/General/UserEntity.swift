//
//  UserEntity.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.07.2024.
//

import Foundation

struct UserEntity: Codable {
    var id: Data
    var displayName: String
    var name: String
    
    init(id: Data, displayName: String, name: String) {
        self.id = id
        self.displayName = displayName
        self.name = name
    }
    
    init() {
        self.id = Data()
        self.displayName = ""
        self.name = ""
    }
}
