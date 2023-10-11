//
//  OIDCClient.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 09.10.2023.
//

import RealmSwift

final class OIDCClient: Object, ObjectKeyIdentifiable {
    
    @Persisted var sno: String
    @Persisted var clientName: String
    @Persisted var clientId: String
    @Persisted var clientSecret: String
    @Persisted var recentGeneratedIdToken: String
    @Persisted var recentGeneratedAccessToken: String
    @Persisted var scope: String
}
