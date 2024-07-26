//
//  CredentialSafe.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.07.2024.
//

import Foundation

final class CredentialSafe {
    
    func generateCredential(rpEntityId: String, userHandle: Data, userDisplayName: String, passwordText: String) -> PublicKeyCredentialSource {
        let credentialSource = PublicKeyCredentialSource(rpId: rpEntityId, userHandle: userHandle, userDisplayName: userDisplayName)
        credentialSource.userPassword = passwordText
        RealmManager.shared.save(object: credentialSource)
//        generateNewES256KeyPair(credentialSource.keyPairAlias); // return not captured -- will retrieve credential by alias
        
        return credentialSource
    }
    
    func allCredentialSource() -> [PublicKeyCredentialSource]? {
        let allCredentialSource : [PublicKeyCredentialSource]? = RealmManager.shared.getObjects()
        return allCredentialSource
    }
    
    func getKeysForEntity(rpEntityId: String) -> [PublicKeyCredentialSource] {
        allCredentialSource()?.filter { $0.rpId == rpEntityId } ?? []
    }
    
    func keyRequiresVerification(alias: String) -> Bool {
        WebAuthnCryptography().isUserAuthenticationRequired(alias: alias)
    }
    
    func incrementCredentialUseCounter(credential: PublicKeyCredentialSource) -> Int {
        RealmManager.shared.incrementUseCounter(credential: credential)
    }
}
