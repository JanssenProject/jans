//
//  OPConfigurationObject.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 11.10.2023.
//

import RealmSwift

final class OPConfigurationObject: Object, ObjectKeyIdentifiable {
    
    @Persisted var sno: String
    @Persisted var issuer: String
    @Persisted var registrationEndpoint: String
    @Persisted var tokenEndpoint: String
    @Persisted var userinfoEndpoint: String
    @Persisted var authorizationChallengeEndpoint: String
    @Persisted var revocationEndpoint: String
    @Persisted var isSuccessful: Bool = false
}
