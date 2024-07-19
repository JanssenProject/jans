//
//  PublicKeyCredentialSource.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.07.2024.
//

import RealmSwift
import Foundation
import Security

final class PublicKeyCredentialSource: Object, ObjectKeyIdentifiable {
    
    public static let type = "public-key"
    
    override static func primaryKey() -> String? {
        "index"
    }
    
    @Persisted var index: String = "1"
    
    @Persisted var id: Data = Data()
    @Persisted var keyPairAlias: String = ""
    @Persisted var rpId: String = ""
    @Persisted var userHandle: Data = Data()
    @Persisted var userDisplayName: String = ""
    @Persisted var userPassword: String = ""
    @Persisted var otherUI: String?
    @Persisted var keyUseCounter: Int = 1
    @Persisted var error: String?
    
    private static let KEYPAIR_PREFIX = "virgil-keypair-"
    
    convenience init(rpId: String, userHandle: Data, userDisplayName: String) {
        self.init()
        
        self.id = PublicKeyCredentialSource.secureRandomData(count: 32)
        self.userDisplayName = userDisplayName
        
        self.rpId = rpId
        self.keyPairAlias = PublicKeyCredentialSource.KEYPAIR_PREFIX + self.id.base64EncodedString()
        self.userHandle = userHandle
        self.keyUseCounter = 1
    }
    
    private static func secureRandomData(count: Int) -> Data {
        var bytes = [Int8](repeating: 0, count: count)

        // Fill bytes with secure random data
        let status = SecRandomCopyBytes(
            kSecRandomDefault,
            count,
            &bytes
        )
        
        // A status of errSecSuccess indicates success
        if status == errSecSuccess {
            // Convert bytes to Data
            let data = Data(bytes: bytes, count: count)
            return data
        } else {
            // Handle error
            return Data()
        }
    }
}
