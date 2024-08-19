//
//  LoginViewState.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

final class LoginViewState: ObservableObject {
    
    private let authAdapter = AuthAdaptor()
    
    var creds: [PublicKeyCredentialSource] {
        authAdapter.getAllCredentials() ?? []
    }
    
    @Published var passkeyList = [PublicKeyCredentialRow]()
    
    var loadingVisible = false
    
    var titleText: String {
        passkeyList.isEmpty ? "No passkey enrolled" : "Choose your passkey"
    }
    
    func loadPasskeyList() {
        passkeyList = getPasskeyList()
    }
    
    // MARK: - Private
    
    private func getPasskeyList() -> [PublicKeyCredentialRow] {
        var list: [PublicKeyCredentialRow] = []
        creds.forEach {
            list.append(PublicKeyCredentialRow(publicKeyCredential: $0))
        }
        
        return list
    }
}
