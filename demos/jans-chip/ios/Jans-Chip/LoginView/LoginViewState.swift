//
//  LoginViewState.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

final class LoginViewState: ObservableObject {
    
    private let authAdapter = AuthAdaptor()
    
    var loadingVisible = false
    
    var creds: [PublicKeyCredentialSource] {
        authAdapter.getAllCredentials() ?? []
    }
    
    var passkeyList: [PublicKeyCredentialRow] {
        var list: [PublicKeyCredentialRow] = []
        creds.forEach {
            list.append(PublicKeyCredentialRow(publicKeyCredential: $0))
        }
        
        return list
    }
    
    var titleText: String {
        passkeyList.isEmpty ? "No passkey enrolled" : "Choose your passkey"
    }
}
