//
//  FidoConfiguration.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 08.07.2024.
//

import Foundation
import RealmSwift

final class FidoConfiguration: Object, Codable, ObjectKeyIdentifiable {
    
    override static func primaryKey() -> String? {
        "sno"
    }
    
    private enum CodingKeys: String, CodingKey {
        case sno
        case issuer
        case attestation
    }
    
    @Persisted var sno: String?
    @Persisted var issuer: String?
    @Persisted var attestation: Attestation?
    
    @Persisted var isSuccessful: Bool?
    @Persisted var errorMessage: String?
}
