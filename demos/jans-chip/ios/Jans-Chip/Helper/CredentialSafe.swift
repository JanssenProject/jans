//
//  CredentialSafe.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.07.2024.
//

import Foundation

final class CredentialSafe {
    
    func generateCredential(rpEntityId: String, userHandle: Data, userDisplayName: String) -> PublicKeyCredentialSource {
        let credentialSource = PublicKeyCredentialSource(rpId: rpEntityId, userHandle: userHandle, userDisplayName: userDisplayName)
        RealmManager.shared.save(object: credentialSource)
//        generateNewES256KeyPair(credentialSource.keyPairAlias); // return not captured -- will retrieve credential by alias
        
        return credentialSource
    }
    
    func allCredentialSource() -> [PublicKeyCredentialSource]? {
        let allCredentialSource : [PublicKeyCredentialSource]? = RealmManager.shared.getObjects()
        return allCredentialSource
    }
}
