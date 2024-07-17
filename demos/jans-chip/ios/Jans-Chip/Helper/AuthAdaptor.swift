//
//  AuthAdaptor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 16.07.2024.
//

import Foundation

final class AuthAdaptor {
    
    func getAllCredentials() -> [PublicKeyCredentialSource]? {
        let credentialSafe = CredentialSafe()
        return credentialSafe.allCredentialSource()
    }
    
    func isCredentialsPresent(username: String) -> Bool {
        let allCredentials = getAllCredentials()
        return allCredentials != nil && allCredentials?.filter({ $0.userDisplayName == username }).isEmpty == false
    }
}
