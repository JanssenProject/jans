//
//  CredentialSelector.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 22.07.2024.
//

import Foundation

protocol CredentialSelector {
    func selectFrom(credentialList: [PublicKeyCredentialSource]) -> PublicKeyCredentialSource?
}

final class LocalCredentialSelector: CredentialSelector {
    
    func selectFrom(credentialList: [PublicKeyCredentialSource]) -> PublicKeyCredentialSource? {
        credentialList.first
    }
}
