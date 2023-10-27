//
//  AppIntegrityEntity.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.10.2023.
//

import RealmSwift

final class AppIntegrityEntity: Object, ObjectKeyIdentifiable {
    
    override static func primaryKey() -> String? {
        "id"
    }
    
    @Persisted var id: String = "1"
    @Persisted var sno: String
    @Persisted var appIntegrity: String
    @Persisted var deviceIntegrity: String
    @Persisted var appLicensingVerdict: String
    @Persisted var requestPackageName: String
    @Persisted var nonce: String
    @Persisted var error: String
}
