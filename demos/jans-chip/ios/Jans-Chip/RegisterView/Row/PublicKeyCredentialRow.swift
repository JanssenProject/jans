//
//  PublicKeyCredentialRow.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 17.07.2024.
//

import SwiftUI

struct PublicKeyCredentialRow: Identifiable {
    private let publicKeyCredential: PublicKeyCredentialSource
    
    var heading: String { publicKeyCredential.userDisplayName }
    var subheading: String { publicKeyCredential.rpId }
    var userDisplayName: String { publicKeyCredential.userDisplayName }
    var userPassword: String { publicKeyCredential.userPassword }
    var rpId: String { publicKeyCredential.rpId }
    
    var id = UUID()
    let icon = "passkey_icon"
    
    init(publicKeyCredential: PublicKeyCredentialSource) {
        self.publicKeyCredential = publicKeyCredential
    }
}
